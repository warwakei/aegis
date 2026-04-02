package rich.command.impl;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rich.command.Command;
import rich.command.CommandManager;
import rich.util.config.impl.prefix.PrefixConfig;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static rich.command.impl.HelpCommand.getLine;

public class PrefixCommand extends Command {

    public PrefixCommand() {
        super("prefix", "Изменение префикса команд");
    }

    @Override
    public void execute(String label, String[] args) {
        CommandManager manager = CommandManager.getInstance();

        if (args.length == 0) {
            logDirectRaw(Text.literal(getLine()));
            logDirect("§f§lПРЕФИКС КОМАНД");
            logDirectRaw(Text.literal(getLine()));
            logDirect("§7Текущий префикс: §f" + manager.getPrefix());
            logDirect("§7> prefix set <symbol> §8- §fУстановить новый префикс");
            logDirectRaw(Text.literal(getLine()));
            return;
        }

        String action = args[0].toLowerCase();

        if (action.equals("set")) {
            if (args.length < 2) {
                logDirect("Использование: prefix set <symbol>", Formatting.RED);
                return;
            }

            String newPrefix = args[1];

            if (newPrefix.length() > 3) {
                logDirect("Префикс не может быть длиннее 3 символов!", Formatting.RED);
                return;
            }

            if (newPrefix.contains(" ")) {
                logDirect("Префикс не может содержать пробелы!", Formatting.RED);
                return;
            }

            PrefixConfig.getInstance().setPrefixAndSave(newPrefix);
            logDirect(String.format("§aПрефикс изменен на: §f%s", newPrefix), Formatting.GREEN);
            logDirect(String.format("§7Теперь команды вводятся как: §f%shelp", newPrefix), Formatting.GREEN);
        } else {
            logDirect("Использование: prefix set <symbol>", Formatting.RED);
        }
    }

    @Override
    public Stream<String> tabComplete(String label, String[] args) {
        if (args.length == 1) {
            return Stream.of("set").filter(s -> s.startsWith(args[0].toLowerCase()));
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) {
            return Stream.of(".", "!", "$", "#", "-", "/").filter(s -> s.startsWith(args[1]));
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Изменение префикса команд";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда для изменения префикса команд в чите",
                "Использование:",
                "> prefix - Показать текущий префикс",
                "> prefix set <symbol> - Установить новый префикс"
        );
    }
}