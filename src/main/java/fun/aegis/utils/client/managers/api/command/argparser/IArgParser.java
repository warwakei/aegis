package fun.aegis.utils.client.managers.api.command.argparser;

import fun.aegis.utils.client.managers.api.command.argument.ICommandArgument;

public interface IArgParser<T> {
    Class<T> getTarget();

    interface Stateless<T> extends IArgParser<T> {
        T parseArg(ICommandArgument arg) throws Exception;
    }

    interface Stated<T, S> extends IArgParser<T> {
        Class<S> getStateType();

        T parseArg(ICommandArgument arg, S state) throws Exception;
    }
}
