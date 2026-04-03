package rich.netpanel.loggers;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

/**
 * Bridge for reading and sending chat messages from the web panel.
 */
public class ChatBridge {

    private static final LogBuffer BUFFER = new LogBuffer(300);
    private static final Gson GSON = new Gson();

    /**
     * Send a chat message to the server.
     */
    public static void sendChatMessage(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.getNetworkHandler() != null) {
            if (message.startsWith("/")) {
                mc.getNetworkHandler().sendChatCommand(message.substring(1));
                BUFFER.add("CMD", message);
            } else {
                mc.getNetworkHandler().sendChatMessage(message);
                BUFFER.add("SEND", message);
            }
        }
    }

    /**
     * Log an incoming chat message with full formatting (colors).
     */
    public static void logReceived(String sender, String message) {
        BUFFER.add("RECV", "[" + sender + "] " + message);
    }

    /**
     * Log an incoming chat message from a Text object (preserves colors).
     * Serializes the Text to JSON for the frontend to render with colors.
     */
    public static void logReceivedText(String sender, Text text) {
        if (text == null) return;
        try {
            // Try to serialize Text to JSON via reflection (API varies by version)
            String json = textToJson(text);
            BUFFER.add("RECV", "[JSON][" + sender + "] " + json);
        } catch (Exception e) {
            // Fallback to plain string
            BUFFER.add("RECV", "[" + sender + "] " + text.getString());
        }
    }

    private static String textToJson(Text text) {
        // Try TextCodecs or Serializer via reflection
        try {
            // 1.21.x: Text.Serialization.toJsonTree(Text, RegistryWrapper)
            // Fallback: use the text's own codec via reflection
            java.lang.reflect.Method toJson = text.getClass().getMethod("toJson");
            toJson.setAccessible(true);
            Object result = toJson.invoke(text);
            if (result instanceof JsonElement je) {
                return GSON.toJson(je);
            }
            if (result != null) {
                return GSON.toJson(result);
            }
        } catch (Exception ignored) {}

        // Last resort: try Text.Serializer static method
        try {
            Class<?> serializerClass = Class.forName("net.minecraft.text.Text$Serializer");
            java.lang.reflect.Method toJson = serializerClass.getMethod("toJson", Text.class);
            toJson.setAccessible(true);
            Object result = toJson.invoke(null, text);
            if (result instanceof String s) return s;
        } catch (Exception ignored) {}

        // Absolute fallback
        return text.getString();
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
