package antidaunleak.api;

import net.minecraft.client.MinecraftClient;

import java.util.HashMap;
import java.util.Map;

public class UserProfile {

    private static final UserProfile instance = new UserProfile();

    public static UserProfile getInstance() {
        return instance;
    }

    private final Map<String, String> cache = new HashMap<>();

    public Map<String, String> getCache() {
        return cache;
    }

    private UserProfile() {
        cache.put("username", getPlayerName());
        cache.put("hwid", "Beta");
        cache.put("role", "User");
        cache.put("uid", "Beta");
        cache.put("subTime", "unlimited");
    }

    private String getPlayerName() {
        try {
            if (MinecraftClient.getInstance() != null && MinecraftClient.getInstance().getSession() != null) {
                return MinecraftClient.getInstance().getSession().getUsername();
            }
        } catch (Exception ignored) {}
        return "Player";
    }

    public String profile(String key) {
        if ("username".equals(key)) {
            String name = getPlayerName();
            cache.put("username", name);
            return name;
        }
        return cache.get(key);
    }
}
