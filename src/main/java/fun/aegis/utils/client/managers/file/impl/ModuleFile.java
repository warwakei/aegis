package fun.aegis.utils.client.managers.file.impl;

import antidaunleak.api.annotation.Native;
import com.google.gson.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import fun.aegis.utils.client.managers.api.draggable.AbstractDraggable;
import fun.aegis.utils.client.managers.api.draggable.DraggableRepository;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleRepository;
import fun.aegis.features.module.setting.Setting;
import fun.aegis.features.module.setting.implement.*;
import fun.aegis.utils.client.managers.file.ClientFile;
import fun.aegis.utils.client.managers.file.exception.FileLoadException;
import fun.aegis.utils.client.managers.file.exception.FileSaveException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ModuleFile extends ClientFile {
    Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    ModuleRepository moduleRepository;
    DraggableRepository draggableRepository;

    public ModuleFile(ModuleRepository moduleRepository, DraggableRepository draggableRepository) {
        super("AutoCfg");
        this.moduleRepository = moduleRepository;
        this.draggableRepository = draggableRepository;
    }

    @Override
    public void saveToFile(File path) throws FileSaveException {
        saveToFile(path, getName() + ".json");
    }

    @Override
    public void loadFromFile(File path) throws FileLoadException {
        loadFromFile(path, getName() + ".json");
    }

    @Override
    public void saveToFile(File path, String fileName) throws FileSaveException {
        JsonObject functionObject = createJsonObjectFromModules();
        File file = new File(path, fileName);
        writeJsonToFile(functionObject, file);
        super.saveToFile(path, fileName);
    }

    @Override
    public void loadFromFile(File path, String fileName) throws FileLoadException {
        File file = new File(path, fileName);
        if (!file.exists() || file.length() == 0) {
            return; // Skip empty or non-existent files
        }
        JsonObject functionObject = readJsonFromFile(file);
        if (functionObject != null) {
            updateModulesFromJsonObject(functionObject);
        }
        super.loadFromFile(path, fileName);
    }

    private JsonObject createJsonObjectFromModules() {
        JsonObject functionObject = new JsonObject();
        for (Module module : moduleRepository.modules()) {
            JsonObject moduleObject = new JsonObject();
            moduleObject.addProperty("bind", module.getKey());
            moduleObject.addProperty("state", module.isState());
            module.settings().forEach(setting -> addSettingToJsonObject(moduleObject, setting));
            functionObject.add(module.getName().toLowerCase(), moduleObject);
        }

        for (AbstractDraggable draggable : draggableRepository.draggable()) {
            JsonObject draggableObject = new JsonObject();
            draggableObject.addProperty("posX", draggable.getX());
            draggableObject.addProperty("posY", draggable.getY());
            functionObject.add(draggable.getName().toLowerCase(), draggableObject);
        }

        return functionObject;
    }

    private void addSettingToJsonObject(JsonObject moduleObject, Setting setting) {
        if (setting instanceof BooleanSetting booleanSetting) {
            moduleObject.addProperty(setting.getName(), booleanSetting.isValue());
        }
        if (setting instanceof SliderSettings sliderSetting) {
            moduleObject.addProperty(setting.getName(), sliderSetting.getValue());
        }
        if (setting instanceof ValueSetting valueSetting) {
            moduleObject.addProperty(setting.getName(), valueSetting.getValue());
        }
        if (setting instanceof ColorSetting colorSetting) {
            moduleObject.addProperty(setting.getName(), colorSetting.getColor());
        }
        if (setting instanceof BindSetting bindSetting) {
            moduleObject.addProperty(setting.getName(), bindSetting.getKey());
        }
        if (setting instanceof TextSetting textSetting) {
            moduleObject.addProperty(setting.getName(), textSetting.getText());
        }
        if (setting instanceof SelectSetting selectSetting) {
            moduleObject.addProperty(setting.getName(), selectSetting.getSelected());
        }
        if (setting instanceof MultiSelectSetting multiSelectSetting) {
            List<String> selected = multiSelectSetting.getSelected();
            String selectedAsString = String.join(",", selected);
            moduleObject.addProperty(setting.getName(), selectedAsString);
        }
        if (setting instanceof GroupSetting groupSetting) {
            JsonObject groupObject = new JsonObject();
            groupObject.addProperty("state", groupSetting.isValue());
            for (Setting subSetting : groupSetting.getSubSettings()) {
                addSettingToJsonObject(groupObject, subSetting);
            }
            moduleObject.add(setting.getName(), groupObject);
        }
    }

    private void writeJsonToFile(JsonObject functionObject, File file) throws FileSaveException {
        try (FileWriter writer = new FileWriter(file)) {
            GSON.toJson(functionObject, writer);
            writer.flush();
            // Проверяем что файл не пустой после записи
            if (file.length() == 0) {
                throw new FileSaveException("File is empty after write operation");
            }
        } catch (IOException e) {
            throw new FileSaveException("Failed to save module to file", e);
        }
    }

    private JsonObject readJsonFromFile(File file) throws FileLoadException {
        try (FileReader reader = new FileReader(file)) {
            JsonElement jsonElement = JsonParser.parseReader(reader);
            if (jsonElement == null || jsonElement.isJsonNull()) {
                throw new FileLoadException("JSON file is empty or null");
            }
            return jsonElement.getAsJsonObject();
        } catch (IOException e) {
            throw new FileLoadException("Failed to load module from file", e);
        } catch (JsonSyntaxException | JsonIOException e) {
            throw new FileLoadException("Failed to parse JSON from file", e);
        } catch (IllegalStateException e) {
            throw new FileLoadException("JSON is not a valid object", e);
        }
    }

    private void updateModulesFromJsonObject(JsonObject functionObject) {
        for (Module module : moduleRepository.modules()) {
            JsonObject moduleObject = functionObject.getAsJsonObject(module.getName().toLowerCase());
            if (moduleObject == null) continue;

            if (moduleObject.has("bind") && moduleObject.has("state")) {
                module.setKey(moduleObject.get("bind").getAsInt());
                module.setState(moduleObject.get("state").getAsBoolean());
            }
            module.settings().forEach(setting -> updateSettingFromJsonObject(moduleObject, setting));
        }

        for (AbstractDraggable draggable : draggableRepository.draggable()) {
            JsonObject draggableObject = functionObject.getAsJsonObject(draggable.getName().toLowerCase());
            
            // Обратная совместимость со старыми английскими названиями
            if (draggableObject == null) {
                String oldName = getOldEnglishName(draggable.getName());
                if (oldName != null) {
                    draggableObject = functionObject.getAsJsonObject(oldName.toLowerCase());
                }
            }
            
            if (draggableObject == null) continue;

            if (draggableObject.has("posX") && draggableObject.has("posY")) {
                draggable.setX(draggableObject.get("posX").getAsInt());
                draggable.setY(draggableObject.get("posY").getAsInt());
            }
        }
    }
    
    private String getOldEnglishName(String russianName) {
        return switch (russianName) {
            case "Кейбинды" -> "Hotkeys";
            case "Кулдауны" -> "Cooldowns";
            case "Эффекты" -> "Potions";
            case "Инвентарь" -> "Inventory";
            case "Список стафа" -> "Staff List";
            case "Таргет худ" -> "Target Hud";
            case "Вотермарка" -> "Watermark";
            case "Уведомления" -> "Notifications";
            case "Инфо игрока" -> "Player Info";
            case "Бинды" -> "Binds";
            default -> null;
        };
    }

    private void updateSettingFromJsonObject(JsonObject moduleObject, Setting setting) {
        JsonElement settingElement = moduleObject.get(setting.getName());
        if (settingElement == null || settingElement.isJsonNull()) {
            // Если FOV не найден - ставим максимальный
            if (setting.getName().equals("FOV") && setting instanceof SliderSettings sliderSetting) {
                sliderSetting.setValue(360.0f);
            }
            return;
        }

        if (setting instanceof BooleanSetting booleanSetting) {
            booleanSetting.setValue(settingElement.getAsBoolean());
        }
        if (setting instanceof SliderSettings sliderSetting) {
            sliderSetting.setValue(settingElement.getAsFloat());
        }
        if (setting instanceof ValueSetting valueSetting) {
            valueSetting.setValue(settingElement.getAsDouble());
        }
        if (setting instanceof ColorSetting colorSetting) {
            colorSetting.setColor(settingElement.getAsInt());
        }
        if (setting instanceof BindSetting bindSetting) {
            bindSetting.setKey(settingElement.getAsInt());
        }
        if (setting instanceof TextSetting textSetting) {
            textSetting.setText(settingElement.getAsString());
        }
        if (setting instanceof SelectSetting selectSetting) {
            selectSetting.setSelected(settingElement.getAsString());
        }
        if (setting instanceof MultiSelectSetting multiSelectSetting) {
            String asString = settingElement.getAsString();
            List<String> selectedList = new ArrayList<>(Arrays.asList(asString.split(",")));
            selectedList.removeIf(s -> !multiSelectSetting.getList().contains(s));
            multiSelectSetting.setSelected(selectedList);
        }
        if (setting instanceof GroupSetting groupSetting) {
            JsonObject groupObject = settingElement.getAsJsonObject();

            if (groupObject.has("state")) {
                groupSetting.setValue(groupObject.get("state").getAsBoolean());
            }
            for (Setting subSetting : groupSetting.getSubSettings()) {
                updateSettingFromJsonObject(groupObject, subSetting);
            }
        }
    }
}
