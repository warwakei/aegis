package fun.aegis.utils.client.managers.file;

import fun.aegis.utils.client.logs.Logger;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import fun.aegis.utils.client.managers.file.exception.FileLoadException;
import fun.aegis.utils.client.managers.file.exception.FileSaveException;
import fun.aegis.utils.client.managers.file.impl.ModuleFile;

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FileController {
    List<ClientFile> clientFiles;
    File directory;
    ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public FileController(List<ClientFile> clientFiles, File directory) {
        this.clientFiles = clientFiles;
        this.directory = directory;
        startAutoSave();
    }

    public void startAutoSave() {
        Logger.info("Auto-save system started!");
        scheduler.scheduleAtFixedRate(() -> {
            try {
                Logger.info("Saving with auto-save.");
                saveFiles();
            } catch (FileSaveException e) {
                Logger.error("Failed to auto-save files: " + e.getMessage());
            }
        }, 1, 1, TimeUnit.MINUTES);
    }

    public void stopAutoSave() {
        Logger.info("Auto-save shutdown!");
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(1, TimeUnit.MINUTES)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }

    public void saveFiles() throws FileSaveException {
        if (clientFiles.isEmpty()) {
            Logger.warn("No files to save from directory: " + directory.getPath());
            return;
        }

        for (ClientFile clientFile : clientFiles) {
            try {
                clientFile.saveToFile(directory);
                Logger.info("Successfully saved file: " + clientFile.getName() + " to " + directory.getPath());
            } catch (FileSaveException e) {
                Logger.error("Failed to save file: " + clientFile.getName() + " - " + e.getMessage());
                throw new FileSaveException("Failed to save file: " + clientFile.getName(), e);
            }
        }
    }

    public void loadFiles() throws FileLoadException {
        if (clientFiles.isEmpty()) {
            Logger.warn("No files to load from directory: " + directory.getPath());
            return;
        }

        for (ClientFile clientFile : clientFiles) {
            try {
                clientFile.loadFromFile(directory);
                Logger.info("Successfully loaded file: " + clientFile.getName() + " from " + directory.getPath());
            } catch (FileLoadException e) {
                throw new FileLoadException("Failed to load file: " + clientFile.getName(), e);
            }
        }
    }

    public void saveFile(String fileName) throws FileSaveException {
        for (ClientFile clientFile : clientFiles) {
            if (clientFile instanceof ModuleFile) {
                try {
                    clientFile.saveToFile(directory, fileName);
                    Logger.info("Successfully saved file: " + fileName + " to " + directory.getPath());
                } catch (FileSaveException e) {
                    throw new FileSaveException("Failed to save file: " + fileName, e);
                }
            }
        }
    }

    public void loadFile(String fileName) throws FileLoadException {
        for (ClientFile clientFile : clientFiles) {
            if (clientFile instanceof ModuleFile) {
                try {
                    clientFile.loadFromFile(directory, fileName);
                    Logger.info("Successfully loaded file: " + fileName + " from " + directory.getPath());
                } catch (FileLoadException e) {
                    throw new FileLoadException("Failed to load file: " + fileName, e);
                }
            }
        }
    }

    public void saveFile(Class<? extends ClientFile> fileClass) {
        clientFiles.stream()
                .filter(fileClass::isInstance)
                .findFirst()
                .ifPresent(file -> {
                    try {
                        file.saveToFile(directory);
                        Logger.info("Successfully saved file on-demand: " + file.getName());
                    } catch (FileSaveException e) {
                        Logger.error("Failed to save file on-demand: " + file.getName(), e);
                    }
                });
    }
}
