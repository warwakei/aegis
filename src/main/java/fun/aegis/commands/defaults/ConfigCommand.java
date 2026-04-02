package fun.aegis.commands.defaults;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import fun.aegis.utils.client.managers.file.FileRepository;
import fun.aegis.utils.client.managers.file.impl.ModuleFile;
import fun.aegis.display.screens.clickgui.components.implement.other.BackgroundComponent;
import fun.aegis.Aegis;
import fun.aegis.main.client.ClientInfoProvider;
import fun.aegis.utils.client.managers.api.command.Command;
import fun.aegis.utils.client.managers.api.command.argument.IArgConsumer;
import fun.aegis.utils.client.managers.api.command.datatypes.ConfigFileDataType;
import fun.aegis.utils.client.managers.api.command.exception.CommandException;
import fun.aegis.utils.client.managers.api.command.helpers.Paginator;
import fun.aegis.utils.client.managers.api.command.helpers.TabCompleteHelper;
import fun.aegis.utils.client.managers.file.FileController;
import fun.aegis.utils.client.managers.file.exception.FileProcessingException;
import fun.aegis.utils.client.sound.SoundManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static fun.aegis.utils.client.managers.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ConfigCommand extends Command {
    FileController fileController;
    ClientInfoProvider clientInfoProvider;

    protected ConfigCommand(Aegis main) {
        super("config", "cfg");
        this.fileController = main.getFileController();
        this.clientInfoProvider = main.getClientInfoProvider();
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String arg = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
        args.requireMax(1);
        if (arg.contains("load")) {
            String name = args.getString();
            File customDir = new File(clientInfoProvider.configsDir(), "Custom");
            File configFile = new File(customDir, name + ".clysm");
            if (configFile.exists() && configFile.length() > 0) {
                try {
                    // Создаём ModuleFile и загружаем напрямую из папки Custom
                    ModuleFile moduleFile = new ModuleFile(Aegis.getInstance().getModuleRepository(), Aegis.getInstance().getDraggableRepository());
                    moduleFile.loadFromFile(customDir, name + ".clysm");
                    logDirect(String.format("Конфигурация %s загружена!", name));
                    SoundManager.playSound(SoundManager.LOADED);
                } catch (Exception e) {
                    logDirect(String.format("Ошибка при загрузке конфига! Детали: %s", e.getMessage()), Formatting.RED);
                }
            } else {
                logDirect(String.format("Конфигурация %s не найдена или пустая!", name));
            }
        }
        if (arg.contains("save")) {

            String name = args.getString();
            File customDir = new File(clientInfoProvider.configsDir(), "Custom");

            try {
                // Используем существующий FileController и ModuleFile
                var fileController = Aegis.getInstance().getFileController();
                // Сохраняем только ModuleFile (конфигурацию модулей) в папку Custom
                ModuleFile moduleFile = new ModuleFile(Aegis.getInstance().getModuleRepository(), Aegis.getInstance().getDraggableRepository());
                moduleFile.saveToFile(customDir, name + ".clysm");
                
                // Проверяем что файл сохранился корректно
                File savedFile = new File(customDir, name + ".clysm");
                if (savedFile.exists() && savedFile.length() > 0) {
                    logDirect(String.format("Конфигурация %s сохранена!", name));
                    SoundManager.playSound(SoundManager.SAVED);
                    System.out.println("saved");
                } else {
                    logDirect("Ошибка: конфиг сохранен но файл пустой!", Formatting.RED);
                }
                
                // Обновляем GUI если открыт
                refreshGuiConfigs();
            } catch (Exception e) {
                System.out.println(e.getMessage());
                System.out.printf("error %s%n", e.getCause().getMessage());
                logDirect(String.format("Ошибка при сохранении конфига! Детали: %s", e.getCause().getMessage()), Formatting.RED);
            }
        }
        if (arg.contains("list")) {
            Paginator.paginate(
                    args, new Paginator<>(
                            getConfigs()),
                    () -> logDirect("Список конфигов:"),
                    config -> {
                        MutableText namesComponent = Text.literal(config);
                        namesComponent.setStyle(namesComponent.getStyle().withColor(Formatting.WHITE));
                        return namesComponent;
                    },
                    FORCE_COMMAND_PREFIX + label
            );
        }
        if (arg.contains("dir")) {
            try {
                File customDir = new File(clientInfoProvider.configsDir(), "Custom");
                Runtime.getRuntime().exec("explorer " + customDir.getAbsolutePath());
            } catch (IOException e) {
                logDirect("Папка с конфигурациями не найдена!" + e.getMessage());
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasAny()) {
            String arg = args.getString();
            if (args.hasExactlyOne()) {
                if (arg.equalsIgnoreCase("load")) {
                    return args.tabCompleteDatatype(ConfigFileDataType.INSTANCE);
                } else if (arg.equalsIgnoreCase("save")) {
                    return args.tabCompleteDatatype(ConfigFileDataType.INSTANCE);
                }
            } else {
                return new TabCompleteHelper()
                        .sortAlphabetically()
                        .prepend("load", "save", "list", "dir")
                        .filterPrefix(arg)
                        .stream();
            }
        }
        return Stream.empty();
    }


    @Override
    public String getShortDesc() {
        return "Позволяет взаимодействовать с конфигами в чите";
    }


    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "С помощью этой команды можно загружать/сохранять конфиги",
                "",
                "Использование:",
                "> config load <name> - Загружает конфиг.",
                "> config save <name> - Сохраняет конфиг.",
                "> config list - Возвращает список конфигов",
                "> config dir - Открывает папку с конфигами."
        );
    }

    public List<String> getConfigs() {
        List<String> configs = new ArrayList<>();
        File customDir = new File(Aegis.getInstance().getClientInfoProvider().configsDir(), "Custom");
        File[] configFiles = customDir.listFiles();

        if (configFiles != null) {
            for (File configFile : configFiles) {
                if (configFile.isFile() && configFile.getName().endsWith(".clysm")) {
                    String configName = configFile.getName().replace(".clysm", "");
                    configs.add(configName);
                }
            }
        }

        return configs;
    }
    
    private void refreshGuiConfigs() {
        // Обновляем GUI если открыт и находится в категории CONFIGS
        try {
            var menuScreen = fun.aegis.display.screens.clickgui.MenuScreen.INSTANCE;
            // Проверка на THEMES категория удалена
        } catch (Exception e) {
            // Игнорируем ошибки при обновлении GUI
        }
    }
}
