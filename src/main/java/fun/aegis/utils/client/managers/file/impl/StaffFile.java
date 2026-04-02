package fun.aegis.utils.client.managers.file.impl;

import antidaunleak.api.annotation.Native;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import fun.aegis.utils.client.managers.file.ClientFile;
import fun.aegis.utils.client.managers.file.exception.FileLoadException;
import fun.aegis.utils.client.managers.file.exception.FileSaveException;
import fun.aegis.common.repository.staff.StaffRepository;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class StaffFile extends ClientFile {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public StaffFile() {
        super("Staff");
    }

    @Override
    public void loadFromFile(File directory) throws FileLoadException {
        File file = new File(directory, getName() + ".json");
        if (!file.exists()) return;

        try {
            String content = Files.readString(file.toPath());
            if (content.isEmpty()) return;

            List<StaffRepository.Staff> staff = GSON.fromJson(content, new TypeToken<List<StaffRepository.Staff>>() {}.getType());
            StaffRepository.getStaff().clear();
            StaffRepository.getStaff().addAll(staff);
        } catch (IOException e) {
            throw new FileLoadException("Не удалось прочитать файл персонала", e);
        }
    }

    @Override
    public void saveToFile(File directory) throws FileSaveException {
        File file = new File(directory, getName() + ".json");
        try {
            Files.writeString(file.toPath(), GSON.toJson(StaffRepository.getStaff()), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            throw new FileSaveException("Не удалось сохранить файл персонала", e);
        }
    }
}
