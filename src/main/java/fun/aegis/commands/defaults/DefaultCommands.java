package fun.aegis.commands.defaults;

import fun.aegis.Aegis;
import fun.aegis.utils.client.managers.api.command.ICommand;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class DefaultCommands {
    public static List<ICommand> createAll() {
        Aegis main = Aegis.getInstance();
        List<ICommand> commands = new ArrayList<>(Arrays.asList(
                new ConfigCommand(main),
                new MacroCommand(main),
                new HelpCommand(main),
                new BindCommand(main),
                new ColorCommand(main),
                new IgnoreCommand(main),
                new WayCommand(main),
                new RCTCommand(main),
                new FriendCommand(),
                new PrefixCommand(),
                new TargetCommand(),
                new StaffCommand(),
                new BlockESPCommand(),
                new TabParserCommand(),
                new ClipCommand(),
                new BotCommand(main)
        ));
        return Collections.unmodifiableList(commands);
    }
}
