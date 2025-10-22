package dbu.services.connect;

import dbu.exceptions.DatabaseConnectionException;
import dbu.models.ConnectionParams;

public interface ConnectService {
	void connect(ConnectionParams connectionParams) throws DatabaseConnectionException;
}
