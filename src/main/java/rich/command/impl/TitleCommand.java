package rich.command.impl;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rich.command.Command;
import rich.util.TitleManager;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static rich.command.impl.HelpCommand.getLine;

public class TitleCommand extends Command {

    public TitleCommand() {
        super("title", "Настройка заголовка окна Minecraft", "t");
    }

    @Override
    public void execute(String label, String[] args) {
        TitleManager titleManager = TitleManager.getInstance();

        if (args.length == 0) {
            logDirectRaw(Text.literal(getLine()));
            logDirect("§f§lЗАГОЛОВОК ОКНА");
            logDirectRaw(Text.literal(getLine()));
            String currentMode = titleManager.getMode() == TitleManager.TitleMode.AEGIS ? "§aAegis" : "§7Default";
            logDirect("§7Текущий режим: " + currentMode);
            logDirect("§7> title default §8- §fСтандартный заголовок Minecraft");
            logDirect("§7> title aegis §8- §fЗаголовок Aegis Neo");
            logDirectRaw(Text.literal(getLine()));
            return;
        }

        String action = args[0].toLowerCase();

        switch (action) {
            case "default", "def" -> {
                titleManager.setMode(TitleManager.TitleMode.DEFAULT);
                logDirect("§aУстановлен стандартный заголовок Minecraft", Formatting.GREEN);
            }
            case "aegis", "client" -> {
                titleManager.setMode(TitleManager.TitleMode.AEGIS);
                logDirect("§aУстановлен заголовок Aegis Neo", Formatting.GREEN);
            }
            default -> {
                logDirect("Неизвестный режим. Используйте: default, def, aegis, client", Formatting.RED);
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, String[] args) {
        if (args.length == 1) {
            return Stream.of("default", "def", "aegis", "client")
                    .filter(s -> s.startsWith(args[0].toLowerCase()));
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Настройка заголовка окна Minecraft";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда для изменения заголовка окна Minecraft",
                "Использование:",
                "> title - Показать текущий режим",
                "> title default/def - Стандартный заголовок Minecraft",
                "> title aegis/client - Заголовок Aegis Neo"
        );
    }
}
