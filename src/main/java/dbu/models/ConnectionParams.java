package dbu.models;

import java.io.Serializable;

import dbu.enums.DatabaseType;
import lombok.Data;

@Data
public class ConnectionParams implements Serializable {
	private static final long serialVersionUID = 1L;
	private String host;
	private int port;
	private DatabaseType databaseType;
	private String databaseName;
	private String username;
	private String password;
}
