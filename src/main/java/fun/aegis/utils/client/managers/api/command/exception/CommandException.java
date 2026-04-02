package fun.aegis.utils.client.managers.api.command.exception;

public class CommandException extends Exception implements ICommandException {

    public CommandException(String reason) {
        super(reason);
    }

    protected CommandException(String reason, Throwable cause) {
        super(reason, cause);
    }
}
