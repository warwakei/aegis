package rich.util.config;

import rich.util.config.impl.ConfigFileHandler;
import rich.util.config.impl.ConfigPath;
import rich.util.config.impl.ConfigSerializer;
import rich.util.config.impl.autosaver.ConfigAutoSaver;
import rich.util.config.impl.consolelogger.Logger;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *  © 2026 Copyright Aegis Client
 *        All Rights Reserved ®
 */

public class ConfigSystem {

    private static ConfigSystem instance;

    private final ConfigSerializer serializer;
    private final ConfigFileHandler fileHandler;
    private final ConfigAutoSaver autoSaver;
    private final AtomicBoolean initialized;
    private final AtomicBoolean saving;

    public ConfigSystem() {
        instance = this;
        this.serializer = new ConfigSerializer();
        this.fileHandler = new ConfigFileHandler();
        this.autoSaver = new ConfigAutoSaver(this::save);
        this.initialized = new AtomicBoolean(false);
        this.saving = new AtomicBoolean(false);
    }

    public static ConfigSystem getInstance() {
        return instance;
    }

    public void init() {
        if (initialized.compareAndSet(false, true)) {
            ConfigPath.init();
            fileHandler.createDirectories();
            load();
            autoSaver.start();
            registerShutdownHook();
            Logger.success("ConfigSystem: Initialized!");
        }
    }

    private void registerShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Logger.info("ConfigSystem: Shutdown detected, saving...");
            shutdown();
        }, "Aegis-ConfigShutdown"));
    }

    // Сохраняет текущий конфиг в autoconfig.aegisconfig
    public void save() {
        saveToPath(ConfigPath.getConfigFile());
    }

    // Сохраняет текущий конфиг в указанный файл
    public void save(String name) {
        Path targetPath = ConfigPath.getConfigFile(name);
        saveToPath(targetPath);
    }

    private void saveToPath(Path targetPath) {
        if (!initialized.get()) {
            return;
        }
        if (!saving.compareAndSet(false, true)) {
            return;
        }
        try {
            String data = serializer.serialize();
            boolean success = fileHandler.write(data, targetPath);
            if (success) {
                Logger.success("ConfigSystem: Saved to " + targetPath.getFileName());
            } else {
                Logger.error("ConfigSystem: Save failed for " + targetPath.getFileName());
            }
        } catch (Exception e) {
            Logger.error("ConfigSystem: Save error! " + e.getMessage());
        } finally {
            saving.set(false);
        }
    }

    public CompletableFuture<Void> saveAsync() {
        return CompletableFuture.runAsync(this::save);
    }

    // Загружает конфиг из autoconfig.aegisconfig
    public void load() {
        loadFromPath(ConfigPath.getConfigFile());
    }

    // Загружает конфиг из указанного файла
    public void load(String name) {
        Path sourcePath = ConfigPath.getConfigFile(name);
        loadFromPath(sourcePath);
    }

    private void loadFromPath(Path sourcePath) {
        if (!fileHandler.exists(sourcePath)) {
            Logger.info("ConfigSystem: Config not found (" + sourcePath.getFileName() + "), creating new...");
            saveToPath(sourcePath);
            return;
        }
        try {
            String data = fileHandler.read(sourcePath);
            if (data != null && !data.isEmpty()) {
                serializer.deserialize(data);
                Logger.success("ConfigSystem: Loaded " + sourcePath.getFileName());
            }
        } catch (Exception e) {
            Logger.error("ConfigSystem: Load error! " + e.getMessage());
        }
    }

    public void shutdown() {
        if (!initialized.get()) {
            return;
        }
        autoSaver.shutdown();
        save();
        Logger.success("ConfigSystem: Shutdown complete!");
    }

    public void reload() {
        load();
        Logger.success("ConfigSystem: Config reloaded!");
    }

    public boolean isInitialized() {
        return initialized.get();
    }

    public boolean isSaving() {
        return saving.get();
    }

    public ConfigAutoSaver getAutoSaver() {
        return autoSaver;
    }
}