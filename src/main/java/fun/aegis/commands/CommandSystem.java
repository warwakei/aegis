package fun.aegis.commands;

import fun.aegis.utils.client.managers.api.command.ICommandSystem;
import fun.aegis.utils.client.managers.api.command.argparser.IArgParserManager;
import fun.aegis.commands.argparser.ArgParserManager;

public enum CommandSystem implements ICommandSystem {
    INSTANCE;

    @Override
    public IArgParserManager getParserManager() {
        return ArgParserManager.INSTANCE;
    }
}
