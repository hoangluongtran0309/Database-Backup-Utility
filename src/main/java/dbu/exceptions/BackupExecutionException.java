package dbu.exceptions;

public class BackupExecutionException extends BaseException {
    public BackupExecutionException(String message) {
        super(message);
    }

    public BackupExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
