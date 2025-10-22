package dbu.models;

import java.io.Serializable;

import dbu.enums.CompressType;
import lombok.Data;

@Data
public class BackupConfig implements Serializable {
	private static final long serialVersionUID = 1L;
	private ConnectionParams connectionParams;
	private String backupFilePath;
	private CompressType compressType;
	private String cronSchedule;
}
