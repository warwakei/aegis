package fun.aegis.utils.client.managers.api.command.manager;

import net.minecraft.util.Pair;
import fun.aegis.utils.client.managers.api.command.ICommand;
import fun.aegis.utils.client.managers.api.command.argument.ICommandArgument;
import fun.aegis.utils.client.managers.api.command.registry.Registry;

import java.util.List;
import java.util.stream.Stream;

public interface ICommandManager {
    Registry<ICommand> getRegistry();

    ICommand getCommand(String name);

    boolean execute(String string);

    boolean execute(Pair<String, List<ICommandArgument>> expanded);

    Stream<String> tabComplete(Pair<String, List<ICommandArgument>> expanded);

    Stream<String> tabComplete(String prefix);
}
