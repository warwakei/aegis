package rich.util.config.impl.proxy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import lombok.Setter;
import rich.util.config.impl.consolelogger.Logger;
import rich.util.proxy.Proxy;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProxyConfig {
    private static ProxyConfig instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;

    @Getter @Setter
    private boolean proxyEnabled = false;

    @Getter @Setter
    private Proxy defaultProxy = new Proxy();

    @Getter @Setter
    private Proxy lastUsedProxy = new Proxy();

    private ProxyConfig() {
        Path configDir = Paths.get("Rich", "configs", "proxy");
        try {
            Files.createDirectories(configDir);
        } catch (IOException ignored) {}
        configPath = configDir.resolve("proxy.json");
    }

    public static ProxyConfig getInstance() {
        if (instance == null) {
            instance = new ProxyConfig();
        }
        return instance;
    }

    public void save() {
        try {
            JsonObject root = new JsonObject();
            root.addProperty("proxyEnabled", proxyEnabled);

            JsonObject defaultProxyJson = new JsonObject();
            defaultProxyJson.addProperty("ipPort", defaultProxy.ipPort);
            defaultProxyJson.addProperty("type", defaultProxy.type.name());
            defaultProxyJson.addProperty("username", defaultProxy.username);
            defaultProxyJson.addProperty("password", defaultProxy.password);
            root.add("defaultProxy", defaultProxyJson);

            JsonObject lastUsedProxyJson = new JsonObject();
            lastUsedProxyJson.addProperty("ipPort", lastUsedProxy.ipPort);
            lastUsedProxyJson.addProperty("type", lastUsedProxy.type.name());
            lastUsedProxyJson.addProperty("username", lastUsedProxy.username);
            lastUsedProxyJson.addProperty("password", lastUsedProxy.password);
            root.add("lastUsedProxy", lastUsedProxyJson);

            Files.writeString(configPath, gson.toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.error("ProxyConfig: Save failed! " + e.getMessage());
        }
    }

    public void load() {
        try {
            if (!Files.exists(configPath)) {
                save();
                return;
            }

            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            if (json.isEmpty()) {
                return;
            }

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            if (root.has("proxyEnabled")) {
                proxyEnabled = root.get("proxyEnabled").getAsBoolean();
            }

            if (root.has("defaultProxy")) {
                defaultProxy = parseProxy(root.getAsJsonObject("defaultProxy"));
            }

            if (root.has("lastUsedProxy")) {
                lastUsedProxy = parseProxy(root.getAsJsonObject("lastUsedProxy"));
            }

            Logger.success("ProxyConfig: proxy.json loaded successfully!");
        } catch (Exception e) {
            Logger.error("ProxyConfig: Load failed! " + e.getMessage());
        }
    }

    private Proxy parseProxy(JsonObject json) {
        Proxy proxy = new Proxy();

        if (json.has("ipPort")) {
            proxy.ipPort = json.get("ipPort").getAsString();
        }
        if (json.has("type")) {
            try {
                proxy.type = Proxy.ProxyType.valueOf(json.get("type").getAsString());
            } catch (IllegalArgumentException ignored) {}
        }
        if (json.has("username")) {
            proxy.username = json.get("username").getAsString();
        }
        if (json.has("password")) {
            proxy.password = json.get("password").getAsString();
        }

        return proxy;
    }

    public void setDefaultProxyAndSave(Proxy proxy) {
        this.defaultProxy = proxy;
        save();
    }

    public void setProxyEnabledAndSave(boolean enabled) {
        this.proxyEnabled = enabled;
        save();
    }

    public void setLastUsedProxyAndSave(Proxy proxy) {
        this.lastUsedProxy = proxy;
        save();
    }
}