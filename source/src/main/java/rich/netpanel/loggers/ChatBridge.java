package rich.netpanel.loggers;

import net.minecraft.client.MinecraftClient;

/**
 * чат с панелькой дружим тут
 */
public class ChatBridge {

    private static final LogBuffer BUFFER = new LogBuffer(10000);

    /**
     * чат на сервак шлём
     */
    public static void sendChatMessage(String message) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && mc.getNetworkHandler() != null) {
            if (message.startsWith("/")) {
                mc.getNetworkHandler().sendChatCommand(message.substring(1));
                // команды как цмд пишем
                BUFFER.add("CMD", message);
            } else {
                mc.getNetworkHandler().sendChatMessage(message);
                // исходящий чат не логаем сам прилетит
            }
        }
    }

    /**
     * входящий чат в панельку кладём
     */
    public static void logReceived(String sender, String message) {
        String cleanSender = cleanSenderName(sender);
        BUFFER.add("RECV", "[" + cleanSender + "] " + message);
    }

    /**
     * префикс головы у ника чистим
     */
    private static String cleanSenderName(String sender) {
        if (sender == null) return "";
        // паттерн со скобками ищем
        int bracketStart = sender.indexOf('[');
        int bracketEnd = sender.indexOf(']');
        if (bracketStart >= 0 && bracketEnd > bracketStart && bracketEnd + 1 < sender.length()) {
            String inside = sender.substring(bracketStart + 1, bracketEnd);
            String after = sender.substring(bracketEnd + 1);
            // слово голова внутри чекаем
            if (inside.toLowerCase().contains("head")) {
                // чистый ник назад отдаём
                return after;
            }
        }
        return sender;
    }

    /**
     * системный чат логаем
     */
    public static void logSystem(String message) {
        BUFFER.add("SYS", message);
    }

    public static LogBuffer getBuffer() {
        return BUFFER;
    }
}
