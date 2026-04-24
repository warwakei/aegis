package rich.command.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rich.command.Command;
import rich.command.CommandManager;
import rich.command.helpers.Paginator;
import rich.command.helpers.TabCompleteHelper;
import rich.util.repository.ignore.ChatFilter;
import rich.util.repository.ignore.IgnoreUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static rich.command.impl.HelpCommand.getLine;

public class IgnoreCommand extends Command {

    public IgnoreCommand() {
        super("ignore", "Управление игнор-листом", "ign", "blacklist");
    }

    @Override
    public void execute(String label, String[] args) {
        CommandManager manager = CommandManager.getInstance();

        String arg = args.length > 0 ? args[0].toLowerCase(Locale.US) : "list";

        switch (arg) {
            case "add" -> {
                if (args.length < 2) {
                    logDirect("Использование: ignore add <name>", Formatting.RED);
                    return;
                }
                String name = args[1];
                if (IgnoreUtils.isIgnore(name)) {
                    logDirect(String.format("Игрок %s уже в игнор-листе!", name), Formatting.RED);
                    return;
                }
                IgnoreUtils.addIgnoreAndSave(name);
                logDirect(String.format("Игрок %s добавлен в игнор-лист!", name), Formatting.GREEN);
            }
            case "remove", "del", "delete" -> {
                if (args.length < 2) {
                    logDirect("Использование: ignore remove <name>", Formatting.RED);
                    return;
                }
                String name = args[1];
                if (!IgnoreUtils.isIgnore(name)) {
                    logDirect(String.format("Игрок %s не найден в игнор-листе!", name), Formatting.RED);
                    return;
                }
                IgnoreUtils.removeIgnoreAndSave(name);
                logDirect(String.format("Игрок %s удален из игнор-листа!", name), Formatting.GREEN);
            }
            case "clear" -> {
                int count = IgnoreUtils.size();
                IgnoreUtils.clearAndSave();
                logDirect(String.format("Игнор-лист очищен! Удалено: %d", count), Formatting.GREEN);
            }
            case "filter" -> {
                handleFilterCommand(args);
            }
            case "list" -> {
                int page = 1;
                if (args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {}
                }

                List<String> ignores = IgnoreUtils.getIgnoreNames();

                if (ignores.isEmpty()) {
                    logDirect("Игнор-лист пуст!", Formatting.RED);
                    return;
                }

                Paginator<String> paginator = new Paginator<>(ignores);
                paginator.setPage(page);

                paginator.display(
                        () -> {
                            logDirectRaw(Text.literal(getLine()));
                            logDirect("§f§lИГНОР-ЛИСТ §7(" + ignores.size() + ")");
                            logDirectRaw(Text.literal(getLine()));
                        },
                        ignore -> {
                            MutableText nameComponent = Text.literal("  §c● §f" + ignore);

                            MutableText hoverText = Text.literal("§7Нажмите чтобы удалить §f" + ignore + " §7из игнор-листа");
                            String removeCommand = manager.getPrefix() + "ignore remove " + ignore;

                            nameComponent.setStyle(nameComponent.getStyle()
                                    .withHoverEvent(new HoverEvent.ShowText(hoverText))
                                    .withClickEvent(new ClickEvent.RunCommand(removeCommand)));

                            return nameComponent;
                        },
                        manager.getPrefix() + label + " list"
                );
            }
            default -> {
                showHelp();
            }
        }
    }
    
    private void handleFilterCommand(String[] args) {
        if (args.length < 2) {
            showFilterHelp();
            return;
        }
        
        String filterAction = args[1].toLowerCase();
        
        switch (filterAction) {
            case "warps" -> {
                ChatFilter filter = new ChatFilter(ChatFilter.FilterType.WARPS, "");
                IgnoreUtils.addChatFilterAndSave(filter);
                logDirect("Фильтр для warp сообщений добавлен!", Formatting.GREEN);
            }
            case "custom" -> {
                if (args.length < 4) {
                    logDirect("Использование: ignore filter custom <contains/startsFrom/endsAt/msgCreator> <value>", Formatting.RED);
                    return;
                }
                
                String customType = args[2].toLowerCase();
                String value = String.join(" ", Arrays.copyOfRange(args, 3, args.length));
                
                ChatFilter.FilterType filterType = switch (customType) {
                    case "contains" -> ChatFilter.FilterType.CONTAINS;
                    case "startsfrom" -> ChatFilter.FilterType.STARTS_WITH;
                    case "endsat" -> ChatFilter.FilterType.ENDS_WITH;
                    case "msgcreator" -> ChatFilter.FilterType.SENDER;
                    default -> null;
                };
                
                if (filterType == null) {
                    logDirect("Неверный тип фильтра! Доступные: contains, startsFrom, endsAt, msgCreator", Formatting.RED);
                    return;
                }
                
                ChatFilter filter = new ChatFilter(filterType, value);
                IgnoreUtils.addChatFilterAndSave(filter);
                logDirect(String.format("Фильтр %s '%s' добавлен!", customType, value), Formatting.GREEN);
            }
            case "list" -> {
                List<ChatFilter> filters = IgnoreUtils.getChatFilters();
                if (filters.isEmpty()) {
                    logDirect("Список фильтров пуст!", Formatting.RED);
                    return;
                }
                
                logDirectRaw(Text.literal(getLine()));
                logDirect("§f§lФИЛЬТРЫ СООБЩЕНИЙ §7(" + filters.size() + ")");
                logDirectRaw(Text.literal(getLine()));
                
                for (int i = 0; i < filters.size(); i++) {
                    ChatFilter filter = filters.get(i);
                    String typeStr = switch (filter.getType()) {
                        case CONTAINS -> "содержит";
                        case STARTS_WITH -> "начинается с";
                        case ENDS_WITH -> "заканчивается на";
                        case SENDER -> "отправитель";
                        case WARPS -> "warp сообщения";
                    };
                    
                    String valueStr = filter.getType() == ChatFilter.FilterType.WARPS ? "" : " '" + filter.getValue() + "'";
                    logDirect(String.format("  §c%d. §f%s%s", i + 1, typeStr, valueStr));
                }
                logDirectRaw(Text.literal(getLine()));
            }
            case "remove" -> {
                if (args.length < 3) {
                    logDirect("Использование: ignore filter remove <номер>", Formatting.RED);
                    return;
                }
                
                try {
                    int index = Integer.parseInt(args[2]) - 1;
                    List<ChatFilter> filters = IgnoreUtils.getChatFilters();
                    
                    if (index < 0 || index >= filters.size()) {
                        logDirect("Неверный номер фильтра!", Formatting.RED);
                        return;
                    }
                    
                    IgnoreUtils.removeChatFilterAndSave(index);
                    logDirect("Фильтр удален!", Formatting.GREEN);
                } catch (NumberFormatException e) {
                    logDirect("Неверный номер фильтра!", Formatting.RED);
                }
            }
            case "clear" -> {
                int count = IgnoreUtils.getChatFilters().size();
                IgnoreUtils.clearChatFiltersAndSave();
                logDirect(String.format("Все фильтры очищены! Удалено: %d", count), Formatting.GREEN);
            }
            default -> {
                showFilterHelp();
            }
        }
    }
    
