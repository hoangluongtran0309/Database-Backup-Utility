package dbu.exceptions;

public class RestoreExecutionException extends BaseException {
    public RestoreExecutionException(String message) {
        super(message);
    }

    public RestoreExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
