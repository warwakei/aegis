package rich.util.config.impl;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 *  © 2026 Copyright Aegis Client
 *        All Rights Reserved ®
 */

public class ConfigPath {

    private static final String ROOT_DIR = "AegisNeo";
    private static final String CONFIG_DIR = "configs";
    private static final String CONFIG_FILE = "autoconfig.aegisconfig";

    private static Path runDirectory;

    public static void init() {
        runDirectory = Paths.get("").toAbsolutePath();
    }

    public static Path getConfigDirectory() {
        return runDirectory.resolve(ROOT_DIR).resolve(CONFIG_DIR);
    }

    public static Path getConfigFile() {
        return getConfigDirectory().resolve(CONFIG_FILE);
    }

    public static Path getConfigFile(String name) {
        return getConfigDirectory().resolve(name + ".aegisconfig");
    }
}