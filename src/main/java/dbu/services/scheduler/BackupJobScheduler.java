package dbu.services.scheduler;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dbu.models.BackupConfig;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class BackupJobScheduler {

    private static final Logger logger = LoggerFactory.getLogger(BackupJobScheduler.class);

    private final Scheduler scheduler;

    public void scheduleBackupJob(BackupConfig backupConfig) throws SchedulerException {

        logger.info("Scheduling backup job for database: {}", backupConfig.getConnectionParams().getDatabaseName());


        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("backupConfig", backupConfig);

        
        JobDetail jobDetail = JobBuilder.newJob(BackupJob.class)
                .withIdentity("backupJob_" + backupConfig.getConnectionParams().getDatabaseName(), backupConfig.getConnectionParams().getDatabaseType().name())
                .setJobData(jobDataMap)
                .storeDurably()
                .build();

        logger.info("JobDetail created for database: {} with identity: {}", backupConfig.getConnectionParams().getDatabaseName(), jobDetail.getKey());

  
        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger_" + backupConfig.getConnectionParams().getDatabaseName(), backupConfig.getConnectionParams().getDatabaseType().name())
                .withSchedule(CronScheduleBuilder.cronSchedule(backupConfig.getCronSchedule()))
                .forJob(jobDetail)
                .build();

        logger.info("Trigger created for database: {} with cron schedule: {}", backupConfig.getConnectionParams().getDatabaseName(), backupConfig.getCronSchedule());

        try {
          
            scheduler.scheduleJob(jobDetail, trigger);
            logger.info("Backup job scheduled successfully for database: {}", backupConfig.getConnectionParams().getDatabaseName());
        } catch (SchedulerException e) {
            logger.error("Failed to schedule backup job for database: {}. Error: {}", backupConfig.getConnectionParams().getDatabaseName(), e.getMessage(), e);
            throw e; 
        }
    }
}
