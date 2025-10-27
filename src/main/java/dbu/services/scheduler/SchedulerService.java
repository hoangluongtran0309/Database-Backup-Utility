package dbu.services.scheduler;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import dbu.models.BackupConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Getter
public class SchedulerService {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerService.class);
    private final Scheduler scheduler;

    public void scheduleJob(BackupConfig backupConfig) throws SchedulerException {
        String dbName = backupConfig.getConnectionParams().getDatabaseName();
        logger.info("Scheduling backup job for database: {}", dbName);

        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("backupConfig", backupConfig);

        JobDetail jobDetail = JobBuilder.newJob(SchedulerJob.class)
                .withIdentity("backupJob_" + dbName,
                        backupConfig.getConnectionParams().getDatabaseType().name())
                .setJobData(jobDataMap)
                .storeDurably()
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger_" + dbName,
                        backupConfig.getConnectionParams().getDatabaseType().name())
                .withSchedule(CronScheduleBuilder.cronSchedule(backupConfig.getCronSchedule()))
                .forJob(jobDetail)
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
        logger.info("Backup job [{}] scheduled successfully (cron: {})", dbName, backupConfig.getCronSchedule());
    }

    public List<JobInfo> listAllJobs() {
        List<JobInfo> jobInfos = new ArrayList<>();
        try {
            for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.anyJobGroup())) {
                List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
                if (triggers.isEmpty())
                    continue;

                Trigger trigger = triggers.get(0);
                JobInfo info = new JobInfo(
                        jobKey.getName(),
                        jobKey.getGroup(),
                        scheduler.getTriggerState(trigger.getKey()).name(),
                        trigger.getNextFireTime(),
                        trigger.getPreviousFireTime());
                jobInfos.add(info);

                if (logger.isDebugEnabled()) {
                    logger.debug("Job: {} | Group: {} | Next: {} | Prev: {}",
                            info.getJobName(), info.getGroup(),
                            info.getNextFireTime(), info.getPreviousFireTime());
                }
            }
        } catch (SchedulerException e) {
            logger.error("Error while listing jobs from scheduler: {}", e.getMessage(), e);
        }
        return jobInfos;
    }

    public boolean pauseJob(JobKey jobKey) {
        try {
            if (!scheduler.checkExists(jobKey))
                return false;
            scheduler.pauseJob(jobKey);
            logger.info("Paused job: {}", jobKey);
            return true;
        } catch (SchedulerException e) {
            logger.error("Error pausing job {}: {}", jobKey, e.getMessage());
            return false;
        }
    }

    public boolean resumeJob(JobKey jobKey) {
        try {
            if (!scheduler.checkExists(jobKey))
                return false;
            scheduler.resumeJob(jobKey);
            logger.info("Resumed job: {}", jobKey);
            return true;
        } catch (SchedulerException e) {
            logger.error("Error resuming job {}: {}", jobKey, e.getMessage());
            return false;
        }
    }

    public boolean deleteJob(JobKey jobKey) {
        try {
            if (!scheduler.checkExists(jobKey))
                return false;
            scheduler.deleteJob(jobKey);
            logger.info("Deleted job: {}", jobKey);
            return true;
        } catch (SchedulerException e) {
            logger.error("Error deleting job {}: {}", jobKey, e.getMessage());
            return false;
        }
    }

    public boolean pauseAllJobs() {
        try {
            scheduler.pauseAll();
            logger.info("Paused all backup jobs.");
            return true;
        } catch (SchedulerException e) {
            logger.error("Error pausing all jobs: {}", e.getMessage());
            return false;
        }
    }

    public boolean resumeAllJobs() {
        try {
            scheduler.resumeAll();
            logger.info("Resumed all backup jobs.");
            return true;
        } catch (SchedulerException e) {
            logger.error("Error resuming all jobs: {}", e.getMessage());
            return false;
        }
    }

    @Data
    @AllArgsConstructor
    public static class JobInfo {
        private String jobName;
        private String group;
        private String state;
        private Date nextFireTime;
        private Date previousFireTime;
    }
}
