package rich.util.config.impl.autobuyconfig;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.Getter;
import lombok.Setter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class AutoBuyConfig {
    private static AutoBuyConfig instance;
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    private final Path configPath;
    @Getter
    private ConfigData data = new ConfigData();

    @Getter
    @Setter
    public static class ItemConfig {
        private boolean enabled = false;
        private int buyBelow = 1000;
        private int minQuantity = 1;

        public ItemConfig() {}

        public ItemConfig(boolean enabled, int buyBelow, int minQuantity) {
            this.enabled = enabled;
            this.buyBelow = buyBelow;
            this.minQuantity = minQuantity;
        }
    }

    @Getter
    @Setter
    public static class ConfigData {
        private boolean globalEnabled = false;
        private Map<String, ItemConfig> items = new HashMap<>();
    }

    private AutoBuyConfig() {
        Path configDir = Paths.get("Rich", "configs", "autobuy");
        try {
            Files.createDirectories(configDir);
        } catch (IOException ignored) {}
        configPath = configDir.resolve("autobuy.json");
        load();
    }

    public static AutoBuyConfig getInstance() {
        if (instance == null) {
            instance = new AutoBuyConfig();
        }
        return instance;
    }

    public void load() {
        try {
            if (Files.exists(configPath)) {
                String json = Files.readString(configPath);
                ConfigData loaded = gson.fromJson(json, ConfigData.class);
                if (loaded != null) {
                    this.data = loaded;
                    if (this.data.getItems() == null) {
                        this.data.setItems(new HashMap<>());
                    }
                }
            }
        } catch (IOException ignored) {}
    }

    public void save() {
        try {
            String json = gson.toJson(data);
            Files.writeString(configPath, json);
        } catch (IOException ignored) {}
    }

    public void reset() {
        data = new ConfigData();
        try {
            if (Files.exists(configPath)) {
                Files.delete(configPath);
            }
        } catch (IOException ignored) {}
        save();
    }

    public boolean isGlobalEnabled() {
        return data.isGlobalEnabled();
    }

    public void setGlobalEnabled(boolean enabled) {
        data.setGlobalEnabled(enabled);
    }

    public void setGlobalEnabledAndSave(boolean enabled) {
        data.setGlobalEnabled(enabled);
        save();
    }

    public ItemConfig getItemConfig(String itemName) {
        return data.getItems().computeIfAbsent(itemName, k -> new ItemConfig());
    }

    public ItemConfig getItemConfigOrNull(String itemName) {
        return data.getItems().get(itemName);
    }

    public void setItemConfig(String itemName, ItemConfig config) {
        data.getItems().put(itemName, config);
    }

    public void setItemConfigAndSave(String itemName, ItemConfig config) {
        data.getItems().put(itemName, config);
        save();
    }

    public void setItemEnabled(String itemName, boolean enabled) {
        ItemConfig config = getItemConfig(itemName);
        config.setEnabled(enabled);
    }

    public void setItemEnabledAndSave(String itemName, boolean enabled) {
        ItemConfig config = getItemConfig(itemName);
        config.setEnabled(enabled);
        save();
    }

    public void setItemBuyBelow(String itemName, int buyBelow) {
        ItemConfig config = getItemConfig(itemName);
        config.setBuyBelow(buyBelow);
    }

    public void setItemBuyBelowAndSave(String itemName, int buyBelow) {
        ItemConfig config = getItemConfig(itemName);
        config.setBuyBelow(buyBelow);
        save();
    }

    public void setItemMinQuantity(String itemName, int minQuantity) {
        ItemConfig config = getItemConfig(itemName);
        config.setMinQuantity(minQuantity);
    }

    public void setItemMinQuantityAndSave(String itemName, int minQuantity) {
        ItemConfig config = getItemConfig(itemName);
        config.setMinQuantity(minQuantity);
        save();
    }

    public boolean isItemEnabled(String itemName) {
        ItemConfig config = getItemConfigOrNull(itemName);
        return config != null && config.isEnabled();
    }

    public int getItemBuyBelow(String itemName) {
        return getItemConfig(itemName).getBuyBelow();
    }

    public int getItemMinQuantity(String itemName) {
        return getItemConfig(itemName).getMinQuantity();
    }

    public boolean hasItemConfig(String itemName) {
        return data.getItems().containsKey(itemName);
    }

    public void loadItemSettings(String itemName, int defaultPrice) {
        if (!hasItemConfig(itemName)) {
            ItemConfig config = new ItemConfig(false, defaultPrice, 1);
            data.getItems().put(itemName, config);
        }
    }

    public Map<String, ItemConfig> getAllItemConfigs() {
        return new HashMap<>(data.getItems());
    }
}