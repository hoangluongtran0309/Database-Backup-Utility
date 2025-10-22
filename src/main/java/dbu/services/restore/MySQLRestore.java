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

@Service("mysqlRestore")
@RequiredArgsConstructor
public class MySQLRestore implements RestoreService {

    private static final Logger logger = LoggerFactory.getLogger(MySQLRestore.class);

    @Override
    public boolean restore(RestoreConfig restoreConfig) throws RestoreExecutionException {
        return performRestore(restoreConfig);
    }

    private boolean performRestore(RestoreConfig restoreConfig) throws RestoreExecutionException {
        Path backupFile = Paths.get(restoreConfig.getBackupFilePath());
        try {
            logger.info("Starting MySQL restore for database: {}",
                    restoreConfig.getConnectionParams().getDatabaseName());

            Path sqlFile = DecompressUtils.decompressIfNeeded(backupFile);
            logger.debug("Decompressed SQL file path: {}", sqlFile.toAbsolutePath());

            List<String> command = new ArrayList<>();
            command.add("mysql");
            command.add("--user=" + restoreConfig.getConnectionParams().getUsername());
            command.add("--password=" + restoreConfig.getConnectionParams().getPassword());
            command.add("--host=" + restoreConfig.getConnectionParams().getHost());
            command.add("--port=" + restoreConfig.getConnectionParams().getPort());
            command.add(restoreConfig.getConnectionParams().getDatabaseName());

            command.add("-e");
            command.add("source " + sqlFile.toAbsolutePath().toString());

            logger.debug("Executing command: {}", String.join(" ", command));

            Process process = new ProcessBuilder(command).start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String errorMsg = "Restore failed with exit code: " + exitCode;
                logger.error(errorMsg);
                throw new RestoreExecutionException(errorMsg);
            }

            logger.info("MySQL restore completed successfully.");
            return true;

        } catch (IOException e) {
            logger.error("IO Exception during MySQL restore", e);
            throw new RestoreExecutionException("Restore IO error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.error("Restore interrupted", e);
            throw new RestoreExecutionException("Restore interrupted: " + e.getMessage(), e);
        } catch (RestoreExecutionException e) {
            logger.error("Unexpected exception during restore", e);
            throw new RestoreExecutionException("Unexpected restore error: " + e.getMessage(), e);
        }
    }
}
