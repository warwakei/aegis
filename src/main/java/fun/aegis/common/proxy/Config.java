package fun.aegis.common.proxy;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.apache.commons.io.FileUtils;
import net.minecraft.client.MinecraftClient;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;

public class Config {
    private static final File CONFIG_DIR = new File(MinecraftClient.getInstance().runDirectory, "Aegis/Proxy");
    private static final File CONFIG_FILE = new File(CONFIG_DIR, "Proxyconfig.json");

    public static HashMap<String, Proxy> accounts = new HashMap<>();
    public static String lastPlayerName = "";

    public static void loadConfig() {
        try {
            if (!CONFIG_DIR.exists()) {
                CONFIG_DIR.mkdirs();
            }

            if (!CONFIG_FILE.exists()) {
                if (!CONFIG_FILE.createNewFile()) {
                    System.out.println("Error creating Proxyconfig.json file");
                }
                saveConfig();
                return;
            }

            String configString = FileUtils.readFileToString(CONFIG_FILE, StandardCharsets.UTF_8);

            if (!configString.isEmpty()) {
                JsonObject configJson = JsonParser.parseString(configString).getAsJsonObject();

                if (configJson.has("proxy-enabled")) {
                    ProxyServer.proxyEnabled = configJson.get("proxy-enabled").getAsBoolean();
                }

                Type type = new TypeToken<HashMap<String, Proxy>>() {}.getType();
                if (configJson.has("accounts")) {
                    accounts = new Gson().fromJson(configJson.get("accounts"), type);
                }

                if (accounts == null) {
                    accounts = new HashMap<>();
                }

                if (accounts.containsKey("")) {
                    ProxyServer.proxy = accounts.get("");
                } else {
                    ProxyServer.proxy = new Proxy();
                }
            }
        } catch (Exception e) {
            System.out.println("Error reading Proxyconfig.json file");
            e.printStackTrace();
        }
    }

    public static void setDefaultProxy(Proxy proxy) {
        accounts.put("", proxy);
    }

    public static void saveConfig() {
        try {
            if (!CONFIG_DIR.exists()) {
                CONFIG_DIR.mkdirs();
            }

            JsonElement accountsJsonObject = new Gson().toJsonTree(accounts);

            JsonObject configJson = new JsonObject();
            configJson.addProperty("proxy-enabled", ProxyServer.proxyEnabled);
            configJson.add("accounts", accountsJsonObject);

            Gson gsonPretty = new GsonBuilder().setPrettyPrinting().create();
            FileUtils.write(CONFIG_FILE, gsonPretty.toJson(configJson), StandardCharsets.UTF_8);
        } catch (IOException e) {
            System.out.println("Error writing Proxyconfig.json file");
            e.printStackTrace();
        }
    }
}
