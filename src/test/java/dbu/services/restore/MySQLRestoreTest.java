package dbu.services.restore;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import dbu.exceptions.RestoreExecutionException;
import dbu.models.ConnectionParams;
import dbu.models.RestoreConfig;
import dbu.utils.DecompressUtils;

public class MySQLRestoreTest {

    @Test
    void testRestoreSuccess() throws Exception {
        RestoreConfig mockRestoreConfig = mock(RestoreConfig.class);
        ConnectionParams mockParams = mock(ConnectionParams.class);

        when(mockRestoreConfig.getConnectionParams()).thenReturn(mockParams);
        when(mockRestoreConfig.getBackupFilePath()).thenReturn("mock/backup.sql.gzip");
        when(mockParams.getUsername()).thenReturn("user");
        when(mockParams.getPassword()).thenReturn("pass");
        when(mockParams.getHost()).thenReturn("localhost");
        when(mockParams.getPort()).thenReturn(3306);
        when(mockParams.getDatabaseName()).thenReturn("testdb");

        Path decompressedFile = Paths.get("mock/backup.sql");

        try (
                MockedStatic<DecompressUtils> decompressUtilsMock = mockStatic(DecompressUtils.class);

                @SuppressWarnings("unused")
                MockedConstruction<ProcessBuilder> mockedProcessBuilder = mockConstruction(ProcessBuilder.class,
                        (mockBuilder, context) -> {
                            Process mockProcess = mock(Process.class);
                            when(mockProcess.waitFor()).thenReturn(0);
                            when(mockBuilder.start()).thenReturn(mockProcess);
                        })) {
            decompressUtilsMock.when(() -> DecompressUtils.decompressIfNeeded(any())).thenReturn(decompressedFile);

            MySQLRestore mysqlRestore = new MySQLRestore();

            boolean result = mysqlRestore.restore(mockRestoreConfig);

            assertTrue(result);
        }
    }

    @Test
    void testRestoreFailsWhenProcessExitCodeNonZero() throws Exception {
        RestoreConfig mockRestoreConfig = mock(RestoreConfig.class);
        ConnectionParams mockParams = mock(ConnectionParams.class);

        when(mockRestoreConfig.getConnectionParams()).thenReturn(mockParams);
        when(mockRestoreConfig.getBackupFilePath()).thenReturn("mock/backup.sql");
        when(mockParams.getUsername()).thenReturn("user");
        when(mockParams.getPassword()).thenReturn("pass");
        when(mockParams.getHost()).thenReturn("localhost");
        when(mockParams.getPort()).thenReturn(3306);
        when(mockParams.getDatabaseName()).thenReturn("testdb");

        Path decompressedFile = Paths.get("mock/backup.sql");

        try (
                MockedStatic<DecompressUtils> decompressUtilsMock = mockStatic(DecompressUtils.class);

                @SuppressWarnings("unused")
                MockedConstruction<ProcessBuilder> mockedProcessBuilder = mockConstruction(ProcessBuilder.class,
                        (mockBuilder, context) -> {
                            Process mockProcess = mock(Process.class);
                            when(mockProcess.waitFor()).thenReturn(1); // exit code 1 -> failure
                            when(mockBuilder.start()).thenReturn(mockProcess);
                        })) {
            decompressUtilsMock.when(() -> DecompressUtils.decompressIfNeeded(any())).thenReturn(decompressedFile);

            MySQLRestore mysqlRestore = new MySQLRestore();

            RestoreExecutionException ex = assertThrows(RestoreExecutionException.class, () -> {
                mysqlRestore.restore(mockRestoreConfig);
            });

            assertEquals("Unexpected restore error: Restore failed with exit code: 1", ex.getMessage());
        }
    }

    @Test
    void testRestoreThrowsIOException() throws Exception {
        RestoreConfig mockRestoreConfig = mock(RestoreConfig.class);
        ConnectionParams mockParams = mock(ConnectionParams.class);

        when(mockRestoreConfig.getConnectionParams()).thenReturn(mockParams);
        when(mockRestoreConfig.getBackupFilePath()).thenReturn("mock/backup.sql");
        when(mockParams.getUsername()).thenReturn("user");
        when(mockParams.getPassword()).thenReturn("pass");
        when(mockParams.getHost()).thenReturn("localhost");
        when(mockParams.getPort()).thenReturn(3306);
        when(mockParams.getDatabaseName()).thenReturn("testdb");

        try (
                MockedStatic<DecompressUtils> decompressUtilsMock = mockStatic(DecompressUtils.class);

                @SuppressWarnings("unused")
                MockedConstruction<ProcessBuilder> mockedProcessBuilder = mockConstruction(ProcessBuilder.class)) {
            decompressUtilsMock.when(() -> DecompressUtils.decompressIfNeeded(any()))
                    .thenThrow(new IOException("Decompression failed"));

            MySQLRestore mysqlRestore = new MySQLRestore();

            RestoreExecutionException ex = assertThrows(RestoreExecutionException.class, () -> {
                mysqlRestore.restore(mockRestoreConfig);
            });

            assertTrue(ex.getMessage().contains("Restore IO error"));
            assertTrue(ex.getCause() instanceof IOException);
        }
    }
}
