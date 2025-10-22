package dbu.services.connect;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import dbu.exceptions.DatabaseConnectionException;
import dbu.models.ConnectionParams;

@Component("mysqlConnect")
public class MySQLConnect implements ConnectService {

    private final static Logger logger = LoggerFactory.getLogger(MySQLConnect.class);

    @Override
    public void connect(ConnectionParams connectionParams) throws DatabaseConnectionException {
        String uri = String.format("jdbc:mysql://%s:%d/%s",
                connectionParams.getHost(),
                connectionParams.getPort(),
                connectionParams.getDatabaseName());

        try (Connection connection = DriverManager.getConnection(
                uri,
                connectionParams.getUsername(),
                connectionParams.getPassword())) {

            if (connection.isValid(30)) {
                logger.info("Successfully connected to MySQL at {}:{}/{}",
                        connectionParams.getHost(),
                        connectionParams.getPort(),
                        connectionParams.getDatabaseName());
            } else {
                String error = String.format(
                        "Invalid MySQL connection at %s:%d/%s",
                        connectionParams.getHost(),
                        connectionParams.getPort(),
                        connectionParams.getDatabaseName());
                logger.error(error);
                throw new DatabaseConnectionException(error);
            }

        } catch (SQLException e) {
            String error = String.format(
                    "Failed to connect to MySQL at %s:%d/%s - %s",
                    connectionParams.getHost(),
                    connectionParams.getPort(),
                    connectionParams.getDatabaseName(),
                    e.getMessage());
            logger.error(error, e);
            throw new DatabaseConnectionException(error, e);
        }
    }
}