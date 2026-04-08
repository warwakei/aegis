package rich.netpanel.loggers;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Anticheat monitor — tracks violation flags from console and logs them
 * in a structured format for admins to review.
 */
public class AnticheatMonitor {

    private static final LogBuffer BUFFER = new LogBuffer(1000);
    private static final AtomicInteger totalFlags = new AtomicInteger(0);
    private static volatile String lastAnticheatName = "Unknown";

    public static void logFlag(String player, String check, double vl, String detail) {
        totalFlags.incrementAndGet();
        BUFFER.add("FLAG", "[AC] " + player + " | check=" + check + " vl=" + String.format("%.1f", vl) + " | " + detail);
    }

    public static void logViolation(String anticheat, String player, String type, String detail) {
        totalFlags.incrementAndGet();
        lastAnticheatName = anticheat;
        BUFFER.add("VIOLATION", "[AC/" + anticheat + "] " + player + " | " + type + " | " + detail);
    }

    public static void logKick(String player, String anticheat, String reason) {
        BUFFER.add("KICK", "[AC] " + player + " kicked by " + anticheat + " | " + reason);
    }

    public static void logInfo(String msg) {
        BUFFER.add("INFO", "[AC] " + msg);
    }

    public static int getTotalFlags() {
        return totalFlags.get();
    }

    public static String getLastAnticheatName() {
        return lastAnticheatName;
    }

    public static void reset() {
        totalFlags.set(0);
        BUFFER.clear();
    }

    public static LogBuffer getBuffer() {
        return BUFFER;
    }
}
