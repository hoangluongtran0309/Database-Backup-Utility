package dbu.services.scheduler;

import java.util.Map;

import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import dbu.enums.DatabaseType;
import dbu.exceptions.BackupExecutionException;
import dbu.models.BackupConfig;
import dbu.services.backup.BackupService;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class BackupJob implements Job {

    private static final Logger logger = LoggerFactory.getLogger(BackupJob.class);

    private final Map<String, BackupService> backupExecutors;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();

        BackupConfig backupConfig = (BackupConfig) dataMap.get("backupConfig");
        String dbName = backupConfig.getConnectionParams().getDatabaseName();
        DatabaseType dbType = backupConfig.getConnectionParams().getDatabaseType();

        logger.info("Executing backup job for database: {} ({})", dbName, dbType);

        try {
            BackupService backupService = resolverExecutor(dbType);
            logger.debug("Resolved backup service: {} for database type: {}", backupService.getClass().getSimpleName(),
                    dbType);

            backupService.backup(backupConfig);

            logger.info("Backup job completed successfully for database: {} ({})", dbName, dbType);
        } catch (BackupExecutionException e) {
            logger.error("Error occurred while executing backup job for database: {} ({}). Message: {}", dbName, dbType,
                    e.getMessage(), e);
            throw new JobExecutionException("Backup job failed for database: " + dbName, e);
        }
    }

    private BackupService resolverExecutor(DatabaseType databaseType) {
        String keyService = databaseType.name().toLowerCase() + "Backup";
        BackupService executor = backupExecutors.get(keyService);

        if (executor == null) {
            logger.error("No backup service found for database type: {}", databaseType);
            throw new IllegalArgumentException("No backup service found for database type: " + databaseType);
        }

        return executor;
    }
}