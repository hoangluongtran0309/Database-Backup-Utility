package dbu.services.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobStorageException;

import dbu.exceptions.StorageExecutionException;
import dbu.models.StorageFileInfo;
import lombok.RequiredArgsConstructor;

@Service("azureStorage")
@RequiredArgsConstructor
public class AzureStorage implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(AzureStorage.class);

    private final BlobContainerClient containerClient;

    @Override
    public String uploadFile(String key, Path filePath) {
        try {
            logger.info("Uploading file '{}' to Azure Blob with key '{}'", filePath, key);
            containerClient.getBlobClient(key)
                    .uploadFromFile(filePath.toString(), true);

            BlobClient blobClient = containerClient.getBlobClient(key);
            String url = blobClient.getBlobUrl();

            logger.info("Upload successful. Blob URL: {}", url);
            return url;
        } catch (BlobStorageException e) {
            logger.error("Failed to upload file '{}' to Azure Blob with key '{}': {}", filePath, key, e.getMessage(),
                    e);
            throw new StorageExecutionException("Azure upload failed: " + e.getMessage(), e);
        }
    }

    @Override
    public Path downloadFile(String key, Path destination) {
        try {
            Path resolvedPath = destination;
            if (Files.isDirectory(destination)) {
                resolvedPath = destination.resolve(Path.of(key).getFileName());
            }

            logger.info("Downloading blob with key '{}' to file '{}'", key, resolvedPath);

            containerClient.getBlobClient(key)
                    .downloadToFile(resolvedPath.toString(), true);

            logger.info("Download successful to '{}'", resolvedPath);
            return resolvedPath.toAbsolutePath();
        } catch (BlobStorageException e) {
            logger.error("Failed to download blob with key '{}' to '{}': {}", key, destination, e.getMessage(), e);
            throw new StorageExecutionException("Azure download failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteFile(String key) {
        try {
            logger.info("Deleting blob with key '{}'", key);
            boolean deleted = containerClient.getBlobClient(key).deleteIfExists();

            logger.info("Delete operation for key '{}' completed. Deleted: {}", key, deleted);
            return deleted;
        } catch (BlobStorageException e) {
            logger.error("Failed to delete blob with key '{}': {}", key, e.getMessage(), e);
            throw new StorageExecutionException("Azure delete failed: " + e.getMessage(), e);
        }
    }

    @Override
    public List<StorageFileInfo> listFiles() {
        try {
            return StreamSupport.stream(containerClient.listBlobs().spliterator(), false)
                    .map(obj -> new StorageFileInfo(
                            obj.getName(),
                            obj.getProperties() != null && obj.getProperties().getContentLength() != null
                                    ? obj.getProperties().getContentLength().intValue()
                                    : 0L,
                            obj.getProperties() != null && obj.getProperties().getLastModified() != null
                                    ? obj.getProperties().getLastModified()
                                            .toInstant()
                                            .atZone(java.time.ZoneId.systemDefault())
                                            .toLocalDateTime()
                                    : null))
                    .collect(Collectors.toList());
        } catch (BlobStorageException e) {
            logger.error("Failed to list blobs: {}", e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            return containerClient.getBlobClient(key).exists();
        } catch (BlobStorageException e) {
            logger.warn("Failed to check existence of blob with key '{}': {}", key, e.getMessage(), e);
            return false;
        }
    }
}
