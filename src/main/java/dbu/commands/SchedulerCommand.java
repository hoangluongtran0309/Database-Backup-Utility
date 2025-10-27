package dbu.commands;

import java.util.List;

import org.quartz.JobKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellOption;

import dbu.services.scheduler.SchedulerService;
import lombok.RequiredArgsConstructor;

@ShellComponent
@RequiredArgsConstructor
public class SchedulerCommand {

    private static final Logger logger = LoggerFactory.getLogger(SchedulerCommand.class);
    private final SchedulerService backupJobScheduler;

    @ShellMethod(key = "list-schedulers", value = "Show all backup jobs with their status")
    public void listAllSchedulers() {
        logger.info("Executing command: list-schedulers");
        List<SchedulerService.JobInfo> jobs = backupJobScheduler.listAllJobs();

        if (jobs.isEmpty()) {
            System.out.println("No backup jobs found.");
            return;
        }

        System.out.println("LIST OF BACKUP JOBS:");
        System.out.println("=".repeat(80));
        System.out.printf("%-30s %-12s %-12s %-24s %-24s%n",
                "JOB NAME", "GROUP", "STATE", "NEXT FIRE", "PREVIOUS FIRE");
        System.out.println("-".repeat(80));

        for (var job : jobs) {
            System.out.printf("%-30s %-12s %-12s %-24s %-24s%n",
                    job.getJobName(),
                    job.getGroup(),
                    job.getState(),
                    job.getNextFireTime() != null ? job.getNextFireTime() : "N/A",
                    job.getPreviousFireTime() != null ? job.getPreviousFireTime() : "N/A");
        }

        System.out.println("=".repeat(80));
    }

    @ShellMethod(key = "pause-scheduler", value = "Pause a specific backup job")
    public void pauseScheduler(
            @ShellOption(value = { "-j", "--job-name" }) String jobName,
            @ShellOption(value = { "-g", "--job-group" }) String group) {

        logger.info("Executing command: pause-scheduler for job {} ({})", jobName, group);
        JobKey jobKey = new JobKey(jobName, group);

        if (backupJobScheduler.pauseJob(jobKey)) {
            System.out.printf("Paused job: %s (%s)%n", jobName, group);
        } else {
            System.err.printf("Failed to pause job: %s (%s)%n", jobName, group);
        }
    }

    @ShellMethod(key = "resume-scheduler", value = "Resume a specific backup job")
    public void resumeScheduler(
            @ShellOption(value = { "-j", "--job-name" }) String jobName,
            @ShellOption(value = { "-g", "--job-group" }) String group) {

        logger.info("Executing command: resume-scheduler for job {} ({})", jobName, group);
        JobKey jobKey = new JobKey(jobName, group);

        if (backupJobScheduler.resumeJob(jobKey)) {
            System.out.printf("Resumed job: %s (%s)%n", jobName, group);
        } else {
            System.err.printf("Failed to resume job: %s (%s)%n", jobName, group);
        }
    }

    @ShellMethod(key = "delete-scheduler", value = "Delete a specific backup job")
    public void deleteScheduler(
            @ShellOption(value = { "-j", "--job-name" }) String jobName,
            @ShellOption(value = { "-g", "--job-group" }) String group) {

        logger.info("Executing command: delete-scheduler for job {} ({})", jobName, group);
        JobKey jobKey = new JobKey(jobName, group);

        if (backupJobScheduler.deleteJob(jobKey)) {
            System.out.printf("Deleted job: %s (%s)%n", jobName, group);
        } else {
            System.err.printf("Failed to delete job: %s (%s)%n", jobName, group);
        }
    }

    @ShellMethod(key = "pause-all", value = "Pause all backup jobs")
    public void pauseAll() {
        logger.info("Executing command: pause-all");
        if (backupJobScheduler.pauseAllJobs()) {
            System.out.println("Paused all backup jobs.");
        } else {
            System.err.println("Failed to pause all backup jobs.");
        }
    }

    @ShellMethod(key = "resume-all", value = "Resume all backup jobs")
    public void resumeAll() {
        logger.info("Executing command: resume-all");
        if (backupJobScheduler.resumeAllJobs()) {
            System.out.println("Resumed all backup jobs.");
        } else {
            System.err.println("Failed to resume all backup jobs.");
        }
    }
}
