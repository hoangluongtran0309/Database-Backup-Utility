package dbu.services.storage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageException;

import dbu.config.AppProperties;
import dbu.exceptions.StorageExecutionException;
import lombok.RequiredArgsConstructor;

@Service("gcpStorage")
@RequiredArgsConstructor
public class GCPStorage implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(GCPStorage.class);

    private final AppProperties props;
    private final Storage storage;

    @Override
    public String uploadFile(String key, Path filePath) {
        try {
            logger.info("Uploading file '{}' to GCP bucket '{}' with key '{}'", filePath,
                    props.getCloud().getGcp().getBucketName(), key);

            Bucket bucket = storage.get(props.getCloud().getGcp().getBucketName());
            Blob blob = bucket.create(key, Files.readAllBytes(filePath));
            BlobInfo blobInfo = blob.asBlobInfo();

            long expireTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
            URL url = storage.signUrl(blobInfo, expireTime, TimeUnit.MILLISECONDS,
                    Storage.SignUrlOption.withV4Signature(),
                    Storage.SignUrlOption.httpMethod(HttpMethod.GET));

            logger.info("Upload successful. Signed URL: {}", url);
            return url.toString();
        } catch (NoSuchFileException e) {
            logger.error("Local file '{}' not found: {}", filePath, e.getMessage(), e);
            throw new StorageExecutionException("File not found: " + filePath, e);
        } catch (StorageException e) {
            logger.error("GCP Storage error during upload: {}", e.getMessage(), e);
            throw new StorageExecutionException("GCP upload failed: " + e.getMessage(), e);
        } catch (IOException e) {
            logger.error("Unexpected error during GCP upload: {}", e.getMessage(), e);
            throw new StorageExecutionException("Unexpected error during GCP upload", e);
        }
    }

    @Override
    public Path downloadFile(String key, Path destination) {
        try {
            logger.info("Downloading blob with key '{}' from GCP bucket '{}' to '{}'", key,
                    props.getCloud().getGcp().getBucketName(), destination);

            Blob blob = storage.get(props.getCloud().getGcp().getBucketName(), key);
            if (blob == null || !blob.exists()) {
                throw new StorageExecutionException("Blob with key '" + key + "' does not exist");
            }

            blob.downloadTo(destination);
            logger.info("Download successful to '{}'", destination);
            return destination.toAbsolutePath();
        } catch (StorageException e) {
            logger.error("GCP Storage error during download: {}", e.getMessage(), e);
            throw new StorageExecutionException("GCP download failed: " + e.getMessage(), e);
        } catch (StorageExecutionException e) {
            logger.error("Unexpected error during GCP download: {}", e.getMessage(), e);
            throw new StorageExecutionException("Unexpected error during GCP download", e);
        }
    }

    @Override
    public boolean deleteFile(String key) {
        try {
            logger.info("Deleting blob with key '{}' from GCP bucket '{}'", key,
                    props.getCloud().getGcp().getBucketName());
            boolean result = storage.delete(props.getCloud().getGcp().getBucketName(), key);
            logger.info("Delete operation completed. Deleted: {}", result);
            return result;
        } catch (StorageException e) {
            logger.error("GCP Storage error during delete: {}", e.getMessage(), e);
            throw new StorageExecutionException("GCP delete failed: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            Blob blob = storage.get(props.getCloud().getGcp().getBucketName(), key);
            boolean exists = blob != null && blob.exists();
            logger.debug("Existence check for blob '{}': {}", key, exists);
            return exists;
        } catch (StorageException e) {
            logger.warn("GCP Storage error during exists check: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public List<String> listFiles() {
        try {
            return StreamSupport.stream(
                    storage.list(props.getCloud().getGcp().getBucketName())
                            .iterateAll()
                            .spliterator(),
                    false)
                    .map(Blob::getName)
                    .collect(Collectors.toList());
        } catch (StorageException e) {
            logger.error("GCP Storage error during list files: {}", e.getMessage(), e);
            return List.of();
        }
    }
}
