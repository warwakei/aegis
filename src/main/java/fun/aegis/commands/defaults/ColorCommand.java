package fun.aegis.commands.defaults;

import fun.aegis.Aegis;
import fun.aegis.display.screens.clickgui.newgui.theme.Theme;
import fun.aegis.display.screens.clickgui.newgui.theme.ThemeManager;
import fun.aegis.utils.client.managers.api.command.Command;
import fun.aegis.utils.client.managers.api.command.argument.IArgConsumer;
import fun.aegis.utils.client.managers.api.command.exception.CommandException;
import fun.aegis.utils.client.managers.api.command.helpers.TabCompleteHelper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.Formatting;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static fun.aegis.utils.client.managers.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ColorCommand extends Command {

    public ColorCommand(Aegis main) {
        super("color");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String action = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
        switch (action) {
            case "set":
                handleSetColor(args);
                break;
            case "list":
                handleListColors(args, label);
                break;
            default:
                throw new CommandException("Неизвестная команда. Используйте: set <theme> или list");
        }
    }

    private void handleSetColor(IArgConsumer args) throws CommandException {
        args.requireMin(1);
        String themeName = args.getString().toLowerCase(Locale.US);

        Theme theme = getThemeByName(themeName);
        if (theme == null) {
            throw new CommandException("Неизвестная тема: " + themeName + ". Доступные темы: dark, light, purple, blue, cyan, green, red, orange, neon, sunset, ocean, forest, lavender, coral, mint, peach, midnight, aurora, cyberpunk, sakura");
        }

        ThemeManager.getInstance().setTheme(theme);
        logDirect(Formatting.GREEN + "Тема изменена на: " + Formatting.RED + theme.getName());
    }

    private void handleListColors(IArgConsumer args, String label) throws CommandException {
        args.requireMax(0);
        Theme current = ThemeManager.getInstance().getCurrentTheme();
        logDirect(Formatting.GRAY + "Текущая тема: " + Formatting.WHITE + current.getName());
        logDirect(Formatting.GRAY + "Доступные темы:");
        logDirect(Formatting.WHITE + "  - dark, light, purple, blue, cyan, green, red, orange");
        logDirect(Formatting.WHITE + "  - neon, sunset, ocean, forest, lavender, coral, mint, peach");
        logDirect(Formatting.WHITE + "  - midnight, aurora, cyberpunk, sakura");
    }

    private Theme getThemeByName(String name) {
        return switch (name) {
            case "dark" -> Theme.DARK;
            case "light" -> Theme.LIGHT;
            case "purple" -> Theme.PURPLE;
            case "blue" -> Theme.BLUE;
            case "cyan" -> Theme.CYAN;
            case "green" -> Theme.GREEN;
            case "red" -> Theme.RED;
            case "orange" -> Theme.ORANGE;
            case "neon" -> Theme.NEON;
            case "sunset" -> Theme.SUNSET;
            case "ocean" -> Theme.OCEAN;
            case "forest" -> Theme.FOREST;
            case "lavender" -> Theme.LAVENDER;
            case "coral" -> Theme.CORAL;
            case "mint" -> Theme.MINT;
            case "peach" -> Theme.PEACH;
            case "midnight" -> Theme.MIDNIGHT;
            case "aurora" -> Theme.AURORA;
            case "cyberpunk" -> Theme.CYBERPUNK;
            case "sakura" -> Theme.SAKURA;
            default -> null;
        };
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper()
                    .sortAlphabetically()
                    .prepend("set", "list")
                    .filterPrefix(args.getString())
                    .stream();
        } else {
            String arg = args.getString();
            if (arg.equalsIgnoreCase("set")) {
                if (args.hasExactly(1)) {
                    return Stream.of("dark", "light", "purple", "blue", "cyan", "green", "red", "orange", "neon", "sunset", "ocean", "forest", "lavender", "coral", "mint", "peach", "midnight", "aurora", "cyberpunk", "sakura")
                            .filter(s -> {
                                try {
                                    return s.startsWith(args.getString().toLowerCase(Locale.US));
                                } catch (Exception e) {
                                    return false;
                                }
                            });
                }
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управление цветовой схемой ClickGUI.";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Эта команда позволяет управлять цветовой схемой интерфейса.",
                "",
                "Использование:",
                "> color set <theme> - Устанавливает цветовую схему (dark/light).",
                "> color list - Показывает текущую и доступные темы."
        );
    }
}
