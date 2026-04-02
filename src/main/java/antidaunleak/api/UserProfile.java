package antidaunleak.api;

import antidaunleak.api.annotation.Native;

import java.util.HashMap;
import java.util.Map;

public class UserProfile {

    private static final UserProfile instance = new UserProfile();

    public static UserProfile getInstance() {
        return instance;
    }

    private final Map<String, String> cache = new HashMap<>();

    private boolean nativeFailed = false;

    private UserProfile() {
        try {
            cache.put("username", getUsername());
            cache.put("hwid", getHwid());
            cache.put("role", getRole());
            cache.put("uid", getUid());
            cache.put("subTime", getSubsTime());
        } catch (UnsatisfiedLinkError e) {
            nativeFailed = true;
            cache.put("username", "null");
            cache.put("hwid", "null");
            cache.put("role", "null");
            cache.put("uid", "null");
            cache.put("subTime", "null");
        }
    }

    @Native(type = Native.Type.STANDARD)
    private native String getUsername();

    @Native(type = Native.Type.STANDARD)
    private native String getHwid();

    @Native(type = Native.Type.STANDARD)
    private native String getRole();

    @Native(type = Native.Type.STANDARD)
    private native String getUid();

    @Native(type = Native.Type.STANDARD)
    private native String getSubsTime();

    public String profile(String profile) {
        return cache.getOrDefault(profile, "");
    }
}
