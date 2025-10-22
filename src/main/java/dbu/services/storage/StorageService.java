package dbu.services.storage;

import java.nio.file.Path;
import java.util.List;

import dbu.exceptions.StorageExecutionException;

public interface StorageService {

    String uploadFile(String key, Path filePath) throws StorageExecutionException;

    Path downloadFile(String key, Path destination) throws StorageExecutionException;

    boolean deleteFile(String key) throws StorageExecutionException;

    boolean exists(String key);

    List<String> listFiles();
}
