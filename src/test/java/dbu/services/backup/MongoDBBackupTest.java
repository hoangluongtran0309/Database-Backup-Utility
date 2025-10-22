package dbu.services.backup;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;

import dbu.enums.CompressType;
import dbu.exceptions.BackupExecutionException;
import dbu.models.BackupConfig;
import dbu.models.ConnectionParams;
import dbu.utils.BackupPathUtils;
import dbu.utils.CompressUtils;

public class MongoDBBackupTest {

    @Autowired
    private MongoDBBackup mongoDBBackup;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        mongoDBBackup = new MongoDBBackup();

    }

    @Test
    void testBackupSuccessWithGzipCompression() throws Exception {
        BackupConfig mockConfig = mock(BackupConfig.class);
        ConnectionParams mockParams = mock(ConnectionParams.class);

        when(mockConfig.getConnectionParams()).thenReturn(mockParams);
        when(mockParams.getHost()).thenReturn("localhost");
        when(mockParams.getPort()).thenReturn(27017);
        when(mockParams.getUsername()).thenReturn("admin");
        when(mockParams.getPassword()).thenReturn("password");
        when(mockParams.getDatabaseName()).thenReturn("testdb");
        when(mockConfig.getCompressType()).thenReturn(CompressType.GZIP);

        Path mockBackupPath = Path.of("mock/backup/path");
        Path mockCompressedPath = Path.of("mock/backup/path.gzip");

        try (
                MockedStatic<BackupPathUtils> backupPathUtilsMock = mockStatic(BackupPathUtils.class);
                MockedStatic<CompressUtils> compressUtilsMock = mockStatic(CompressUtils.class);
                MockedStatic<Files> filesMock = mockStatic(Files.class);

                @SuppressWarnings("unused")
                MockedConstruction<ProcessBuilder> mockedProcessBuilder = mockConstruction(ProcessBuilder.class,
                        (mockBuilder, context) -> {
                            Process mockProcess = mock(Process.class);
                            when(mockProcess.waitFor()).thenReturn(0);
                            when(mockBuilder.start()).thenReturn(mockProcess);
                        })) {
           
            backupPathUtilsMock.when(() -> BackupPathUtils.createBackupPath(any(), eq("backup_testdb"), eq("")))
                    .thenReturn(mockBackupPath);
            backupPathUtilsMock.when(() -> BackupPathUtils.createBackupPath(any(), eq("backup_testdb"), eq(".gzip")))
                    .thenReturn(mockCompressedPath);

         
            compressUtilsMock.when(() -> CompressUtils.compressGzip(mockBackupPath, mockCompressedPath))
                    .thenReturn(mockCompressedPath);

            filesMock.when(() -> Files.createDirectory(mockBackupPath)).thenReturn(mockBackupPath);
           
            Path result = mongoDBBackup.backup(mockConfig);

            assertEquals(mockCompressedPath, result);
        }
    }

    @Test
    void testBackupFailsWhenMongodumpFails() {
        BackupConfig mockConfig = mock(BackupConfig.class);
        ConnectionParams mockParams = mock(ConnectionParams.class);

        when(mockConfig.getConnectionParams()).thenReturn(mockParams);
        when(mockParams.getHost()).thenReturn("localhost");
        when(mockParams.getPort()).thenReturn(27017);
        when(mockParams.getUsername()).thenReturn("admin");
        when(mockParams.getPassword()).thenReturn("password");
        when(mockParams.getDatabaseName()).thenReturn("testdb");
        when(mockConfig.getCompressType()).thenReturn(CompressType.GZIP);

        Path mockBackupPath = Path.of("mock/backup/path");

        try (
                MockedStatic<BackupPathUtils> pathUtilsMock = mockStatic(BackupPathUtils.class);
                MockedStatic<Files> filesMock = mockStatic(Files.class);

                @SuppressWarnings("unused")
                MockedConstruction<ProcessBuilder> mockedProcessBuilder = mockConstruction(ProcessBuilder.class,
                        (mockBuilder, context) -> {
                            Process mockProcess = mock(Process.class);
                            when(mockProcess.waitFor()).thenReturn(1);
                            when(mockBuilder.start()).thenReturn(mockProcess);
                        })) {
            pathUtilsMock.when(() -> BackupPathUtils.createBackupPath(any(), anyString(), eq("")))
                    .thenReturn(mockBackupPath);

            filesMock.when(() -> Files.createDirectory(mockBackupPath)).thenReturn(mockBackupPath);

            Throwable exception = assertThrows(BackupExecutionException.class, () -> mongoDBBackup.backup(mockConfig));

            assertEquals("mongodump failed with exit code: 1", exception.getMessage());

        } catch (Exception e) {
            fail("Unexpected exception: " + e.getMessage());
        }
    }

}
