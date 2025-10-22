package dbu.services.connect;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import dbu.exceptions.DatabaseConnectionException;
import dbu.models.ConnectionParams;

@Component("mongodbConnect")
public class MongoDBConnect implements ConnectService {

    private static final Logger logger = LoggerFactory.getLogger(MongoDBConnect.class);

    @Override
    public void connect(ConnectionParams connectionParams) throws DatabaseConnectionException {
        String uri = String.format("mongodb://%s:%s@%s:%d/%s",
                connectionParams.getUsername(),
                connectionParams.getPassword(),
                connectionParams.getHost(),
                connectionParams.getPort(),
                connectionParams.getDatabaseName());

        ConnectionString connString = new ConnectionString(uri);
        MongoClientSettings settings = MongoClientSettings.builder()
                .applyConnectionString(connString)
                .build();

        try (MongoClient mongoClient = MongoClients.create(settings)) {
            String firstDb = mongoClient.listDatabaseNames().first();

            if (firstDb != null) {
                logger.info("Successfully connected to MongoDB at {}:{} - First available DB: {}",
                        connectionParams.getHost(),
                        connectionParams.getPort(),
                        firstDb);
            } else {
                String error = "Connected to MongoDB, but no databases were found.";
                logger.error(error);
                throw new DatabaseConnectionException(error);
            }

        } catch (MongoException e) {
            String error = String.format("Failed to connect to MongoDB at %s:%d - %s",
                    connectionParams.getHost(),
                    connectionParams.getPort(),
                    e.getMessage());
            logger.error(error, e);
            throw new DatabaseConnectionException(error, e);
        }
    }
}
