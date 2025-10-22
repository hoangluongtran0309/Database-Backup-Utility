package dbu.services.backup;

import java.nio.file.Path;

import dbu.exceptions.BackupExecutionException;
import dbu.models.BackupConfig;

public interface BackupService {
	Path backup(BackupConfig backupConfig) throws BackupExecutionException;
}
