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

@Service("mongodbRestore")
@RequiredArgsConstructor
public class MongoDBRestore implements RestoreService {

    private static final Logger logger = LoggerFactory.getLogger(MongoDBRestore.class);

    @Override
    public boolean restore(RestoreConfig restoreConfig) throws RestoreExecutionException {
        return performRestore(restoreConfig);
    }

    private boolean performRestore(RestoreConfig restoreConfig) throws RestoreExecutionException {
        Path extractedPath;

        try {
            logger.info("Starting MongoDB restore for database: {}",
                    restoreConfig.getConnectionParams().getDatabaseName());

            Path backupFile = Paths.get(restoreConfig.getBackupFilePath());
            extractedPath = DecompressUtils.decompressIfNeeded(backupFile);

            logger.debug("Decompressed path: {}", extractedPath.toAbsolutePath());

            List<String> command = new ArrayList<>();
            command.add("mongorestore");
            command.add("--host=" + restoreConfig.getConnectionParams().getHost());
            command.add("--port=" + restoreConfig.getConnectionParams().getPort());
            command.add("--username=" + restoreConfig.getConnectionParams().getUsername());
            command.add("--password=" + restoreConfig.getConnectionParams().getPassword());
            command.add("--db=" + restoreConfig.getConnectionParams().getDatabaseName());
            command.add(extractedPath.toString());

            logger.debug("Executing mongorestore command: {}", String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            Process process = pb.start();
            int exitCode = process.waitFor();

            if (exitCode != 0) {
                String error = "mongorestore failed with exit code: " + exitCode;
                logger.error(error);
                throw new RestoreExecutionException(error);
            }

            logger.info("MongoDB restore completed successfully for database: {}",
                    restoreConfig.getConnectionParams().getDatabaseName());
            return true;

        } catch (IOException e) {
            logger.error("IO exception during MongoDB restore", e);
            throw new RestoreExecutionException("Restore IO error: " + e.getMessage(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // phục hồi trạng thái thread
            logger.error("MongoDB restore interrupted", e);
            throw new RestoreExecutionException("Restore interrupted: " + e.getMessage(), e);
        } catch (RestoreExecutionException e) {
            logger.error("Unexpected error during MongoDB restore", e);
            throw new RestoreExecutionException("Unexpected restore error: " + e.getMessage(), e);
        }
    }
}
