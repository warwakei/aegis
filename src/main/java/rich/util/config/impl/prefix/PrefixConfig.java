package rich.util.config.impl.prefix;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import rich.command.CommandManager;
import rich.util.config.impl.consolelogger.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PrefixConfig {
    private static PrefixConfig instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;

    @Getter
    private String prefix = ".";

    private PrefixConfig() {
        Path configDir = Paths.get("AegisNeo", "configs");
        try {
            Files.createDirectories(configDir);
        } catch (IOException ignored) {}
        configPath = configDir.resolve("prefix.json");
    }

    public static PrefixConfig getInstance() {
        if (instance == null) {
            instance = new PrefixConfig();
        }
        return instance;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
        if (CommandManager.getInstance() != null) {
            CommandManager.getInstance().setPrefix(prefix);
        }
    }

    public void setPrefixAndSave(String prefix) {
        setPrefix(prefix);
        save();
    }

    public void save() {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("prefix", prefix);
            Files.writeString(configPath, gson.toJson(obj), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.error("PrefixConfig: Save failed! " + e.getMessage());
        }
    }

    public void load() {
        try {
            if (!Files.exists(configPath)) {
                return;
            }
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("prefix")) {
                String loadedPrefix = obj.get("prefix").getAsString();
                if (!loadedPrefix.isEmpty()) {
                    this.prefix = loadedPrefix;
                }
            }
            Logger.success("PrefixConfig: prefix.json loaded successfully!");
        } catch (Exception e) {
            Logger.error("PrefixConfig: Load failed! " + e.getMessage());
        }
    }
}