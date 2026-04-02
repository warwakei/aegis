package rich.util.config.impl.drag;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import rich.Initialization;
import rich.client.draggables.HudElement;
import rich.client.draggables.HudManager;
import rich.util.config.impl.consolelogger.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class DragConfig {
    private static DragConfig instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;

    private DragConfig() {
        Path configDir = Paths.get("AegisNeo", "configs");
        try {
            Files.createDirectories(configDir);
        } catch (IOException ignored) {}
        configPath = configDir.resolve("draggables.json");
    }

    public static DragConfig getInstance() {
        if (instance == null) {
            instance = new DragConfig();
        }
        return instance;
    }

    public void save() {
        try {
            HudManager hudManager = getHudManager();
            if (hudManager == null || !hudManager.isInitialized()) return;

            JsonObject root = new JsonObject();
            for (HudElement element : hudManager.getElements()) {
                JsonObject elementJson = new JsonObject();
                elementJson.addProperty("x", element.getX());
                elementJson.addProperty("y", element.getY());
                elementJson.addProperty("width", element.getWidth());
                elementJson.addProperty("height", element.getHeight());
                root.add(element.getName(), elementJson);
            }
            Files.writeString(configPath, gson.toJson(root), StandardCharsets.UTF_8);
            Logger.success("DragConfig: draggables.json saved successfully!");
        } catch (IOException e) {
            Logger.error("DragConfig: Save failed! " + e.getMessage());
        }
    }

    public void load() {
        try {
            if (!Files.exists(configPath)) {
                Logger.info("DragConfig: No config file found, using defaults.");
                return;
            }

            HudManager hudManager = getHudManager();
            if (hudManager == null) {
                Logger.error("DragConfig: HudManager is null, cannot load.");
                return;
            }

            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            if (json == null || json.trim().isEmpty()) {
                Logger.error("DragConfig: Config file is empty.");
                return;
            }

            JsonObject root = JsonParser.parseString(json).getAsJsonObject();

            for (HudElement element : hudManager.getElements()) {
                if (root.has(element.getName())) {
                    JsonObject elementJson = root.getAsJsonObject(element.getName());
                    if (elementJson.has("x")) {
                        element.setX(elementJson.get("x").getAsInt());
                    }
                    if (elementJson.has("y")) {
                        element.setY(elementJson.get("y").getAsInt());
                    }
                    if (elementJson.has("width")) {
                        element.setWidth(elementJson.get("width").getAsInt());
                    }
                    if (elementJson.has("height")) {
                        element.setHeight(elementJson.get("height").getAsInt());
                    }
                }
            }
            Logger.success("DragConfig: draggables.json loaded successfully!");
        } catch (Exception e) {
            Logger.error("DragConfig: Load failed! " + e.getMessage());
        }
    }

    private HudManager getHudManager() {
        if (Initialization.getInstance() == null) return null;
        if (Initialization.getInstance().getManager() == null) return null;
        return Initialization.getInstance().getManager().getHudManager();
    }
}