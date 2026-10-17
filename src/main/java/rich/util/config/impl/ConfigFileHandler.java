package rich.util.config.impl;

import rich.util.config.impl.consolelogger.Logger;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * копирайт эгиса не трогаем
 */

public class ConfigFileHandler {

    private final ReentrantReadWriteLock lock;

    public ConfigFileHandler() {
        this.lock = new ReentrantReadWriteLock();
    }

    public void createDirectories() {
        try {
            Files.createDirectories(ConfigPath.getConfigDirectory());
        } catch (IOException e) {
            Logger.error("ConfigSystem: Failed to create directories!");
        }
    }

    public boolean write(String content) {
        return write(content, ConfigPath.getConfigFile());
    }

    public boolean write(String content, Path targetPath) {
        lock.writeLock().lock();
        Path tempFile = null;
        try {
            Path target = targetPath.toAbsolutePath();
            Files.createDirectories(target.getParent());
            tempFile = Files.createTempFile(target.getParent(), target.getFileName() + ".", ".tmp");
            Files.writeString(tempFile, content, StandardCharsets.UTF_8);
            try {
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(tempFile, target, StandardCopyOption.REPLACE_EXISTING);
            }

            return true;
        } catch (IOException e) {
            Logger.error("ConfigSystem: Write failed! " + e.getMessage());
            return false;
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (IOException ignored) {}
            }
            lock.writeLock().unlock();
        }
    }

    public String read() {
        return read(ConfigPath.getConfigFile());
    }

    public String read(Path sourcePath) {
        lock.readLock().lock();
        try {
            if (!Files.exists(sourcePath)) {
                return null;
            }

            return Files.readString(sourcePath, StandardCharsets.UTF_8);
        } catch (IOException e) {
            Logger.error("ConfigSystem: Read failed! " + e.getMessage());
            return null;
        } finally {
            lock.readLock().unlock();
        }
    }

    public boolean exists() {
        return Files.exists(ConfigPath.getConfigFile());
    }

    public boolean exists(Path path) {
        return Files.exists(path);
    }

    public boolean copy(Path source, Path target) {
        try {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            Logger.error("ConfigSystem: Copy failed! " + e.getMessage());
            return false;
        }
    }

    public boolean delete(Path path) {
        try {
            Files.deleteIfExists(path);
            return true;
        } catch (IOException e) {
            Logger.error("ConfigSystem: Delete failed! " + e.getMessage());
            return false;
        }
    }
}
