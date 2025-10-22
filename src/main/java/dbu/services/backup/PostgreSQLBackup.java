package dbu.services.backup;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dbu.exceptions.BackupExecutionException;
import dbu.models.BackupConfig;
import dbu.utils.BackupPathUtils;
import dbu.utils.CompressUtils;
import lombok.RequiredArgsConstructor;

@Service("postgresqlBackup")
@RequiredArgsConstructor
public class PostgreSQLBackup implements BackupService {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLBackup.class);

    @Override
    public Path backup(BackupConfig backupConfig) throws BackupExecutionException {
        return performBackup(backupConfig, backupConfig.getConnectionParams().getDatabaseName());
    }

    private Path performBackup(BackupConfig backupConfig, String databaseName) throws BackupExecutionException {
        Path backupPath;
        try {
            logger.info("Starting PostgreSQL backup for database: {}", databaseName);

            backupPath = BackupPathUtils.createBackupPath(
                    backupConfig,
                    "backup_" + databaseName.toLowerCase(),
                    ".sql");

            List<String> command = new ArrayList<>();
            command.add("pg_dump");
            command.add("--username=" + backupConfig.getConnectionParams().getUsername());
            command.add("--host=" + backupConfig.getConnectionParams().getHost());
            command.add("--port=" + backupConfig.getConnectionParams().getPort());
            command.add("--dbname=" + databaseName);
            command.add("--file=" + backupPath.toString());

            logger.debug("Executing pg_dump with command: {}", String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().put("PGPASSWORD", backupConfig.getConnectionParams().getPassword());

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String error = "pg_dump failed with exit code: " + exitCode;
                logger.error(error);
                throw new BackupExecutionException(error);
            }

            logger.info("pg_dump completed successfully. Backup file created at: {}", backupPath);

            Path compressedPath = compressBackup(backupConfig, backupPath, "backup_" + databaseName.toLowerCase());

            logger.info("Backup compressed successfully at: {}", compressedPath);
            return compressedPath;

        } catch (IOException | InterruptedException e) {
            Thread.currentThread().interrupt();
            String error = "PostgreSQL backup failed for database: " + databaseName + " - " + e.getMessage();
            logger.error(error, e);
            throw new BackupExecutionException(error, e);
        }
    }

    private Path compressBackup(BackupConfig backupConfig, Path backupPath, String prefix) throws IOException {
        Path compressedPath;
        switch (backupConfig.getCompressType()) {
            case GZIP -> {
                compressedPath = BackupPathUtils.createBackupPath(backupConfig, prefix, ".gzip");
                logger.debug("Compressing backup using GZIP to: {}", compressedPath);
                CompressUtils.compressGzip(backupPath, compressedPath);
            }
            case ZIP -> {
                compressedPath = BackupPathUtils.createBackupPath(backupConfig, prefix, ".zip");
                logger.debug("Compressing backup using ZIP to: {}", compressedPath);
                CompressUtils.compressZip(backupPath, compressedPath);
            }
            case TARGZ -> {
                compressedPath = BackupPathUtils.createBackupPath(backupConfig, prefix, ".tar.gz");
                logger.debug("Compressing backup using TAR.GZ to: {}", compressedPath);
                CompressUtils.compressTarGz(backupPath, compressedPath);
            }
            default -> {
                logger.warn("No compression type specified. Returning raw backup path: {}", backupPath);
                compressedPath = backupPath;
            }
        }
        return compressedPath;
    }
}
