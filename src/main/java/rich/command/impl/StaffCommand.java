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
import rich.util.repository.staff.StaffUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static rich.command.impl.HelpCommand.getLine;

public class StaffCommand extends Command {

    public StaffCommand() {
        super("staff", "Управление списком персонала");
    }

    @Override
    public void execute(String label, String[] args) {
        CommandManager manager = CommandManager.getInstance();

        String action = args.length > 0 ? args[0].toLowerCase(Locale.US) : "list";

        switch (action) {
            case "add" -> {
                if (args.length < 2) {
                    logDirect("Использование: staff add <name>", Formatting.RED);
                    return;
                }
                String name = args[1];
                if (StaffUtils.isStaff(name)) {
                    logDirect(String.format("Игрок %s уже в списке персонала!", name), Formatting.RED);
                    return;
                }
                StaffUtils.addStaffAndSave(name);
                logDirect(String.format("Игрок %s добавлен в персонал!", name), Formatting.GREEN);
            }
            case "remove", "del", "delete" -> {
                if (args.length < 2) {
                    logDirect("Использование: staff remove <name>", Formatting.RED);
                    return;
                }
                String name = args[1];
                if (!StaffUtils.isStaff(name)) {
                    logDirect(String.format("Игрок %s не найден в списке персонала!", name), Formatting.RED);
                    return;
                }
                StaffUtils.removeStaffAndSave(name);
                logDirect(String.format("Игрок %s удален из персонала!", name), Formatting.GREEN);
            }
            case "clear" -> {
                int count = StaffUtils.size();
                StaffUtils.clearAndSave();
                logDirect(String.format("Список персонала очищен! Удалено: %d", count), Formatting.GREEN);
            }
            case "list" -> {
                int page = 1;
                if (args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {}
                }

                List<String> staff = StaffUtils.getStaffNames();

                if (staff.isEmpty()) {
                    logDirect("Список персонала пуст!", Formatting.RED);
                    return;
                }

                Paginator<String> paginator = new Paginator<>(staff);
                paginator.setPage(page);

                paginator.display(
                        () -> {
                            logDirectRaw(Text.literal(getLine()));
                            logDirect("§f§lСПИСОК ПЕРСОНАЛА §7(" + staff.size() + ")");
                            logDirectRaw(Text.literal(getLine()));
                        },
                        staffName -> {
                            MutableText nameComponent = Text.literal("  §c● §f" + staffName);

                            MutableText hoverText = Text.literal("§7Нажмите чтобы удалить §f" + staffName + " §7из персонала");
                            String removeCommand = manager.getPrefix() + "staff remove " + staffName;

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
                logDirect("§f§lУПРАВЛЕНИЕ ПЕРСОНАЛОМ");
                logDirectRaw(Text.literal(getLine()));
                logDirect("§7> staff add <name> §8- §fДобавить игрока в персонал");
                logDirect("§7> staff remove <name> §8- §fУдалить игрока из персонала");
                logDirect("§7> staff list §8- §fПоказать список персонала");
                logDirect("§7> staff clear §8- §fОчистить список персонала");
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
                        .append(StaffUtils.getStaffNames().toArray(new String[0]))
                        .filterPrefix(args[1])
                        .stream();
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управление списком персонала";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда для управления списком персонала сервера",
                "Использование:",
                "> staff add <name> - Добавить игрока в персонал",
                "> staff remove <name> - Удалить игрока из персонала",
                "> staff list - Показать список персонала",
                "> staff clear - Очистить список персонала"
        );
    }

    private List<String> getOnlinePlayers() {
        List<String> players = new ArrayList<>();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() != null) {
            for (PlayerListEntry entry : mc.getNetworkHandler().getPlayerList()) {
                String name = entry.getProfile().name();
                if (!StaffUtils.isStaff(name)) {
                    players.add(name);
                }
            }
        }
        return players;
    }
}