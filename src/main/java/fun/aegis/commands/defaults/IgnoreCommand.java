package fun.aegis.commands.defaults;

import fun.aegis.Aegis;
import fun.aegis.utils.client.managers.api.command.Command;
import fun.aegis.utils.client.managers.api.command.argument.IArgConsumer;
import fun.aegis.utils.client.managers.api.command.exception.CommandException;
import fun.aegis.utils.client.managers.api.command.helpers.TabCompleteHelper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static fun.aegis.utils.client.managers.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IgnoreCommand extends Command {
    private static final Map<String, Long> IGNORED_PLAYERS = new ConcurrentHashMap<>();

    public IgnoreCommand(Aegis main) {
        super("ignore");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String action = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
        switch (action) {
            case "add":
                handleAdd(args);
                break;
            case "remove":
                handleRemove(args);
                break;
            case "list":
                handleList(args);
                break;
            case "clear":
                handleClear(args);
                break;
            case "time":
                handleTime(args);
                break;
            default:
                throw new CommandException("Неизвестная команда. Используйте: add, remove, list, clear, time");
        }
    }

    private void handleAdd(IArgConsumer args) throws CommandException {
        args.requireMin(1);
        String nickname = args.getString();
        IGNORED_PLAYERS.put(nickname.toLowerCase(), Long.MAX_VALUE);
        logDirect(Formatting.GREEN + "Игрок " + Formatting.RED + nickname + Formatting.GREEN + " добавлен в игнор");
    }

    private void handleRemove(IArgConsumer args) throws CommandException {
        args.requireMin(1);
        String nickname = args.getString();
        if (IGNORED_PLAYERS.remove(nickname.toLowerCase()) != null) {
            logDirect(Formatting.GREEN + "Игрок " + Formatting.RED + nickname + Formatting.GREEN + " удален из игнора");
        } else {
            logDirect(Formatting.RED + "Игрок " + nickname + " не найден в игноре");
        }
    }

    private void handleList(IArgConsumer args) throws CommandException {
        args.requireMax(0);
        if (IGNORED_PLAYERS.isEmpty()) {
            logDirect(Formatting.GRAY + "Список игнора пуст");
            return;
        }

        logDirect(Formatting.GRAY + "Игнорируемые игроки:");
        IGNORED_PLAYERS.forEach((nick, expireTime) -> {
            if (expireTime == Long.MAX_VALUE) {
                logDirect(Formatting.WHITE + "  - " + nick + Formatting.GRAY + " (навсегда)");
            } else {
                long remainingMs = expireTime - System.currentTimeMillis();
                if (remainingMs > 0) {
                    String timeStr = formatTime(remainingMs);
                    logDirect(Formatting.WHITE + "  - " + nick + Formatting.GRAY + " (" + timeStr + ")");
                }
            }
        });
    }

    private void handleClear(IArgConsumer args) throws CommandException {
        args.requireMax(0);
        IGNORED_PLAYERS.clear();
        logDirect(Formatting.GREEN + "Список игнора очищен");
    }

    private void handleTime(IArgConsumer args) throws CommandException {
        args.requireMin(3);
        String subAction = args.getString().toLowerCase(Locale.US);

        if (!subAction.equals("add")) {
            throw new CommandException("Используйте: ignore time add <nickname> <time>");
        }

        String nickname = args.getString();
        String timeStr = args.getString();

        long durationMs = parseTime(timeStr);
        if (durationMs <= 0) {
            throw new CommandException("Неверный формат времени. Используйте: 1m, 1h, 1d, 1w");
        }

        long expireTime = System.currentTimeMillis() + durationMs;
        IGNORED_PLAYERS.put(nickname.toLowerCase(), expireTime);
        logDirect(Formatting.GREEN + "Игрок " + Formatting.RED + nickname + Formatting.GREEN + " добавлен в игнор на " + formatTime(durationMs));
    }

    private long parseTime(String timeStr) {
        timeStr = timeStr.toLowerCase();
        long multiplier = 0;

        if (timeStr.endsWith("m")) {
            multiplier = 60 * 1000;
            timeStr = timeStr.substring(0, timeStr.length() - 1);
        } else if (timeStr.endsWith("h")) {
            multiplier = 60 * 60 * 1000;
            timeStr = timeStr.substring(0, timeStr.length() - 1);
        } else if (timeStr.endsWith("d")) {
            multiplier = 24 * 60 * 60 * 1000;
            timeStr = timeStr.substring(0, timeStr.length() - 1);
        } else if (timeStr.endsWith("w")) {
            multiplier = 7 * 24 * 60 * 60 * 1000;
            timeStr = timeStr.substring(0, timeStr.length() - 1);
        } else {
            return -1;
        }

        try {
            long value = Long.parseLong(timeStr);
            return value * multiplier;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private String formatTime(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        long weeks = days / 7;

        if (weeks > 0) {
            return weeks + "w " + (days % 7) + "d";
        } else if (days > 0) {
            return days + "d " + (hours % 24) + "h";
        } else if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        } else if (minutes > 0) {
            return minutes + "m " + (seconds % 60) + "s";
        } else {
            return seconds + "s";
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper()
                    .sortAlphabetically()
                    .prepend("add", "remove", "list", "clear", "time")
                    .filterPrefix(args.getString())
                    .stream();
        } else {
            String arg = args.getString();
            if (arg.equalsIgnoreCase("time")) {
                if (args.hasExactly(1)) {
                    return Stream.of("add").filter(s -> {
                        try {
                            return s.startsWith(args.getString().toLowerCase(Locale.US));
                        } catch (Exception e) {
                            return false;
                        }
                    });
                } else if (args.hasExactly(3)) {
                    return Stream.of("1m", "5m", "10m", "30m", "1h", "2h", "1d", "1w")
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
        return "Управление списком игнорируемых игроков.";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Эта команда позволяет управлять списком игнорируемых игроков.",
                "",
                "Использование:",
                "> ignore add <nickname> - Добавляет игрока в игнор навсегда.",
                "> ignore remove <nickname> - Удаляет игрока из игнора.",
                "> ignore list - Показывает список игнорируемых игроков.",
                "> ignore clear - Очищает весь список игнора.",
                "> ignore time add <nickname> <time> - Добавляет игрока в игнор на время (1m, 1h, 1d, 1w)."
        );
    }

    public static boolean isIgnored(String nickname) {
        Long expireTime = IGNORED_PLAYERS.get(nickname.toLowerCase());
        if (expireTime == null) {
            return false;
        }

        if (expireTime == Long.MAX_VALUE) {
            return true;
        }

        if (System.currentTimeMillis() > expireTime) {
            IGNORED_PLAYERS.remove(nickname.toLowerCase());
            return false;
        }

        return true;
    }

    public static Map<String, Long> getIgnoredPlayers() {
        return new HashMap<>(IGNORED_PLAYERS);
    }
}
