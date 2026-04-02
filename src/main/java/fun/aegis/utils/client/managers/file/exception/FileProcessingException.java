package fun.aegis.utils.client.managers.file.exception;

public class FileProcessingException extends Exception {
    public FileProcessingException(String message) {
        super(message);
    }

    public FileProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
