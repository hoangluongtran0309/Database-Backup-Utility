package dbu.commands;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import dbu.enums.StorageType;
import dbu.exceptions.StorageExecutionException;
import dbu.services.storage.StorageService;
import lombok.RequiredArgsConstructor;

@ShellComponent
@RequiredArgsConstructor
public class StorageCommand {

    private static final Logger logger = LoggerFactory.getLogger(StorageCommand.class);

    private final Map<String, StorageService> storageExecutors;

    private StorageService resolverExecutor(StorageType storageType) {
        String keyService = storageType.name().toLowerCase() + "Storage";
        StorageService executor = storageExecutors.get(keyService);

        if (executor == null) {
            String error = "No storage service found for type: " + storageType;
            logger.error(error);
            throw new IllegalArgumentException(error);
        }
        return executor;
    }

    @ShellMethod(key = { "upload" }, value = "Upload file to cloud storage")
    public void upload(
            @ShellOption(value = { "-s",
                    "--storage-type" }, help = "Storage type (AWS, AZURE, GCP)") StorageType storageType,
            @ShellOption(value = { "-k", "--key" }) String key,
            @ShellOption(value = { "-f", "--file-path" }) String filePath) {

        try {
            logger.info("Uploading file '{}' to storage '{}' with key '{}'", filePath, storageType, key);

            String resultUrl = resolverExecutor(storageType).uploadFile(key, Paths.get(filePath).toAbsolutePath());

            logger.info("Upload successful, file accessible at: {}", resultUrl);
            System.out.println("Upload successful! URL: " + resultUrl);

        } catch (StorageExecutionException e) {
            logger.error("Upload failed for key '{}' on storage '{}': {}", key, storageType, e.getMessage(), e);
            System.err.println("Upload failed: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    @ShellMethod(key = { "download" }, value = "Download file from cloud storage")
    public void download(
            @ShellOption(value = { "-s",
                    "--storage-type" }, help = "Storage type (AWS, AZURE, GCP)") StorageType storageType,
            @ShellOption(value = { "-k", "--key" }) String key,
            @ShellOption(value = { "-d", "--destination" }) String destination) {

        try {
            logger.info("Downloading file with key '{}' from storage '{}' to '{}'", key, storageType, destination);

            Path resultPath = resolverExecutor(storageType).downloadFile(key, Paths.get(destination).toAbsolutePath());

            logger.info("Download successful, saved to: {}", resultPath);
            System.out.println("Download successful! Saved to: " + resultPath);

        } catch (StorageExecutionException e) {
            logger.error("Download failed for key '{}' on storage '{}': {}", key, storageType, e.getMessage(), e);
            System.err.println("Download failed: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    @ShellMethod(key = { "delete" }, value = "Delete file from cloud storage")
    public void delete(
            @ShellOption(value = { "-s",
                    "--storage-type" }, help = "Storage type (AWS, AZURE, GCP)") StorageType storageType,
            @ShellOption(value = { "-k", "--key" }) String key) {

        try {
            logger.info("Deleting file with key '{}' from storage '{}'", key, storageType);

            boolean result = resolverExecutor(storageType).deleteFile(key);

            if (result) {
                logger.info("Delete successful for key '{}'", key);
                System.out.println("File deleted successfully.");
            } else {
                logger.warn("Delete operation did not confirm deletion for key '{}'", key);
                System.out.println("File deletion not confirmed.");
            }

        } catch (StorageExecutionException e) {
            logger.error("Delete failed for key '{}' on storage '{}': {}", key, storageType, e.getMessage(), e);
            System.err.println("Delete failed: " + e.getMessage());
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }

    @ShellMethod(key = { "list" }, value = "List all files in cloud storage")
    public void listFiles(
            @ShellOption(value = { "-s",
                    "--storage-type" }, help = "Storage type (AWS, AZURE, GCP)") StorageType storageType) {

        try {
            logger.info("Listing files in storage '{}'", storageType);

            var files = resolverExecutor(storageType).listFiles();

            if (files.isEmpty()) {
                System.out.println("No files found in storage.");
            } else {
                System.out.println("Files in storage:");
                files.forEach(System.out::println);
            }

        } catch (StorageExecutionException e) {
            logger.error("Failed to list files in storage '{}': {}", storageType, e.getMessage(), e);
            System.err.println("Failed to list files: " + e.getMessage());
        }
    }

    @ShellMethod(key = { "check" }, value = "Check if a file exists in cloud storage")
    public boolean check(
            @ShellOption(value = { "-s",
                    "--storage-type" }, help = "Storage type (AWS, AZURE, GCP)") StorageType storageType,
            @ShellOption(value = { "-k", "--key" }) String key) {

        try {
            logger.info("Checking existence of file with key '{}' in storage '{}'", key, storageType);
            boolean result = resolverExecutor(storageType).exists(key);
            System.out.println("File exists: " + result);
            return result;
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            return false;
        }
    }
}
