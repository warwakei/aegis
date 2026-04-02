package rich.util.config.impl.way;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import rich.util.config.impl.consolelogger.Logger;
import rich.util.repository.way.Way;
import rich.util.repository.way.WayRepository;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class WayConfig {
    private static WayConfig instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;

    private WayConfig() {
        Path configDir = Paths.get("AegisNeo", "configs");
        try {
            Files.createDirectories(configDir);
        } catch (IOException ignored) {}
        configPath = configDir.resolve("waypoints.json");
    }

    public static WayConfig getInstance() {
        if (instance == null) {
            instance = new WayConfig();
        }
        return instance;
    }

    public void save() {
        try {
            JsonArray array = new JsonArray();
            for (Way way : WayRepository.getInstance().getWayList()) {
                JsonObject obj = new JsonObject();
                obj.addProperty("name", way.name());
                obj.addProperty("x", way.pos().getX());
                obj.addProperty("y", way.pos().getY());
                obj.addProperty("z", way.pos().getZ());
                obj.addProperty("server", way.server());
                array.add(obj);
            }
            Files.writeString(configPath, gson.toJson(array), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.error("WayConfig: Save failed! " + e.getMessage());
        }
    }

    public void load() {
        try {
            if (!Files.exists(configPath)) {
                return;
            }
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            List<Way> ways = new ArrayList<>();
            array.forEach(element -> {
                JsonObject obj = element.getAsJsonObject();
                String name = obj.get("name").getAsString();
                int x = obj.get("x").getAsInt();
                int y = obj.get("y").getAsInt();
                int z = obj.get("z").getAsInt();
                String server = obj.get("server").getAsString();
                ways.add(new Way(name, new net.minecraft.util.math.BlockPos(x, y, z), server));
            });
            WayRepository.getInstance().setWays(ways);
            Logger.success("WayConfig: waypoints.json loaded successfully!");
        } catch (Exception e) {
            Logger.error("WayConfig: Load failed! " + e.getMessage());
        }
    }
}