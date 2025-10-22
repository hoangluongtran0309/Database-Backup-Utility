package dbu.utils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import dbu.models.BackupConfig;

public class BackupPathUtils {

	public static Path createBackupPath(BackupConfig backupConfig, String prefix, String suffix) throws IOException {
		Path outputPath = Paths.get(backupConfig.getBackupFilePath()).toAbsolutePath().normalize();

		if (Files.notExists(outputPath) || Files.isDirectory(outputPath)) {
			if (Files.notExists(outputPath)) {
				Files.createDirectories(outputPath);
			}

			String fileName = prefix + "_"
					+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + suffix;

			return outputPath.resolve(fileName);
		}

		Path parent = outputPath.getParent();
		if (parent != null && !Files.exists(parent)) {
			Files.createDirectories(parent);
		}

		return outputPath;
	}

}
