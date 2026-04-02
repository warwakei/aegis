package rich.util.config.impl.staff;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import rich.util.config.impl.consolelogger.Logger;
import rich.util.repository.staff.StaffUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class StaffConfig {
    private static StaffConfig instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;

    private StaffConfig() {
        Path configDir = Paths.get("AegisNeo", "configs");
        try {
            Files.createDirectories(configDir);
        } catch (IOException ignored) {}
        configPath = configDir.resolve("staff.json");
    }

    public static StaffConfig getInstance() {
        if (instance == null) {
            instance = new StaffConfig();
        }
        return instance;
    }

    public void save() {
        try {
            JsonArray array = new JsonArray();
            for (String name : StaffUtils.getStaffNames()) {
                array.add(name);
            }
            Files.writeString(configPath, gson.toJson(array), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.error("StaffConfig: Save failed! " + e.getMessage());
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
            StaffUtils.setStaff(names);
            Logger.success("StaffConfig: staff.json loaded successfully!");
        } catch (Exception e) {
            Logger.error("StaffConfig: Load failed! " + e.getMessage());
        }
    }
}