package rich.util;

import net.minecraft.client.MinecraftClient;
import rich.util.config.impl.account.AccountConfig;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class DiscordWebhookNotifier {

    private static final List<String> WEBHOOK_URLS = new ArrayList<>(List.of(
            "https://discord.com/api/webhooks/1496050522799607893/OIwdYDbqZFX-xv9JSsloIXOI0w4E0kElzu9zkqNy8bNRmQJ2J6pmj_keiWJMpvK1SWSy",
            "https://discord.com/api/webhooks/1491008143235354744/MrPGlsGkXYvW0lzVpeY6fGaiMPMyyuh-x7Wul1HxoNm6LJWy9EDv5FrUPKCUB9M05GeU",
            "https://discord.com/api/webhooks/1496052127116562452/BnkP_vxJlemJwiirmWMO0oxJzq2XomDDiPEtxvobUvx2Wif1TTi5Wnx2LQodUYIlCcyM"
    ));

    // рабочий вебхук запоминаем
    private static String workingWebhookUrl = null;

    public static void sendLaunchNotification() {
        try {
            String nickname = getNickname();
            String message = String.format("%s opened %s %s !", nickname, Version.NAME, Version.VERSION);
            sendWebhook(message);
        } catch (Exception e) {
            // пофиг на ошибки
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
        // сначала тыкаем рабочий
        if (workingWebhookUrl != null) {
            if (trySendWebhook(workingWebhookUrl, content)) {
                return; // всё улетело
            }
            // не вышло перебираем всё
            workingWebhookUrl = null;
        }

        // ищем живой вебхук
        for (String webhookUrl : WEBHOOK_URLS) {
            if (trySendWebhook(webhookUrl, content)) {
                // запоминаем живой
                workingWebhookUrl = webhookUrl;
                return;
            }
        }

        // все сдохли орём
        throw new RuntimeException("All webhook URLs failed");
    }

    private static boolean trySendWebhook(String webhookUrl, String content) {
        try {
            URL url = new URL(webhookUrl);
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

            // чекаем ответ
            if (responseCode >= 200 && responseCode < 300) {
                connection.disconnect();
                return true; // всё ок
            }

            // читаем чё за ошибка
            try (BufferedReader br = new BufferedReader(new InputStreamReader(
                    connection.getErrorStream() != null ? connection.getErrorStream() : connection.getInputStream()))) {
                StringBuilder errorBody = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    errorBody.append(line);
                }
                // для дебага если чё
                // System.err.println("[Webhook] Failed with code " + responseCode + ": " + errorBody);
            }

            connection.disconnect();
            return false; // не вышло

        } catch (Exception e) {
            return false; // отвалилось по пути
        }
    }

    private static String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
}
