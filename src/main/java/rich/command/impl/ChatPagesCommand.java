package rich.command.impl;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rich.command.Command;
import rich.command.CommandManager;
import rich.command.helpers.TabCompleteHelper;
import rich.util.config.impl.chattab.ChatTab;
import rich.util.config.impl.chattab.ChatTabManager;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static rich.command.impl.HelpCommand.getLine;

public class ChatPagesCommand extends Command {

    public ChatPagesCommand() {
        super("chatpages", "Управление вкладками чата", "cp", "chattabs");
    }

    @Override
    public void execute(String label, String[] args) {
        CommandManager manager = CommandManager.getInstance();
        ChatTabManager tabManager = ChatTabManager.getInstance();

        String arg = args.length > 0 ? args[0].toLowerCase(Locale.US) : "list";

        switch (arg) {
            case "rename" -> {
                if (args.length < 3) {
                    logDirect("Использование: chatpages rename <oldName> <newName>", Formatting.RED);
                    return;
                }
                String oldName = args[1];
                String newName = args[2];

                ChatTab tab = tabManager.getTab(oldName);
                if (tab == null) {
                    logDirect(String.format("Вкладка '%s' не найдена!", oldName), Formatting.RED);
                    return;
                }

                // Создаём новую вкладку с новым именем
                ChatTab newTab = new ChatTab(newName, tab.getFilterType(), tab.getFilterValue());
                tabManager.removeTab(oldName);
                tabManager.addTab(newTab);
                logDirect(String.format("Вкладка переименована: '%s' -> '%s'", oldName, newName), Formatting.GREEN);
            }
            case "add" -> {
                if (args.length < 5) {
                    logDirect("Использование: chatpages add <filterType> <filterValue> pageName <name>", Formatting.RED);
                    logDirect("filterType: StartingFrom, Contains, FromPlayer, Friends, Ignored", Formatting.GRAY);
                    return;
                }
                String filterTypeStr = args[1];
                String filterValue = args[2];
                String pageName = args[4];

                ChatTab.FilterType filterType;
                try {
                    filterType = ChatTab.FilterType.valueOf(filterTypeStr);
                } catch (IllegalArgumentException e) {
                    logDirect("Неверный тип фильтра! Доступны: StartingFrom, Contains, FromPlayer, Friends, Ignored", Formatting.RED);
                    return;
                }

                if (tabManager.getTab(pageName) != null) {
                    logDirect(String.format("Вкладка '%s' уже существует!", pageName), Formatting.RED);
                    return;
                }

                ChatTab tab = new ChatTab(pageName, filterType, filterValue);
                tabManager.addTab(tab);
                logDirect(String.format("Вкладка '%s' добавлена!", pageName), Formatting.GREEN);
            }
            case "remove", "del", "delete" -> {
                if (args.length < 2) {
                    logDirect("Использование: chatpages remove <name>", Formatting.RED);
                    return;
                }
                String name = args[1];
                if (tabManager.getTab(name) == null) {
                    logDirect(String.format("Вкладка '%s' не найдена!", name), Formatting.RED);
                    return;
                }
                tabManager.removeTab(name);
                logDirect(String.format("Вкладка '%s' удалена!", name), Formatting.GREEN);
            }
            case "switch", "set" -> {
                if (args.length < 2) {
                    logDirect("Использование: chatpages switch <name>", Formatting.RED);
                    logDirect("Или: chatpages switch full - показать все сообщения", Formatting.GRAY);
                    return;
                }
                String name = args[1];
                tabManager.setActiveTab(name);
                if (tabManager.hasActiveTab()) {
                    logDirect(String.format("Активна вкладка: %s", tabManager.getActiveTabName()), Formatting.GREEN);
                } else {
                    logDirect("Показаны все сообщения", Formatting.GREEN);
                }
            }
            case "clear" -> {
                int count = tabManager.getTabs().size();
                tabManager.clearTabs();
                tabManager.setActiveTab(null);
                logDirect(String.format("Все вкладки удалены! Удалено: %d", count), Formatting.GREEN);
            }
            case "list" -> {
                List<ChatTab> tabs = tabManager.getTabs();

                logDirectRaw(Text.literal(getLine()));
                logDirect("§f§lВКЛАДКИ ЧАТА §7(" + tabs.size() + ")");
                logDirectRaw(Text.literal(getLine()));

                if (tabs.isEmpty()) {
                    logDirect("Список вкладок пуст!", Formatting.RED);
                    logDirect("Используйте: chatpages add <filterType> <value> pageName <name>", Formatting.GRAY);
                } else {
                    String activeName = tabManager.getActiveTabName();
                    for (ChatTab tab : tabs) {
                        String isActive = tab.getName().equalsIgnoreCase(activeName) ? " §a[ACTIVE]" : "";
                        logDirect(String.format("  §7- §f%s §8| §e%s §8= §f%s%s",
                                tab.getName(), tab.getFilterType(), tab.getFilterValue(), isActive));
                    }
                }

                logDirectRaw(Text.literal(getLine()));
                logDirect("§7> chatpages switch <name> §8- §fПереключить вкладку", Formatting.GRAY);
                logDirect("§7> chatpages switch full §8- §fПоказать все сообщения", Formatting.GRAY);
                logDirectRaw(Text.literal(getLine()));
            }
            default -> {
                logDirectRaw(Text.literal(getLine()));
                logDirect("§f§lУПРАВЛЕНИЕ ВКЛАДКАМИ ЧАТА");
                logDirectRaw(Text.literal(getLine()));
                logDirect("§7> chatpages add <type> <value> pageName <name>", Formatting.WHITE);
                logDirect("  §8Типы фильтров: StartingFrom, Contains, FromPlayer, Friends, Ignored", Formatting.GRAY);
                logDirect("§7> chatpages rename <oldName> <newName> §8- §fПереименовать вкладку", Formatting.WHITE);
                logDirect("§7> chatpages remove <name> §8- §fУдалить вкладку", Formatting.WHITE);
                logDirect("§7> chatpages switch <name> §8- §fПереключить вкладку", Formatting.WHITE);
                logDirect("§7> chatpages list §8- §fПоказать список вкладок", Formatting.WHITE);
                logDirect("§7> chatpages clear §8- §fОчистить все вкладки", Formatting.WHITE);
                logDirectRaw(Text.literal(getLine()));
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, String[] args) {
        if (args.length == 1) {
            return new TabCompleteHelper()
                    .append("add", "remove", "switch", "rename", "list", "clear")
                    .sortAlphabetically()
                    .filterPrefix(args[0])
                    .stream();
        }
        if (args.length == 2) {
            String action = args[0].toLowerCase();
            if (action.equals("add")) {
                return new TabCompleteHelper()
                        .append("StartingFrom", "Contains", "FromPlayer", "Friends", "Ignored")
                        .filterPrefix(args[1])
                        .stream();
            }
            if (action.equals("remove") || action.equals("del") || action.equals("delete") ||
                action.equals("switch") || action.equals("set") || action.equals("rename")) {
                ChatTabManager tabManager = ChatTabManager.getInstance();
                List<String> names = new java.util.ArrayList<>();
                names.add("full");
                names.addAll(tabManager.getTabs().stream().map(ChatTab::getName).toList());
                return new TabCompleteHelper()
                        .append(names.toArray(new String[0]))
                        .filterPrefix(args[1])
                        .stream();
            }
        }
        if (args.length == 4 && args[0].equalsIgnoreCase("add")) {
            return new TabCompleteHelper()
                    .append("pageName")
                    .filterPrefix(args[3])
                    .stream();
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управление вкладками чата";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда для управления вкладками чата",
                "Использование:",
                "> chatpages add <type> <value> pageName <name> - Добавить вкладку",
                "> chatpages remove <name> - Удалить вкладку",
                "> chatpages switch <name> - Переключить вкладку",
                "> chatpages list - Показать список вкладок",
                "> chatpages clear - Очистить все вкладки"
        );
    }
}
