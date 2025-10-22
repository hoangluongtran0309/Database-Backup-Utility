package dbu.models;

import lombok.Data;

@Data
public class RestoreConfig {
	private ConnectionParams connectionParams;
	private String backupFilePath;
}