    private void showFilterHelp() {
        logDirectRaw(Text.literal(getLine()));
        logDirect("§f§lФИЛТРЫ СООБЩЕНИЙ");
        logDirectRaw(Text.literal(getLine()));
        logDirect("§7> ignore filter warps §8- §fСкрыть warp сообщения");
        logDirect("§7> ignore filter custom contains <текст> §8- §fСкрыть сообщения содержащие текст");
        logDirect("§7> ignore filter custom startsFrom <текст> §8- §fСкрыть сообщения начинающиеся с текста");
        logDirect("§7> ignore filter custom endsAt <текст> §8- §fСкрыть сообщения заканчивающиеся текстом");
        logDirect("§7> ignore filter custom msgCreator <ник> §8- §fСкрыть сообщения от игрока");
        logDirect("§7> ignore filter list §8- §fПоказать все фильтры");
        logDirect("§7> ignore filter remove <номер> §8- §fУдалить фильтр");
        logDirect("§7> ignore filter clear §8- §fОчистить все фильтры");
        logDirectRaw(Text.literal(getLine()));
    }
    
    private void showHelp() {
        logDirectRaw(Text.literal(getLine()));
        logDirect("§f§lУПРАВЛЕНИЕ ИГНОР-ЛИСТОМ");
        logDirectRaw(Text.literal(getLine()));
        logDirect("§7> ignore add <name> §8- §fДобавить игрока в игнор-лист");
        logDirect("§7> ignore remove <name> §8- §fУдалить игрока из игнор-листа");
        logDirect("§7> ignore list §8- §fПоказать список игнор-листа");
        logDirect("§7> ignore clear §8- §fОчистить игнор-лист");
        logDirect("§7> ignore filter §8- §fУправление фильтрами сообщений");
        logDirectRaw(Text.literal(getLine()));
    }

    @Override
    public Stream<String> tabComplete(String label, String[] args) {
        if (args.length == 1) {
            return new TabCompleteHelper()
                    .append("add", "remove", "list", "clear", "filter")
                    .sortAlphabetically()
                    .filterPrefix(args[0])
                    .stream();
        }
        if (args.length == 2) {
            String action = args[0].toLowerCase();
            if (action.equals("add")) {
                return new TabCompleteHelper()
                        .append(getOnlinePlayers().toArray(new String[0]))
                        .filterPrefix(args[1])
                        .stream();
            }
            if (action.equals("remove") || action.equals("del") || action.equals("delete")) {
                return new TabCompleteHelper()
                        .append(IgnoreUtils.getIgnoreNames().toArray(new String[0]))
                        .filterPrefix(args[1])
                        .stream();
            }
            if (action.equals("filter")) {
                return new TabCompleteHelper()
                        .append("warps", "custom", "list", "remove", "clear")
                        .filterPrefix(args[1])
                        .stream();
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("filter") && args[1].equalsIgnoreCase("custom")) {
            return new TabCompleteHelper()
                    .append("contains", "startsFrom", "endsAt", "msgCreator")
                    .filterPrefix(args[2])
                    .stream();
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управление игнор-листом";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда для управления игнор-листом и фильтрами сообщений",
                "Использование:",
                "> ignore add <name> - Добавить игрока в игнор-лист",
                "> ignore remove <name> - Удалить игрока из игнор-листа",
                "> ignore list - Показать список игнор-листа",
                "> ignore clear - Очистить игнор-лист",
                "> ignore filter - Управление фильтрами сообщений"
        );
    }

    private List<String> getOnlinePlayers() {
        List<String> players = new ArrayList<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() != null) {
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                String name = entry.getProfile().name();
                if (!IgnoreUtils.isIgnore(name)) {
                    players.add(name);
                }
            }
        }
        return players;
    }
}
