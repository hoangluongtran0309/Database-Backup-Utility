package dbu.services.connect;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.MockedStatic;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.springframework.beans.factory.annotation.Autowired;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoIterable;

import dbu.exceptions.DatabaseConnectionException;
import dbu.models.ConnectionParams;

public class MongoDBConnectTest {

    @Autowired
    private ConnectionParams params;

    @Autowired
    private MongoDBConnect mongodbConnect;

    @BeforeEach
    @SuppressWarnings("unused")
    void setup() {
        params = new ConnectionParams();
        params.setHost("localhost");
        params.setPort(27017);
        params.setDatabaseName("testdb");
        params.setUsername("user");
        params.setPassword("pass");

        mongodbConnect = new MongoDBConnect();
    }

    @Test
    void shouldConnectSuccessfully() {

        MongoClient mockClient = mock(MongoClient.class);
        MongoIterable<String> mockDbNames = mock(MongoIterable.class);
        when(mockClient.listDatabaseNames()).thenReturn(mockDbNames);
        when(mockDbNames.first()).thenReturn("testdb");

        try (MockedStatic<MongoClients> mc = mockStatic(MongoClients.class)) {
            mc.when(() -> MongoClients.create(any(MongoClientSettings.class))).thenReturn(mockClient);

            assertDoesNotThrow(() -> mongodbConnect.connect(params));

            verify(mockClient).listDatabaseNames();
        }
    }

    @Test
    void shouldThrowExceptionWhenNoDatabaseFound() {

        MongoClient mockClient = mock(MongoClient.class);
        MongoIterable<String> mockDbNames = mock(MongoIterable.class);
        when(mockClient.listDatabaseNames()).thenReturn(mockDbNames);
        when(mockDbNames.first()).thenReturn(null);

        try (MockedStatic<MongoClients> mockedStatic = mockStatic(MongoClients.class)) {
            mockedStatic.when(() -> MongoClients.create(any(MongoClientSettings.class)))
                    .thenReturn(mockClient);

            DatabaseConnectionException ex = assertThrows(DatabaseConnectionException.class,
                    () -> mongodbConnect.connect(params));

            assertEquals("Connected to MongoDB, but no databases were found.", ex.getMessage());
        }
    }

    @Test
    void shouldThrowExceptionWhenMongoExceptionOccurs() {

        try (MockedStatic<MongoClients> mockedStatic = mockStatic(MongoClients.class)) {
            mockedStatic.when(() -> MongoClients.create(any(MongoClientSettings.class)))
                    .thenThrow(new MongoException("Connection refused"));

            DatabaseConnectionException ex = assertThrows(DatabaseConnectionException.class,
                    () -> mongodbConnect.connect(params));

            assertTrue(
                    ex.getMessage().contains("Failed to connect to MongoDB at localhost:27017 - Connection refused"));
        }
    }

}
