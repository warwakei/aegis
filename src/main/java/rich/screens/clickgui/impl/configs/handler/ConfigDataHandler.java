package rich.screens.clickgui.impl.configs.handler;

import lombok.Getter;
import lombok.Setter;
import rich.util.config.ConfigSystem;
import rich.util.config.impl.ConfigPath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ConfigDataHandler {

    private final List<String> configs = new ArrayList<>();
    private final ConfigAnimationHandler animationHandler;

    private String selectedConfig = null;
    private boolean isCreating = false;
    private String newConfigName = "";

    private double scrollOffset = 0;
    private double targetScrollOffset = 0;
    private float scrollTopFade = 0f;
    private float scrollBottomFade = 0f;

    public ConfigDataHandler(ConfigAnimationHandler animationHandler) {
        this.animationHandler = animationHandler;
    }

    public void refreshConfigs() {
        List<String> oldConfigs = new ArrayList<>(configs);
        configs.clear();
        try {
            Path configDir = ConfigPath.getConfigDirectory();
            if (Files.exists(configDir)) {
                Files.list(configDir)
                        .filter(path -> path.toString().endsWith(".aegisconfig"))
                        .forEach(path -> {
                            String name = path.getFileName().toString();
                            String configName = name.substring(0, name.length() - 12); // remove ".aegisconfig"
                            if (!configName.equalsIgnoreCase("autoconfig")) {
                                configs.add(configName);
                            }
                        });
            }
        } catch (IOException ignored) {}

        for (String config : configs) {
            if (!oldConfigs.contains(config)) {
                animationHandler.getItemAppearAnimations().put(config, 0f);
            }
        }
    }

    public void updateScroll(float deltaTime) {
        scrollOffset += (targetScrollOffset - scrollOffset) * 12f * deltaTime;
    }

    public void updateScrollFades(float visibleHeight) {
        float contentHeight = configs.size() * 27f;

        if (contentHeight <= visibleHeight) {
            scrollTopFade = 0f;
            scrollBottomFade = 0f;
            return;
        }

        float maxScroll = contentHeight - visibleHeight;
        scrollTopFade = (float) Math.min(1f, -scrollOffset / 20f);
        scrollBottomFade = (float) Math.min(1f, (maxScroll + scrollOffset) / 20f);
    }

    public void handleScroll(double vertical, float visibleHeight) {
        float contentHeight = configs.size() * 27f;
        float maxScroll = Math.max(0, contentHeight - visibleHeight);
        targetScrollOffset += vertical * 25;
        targetScrollOffset = Math.max(-maxScroll, Math.min(0, targetScrollOffset));
    }

    public boolean saveConfig(String name) {
        if (name.equalsIgnoreCase("autoconfig")) {
            return false;
        }

        Path targetPath = ConfigPath.getConfigFile(name);
        if (Files.exists(targetPath)) {
            return false;
        }

        ConfigSystem.getInstance().save(name);
        refreshConfigs();
        return true;
    }

    public boolean loadConfig(String name) {
        Path sourcePath = ConfigPath.getConfigFile(name);

        if (!Files.exists(sourcePath)) {
            return false;
        }

        // Копируем выбранный конфиг в autoconfig.aegisconfig
        Path targetPath = ConfigPath.getConfigFile();
        try {
            Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            return false;
        }

        // Загружаем конфиг через ConfigSystem
        ConfigSystem.getInstance().load();

        selectedConfig = name;
        return true;
    }

    public boolean refreshConfig(String name) {
        Path configFile = ConfigPath.getConfigFile(name);

        if (!Files.exists(configFile)) {
            return false;
        }

        ConfigSystem.getInstance().save(name);
        return true;
    }

    public boolean deleteConfig(String name) {
        Path configFile = ConfigPath.getConfigFile(name);

        if (!Files.exists(configFile)) {
            return false;
        }

        try {
            Files.delete(configFile);
            if (name.equals(selectedConfig)) {
                selectedConfig = null;
            }
            refreshConfigs();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public void toggleCreating() {
        isCreating = !isCreating;
        if (!isCreating) {
            newConfigName = "";
        }
    }

    public void appendChar(char chr) {
        if (newConfigName.length() < 32 && (Character.isLetterOrDigit(chr) || chr == '_' || chr == '-')) {
            newConfigName += chr;
        }
    }

    public void removeLastChar() {
        if (!newConfigName.isEmpty()) {
            newConfigName = newConfigName.substring(0, newConfigName.length() - 1);
        }
    }

    public void clearNewConfigName() {
        newConfigName = "";
    }
}
