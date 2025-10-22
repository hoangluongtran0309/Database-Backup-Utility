package dbu.services.connect;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;

import dbu.exceptions.DatabaseConnectionException;
import dbu.models.ConnectionParams;

public class PostgreSQLConnectTest {

    @Autowired
    private ConnectionParams params;

    @Autowired
    private PostgreSQLConnect postgresqlConnect;

    @BeforeEach
    @SuppressWarnings("unused")
    void setup() {
        params = new ConnectionParams();
        params.setHost("localhost");
        params.setPort(5432);
        params.setDatabaseName("testdb");
        params.setUsername("user");
        params.setPassword("pass");

        postgresqlConnect = new PostgreSQLConnect();
    }

    @Test
    void shouldConnectSuccessfully() throws Exception {

        Connection mockConn = mock(Connection.class);
        when(mockConn.isValid(30)).thenReturn(true);

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConn);

            assertDoesNotThrow(() -> postgresqlConnect.connect(params));

            dm.verify(() -> DriverManager.getConnection(
                    "jdbc:postgresql://localhost:5432/testdb", "user", "pass"));
        }
    }

    @Test
    void shouldThrowExceptionWhenConnectionInvalid() throws Exception {

        Connection mockConn = mock(Connection.class);
        when(mockConn.isValid(30)).thenReturn(false);

        try (MockedStatic<DriverManager> dm = mockStatic(DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConn);

            Throwable exception = assertThrows(DatabaseConnectionException.class,
                    () -> postgresqlConnect.connect(params));
            assertEquals("Invalid PostgreSQL connection at localhost:5432/testdb", exception.getMessage());

        }
    }

    @Test
    void shouldThrowExceptionWhenSqlErrorOccurs() throws Exception {

        Connection mockConn = mock(Connection.class);
        when(!mockConn.isValid(30) || mockConn.isClosed()).thenThrow(new SQLException("Access denied"));

        try (MockedStatic<DriverManager> mocked = mockStatic(DriverManager.class)) {
            mocked.when(() -> DriverManager.getConnection(anyString(), anyString(), anyString()))
                    .thenReturn(mockConn);

            Throwable exception = assertThrows(DatabaseConnectionException.class,
                    () -> postgresqlConnect.connect(params));
            assertEquals("Failed to connect to PostgreSQL at localhost:5432/testdb - Access denied",
                    exception.getMessage());
        }
    }
}
