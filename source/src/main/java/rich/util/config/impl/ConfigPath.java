package rich.util.config.impl;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * копирайт эгиса не трогаем
 */

public class ConfigPath {

    private static final String ROOT_DIR = "AegisNeo";
    private static final String CONFIG_DIR = "configs";
    private static final String CONFIG_FILE = "autoconfig.aegisconfig";

    private static Path runDirectory = Paths.get("").toAbsolutePath();

    public static void init() {
        runDirectory = FabricLoader.getInstance().getGameDir().toAbsolutePath();
    }

    public static Path getConfigDirectory() {
        return runDirectory.resolve(ROOT_DIR).resolve(CONFIG_DIR);
    }

    public static Path getConfigFile() {
        return getConfigDirectory().resolve(CONFIG_FILE);
    }

    public static Path getConfigFile(String name) {
        if (name == null || name.isBlank() || name.length() > 64
                || name.matches(".*[\\\\/:*?\"<>|\\p{Cntrl}].*")
                || name.endsWith(".") || name.endsWith(" ")) {
            throw new IllegalArgumentException("Недопустимое имя конфигурации");
        }
        Path directory = getConfigDirectory().toAbsolutePath().normalize();
        Path config = directory.resolve(name + ".aegisconfig").normalize();
        if (!directory.equals(config.getParent())) {
            throw new IllegalArgumentException("Конфигурация должна находиться в папке configs");
        }
        return config;
    }
}
