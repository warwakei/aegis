package rich.screens.account;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.PlayerSkinProvider;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

@SuppressWarnings("unchecked")
public class SkinManager {

    private static final Map<String, Identifier> SKIN_CACHE = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> LOADING = new ConcurrentHashMap<>();
    private static final Identifier STEVE_SKIN = Identifier.of("minecraft", "textures/entity/player/wide/steve.png");
    private static final Identifier ALEX_SKIN = Identifier.of("minecraft", "textures/entity/player/wide/alex.png");

    private static final Executor EXECUTOR = Executors.newFixedThreadPool(3, r -> {
        Thread t = new Thread(r, "SkinLoader");
        t.setDaemon(true);
        return t;
    });

    public static Identifier getSkin(String playerName) {
        if (playerName == null || playerName.isEmpty()) {
            return STEVE_SKIN;
        }

        String key = playerName.toLowerCase();
        Identifier cached = SKIN_CACHE.get(key);
        if (cached != null) {
            return cached;
        }

        if (!LOADING.containsKey(key)) {
            LOADING.put(key, true);
            loadSkinAsync(playerName);
        }

        return getDefaultSkin(playerName);
    }

    private static Identifier getDefaultSkin(String playerName) {
        UUID offlineUUID = UUID.nameUUIDFromBytes(("OfflinePlayer:" + playerName).getBytes());
        return (offlineUUID.hashCode() & 1) == 0 ? STEVE_SKIN : ALEX_SKIN;
    }

    private static void loadSkinAsync(String playerName) {
        String key = playerName.toLowerCase();

        CompletableFuture.runAsync(() -> {
            try {
                UUID uuid = fetchUUID(playerName);
                if (uuid == null) {
                    LOADING.remove(key);
                    return;
                }

                MinecraftClient client = MinecraftClient.getInstance();
                if (client == null) {
                    LOADING.remove(key);
                    return;
                }

                GameProfile profile = new GameProfile(uuid, playerName);
                PlayerSkinProvider provider = client.getSkinProvider();

                CompletableFuture<Optional<SkinTextures>> skinFuture = provider.fetchSkinTextures(profile);
                Optional<SkinTextures> texturesOpt = skinFuture.join();

                if (texturesOpt.isPresent()) {
                    SkinTextures textures = texturesOpt.get();
                    if (textures.body() != null) {
                        Identifier skinId = textures.body().texturePath();
                        if (skinId != null) {
                            SKIN_CACHE.put(key, skinId);
                        }
                    }
                }
            } catch (Exception ignored) {
            } finally {
                LOADING.remove(key);
            }
        }, EXECUTOR);
    }

    private static UUID fetchUUID(String playerName) {
        try {
            URL url = new URL("https://api.mojang.com/users/profiles/minecraft/" + playerName);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(5000);
            connection.setReadTimeout(5000);
            connection.setRequestProperty("User-Agent", "Mozilla/5.0");

            int responseCode = connection.getResponseCode();
            if (responseCode != 200) {
                connection.disconnect();
                return null;
            }

            try (InputStreamReader reader = new InputStreamReader(connection.getInputStream())) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                if (json.has("id")) {
                    String id = json.get("id").getAsString();
                    connection.disconnect();
                    return parseUUID(id);
                }
            }
            connection.disconnect();
        } catch (Exception ignored) {
        }
        return null;
    }

    private static UUID parseUUID(String id) {
        try {
            if (id.length() == 32) {
                id = id.substring(0, 8) + "-" + id.substring(8, 12) + "-" +
                        id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20);
            }
            return UUID.fromString(id);
        } catch (Exception e) {
            return null;
        }
    }

    public static void clearCache() {
        SKIN_CACHE.clear();
        LOADING.clear();
    }

    public static void removeSkin(String playerName) {
        if (playerName != null) {
            SKIN_CACHE.remove(playerName.toLowerCase());
            LOADING.remove(playerName.toLowerCase());
        }
    }

    public static void reloadSkin(String playerName) {
        removeSkin(playerName);
        getSkin(playerName);
    }
}