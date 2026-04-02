package rich.command;

import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rich.util.string.chat.ChatMessage;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

/**
 *  © 2026 Copyright Rich Client 2.0
 *        All Rights Reserved ®
 */

public abstract class Command {
    private final String name;
    private final String description;
    private final List<String> aliases;

    public Command(String name, String description, String... aliases) {
        this.name = name;
        this.description = description;
        this.aliases = Arrays.asList(aliases);
    }

    public abstract void execute(String label, String[] args);

    public Stream<String> tabComplete(String label, String[] args) {
        return Stream.empty();
    }

    public String getShortDesc() {
        return description;
    }

    public List<String> getLongDesc() {
        return Arrays.asList(
                description,
                "",
                "Использование:",
                "> " + name + " - " + description
        );
    }

    public boolean hiddenFromHelp() {
        return false;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public List<String> getAliases() {
        return aliases;
    }

    public List<String> getAllNames() {
        List<String> names = new ArrayList<>();
        names.add(name);
        names.addAll(aliases);
        return names;
    }

    public boolean matches(String input) {
        return name.equalsIgnoreCase(input) ||
                aliases.stream().anyMatch(alias -> alias.equalsIgnoreCase(input));
    }

    protected void logDirect(String message) {
        ChatMessage.brandmessage(message);
    }

    protected void logDirect(String message, Formatting formatting) {
        CommandManager manager = CommandManager.getInstance();
        if (formatting == Formatting.RED) {
            manager.sendError(message);
        } else if (formatting == Formatting.GREEN) {
            manager.sendSuccess(message);
        } else {
            manager.sendMessage(message);
        }
    }

    protected void logDirect(Text text) {
        ChatMessage.brandmessage(text.getString());
    }

    protected void logDirect(MutableText text) {
        ChatMessage.brandmessage(text.getString());
    }

    protected void logDirectRaw(Text text) {
        CommandManager.getInstance().sendRaw(text);
    }

    protected void logDirectRaw(MutableText text) {
        CommandManager.getInstance().sendRaw(text);
    }
}