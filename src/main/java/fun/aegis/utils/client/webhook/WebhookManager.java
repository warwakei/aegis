package fun.aegis.utils.client.webhook;

import antidaunleak.api.annotation.Native;
import com.google.gson.JsonObject;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class WebhookManager {

    @Native(type = Native.Type.VMProtectBeginVirtualization)
    public static void sendClientStartWebhook(String username, String uid, String role, String discordName, String discordAvatar, String clientName) {
        new Thread(() -> {
            try {
                JsonObject embed = new JsonObject();
                embed.addProperty("title", "Client Start");
                embed.addProperty("color", 5814783);

                String description = String.format(
                        "**Username:** %s\n**UID:** %s\n**Role:** %s\n**Discord:** %s\n**Client:** %s\n**Time:** %s",
                        username,
                        uid,
                        role,
                        discordName,
                        clientName,
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
                );
                embed.addProperty("description", description);

                if (discordAvatar != null && !discordAvatar.isEmpty() && !discordAvatar.equals("Not Connected")) {
                    JsonObject thumbnail = new JsonObject();
                    thumbnail.addProperty("url", discordAvatar);
                    embed.add("thumbnail", thumbnail);
                }

                JsonObject footer = new JsonObject();
                footer.addProperty("text", "Aegis Client Logger");
                embed.add("footer", footer);

                JsonObject payload = new JsonObject();
                payload.addProperty("username", "Aegis Client");
                payload.addProperty("avatar_url", "https://i.postimg.cc/nznMWbhM/0001-0250.gif");

                com.google.gson.JsonArray embeds = new com.google.gson.JsonArray();
                embeds.add(embed);
                payload.add("embeds", embeds);

                URL url = new URL("https://ptb.discord.com/api/webhooks/1432792379760312320/qy80T0kWwPJ1NQXu1Qbxwoby4hcW_1Y19CwJfaI7zCm2omTwha8YoxL7fqO3bLgJOqdX");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Content-Type", "application/json");
                connection.setDoOutput(true);

                try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = payload.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = connection.getResponseCode();
                connection.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
