package dbu.services.restore;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dbu.exceptions.RestoreExecutionException;
import dbu.models.RestoreConfig;
import dbu.utils.DecompressUtils;
import lombok.RequiredArgsConstructor;

@Service("postgresqlRestore")
@RequiredArgsConstructor
public class PostgreSQLRestore implements RestoreService {

    private static final Logger logger = LoggerFactory.getLogger(PostgreSQLRestore.class);

    @Override
    public boolean restore(RestoreConfig restoreConfig) throws RestoreExecutionException {
        return performRestore(restoreConfig);
    }

    private boolean performRestore(RestoreConfig restoreConfig) throws RestoreExecutionException {
        try {
            logger.info("Starting PostgreSQL restore for database: {}",
                    restoreConfig.getConnectionParams().getDatabaseName());

            Path backupFile = Paths.get(restoreConfig.getBackupFilePath());
            Path sqlFile = DecompressUtils.decompressIfNeeded(backupFile);

            logger.debug("SQL file to restore: {}", sqlFile.toAbsolutePath());

            List<String> command = new ArrayList<>();
            command.add("psql");
            command.add("--username=" + restoreConfig.getConnectionParams().getUsername());
            command.add("--host=" + restoreConfig.getConnectionParams().getHost());
            command.add("--port=" + restoreConfig.getConnectionParams().getPort());
            command.add("--dbname=" + restoreConfig.getConnectionParams().getDatabaseName());
            command.add("--file=" + sqlFile.toAbsolutePath().toString());

            logger.debug("Executing command: {}", String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.environment().put("PGPASSWORD", restoreConfig.getConnectionParams().getPassword());

            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String error = "psql restore failed with exit code: " + exitCode;
                logger.error(error);
                throw new RestoreExecutionException(error);
            }

            logger.info("PostgreSQL restore completed successfully.");
            return true;

        } catch (IOException e) {
            logger.error("IO exception during PostgreSQL restore", e);
            throw new RestoreExecutionException("Restore IO error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // phục hồi trạng thái thread
            logger.error("PostgreSQL restore interrupted", e);
            throw new RestoreExecutionException("Restore interrupted: " + e.getMessage(), e);
        } catch (RestoreExecutionException e) {
            logger.error("Unexpected exception during restore", e);
            throw new RestoreExecutionException("Unexpected restore error: " + e.getMessage(), e);
        }
    }
}
