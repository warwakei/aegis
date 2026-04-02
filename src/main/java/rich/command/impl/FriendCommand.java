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
import rich.util.repository.friend.FriendUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static rich.command.impl.HelpCommand.getLine;

public class FriendCommand extends Command {

    public FriendCommand() {
        super("friend", "Управление списком друзей", "f", "friends");
    }

    @Override
    public void execute(String label, String[] args) {
        CommandManager manager = CommandManager.getInstance();

        String arg = args.length > 0 ? args[0].toLowerCase(Locale.US) : "list";

        switch (arg) {
            case "add" -> {
                if (args.length < 2) {
                    logDirect("Использование: friend add <name>", Formatting.RED);
                    return;
                }
                String name = args[1];
                if (FriendUtils.isFriend(name)) {
                    logDirect(String.format("Игрок %s уже в списке друзей!", name), Formatting.RED);
                    return;
                }
                FriendUtils.addFriendAndSave(name);
                logDirect(String.format("Игрок %s добавлен в друзья!", name), Formatting.GREEN);
            }
            case "remove", "del", "delete" -> {
                if (args.length < 2) {
                    logDirect("Использование: friend remove <name>", Formatting.RED);
                    return;
                }
                String name = args[1];
                if (!FriendUtils.isFriend(name)) {
                    logDirect(String.format("Игрок %s не найден в списке друзей!", name), Formatting.RED);
                    return;
                }
                FriendUtils.removeFriendAndSave(name);
                logDirect(String.format("Игрок %s удален из друзей!", name), Formatting.GREEN);
            }
            case "clear" -> {
                int count = FriendUtils.size();
                FriendUtils.clearAndSave();
                logDirect(String.format("Список друзей очищен! Удалено: %d", count), Formatting.GREEN);
            }
            case "list" -> {
                int page = 1;
                if (args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {}
                }

                List<String> friends = FriendUtils.getFriendNames();

                if (friends.isEmpty()) {
                    logDirect("Список друзей пуст!", Formatting.RED);
                    return;
                }

                Paginator<String> paginator = new Paginator<>(friends);
                paginator.setPage(page);

                paginator.display(
                        () -> {
                            logDirectRaw(Text.literal(getLine()));
                            logDirect("§f§lСПИСОК ДРУЗЕЙ §7(" + friends.size() + ")");
                            logDirectRaw(Text.literal(getLine()));
                        },
                        friend -> {
                            MutableText nameComponent = Text.literal("  §a● §f" + friend);

                            MutableText hoverText = Text.literal("§7Нажмите чтобы удалить §f" + friend + " §7из друзей");
                            String removeCommand = manager.getPrefix() + "friend remove " + friend;

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
                logDirect("§f§lУПРАВЛЕНИЕ ДРУЗЬЯМИ");
                logDirectRaw(Text.literal(getLine()));
                logDirect("§7> friend add <name> §8- §fДобавить игрока в друзья");
                logDirect("§7> friend remove <name> §8- §fУдалить игрока из друзей");
                logDirect("§7> friend list §8- §fПоказать список друзей");
                logDirect("§7> friend clear §8- §fОчистить список друзей");
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
                        .append(FriendUtils.getFriendNames().toArray(new String[0]))
                        .filterPrefix(args[1])
                        .stream();
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управление списком друзей";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда для управления списком друзей",
                "Использование:",
                "> friend add <name> - Добавить игрока в друзья",
                "> friend remove <name> - Удалить игрока из друзей",
                "> friend list - Показать список друзей",
                "> friend clear - Очистить список друзей"
        );
    }

    private List<String> getOnlinePlayers() {
        List<String> players = new ArrayList<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() != null) {
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                String name = entry.getProfile().name();
                if (!FriendUtils.isFriend(name)) {
                    players.add(name);
                }
            }
        }
        return players;
    }
}