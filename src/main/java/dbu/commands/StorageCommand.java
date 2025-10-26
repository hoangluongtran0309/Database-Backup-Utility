package dbu.commands;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import dbu.enums.StorageType;
import dbu.exceptions.StorageExecutionException;
import dbu.models.StorageFileInfo;
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
            @ShellOption(value = { "-s", "--storage-type" }, help = "Storage type (AWS, AZURE, GCP)")
            StorageType storageType) {

        try {
            logger.info("Listing files in storage '{}'", storageType);

            List<StorageFileInfo> files = resolverExecutor(storageType).listFiles();

            if (files.isEmpty()) {
                System.out.println("No files found in storage.");
                return;
            }

            // Tiêu đề bảng
            System.out.printf("%-40s %-12s %-20s%n", "File Name", "Size", "Last Modified");
            System.out.println("──────────────────────────────────────────────────────────────────────────────");

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            // Nội dung bảng
            files.forEach(file -> System.out.printf(
                    "%-40s %-12s %-20s%n",
                    file.getName(),
                    readableSize(file.getSize()),
                    file.getLastModified() != null ? file.getLastModified().format(formatter) : "N/A"
            ));

            System.out.println("──────────────────────────────────────────────────────────────────────────────");
            System.out.printf("Total: %d file(s)%n", files.size());

        } catch (StorageExecutionException e) {
            logger.error("Failed to list files in storage '{}': {}", storageType, e.getMessage(), e);
            System.err.println("Failed to list files: " + e.getMessage());
        }
    }

    private String readableSize(long size) {
        if (size < 1024) return size + " B";
        int exp = (int) (Math.log(size) / Math.log(1024));
        char pre = "KMGTPE".charAt(exp - 1);
        return String.format("%.1f %sB", size / Math.pow(1024, exp), pre);
    }
    

    @ShellMethod(key = { "check" }, value = "Check if a file exists in cloud storage")
    public void check(
            @ShellOption(value = { "-s",
                    "--storage-type" }, help = "Storage type (AWS, AZURE, GCP)") StorageType storageType,
            @ShellOption(value = { "-k", "--key" }) String key) {

        try {
            logger.info("Checking existence of file with key '{}' in storage '{}'", key, storageType);
            boolean result = resolverExecutor(storageType).exists(key);
            System.out.println("File exists: " + result);
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
        }
    }
}
