package rich.command;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rich.command.impl.*;
import rich.events.api.EventManager;
import rich.events.api.EventHandler;
import rich.events.impl.ChatEvent;
import rich.events.impl.TabCompleteEvent;
import rich.util.config.impl.prefix.PrefixConfig;
import rich.util.string.chat.ChatMessage;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

public class CommandManager {
    private static CommandManager instance;
    private final List<Command> commands;
    private String prefix;

    public CommandManager() {
        instance = this;
        this.commands = new CopyOnWriteArrayList<>();
        this.prefix = PrefixConfig.getInstance().getPrefix();
    }

    public static CommandManager getInstance() {
        return instance;
    }

    public void init() {
        registerCommand(new HelpCommand());
        registerCommand(new ConfigCommand());
        registerCommand(new AutoBuyCommand());
        registerCommand(new FriendCommand());
        registerCommand(new IgnoreCommand());
        registerCommand(new ChatPagesCommand());
        registerCommand(new MacroCommand());
        registerCommand(new BindCommand());
        registerCommand(new PrefixCommand());
        registerCommand(new WayCommand());
        registerCommand(new StaffCommand());
        registerCommand(new BlockESPCommand());

        EventManager.register(this);
    }

    public void registerCommand(Command command) {
        commands.add(command);
    }

    public void unregisterCommand(Command command) {
        commands.remove(command);
    }

    public Command getCommand(String name) {
        return commands.stream()
                .filter(cmd -> cmd.matches(name))
                .findFirst()
                .orElse(null);
    }

    public List<Command> getCommands() {
        return new ArrayList<>(commands);
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        String msg = event.getMessage();
        if (msg.startsWith(prefix)) {
            event.cancel();
            String commandStr = msg.substring(prefix.length());

            if (commandStr.trim().isEmpty()) {
                execute("help");
                return;
            }

            if (!execute(commandStr)) {
                sendError("Неизвестная команда. Используйте " + prefix + "help для списка команд.");
            }
        }
    }

    @EventHandler
    public void onTabComplete(TabCompleteEvent event) {
        String eventPrefix = event.prefix;

        if (!eventPrefix.startsWith(prefix)) {
            return;
        }

        String msg = eventPrefix.substring(prefix.length());
        Stream<String> stream = tabComplete(msg);

        String[] parts = msg.split(" ", -1);
        if (parts.length <= 1) {
            stream = stream.map(x -> prefix + x);
        }

        event.completions = stream.toArray(String[]::new);
    }

    public boolean execute(String input) {
        if (input == null || input.trim().isEmpty()) {
            return execute("help");
        }

        String[] parts = input.trim().split("\\s+", 2);
        String commandName = parts[0];
        String[] args = parts.length > 1 ? parts[1].split("\\s+") : new String[0];

        Command command = getCommand(commandName);
        if (command != null) {
            try {
                command.execute(commandName, args);
                return true;
            } catch (Exception e) {
                sendError("Ошибка при выполнении команды: " + e.getMessage());
                e.printStackTrace();
            }
        }

        return false;
    }

    public Stream<String> tabComplete(String input) {
        if (input == null) {
            input = "";
        }

        String[] args = input.split("\\s+", -1);

        if (args.length <= 1) {
            String partial = args.length == 0 ? "" : args[0].toLowerCase();
            return getCommandSuggestions(partial);
        }

        String commandName = args[0];
        Command command = getCommand(commandName);

        if (command != null) {
            String[] subArgs = Arrays.copyOfRange(args, 1, args.length);
            return command.tabComplete(commandName, subArgs);
        }

        return Stream.empty();
    }

    private Stream<String> getCommandSuggestions(String partial) {
        if (partial.isEmpty()) {
            return commands.stream()
                    .map(Command::getName)
                    .sorted();
        }

        Set<String> suggestions = new LinkedHashSet<>();

        for (Command cmd : commands) {
            String mainName = cmd.getName();

            if (mainName.toLowerCase().startsWith(partial)) {
                suggestions.add(mainName);
            } else {
                for (String alias : cmd.getAliases()) {
                    if (alias.toLowerCase().startsWith(partial)) {
                        suggestions.add(alias);
                        break;
                    }
                }
            }
        }

        return suggestions.stream().sorted();
    }

    public void sendMessage(String message) {
        ChatMessage.brandmessage(message);
    }

    public void sendSuccess(String message) {
        if (MinecraftClient.getInstance().player != null) {
            Text prefixText = ChatMessage.brandmessage();
            Text formattedMessage = prefixText.copy()
                    .append(Text.literal(" -> ").formatted(Formatting.DARK_GRAY))
                    .append(Text.literal(message).formatted(Formatting.GREEN));
            MinecraftClient.getInstance().player.sendMessage(formattedMessage, false);
        }
    }

    public void sendError(String message) {
        if (MinecraftClient.getInstance().player != null) {
            Text prefixText = ChatMessage.brandmessage();
            Text formattedMessage = prefixText.copy()
                    .append(Text.literal(" -> ").formatted(Formatting.DARK_GRAY))
                    .append(Text.literal(message).formatted(Formatting.RED));
            MinecraftClient.getInstance().player.sendMessage(formattedMessage, false);
        }
    }

    public void sendRaw(Text text) {
        if (MinecraftClient.getInstance().player != null) {
            MinecraftClient.getInstance().player.sendMessage(text, false);
        }
    }
}