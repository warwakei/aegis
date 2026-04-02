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
                logDirectRaw(Text.literal(getLine()));
                logDirect("§f§lУПРАВЛЕНИЕ ИГНОР-ЛИСТОМ");
                logDirectRaw(Text.literal(getLine()));
                logDirect("§7> ignore add <name> §8- §fДобавить игрока в игнор-лист");
                logDirect("§7> ignore remove <name> §8- §fУдалить игрока из игнор-листа");
                logDirect("§7> ignore list §8- §fПоказать список игнор-листа");
                logDirect("§7> ignore clear §8- §fОчистить игнор-лист");
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
                "Команда для управления игнор-листом",
                "Использование:",
                "> ignore add <name> - Добавить игрока в игнор-лист",
                "> ignore remove <name> - Удалить игрока из игнор-листа",
                "> ignore list - Показать список игнор-листа",
                "> ignore clear - Очистить игнор-лист"
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
