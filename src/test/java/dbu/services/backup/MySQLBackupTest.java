package dbu.services.backup;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
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

public class MySQLBackupTest {

    @Autowired
    private MySQLBackup mySQLBackup;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        mySQLBackup = new MySQLBackup();

    }

    @Test
    void testBackupSuccessWithGzipCompression() throws Exception {

        BackupConfig mockConfig = mock(BackupConfig.class);
        ConnectionParams mockParams = mock(ConnectionParams.class);

        when(mockConfig.getConnectionParams()).thenReturn(mockParams);
        when(mockParams.getHost()).thenReturn("localhost");
        when(mockParams.getPort()).thenReturn(3306);
        when(mockParams.getUsername()).thenReturn("root");
        when(mockParams.getPassword()).thenReturn("password");
        when(mockParams.getDatabaseName()).thenReturn("testdb");
        when(mockConfig.getCompressType()).thenReturn(CompressType.GZIP);

        Path mockBackupPath = Path.of("mock/backup/testdb.sql");
        Path mockCompressedPath = Path.of("mock/backup/testdb.sql.gzip");

        try (
                MockedStatic<BackupPathUtils> backupPathUtilsMock = mockStatic(BackupPathUtils.class);
                MockedStatic<CompressUtils> compressUtilsMock = mockStatic(CompressUtils.class);

                @SuppressWarnings("unused")
                MockedConstruction<ProcessBuilder> mockedProcessBuilder = mockConstruction(ProcessBuilder.class,
                        (mockBuilder, context) -> {
                            Process mockProcess = mock(Process.class);
                            when(mockProcess.waitFor()).thenReturn(0);
                            when(mockBuilder.start()).thenReturn(mockProcess);
                        })) {

            backupPathUtilsMock.when(() -> BackupPathUtils.createBackupPath(any(), eq("backup_testdb"), eq(".sql")))
                    .thenReturn(mockBackupPath);
            backupPathUtilsMock.when(() -> BackupPathUtils.createBackupPath(any(), eq("backup_testdb"), eq(".gzip")))
                    .thenReturn(mockCompressedPath);

            compressUtilsMock.when(() -> CompressUtils.compressGzip(mockBackupPath, mockCompressedPath))
                    .thenReturn(mockCompressedPath);

            Path result = mySQLBackup.backup(mockConfig);

            assertEquals(mockCompressedPath, result);
        }
    }

    @Test
    void testBackupFailsWhenMysqldumpFails() throws Exception {

        BackupConfig mockConfig = mock(BackupConfig.class);
        ConnectionParams mockParams = mock(ConnectionParams.class);

        when(mockConfig.getConnectionParams()).thenReturn(mockParams);
        when(mockParams.getHost()).thenReturn("localhost");
        when(mockParams.getPort()).thenReturn(3306);
        when(mockParams.getUsername()).thenReturn("root");
        when(mockParams.getPassword()).thenReturn("password");
        when(mockParams.getDatabaseName()).thenReturn("testdb");
        when(mockConfig.getCompressType()).thenReturn(CompressType.GZIP);

        Path mockBackupPath = Path.of("mock/backup/testdb.sql");

        try (
                MockedStatic<BackupPathUtils> backupPathUtilsMock = mockStatic(BackupPathUtils.class);

                @SuppressWarnings("unused")
                MockedConstruction<ProcessBuilder> mockedProcessBuilder = mockConstruction(ProcessBuilder.class,
                        (mockBuilder, context) -> {
                            Process mockProcess = mock(Process.class);
                            when(mockProcess.waitFor()).thenReturn(1);
                            when(mockBuilder.start()).thenReturn(mockProcess);
                        })) {
            backupPathUtilsMock.when(() -> BackupPathUtils.createBackupPath(any(), eq("backup_testdb"), eq(".sql")))
                    .thenReturn(mockBackupPath);
            BackupExecutionException ex = assertThrows(BackupExecutionException.class, () -> {
                mySQLBackup.backup(mockConfig);
            });

            assertEquals("mysqldump failed with exit code: 1", ex.getMessage());
        }
    }
}
