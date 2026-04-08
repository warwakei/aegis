package rich.netpanel;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Modular system for NetPanel. Each module can be enabled/disabled and has its own settings.
 * Architecture inspired by Docker — independent, configurable, toggleable components.
 */
public class NetPanelModule {

    private final String id;
    private final String name;
    private final String description;
    private final String icon;
    private final String author;
    private final String version;
    private final Supplier<Boolean> dataProvider;
    private final Map<String, Object> settings;
    private volatile boolean enabled;
    private volatile boolean autoRefresh;
    private int refreshIntervalMs;

    private NetPanelModule(String id, String name, String description, String icon,
                           String author, String version,
                           Supplier<Boolean> dataProvider, Map<String, Object> defaultSettings) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.author = author;
        this.version = version;
        this.dataProvider = dataProvider;
        this.settings = new ConcurrentHashMap<>(defaultSettings != null ? defaultSettings : new HashMap<>());
        this.enabled = true;
        this.autoRefresh = false;
        this.refreshIntervalMs = 5000;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getIcon() { return icon; }
    public String getAuthor() { return author; }
    public String getVersion() { return version; }
    public boolean isEnabled() { return enabled; }
    public boolean isAutoRefresh() { return autoRefresh; }
    public int getRefreshIntervalMs() { return refreshIntervalMs; }
    public Map<String, Object> getSettings() { return settings; }

    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setAutoRefresh(boolean autoRefresh) { this.autoRefresh = autoRefresh; }
    public void setRefreshIntervalMs(int refreshIntervalMs) { this.refreshIntervalMs = refreshIntervalMs; }

    public Object getSetting(String key) { return settings.get(key); }
    public void setSetting(String key, Object value) { settings.put(key, value); }

    public boolean refresh() {
        if (!enabled || dataProvider == null) return false;
        try {
            return dataProvider.get();
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Object> toJson() {
        Map<String, Object> json = new LinkedHashMap<>();
        json.put("id", id);
        json.put("name", name);
        json.put("description", description);
        json.put("icon", icon);
        json.put("author", author);
        json.put("version", version);
        json.put("enabled", enabled);
        json.put("autoRefresh", autoRefresh);
        json.put("refreshIntervalMs", refreshIntervalMs);
        json.put("settings", new LinkedHashMap<>(settings));
        return json;
    }

    public static Builder builder(String id, String name) {
        return new Builder(id, name);
    }

    public static class Builder {
        private final String id;
        private final String name;
        private String description = "";
        private String icon = "⬡";
        private String author = "AegisNeo";
        private String version = "1.0";
        private Supplier<Boolean> dataProvider;
        private Map<String, Object> defaultSettings = new HashMap<>();

        public Builder(String id, String name) {
            this.id = id;
            this.name = name;
        }

        public Builder description(String desc) { this.description = desc; return this; }
        public Builder icon(String icon) { this.icon = icon; return this; }
        public Builder author(String author) { this.author = author; return this; }
        public Builder version(String version) { this.version = version; return this; }
        public Builder dataProvider(Supplier<Boolean> provider) { this.dataProvider = provider; return this; }
        public Builder setting(String key, Object value) { this.defaultSettings.put(key, value); return this; }

        public NetPanelModule build() {
            return new NetPanelModule(id, name, description, icon, author, version, dataProvider, defaultSettings);
        }
    }
}
