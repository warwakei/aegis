package fun.aegis.utils.client.managers.file.impl;

import com.google.gson.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import fun.aegis.display.screens.clickgui.newgui.theme.Theme;
import fun.aegis.display.screens.clickgui.newgui.theme.ThemeManager;
import fun.aegis.utils.client.managers.file.ClientFile;
import fun.aegis.utils.client.managers.file.exception.FileLoadException;
import fun.aegis.utils.client.managers.file.exception.FileSaveException;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ThemeFile extends ClientFile {
    Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public ThemeFile() {
        super("Theme");
    }

    @Override
    public void saveToFile(File path) throws FileSaveException {
        try {
            if (!path.exists()) {
                path.mkdirs();
            }

            JsonObject themeObject = new JsonObject();
            Theme currentTheme = ThemeManager.getInstance().getCurrentTheme();
            themeObject.addProperty("currentTheme", currentTheme.getName());

            File file = new File(path, getName() + ".json");
            try (FileWriter writer = new FileWriter(file)) {
                GSON.toJson(themeObject, writer);
                writer.flush();
            }
        } catch (IOException e) {
            throw new FileSaveException("Failed to save theme to file", e);
        }
    }

    @Override
    public void loadFromFile(File path) throws FileLoadException {
        try {
            File file = new File(path, getName() + ".json");
            if (!file.exists() || file.length() == 0) {
                return;
            }

            try (FileReader reader = new FileReader(file)) {
                JsonElement jsonElement = JsonParser.parseReader(reader);
                if (jsonElement == null || jsonElement.isJsonNull()) {
                    return;
                }

                JsonObject themeObject = jsonElement.getAsJsonObject();
                if (themeObject.has("currentTheme")) {
                    String themeName = themeObject.get("currentTheme").getAsString();
                    Theme theme = getThemeByName(themeName);
                    if (theme != null) {
                        ThemeManager.getInstance().setTheme(theme);
                    }
                }
            }
        } catch (IOException | JsonSyntaxException e) {
            throw new FileLoadException("Failed to load theme from file", e);
        }
    }

    private Theme getThemeByName(String name) {
        return switch (name) {
            case "Dark" -> Theme.DARK;
            case "Light" -> Theme.LIGHT;
            case "Purple" -> Theme.PURPLE;
            case "Blue" -> Theme.BLUE;
            case "Cyan" -> Theme.CYAN;
            case "Green" -> Theme.GREEN;
            case "Red" -> Theme.RED;
            case "Orange" -> Theme.ORANGE;
            case "Neon" -> Theme.NEON;
            case "Sunset" -> Theme.SUNSET;
            case "Ocean" -> Theme.OCEAN;
            case "Forest" -> Theme.FOREST;
            case "Lavender" -> Theme.LAVENDER;
            case "Coral" -> Theme.CORAL;
            case "Mint" -> Theme.MINT;
            case "Peach" -> Theme.PEACH;
            case "Midnight" -> Theme.MIDNIGHT;
            case "Aurora" -> Theme.AURORA;
            case "Cyberpunk" -> Theme.CYBERPUNK;
            case "Sakura" -> Theme.SAKURA;
            default -> Theme.DARK;
        };
    }
}
