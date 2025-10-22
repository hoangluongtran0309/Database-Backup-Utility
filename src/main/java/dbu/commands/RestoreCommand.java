package dbu.commands;

import java.util.Locale;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import dbu.enums.DatabaseType;
import dbu.exceptions.RestoreExecutionException;
import dbu.models.ConnectionParams;
import dbu.models.RestoreConfig;
import dbu.services.restore.RestoreService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@ShellComponent
@RequiredArgsConstructor
public class RestoreCommand {

    private static final Logger logger = LoggerFactory.getLogger(RestoreCommand.class);

    private final Map<String, RestoreService> restoreExecutors;

    @ShellMethod(key = "restore", value = "Restore a database from backup")
    public void restore(
            @ShellOption(value = { "-t",
                    "--database-type" }, help = "Database type (MYSQL, POSTGRESQL, MONGODB)") DatabaseType databaseType,
            @ShellOption(value = { "-H", "--host" }, defaultValue = "localhost") String host,
            @ShellOption(value = { "-p", "--port" }) @Min(1) @Max(65535) int port,
            @ShellOption(value = { "-d", "--database" }) String databaseName,
            @ShellOption(value = { "-u", "--user" }) String user,
            @ShellOption(value = { "-w", "--password" }) String password,
            @ShellOption(value = { "-i", "--input-path" }) String backupFilePath) {

        ConnectionParams params = new ConnectionParams();
        params.setHost(host);
        params.setPort(port);
        params.setUsername(user);
        params.setPassword(password);
        params.setDatabaseName(databaseName);
        params.setDatabaseType(databaseType);

        RestoreConfig config = new RestoreConfig();
        config.setConnectionParams(params);
        config.setBackupFilePath(backupFilePath);

        String keyService = databaseType.name().toLowerCase(Locale.ROOT) + "Restore";
        RestoreService executor = restoreExecutors.get(keyService);

        if (executor == null) {
            System.err.println("No restore service found for database type: " + databaseType);
            logger.error("Restore service not found: {}", keyService);
            return;
        }

        try {
            boolean result = executor.restore(config);
            if (result) {
                System.out.println("Database restore successful.");
                logger.info("Database restore successful for type {}", databaseType);
            } else {
                System.err.println("Restore service executed but returned false.");
                logger.warn("Restore service for {} returned false", databaseType);
            }
        } catch (RestoreExecutionException e) {
            System.err.println("Restore failed: " + e.getMessage());
            logger.error("Restore failed for {}: {}", databaseType, e.getMessage(), e);
        }
    }
}
