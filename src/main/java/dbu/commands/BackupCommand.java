package dbu.commands;

import java.nio.file.Path;
import java.util.Map;

import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import dbu.enums.CompressType;
import dbu.enums.DatabaseType;
import dbu.exceptions.BackupExecutionException;
import dbu.models.BackupConfig;
import dbu.models.ConnectionParams;
import dbu.services.backup.BackupService;
import dbu.services.scheduler.BackupJobScheduler;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@ShellComponent
@RequiredArgsConstructor
public class BackupCommand {

    private static final Logger logger = LoggerFactory.getLogger(BackupCommand.class);

    private final Map<String, BackupService> backupExecutors;

    private final BackupJobScheduler backupJobScheduler;

    @ShellMethod(key = "backup", value = "Backup the database")
    public void backup(
            @ShellOption(value = { "-t", "--database-type" }) DatabaseType databaseType,
            @ShellOption(value = { "-H", "--host" }, defaultValue = "localhost") String host,
            @ShellOption(value = { "-p", "--port" }) @Min(1) @Max(65535) int port,
            @ShellOption(value = { "-d", "--database" }) String databaseName,
            @ShellOption(value = { "-u", "--user" }) String user,
            @ShellOption(value = { "-w", "--password" }) String password,
            @ShellOption(value = { "-c", "--compress" }, defaultValue = "NONE") CompressType compressType,
            @ShellOption(value = { "-o", "--output" }) String backupFilePath,
            @ShellOption(value = { "-C",
                    "--cron" }, defaultValue="") String cronSchedule) {

        logger.info("Received backup command for database '{}' of type '{}'", databaseName, databaseType);

        ConnectionParams params = new ConnectionParams();
        params.setHost(host);
        params.setPort(port);
        params.setUsername(user);
        params.setPassword(password);
        params.setDatabaseName(databaseName);
        params.setDatabaseType(databaseType);

        BackupConfig config = new BackupConfig();
        config.setConnectionParams(params);
        config.setCompressType(compressType);
        config.setBackupFilePath(backupFilePath);

        String keyService = databaseType.name().toLowerCase() + "Backup";
        BackupService executor = backupExecutors.get(keyService);

        if (executor == null) {
            String error = "No backup service found for database type: " + databaseType;
            logger.error(error);
            System.err.println(error);
            return;
        }

        try {
            logger.info("Starting backup execution for database '{}'", databaseName);
            Path resultPath = executor.backup(config);

            if (resultPath != null) {
                String successMsg = "Database backup completed successfully: " + resultPath.toString();
                logger.info(successMsg);
                System.out.println(successMsg);
            } else {
                String warnMsg = "Backup service executed but returned null path.";
                logger.warn(warnMsg);
                System.err.println(warnMsg);
            }

            if (!cronSchedule.isBlank() || !cronSchedule.equals("")) {
                config.setCronSchedule(cronSchedule);
                try {
                    logger.info("Scheduling backup job with cron expression: {}", cronSchedule);
                    backupJobScheduler.scheduleBackupJob(config);
                    String schedMsg = "Backup schedule created successfully. The database will be backed up according to the cron schedule: "
                            + cronSchedule;
                    logger.info(schedMsg);
                    System.out.println(schedMsg);
                } catch (SchedulerException e) {
                    String errMsg = "Failed to schedule backup job: " + e.getMessage();
                    logger.error(errMsg, e);
                    System.err.println(errMsg);
                }
            }

        } catch (BackupExecutionException e) {
            String errMsg = "Backup process failed: " + e.getMessage();
            logger.error(errMsg, e);
            System.err.println(errMsg);
        } catch (Exception e) {
            String errMsg = "An unexpected error occurred: " + e.getMessage();
            logger.error(errMsg, e);
            System.err.println(errMsg);
        }
    }
}
