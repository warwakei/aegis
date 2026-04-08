package rich.netpanel.loggers;

/**
 * Moderation logger — tracks chat moderation events, player warnings, mutes, kicks.
 * Designed for server moderators who need a clean audit trail.
 */
public class ModerationLogger {

    private static final LogBuffer BUFFER = new LogBuffer(2000);

    public static void logWarn(String player, String reason) {
        BUFFER.add("WARN", "[MOD] Warning issued to " + player + " | reason=" + reason);
    }

    public static void logMute(String player, String duration, String reason) {
        BUFFER.add("MUTE", "[MOD] Muted " + player + " | duration=" + duration + " | reason=" + reason);
    }

    public static void logKick(String player, String reason) {
        BUFFER.add("KICK", "[MOD] Kicked " + player + " | reason=" + reason);
    }

    public static void logBan(String player, String duration, String reason) {
        BUFFER.add("BAN", "[MOD] Banned " + player + " | duration=" + duration + " | reason=" + reason);
    }

    public static void logChatClear(String clearedBy) {
        BUFFER.add("INFO", "[MOD] Chat cleared by " + clearedBy);
    }

    public static void logSlowMode(int delay, String setBy) {
        BUFFER.add("INFO", "[MOD] Slowmode set to " + delay + "s by " + setBy);
    }

    public static void logInfo(String msg) {
        BUFFER.add("INFO", "[MOD] " + msg);
    }

    public static void logPlayerActivity(String player, String action) {
        BUFFER.add("ACTIVITY", "[MOD] " + player + " — " + action);
    }

    public static LogBuffer getBuffer() {
        return BUFFER;
    }
}
