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
 * копирайт эгиса не трогаем
 */

public class ConfigSystem {

    private static volatile ConfigSystem instance;

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

    // сейвим дефолтный конфиг
    public boolean save() {
        return saveToPath(ConfigPath.getConfigFile());
    }

    // сейвим в файл
    public boolean save(String name) {
        Path targetPath = ConfigPath.getConfigFile(name);
        return saveToPath(targetPath);
    }

    private synchronized boolean saveToPath(Path targetPath) {
        if (!initialized.get()) {
            return false;
        }
        saving.set(true);
        try {
            String data = serializer.serialize();
            boolean success = fileHandler.write(data, targetPath);
            if (success) {
                Logger.success("ConfigSystem: Saved to " + targetPath.getFileName());
            } else {
                Logger.error("ConfigSystem: Save failed for " + targetPath.getFileName());
            }
            return success;
        } catch (Exception e) {
            Logger.error("ConfigSystem: Save error! " + e.getMessage());
            return false;
        } finally {
            saving.set(false);
        }
    }

    public CompletableFuture<Void> saveAsync() {
        return CompletableFuture.runAsync(this::save);
    }

    // грузим дефолтный конфиг
    public boolean load() {
        return loadFromPath(ConfigPath.getConfigFile());
    }

    // грузим из файла
    public boolean load(String name) {
        Path sourcePath = ConfigPath.getConfigFile(name);
        return loadFromPath(sourcePath);
    }

    private synchronized boolean loadFromPath(Path sourcePath) {
        if (!fileHandler.exists(sourcePath)) {
            Logger.info("ConfigSystem: Config not found (" + sourcePath.getFileName() + "), creating new...");
            return saveToPath(sourcePath);
        }
        try {
            String data = fileHandler.read(sourcePath);
            if (data != null && !data.isEmpty()) {
                serializer.deserialize(data);
                Logger.success("ConfigSystem: Loaded " + sourcePath.getFileName());
                return true;
            }
        } catch (Exception e) {
            Logger.error("ConfigSystem: Load error! " + e.getMessage());
        }
        return false;
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
