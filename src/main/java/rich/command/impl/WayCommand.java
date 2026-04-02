package rich.command.impl;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.Formatting;
import rich.IMinecraft;
import rich.command.Command;
import rich.command.CommandManager;
import rich.command.helpers.Paginator;
import rich.command.helpers.TabCompleteHelper;
import rich.util.repository.way.Way;
import rich.util.repository.way.WayRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static rich.command.impl.HelpCommand.getLine;

public class WayCommand extends Command implements IMinecraft {

    public WayCommand() {
        super("way", "Управление точками на карте", "waypoint", "wp");
    }

    @Override
    public void execute(String label, String[] args) {
        CommandManager manager = CommandManager.getInstance();
        WayRepository repository = WayRepository.getInstance();

        if (mc.player == null) {
            logDirect("Вы должны быть в игре!", Formatting.RED);
            return;
        }

        String action = args.length > 0 ? args[0].toLowerCase(Locale.US) : "list";

        switch (action) {
            case "add" -> {
                if (args.length < 2) {
                    logDirect("Использование: way add <name> [x] [y] [z]", Formatting.RED);
                    return;
                }

                String name = args[1];
                BlockPos pos;

                if (args.length >= 5) {
                    try {
                        int x = Integer.parseInt(args[2]);
                        int y = Integer.parseInt(args[3]);
                        int z = Integer.parseInt(args[4]);
                        pos = new BlockPos(x, y, z);
                    } catch (NumberFormatException e) {
                        logDirect("Неверные координаты!", Formatting.RED);
                        return;
                    }
                } else {
                    pos = mc.player.getBlockPos();
                }

                String server = repository.getCurrentServer();
                if (server.isEmpty()) {
                    logDirect("Не удалось определить сервер!", Formatting.RED);
                    return;
                }

                if (repository.hasWay(name)) {
                    logDirect(String.format("Точка с именем %s уже существует!", name), Formatting.RED);
                    return;
                }

                repository.addWayAndSave(name, pos, server);

                logDirect(String.format("§aТочка §f%s §aдобавлена на координатах §f%d %d %d",
                        name, pos.getX(), pos.getY(), pos.getZ()), Formatting.GREEN);
            }
            case "remove", "del", "delete" -> {
                if (args.length < 2) {
                    logDirect("Использование: way remove <name>", Formatting.RED);
                    return;
                }

                String name = args[1];

                if (!repository.hasWay(name)) {
                    logDirect(String.format("Точка %s не найдена!", name), Formatting.RED);
                    return;
                }

                repository.deleteWayAndSave(name);
                logDirect(String.format("Точка %s удалена!", name), Formatting.GREEN);
            }
            case "clear" -> {
                String server = repository.getCurrentServer();
                int count = 0;

                List<Way> toRemove = repository.getWayList().stream()
                        .filter(way -> way.server().equalsIgnoreCase(server))
                        .toList();

                for (Way way : toRemove) {
                    repository.deleteWay(way.name());
                    count++;
                }

                if (count > 0) {
                    rich.util.config.impl.way.WayConfig.getInstance().save();
                }

                logDirect(String.format("Удалено точек для этого сервера: %d", count), Formatting.GREEN);
            }
            case "clearall" -> {
                int count = repository.size();
                repository.clearListAndSave();
                logDirect(String.format("Все точки удалены! Удалено: %d", count), Formatting.GREEN);
            }
            case "list" -> {
                int page = 1;
                if (args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {}
                }

                String server = repository.getCurrentServer();
                List<Way> serverWays = repository.getWayList().stream()
                        .filter(way -> way.server().equalsIgnoreCase(server))
                        .toList();

                if (serverWays.isEmpty()) {
                    logDirect("Нет точек для этого сервера!", Formatting.RED);
                    return;
                }

                Paginator<Way> paginator = new Paginator<>(serverWays);
                paginator.setPage(page);

                paginator.display(
                        () -> {
                            logDirectRaw(Text.literal(getLine()));
                            logDirect("§f§lТОЧКИ §7(" + serverWays.size() + ")");
                            logDirectRaw(Text.literal(getLine()));
                        },
                        way -> {
                            String wayName = way.name();
                            BlockPos pos = way.pos();
                            double distance = mc.player.getEntityPos().distanceTo(pos.toCenterPos());

                            MutableText component = Text.literal("  §d● §f" + wayName)
                                    .append(Text.literal(String.format(" §8[§7%d %d %d§8]",
                                            pos.getX(), pos.getY(), pos.getZ())))
                                    .append(Text.literal(String.format(" §8(§7%.1fm§8)", distance)));

                            MutableText hoverText = Text.literal("§7Нажмите чтобы удалить точку");
                            String removeCommand = manager.getPrefix() + "way remove " + wayName;

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
                logDirect("§f§lУПРАВЛЕНИЕ ТОЧКАМИ");
                logDirectRaw(Text.literal(getLine()));
                logDirect("§7> way add <name> [x y z] §8- §fДобавить точку");
                logDirect("§7> way remove <name> §8- §fУдалить точку");
                logDirect("§7> way list §8- §fПоказать список точек");
                logDirect("§7> way clear §8- §fУдалить точки для этого сервера");
                logDirect("§7> way clearall §8- §fУдалить все точки");
                logDirectRaw(Text.literal(getLine()));
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, String[] args) {
        WayRepository repository = WayRepository.getInstance();

        if (args.length == 1) {
            return new TabCompleteHelper()
                    .append("add", "remove", "list", "clear", "clearall")
                    .sortAlphabetically()
                    .filterPrefix(args[0])
                    .stream();
        }
        if (args.length == 2) {
            String action = args[0].toLowerCase();
            if (action.equals("remove") || action.equals("del") || action.equals("delete")) {
                String server = repository.getCurrentServer();
                return new TabCompleteHelper()
                        .append(repository.getWayNamesForServer(server).toArray(new String[0]))
                        .filterPrefix(args[1])
                        .stream();
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управление точками на карте";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда для управления waypoints (точками на карте)",
                "Точки отображаются на экране с расстоянием до них",
                "Использование:",
                "> way add <name> [x y z] - Добавить точку (без координат - текущая позиция)",
                "> way remove <name> - Удалить точку",
                "> way list - Показать список точек для текущего сервера",
                "> way clear - Удалить все точки для текущего сервера",
                "> way clearall - Удалить все точки"
        );
    }
}