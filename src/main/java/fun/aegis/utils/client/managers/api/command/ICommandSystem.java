package fun.aegis.utils.client.managers.api.command;

import fun.aegis.utils.client.managers.api.command.argparser.IArgParserManager;

public interface ICommandSystem {
    IArgParserManager getParserManager();
}
