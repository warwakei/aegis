package rich.util;

import net.minecraft.client.MinecraftClient;
import rich.util.config.impl.account.AccountConfig;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DiscordWebhookNotifier {

    private static final String WEBHOOK_URL = "https://discord.com/api/webhooks/1489698970665222334/Y8LccMMT1tS-w5OcAjmN8HgE03fhvT1RqwOZjBBaZf_WXm0DpHwXSLHEEilMLWrwCHHA";

    public static void sendLaunchNotification() {
        try {
            String nickname = getNickname();
            String message = String.format("%s opened %s %s !", nickname, Version.NAME, Version.VERSION);
            sendWebhook(message);
        } catch (Exception e) {
            // Игнорируем ошибки отправки webhook
        }
    }

    private static String getNickname() {
        try {
            AccountConfig accountConfig = AccountConfig.getInstance();
            if (accountConfig != null) {
                accountConfig.load();
                String activeName = accountConfig.getActiveAccountName();
                if (activeName != null && !activeName.isEmpty()) {
                    return activeName;
                }
            }
        } catch (Exception ignored) {}

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc != null && mc.getSession() != null) {
            String username = mc.getSession().getUsername();
            if (username != null && !username.isEmpty()) {
                return username;
            }
        }

        return System.getProperty("user.name", "Unknown");
    }

    private static void sendWebhook(String content) throws Exception {
        URL url = new URL(WEBHOOK_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setDoOutput(true);
        connection.setConnectTimeout(5000);
        connection.setReadTimeout(5000);
        
        String jsonPayload = "{\"content\":\"" + escapeJson(content) + "\"}";
        
        try (OutputStream os = connection.getOutputStream()) {
            os.write(jsonPayload.getBytes(StandardCharsets.UTF_8));
        }
        
        int responseCode = connection.getResponseCode();
        connection.disconnect();
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
