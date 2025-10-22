package dbu.commands;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import dbu.enums.DatabaseType;
import dbu.exceptions.DatabaseConnectionException;
import dbu.models.ConnectionParams;
import dbu.services.connect.ConnectService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@ShellComponent
@RequiredArgsConstructor
public class ConnectCommand {

    private static final Logger logger = LoggerFactory.getLogger(ConnectCommand.class);

    private final Map<String, ConnectService> databaseConnectors;

    @ShellMethod(key = { "connect" }, value = "Connect to a database")
    public void connect(
            @ShellOption(value = { "-t",
                    "--database-type" }, help = "Database type (MYSQL, POSTGRESQL, MONGODB)") DatabaseType databaseType,
            @ShellOption(value = { "-H", "--host" }, defaultValue = "localhost") String host,
            @ShellOption(value = { "-p", "--port" }) @Min(1) @Max(65535) int port,
            @ShellOption(value = { "-d", "--database" }) String databaseName,
            @ShellOption(value = { "-u", "--user" }) String user,
            @ShellOption(value = { "-w", "--password" }) String password) {

        logger.info("Attempting to connect to database '{}' of type '{}'", databaseName, databaseType);

        ConnectionParams params = new ConnectionParams();
        params.setHost(host);
        params.setPort(port);
        params.setUsername(user);
        params.setPassword(password);
        params.setDatabaseName(databaseName);
        params.setDatabaseType(databaseType);

        String keyService = databaseType.name().toLowerCase() + "Connect";
        ConnectService connector = databaseConnectors.get(keyService);

        if (connector == null) {
            String error = "No connection service found for database type: " + databaseType;
            logger.error(error);
            System.err.println(error);
            return;
        }

        try {
            connector.connect(params);
            String successMsg = "Successfully connected to database: " + databaseName + " [" + databaseType + "]";
            logger.info(successMsg);
            System.out.println(successMsg);
        } catch (DatabaseConnectionException e) {
            String errMsg = "Connection failed! Reason: " + e.getMessage();
            logger.error(errMsg, e);
            System.err.println("Connection failed!");
            System.err.println("  Reason: " + e.getMessage());
        }
    }
}
