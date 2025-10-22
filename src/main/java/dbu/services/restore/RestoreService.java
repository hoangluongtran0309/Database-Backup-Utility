package dbu.services.restore;

import dbu.exceptions.RestoreExecutionException;
import dbu.models.RestoreConfig;

public interface RestoreService {
	boolean restore(RestoreConfig restoreConfig) throws RestoreExecutionException;
}
