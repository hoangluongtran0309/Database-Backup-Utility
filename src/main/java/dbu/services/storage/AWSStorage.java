package dbu.services.storage;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dbu.config.AppProperties;
import dbu.exceptions.StorageExecutionException;
import dbu.models.StorageFileInfo;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.exception.SdkClientException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service("awsStorage")
@RequiredArgsConstructor
public class AWSStorage implements StorageService {

    private static final Logger logger = LoggerFactory.getLogger(AWSStorage.class);

    private final AppProperties props;
    private final S3Client s3Client;

    @Override
    public String uploadFile(String key, Path filePath) {
        try {
            logger.info("Uploading file '{}' to bucket '{}' with key '{}'", filePath,
                    props.getCloud().getAws().getBucketName(), key);
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(props.getCloud().getAws().getBucketName())
                    .key(key)
                    .build();
            s3Client.putObject(putObjectRequest, RequestBody.fromFile(filePath));

            String url = s3Client.utilities()
                    .getUrl(builder -> builder.bucket(props.getCloud().getAws().getBucketName()).key(key))
                    .toString();

            logger.info("Upload successful, file URL: {}", url);
            return url;
        } catch (S3Exception e) {
            logger.error("AWS S3 upload error for key '{}': {}", key, e.awsErrorDetails().errorMessage(), e);
            throw new StorageExecutionException(
                    "Failed to upload file to AWS S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public Path downloadFile(String key, Path destination) {
        try {
            Path resolvedPath = destination;

            if (Files.isDirectory(destination)) {
                resolvedPath = destination.resolve(Path.of(key).getFileName());
            }

            logger.info("Downloading file with key '{}' from bucket '{}' to '{}'",
                    key, props.getCloud().getAws().getBucketName(), resolvedPath);

            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(props.getCloud().getAws().getBucketName())
                    .key(key)
                    .build();

            s3Client.getObject(getObjectRequest, resolvedPath);
            logger.info("Download successful to '{}'", resolvedPath);

            return resolvedPath.toAbsolutePath();
        } catch (S3Exception e) {
            logger.error("AWS S3 download error for key '{}': {}", key, e.awsErrorDetails().errorMessage(), e);
            throw new StorageExecutionException(
                    "Failed to download file from AWS S3: " + e.awsErrorDetails().errorMessage(), e);
        } catch (AwsServiceException | SdkClientException e) {
            logger.error("Unexpected error during download: {}", e.getMessage(), e);
            throw new StorageExecutionException("Unexpected download error: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteFile(String key) {
        try {
            logger.info("Deleting file with key '{}' from bucket '{}'", key, props.getCloud().getAws().getBucketName());
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                    .bucket(props.getCloud().getAws().getBucketName())
                    .key(key)
                    .build();
            DeleteObjectResponse response = s3Client.deleteObject(deleteObjectRequest);
            logger.info("Delete operation completed for key '{}', delete marker: {}", key, response.deleteMarker());
            return true;
        } catch (S3Exception e) {
            logger.error("AWS S3 delete error for key '{}': {}", key, e.awsErrorDetails().errorMessage(), e);
            throw new StorageExecutionException(
                    "Failed to delete file from AWS S3: " + e.awsErrorDetails().errorMessage(), e);
        }
    }

    @Override
    public boolean exists(String key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                    .bucket(props.getCloud().getAws().getBucketName())
                    .key(key)
                    .build();
            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            logger.info("File with key '{}' does not exist.", key);
            return false;
        } catch (S3Exception e) {
            logger.warn("S3Exception checking existence of key '{}': {}", key, e.awsErrorDetails().errorMessage());
            return false;
        }
    }

    @Override
    public List<StorageFileInfo> listFiles() {
        try {
            return s3Client.listObjectsV2(ListObjectsV2Request.builder()
                    .bucket(props.getCloud().getAws().getBucketName())
                    .build())
                    .contents()
                    .stream()
                    .map(obj -> new StorageFileInfo(obj.key(), obj.size(), obj.lastModified() != null
                            ? obj.lastModified().atZone(ZoneId.systemDefault()).toLocalDateTime()
                            : null))
                    .collect(Collectors.toList());
        } catch (S3Exception e) {
            logger.error("AWS S3 error listing files: {}", e.awsErrorDetails().errorMessage(), e);
            return List.of();
        }
    }
}
