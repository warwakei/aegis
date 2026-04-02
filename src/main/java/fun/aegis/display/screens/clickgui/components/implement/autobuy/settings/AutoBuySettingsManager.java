package fun.aegis.display.screens.clickgui.components.implement.autobuy.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.util.HashMap;
import java.util.Map;

public class AutoBuySettingsManager {
    private static AutoBuySettingsManager instance;
    private final Map<String, SettingsData> settingsMap = new HashMap<>();
    private final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    private AutoBuySettingsManager() {}

    public static AutoBuySettingsManager getInstance() {
        if (instance == null) {
            instance = new AutoBuySettingsManager();
        }
        return instance;
    }

    public void saveSettings(String itemName, AutoBuyItemSettings settings) {
        String lowerItemName = itemName.toLowerCase();
        settingsMap.put(lowerItemName, new SettingsData(
                settings.getBuyBelow(),
                settings.getSellAbove(),
                settings.getMinQuantity()
        ));
    }

    public void loadSettings(String itemName, AutoBuyItemSettings settings) {
        String lowerItemName = itemName.toLowerCase();
        SettingsData data = settingsMap.get(lowerItemName);
        if (data != null) {
            settings.setBuyBelow(data.buyBelow);
            settings.setSellAbove(data.sellAbove);
            settings.setMinQuantity(data.minQuantity);
        }
    }

    public boolean hasSettings(String itemName) {
        return settingsMap.containsKey(itemName.toLowerCase());
    }

    public JsonObject saveToJson() {
        JsonObject json = new JsonObject();
        for (Map.Entry<String, SettingsData> entry : settingsMap.entrySet()) {
            JsonObject itemJson = new JsonObject();
            itemJson.addProperty("buyBelow", entry.getValue().buyBelow);
            itemJson.addProperty("sellAbove", entry.getValue().sellAbove);
            itemJson.addProperty("minQuantity", entry.getValue().minQuantity);
            json.add(entry.getKey(), itemJson);
        }
        return json;
    }

    public void loadFromJson(JsonObject json) {
        if (json != null) {
            for (String key : json.keySet()) {
                String lowerKey = key.toLowerCase();
                JsonObject itemJson = json.getAsJsonObject(key);
                settingsMap.put(lowerKey, new SettingsData(
                        itemJson.get("buyBelow").getAsInt(),
                        itemJson.get("sellAbove").getAsInt(),
                        itemJson.get("minQuantity").getAsInt()
                ));
            }
        }
    }

    private static class SettingsData {
        int buyBelow;
        int sellAbove;
        int minQuantity;

        SettingsData(int buyBelow, int sellAbove, int minQuantity) {
            this.buyBelow = buyBelow;
            this.sellAbove = sellAbove;
            this.minQuantity = minQuantity;
        }
    }
}
