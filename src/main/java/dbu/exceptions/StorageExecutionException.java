package dbu.exceptions;

public class StorageExecutionException extends BaseException{

    public StorageExecutionException(String message){
        super(message);
    }

    public StorageExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
