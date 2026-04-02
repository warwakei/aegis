package fun.aegis.utils.client.managers.api.command.exception;

public class CommandNoParserForTypeException extends CommandUnhandledException {

    public CommandNoParserForTypeException(Class<?> klass) {
        super(String.format("Could not find a handler for type %s", klass.getSimpleName()));
    }
}
