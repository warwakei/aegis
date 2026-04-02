package rich.util.config.impl.ignore;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import rich.util.config.impl.consolelogger.Logger;
import rich.util.repository.ignore.IgnoreUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class IgnoreConfig {
    private static IgnoreConfig instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;

    private IgnoreConfig() {
        Path configDir = Paths.get("Rich", "configs");
        try {
            Files.createDirectories(configDir);
        } catch (IOException ignored) {}
        configPath = configDir.resolve("ignores.json");
    }

    public static IgnoreConfig getInstance() {
        if (instance == null) {
            instance = new IgnoreConfig();
        }
        return instance;
    }

    public void save() {
        try {
            JsonArray array = new JsonArray();
            for (String name : IgnoreUtils.getIgnoreNames()) {
                array.add(name);
            }
            Files.writeString(configPath, gson.toJson(array), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.error("IgnoreConfig: Save failed! " + e.getMessage());
        }
    }

    public void load() {
        try {
            if (!Files.exists(configPath)) {
                return;
            }
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            List<String> names = new ArrayList<>();
            array.forEach(element -> names.add(element.getAsString()));
            IgnoreUtils.setIgnores(names);
            Logger.success("IgnoreConfig: ignores.json loaded successfully!");
        } catch (Exception e) {
            Logger.error("IgnoreConfig: Load failed! " + e.getMessage());
        }
    }
}
