package fun.aegis.utils.client.managers.file.impl;

import antidaunleak.api.annotation.Native;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.registry.Registries;
import fun.aegis.utils.client.managers.file.ClientFile;
import fun.aegis.utils.client.managers.file.exception.FileLoadException;
import fun.aegis.utils.client.managers.file.exception.FileSaveException;
import fun.aegis.common.repository.box.BoxESPRepository;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EntityESPFile extends ClientFile {
    BoxESPRepository repository;

    public EntityESPFile(BoxESPRepository repository) {
        super("EntityESP");
        this.repository = repository;
    }

    @Override
    public void saveToFile(File path) throws FileSaveException {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        File file = new File(path, getName() + ".json");

        try (FileWriter writer = new FileWriter(file)) {
            List<EntityESP> blocksList = new ArrayList<>();
            repository.entities.forEach((entity, color) -> blocksList.add(new EntityESP(entity.getTranslationKey(), color)));
            gson.toJson(blocksList, writer);
        } catch (JsonIOException | IOException e) {
            throw new FileSaveException(String.format("Failed to save %s to file", getName()), e);
        }
    }

    @Override
    public void loadFromFile(File path) throws FileLoadException {
        Gson gson = new Gson();
        File file = new File(path, getName() + ".json");

        try (FileReader reader = new FileReader(file)) {
            EntityESP[] blockESP = gson.fromJson(reader, EntityESP[].class);
            repository.entities.clear();
            Arrays.asList(blockESP).forEach(esp -> Registries.ENTITY_TYPE.stream().filter(type -> type.getTranslationKey().equals(esp.entity))
                    .findFirst().ifPresent(type -> repository.entities.put(type, esp.color)));
        } catch (IOException e) {
            throw new FileLoadException(String.format("Failed to load %s from file", getName()), e);
        } catch (JsonSyntaxException e) {
            throw new FileLoadException(String.format("JSON syntax error, %s config cannot be loaded", getName()), e);
        } catch (JsonIOException e) {
            throw new FileLoadException(String.format("JSON IO error, %s config cannot be loaded", getName()), e);
        }
    }

    private record EntityESP(String entity, int color) {}
}
