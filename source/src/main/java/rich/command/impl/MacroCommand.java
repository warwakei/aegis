package rich.command.impl;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rich.command.Command;
import rich.command.CommandManager;
import rich.command.helpers.Paginator;
import rich.command.helpers.TabCompleteHelper;
import rich.util.repository.macro.Macro;
import rich.util.repository.macro.MacroRepository;
import rich.util.string.KeyHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static rich.command.impl.HelpCommand.getLine;

public class MacroCommand extends Command {

    public MacroCommand() {
        super("macro", "Управление макросами", "macros");
    }

    @Override
    public void execute(String label, String[] args) {
        CommandManager manager = CommandManager.getInstance();
        MacroRepository macroRepository = MacroRepository.getInstance();

        String action = args.length > 0 ? args[0].toLowerCase(Locale.US) : "list";

        switch (action) {
            case "add" -> {
                if (args.length < 4) {
                    logDirect("Использование: macro add <key> <name> <message>", Formatting.RED);
                    return;
                }

                String keyName = args[1];
                int key = KeyHelper.getKeyCode(keyName);

                if (key == -1) {
                    logDirect(String.format("Неизвестная клавиша: %s", keyName), Formatting.RED);
                    return;
                }

                String name = args[2];

                StringBuilder messageBuilder = new StringBuilder();
                for (int i = 3; i < args.length; i++) {
                    if (i > 3) messageBuilder.append(" ");
                    messageBuilder.append(args[i]);
                }
                String message = messageBuilder.toString();

                if (macroRepository.hasMacro(name)) {
                    logDirect(String.format("Макрос с именем %s уже существует!", name), Formatting.RED);
                    return;
                }

                macroRepository.addMacroAndSave(name, message, key);

                logDirect(String.format("§aДобавлен макрос §f%s §aна клавишу §f%s §aс командой §f%s",
                        name, KeyHelper.getKeyName(key).toLowerCase(), message), Formatting.GREEN);
            }
            case "remove", "del", "delete" -> {
                if (args.length < 2) {
                    logDirect("Использование: macro remove <name>", Formatting.RED);
                    return;
                }

                String name = args[1];

                if (!macroRepository.hasMacro(name)) {
                    logDirect(String.format("Макрос %s не найден!", name), Formatting.RED);
                    return;
                }

                macroRepository.deleteMacroAndSave(name);
                logDirect(String.format("Макрос %s удален!", name), Formatting.GREEN);
            }
            case "clear" -> {
                int count = macroRepository.size();
                macroRepository.clearListAndSave();
                logDirect(String.format("Все макросы удалены! Удалено: %d", count), Formatting.GREEN);
            }
            case "list" -> {
                int page = 1;
                if (args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {}
                }

                List<Macro> macros = macroRepository.getMacroList();

                if (macros.isEmpty()) {
                    logDirect("Список макросов пуст!", Formatting.RED);
                    return;
                }

                Paginator<Macro> paginator = new Paginator<>(macros);
                paginator.setPage(page);

                paginator.display(
                        () -> {
                            logDirectRaw(Text.literal(getLine()));
                            logDirect("§f§lСПИСОК МАКРОСОВ §7(" + macros.size() + ")");
                            logDirectRaw(Text.literal(getLine()));
                        },
                        macro -> {
                            String macroName = macro.name();
                            String keyName = KeyHelper.getKeyName(macro.key()).toLowerCase();
                            String message = macro.message();

                            MutableText component = Text.literal("  §e● §f" + macroName)
                                    .append(Text.literal(" §8[§7" + keyName + "§8]"))
                                    .append(Text.literal(" §8-> §7" + message));

                            MutableText hoverText = Text.literal("§7Нажмите чтобы удалить макрос §f" + macroName);
                            String removeCommand = manager.getPrefix() + "macro remove " + macroName;

                            component.setStyle(component.getStyle()
                                    .withHoverEvent(new HoverEvent.ShowText(hoverText))
                                    .withClickEvent(new ClickEvent.RunCommand(removeCommand)));

                            return component;
                        },
                        manager.getPrefix() + label + " list"
                );
            }
            default -> {
                logDirectRaw(Text.literal(getLine()));
                logDirect("§f§lУПРАВЛЕНИЕ МАКРОСАМИ");
                logDirectRaw(Text.literal(getLine()));
                logDirect("§7> macro add <key> <name> <message> §8- §fДобавить макрос");
                logDirect("§7> macro remove <name> §8- §fУдалить макрос");
                logDirect("§7> macro list §8- §fПоказать список макросов");
                logDirect("§7> macro clear §8- §fУдалить все макросы");
                logDirectRaw(Text.literal(getLine()));
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, String[] args) {
        if (args.length == 1) {
            return new TabCompleteHelper()
                    .append("add", "remove", "list", "clear")
                    .sortAlphabetically()
                    .filterPrefix(args[0])
                    .stream();
        }
        if (args.length == 2) {
            String action = args[0].toLowerCase();
            if (action.equals("add")) {
                return new TabCompleteHelper()
                        .append(KeyHelper.getAllKeyNames())
                        .filterPrefix(args[1])
                        .stream();
            }
            if (action.equals("remove") || action.equals("del") || action.equals("delete")) {
                return new TabCompleteHelper()
                        .append(MacroRepository.getInstance().getMacroNames().toArray(new String[0]))
                        .filterPrefix(args[1])
                        .stream();
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управление макросами";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда для управления макросами",
                "Использование:",
                "> macro add <key> <name> <message> - Добавить макрос",
                "> macro remove <name> - Удалить макрос",
                "> macro list - Показать список макросов",
                "> macro clear - Удалить все макросы"
        );
    }
}