package rich.util.config.impl.chattab;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import rich.util.config.impl.consolelogger.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class ChatTabConfig {
    private static ChatTabConfig instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;

    private ChatTabConfig() {
        Path configDir = Paths.get("Rich", "configs");
        try {
            Files.createDirectories(configDir);
        } catch (IOException ignored) {}
        configPath = configDir.resolve("chattabs.json");
    }

    public static ChatTabConfig getInstance() {
        if (instance == null) {
            instance = new ChatTabConfig();
        }
        return instance;
    }

    public void save(List<ChatTab> tabs) {
        try {
            JsonArray array = new JsonArray();
            for (ChatTab tab : tabs) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", tab.getName());
                obj.addProperty("filterType", tab.getFilterType().name());
                obj.addProperty("filterValue", tab.getFilterValue());
                array.add(obj);
            }
            Files.writeString(configPath, gson.toJson(array), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.error("ChatTabConfig: Save failed! " + e.getMessage());
        }
    }

    public List<ChatTab> load() {
        List<ChatTab> tabs = new ArrayList<>();
        try {
            if (!Files.exists(configPath)) {
                return tabs;
            }
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            for (var element : array) {
                JsonObject obj = element.getAsJsonObject();
                String name = obj.get("name").getAsString();
                ChatTab.FilterType filterType = ChatTab.FilterType.valueOf(obj.get("filterType").getAsString());
                String filterValue = obj.has("filterValue") ? obj.get("filterValue").getAsString() : "";
                tabs.add(new ChatTab(name, filterType, filterValue));
            }
            Logger.success("ChatTabConfig: chattabs.json loaded successfully!");
        } catch (Exception e) {
            Logger.error("ChatTabConfig: Load failed! " + e.getMessage());
        }
        return tabs;
    }
}
