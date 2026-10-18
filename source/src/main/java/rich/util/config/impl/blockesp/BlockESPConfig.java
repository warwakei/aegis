package rich.util.config.impl.blockesp;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import rich.util.config.impl.consolelogger.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class BlockESPConfig {
    private static BlockESPConfig instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;
    private final Set<String> blocks = new CopyOnWriteArraySet<>();

    private BlockESPConfig() {
        Path configDir = Paths.get("AegisNeo", "configs");
        try {
            Files.createDirectories(configDir);
        } catch (IOException ignored) {}
        configPath = configDir.resolve("blockesp.json");
    }

    public static BlockESPConfig getInstance() {
        if (instance == null) {
            instance = new BlockESPConfig();
        }
        return instance;
    }

    public Set<String> getBlocks() {
        return blocks;
    }

    public void addBlock(String block) {
        blocks.add(block);
    }

    public void addBlockAndSave(String block) {
        addBlock(block);
        save();
    }

    public boolean removeBlock(String block) {
        boolean removed = blocks.remove(block);
        return removed;
    }

    public boolean removeBlockAndSave(String block) {
        boolean removed = removeBlock(block);
        if (removed) {
            save();
        }
        return removed;
    }

    public boolean hasBlock(String block) {
        return blocks.contains(block);
    }

    public void clear() {
        blocks.clear();
    }

    public void clearAndSave() {
        clear();
        save();
    }

    public int size() {
        return blocks.size();
    }

    public List<String> getBlockList() {
        return new ArrayList<>(blocks);
    }

    public void save() {
        try {
            JsonArray array = new JsonArray();
            for (String block : blocks) {
                array.add(block);
            }
            Files.writeString(configPath, gson.toJson(array), StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.error("BlockESPConfig: Save failed! " + e.getMessage());
        }
    }

    public void load() {
        try {
            if (!Files.exists(configPath)) {
                return;
            }
            String json = Files.readString(configPath, StandardCharsets.UTF_8);
            JsonArray array = JsonParser.parseString(json).getAsJsonArray();
            blocks.clear();
            array.forEach(element -> blocks.add(element.getAsString()));
            Logger.success("BlockESPConfig: blockesp.json loaded successfully!");
        } catch (Exception e) {
            Logger.error("BlockESPConfig: Load failed! " + e.getMessage());
        }
    }
}