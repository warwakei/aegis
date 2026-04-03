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
     */
    public static void logReceived(String sender, String message) {
        BUFFER.add("RECV", "[" + sender + "] " + message);
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
