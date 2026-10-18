package rich.util.config.impl.bind;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.Getter;
import org.lwjgl.glfw.GLFW;
import rich.util.config.impl.consolelogger.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class BindConfig {
    private static BindConfig instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;

    @Getter
    private int BindKey = GLFW.GLFW_KEY_RIGHT_SHIFT;

    private BindConfig() {
        Path configDir = Paths.get("AegisNeo", "configs");
        try {
            Files.createDirectories(configDir);
        } catch (IOException ignored) {}
        configPath = configDir.resolve("Bind.json");
        load();
    }

    public static BindConfig getInstance() {
        if (instance == null) {
            instance = new BindConfig();
        }
        return instance;
    }

    public void setKey(int key) {
        this.BindKey = key;
    }

    public void setKeyAndSave(int key) {
        setKey(key);
        save();
    }

    public void save() {
        try {
            JsonObject obj = new JsonObject();
            obj.addProperty("BindKey", BindKey);
            Files.writeString(configPath, gson.toJson(obj), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.error("BindConfig: Save failed! " + e.getMessage());
        }
    }

    public void load() {
        try {
            if (!Files.exists(configPath)) {
                return;
            }
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("BindKey")) {
                BindKey = obj.get("BindKey").getAsInt();
            }
            Logger.success("BindConfig: Bind.json loaded successfully!");
        } catch (Exception e) {
            Logger.error("BindConfig: Load failed! " + e.getMessage());
        }
    }
}