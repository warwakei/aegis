package rich.netpanel.loggers;

import net.minecraft.client.MinecraftClient;

/**
 * Bridge for reading and sending chat messages from the web panel.
 */
public class ChatBridge {

    private static final LogBuffer BUFFER = new LogBuffer(800);

    /**
     * Send a chat message to the server.
     * Does NOT log outgoing messages to the panel — they will appear naturally via console [CHAT] capture.
     */
    public static void sendChatMessage(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.getNetworkHandler() != null) {
            if (message.startsWith("/")) {
                mc.getNetworkHandler().sendChatCommand(message.substring(1));
                // Commands are still logged as CMD
                BUFFER.add("CMD", message);
            } else {
                mc.getNetworkHandler().sendChatMessage(message);
                // Don't log outgoing chat messages — they'll appear via console capture
            }
        }
    }

    /**
     * Log an incoming chat message.
     * Strips the [nickname head]nickname pattern from the sender name.
     * Example: "[AsTRe1d head]AsTRe1d" → "AsTRe1d"
     */
    public static void logReceived(String sender, String message) {
        String cleanSender = cleanSenderName(sender);
        BUFFER.add("RECV", "[" + cleanSender + "] " + message);
    }

    /**
     * Removes the [nickname head] prefix from sender name.
     * Handles pattern: "[nickname head]nickname" → "nickname"
     */
    private static String cleanSenderName(String sender) {
        if (sender == null) return "";
        // Match pattern: [something head]nickname
        int bracketStart = sender.indexOf('[');
        int bracketEnd = sender.indexOf(']');
        if (bracketStart >= 0 && bracketEnd > bracketStart && bracketEnd + 1 < sender.length()) {
            String inside = sender.substring(bracketStart + 1, bracketEnd);
            String after = sender.substring(bracketEnd + 1);
            // Check if inside contains "head" keyword
            if (inside.toLowerCase().contains("head")) {
                // The name after the bracket should match — return just the clean name
                return after;
            }
        }
        return sender;
    }

    /**
     * Log a system chat message.
     */
    public static void logSystem(String message) {
        BUFFER.add("SYS", message);
    }

    public static LogBuffer getBuffer() {
        return BUFFER;
    }
}
