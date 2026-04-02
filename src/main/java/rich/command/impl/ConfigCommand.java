package rich.command.impl;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rich.command.Command;
import rich.command.CommandManager;
import rich.command.helpers.Paginator;
import rich.command.helpers.TabCompleteHelper;
import rich.util.config.ConfigSystem;
import rich.util.config.impl.ConfigPath;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static rich.command.impl.HelpCommand.getLine;

public class ConfigCommand extends Command {

    public ConfigCommand() {
        super("config", "Управление конфигурациями", "cfg");
    }

    @Override
    public void execute(String label, String[] args) {
        CommandManager manager = CommandManager.getInstance();

        String arg = args.length > 0 ? args[0].toLowerCase(Locale.US) : "list";

        switch (arg) {
            case "load" -> {
                if (args.length < 2) {
                    logDirect("Использование: config load <name>", Formatting.RED);
                    return;
                }
                String name = args[1];
                Path configDir = ConfigPath.getConfigDirectory();
                Path configFile = configDir.resolve(name + ".json");

                if (Files.exists(configFile)) {
                    try {
                        ConfigSystem.getInstance().load();
                        logDirect(String.format("Конфигурация %s загружена!", name));
                    } catch (Exception e) {
                        logDirect(String.format("Ошибка при загрузке конфига! Детали: %s", e.getMessage()), Formatting.RED);
                    }
                } else {
                    logDirect(String.format("Конфигурация %s не найдена!", name), Formatting.RED);
                }
            }
            case "save" -> {
                if (args.length < 2) {
                    ConfigSystem.getInstance().save();
                    logDirect("Конфигурация сохранена!");
                    return;
                }
                String name = args[1];
                try {
                    Path configDir = ConfigPath.getConfigDirectory();
                    Path newConfig = configDir.resolve(name + ".json");
                    ConfigSystem.getInstance().save();
                    Path currentConfig = ConfigPath.getConfigFile();
                    Files.copy(currentConfig, newConfig);
                    logDirect(String.format("Конфигурация %s сохранена!", name));
                } catch (Exception e) {
                    logDirect(String.format("Ошибка при сохранении конфига! Детали: %s", e.getMessage()), Formatting.RED);
                }
            }
            case "list" -> {
                int page = 1;
                if (args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {}
                }

                List<String> configs = getConfigs();

                if (configs.isEmpty()) {
                    logDirect("Конфигурации не найдены!", Formatting.RED);
                    return;
                }

                Paginator<String> paginator = new Paginator<>(configs);
                paginator.setPage(page);

                paginator.display(
                        () -> {
                            logDirectRaw(Text.literal(getLine()));
                            logDirect("§f§lСПИСОК КОНФИГОВ");
                            logDirectRaw(Text.literal(getLine()));
                        },
                        config -> {
                            MutableText namesComponent = Text.literal("  §b● §f" + config);

                            MutableText hoverText = Text.literal("§7Нажмите чтобы загрузить конфиг §f" + config);
                            String loadCommand = manager.getPrefix() + "config load " + config;

                            namesComponent.setStyle(namesComponent.getStyle()
                                    .withHoverEvent(new HoverEvent.ShowText(hoverText))
                                    .withClickEvent(new ClickEvent.RunCommand(loadCommand)));

                            return namesComponent;
                        },
                        manager.getPrefix() + label + " list"
                );
            }
            case "dir" -> {
                try {
                    Path configDir = ConfigPath.getConfigDirectory();
                    String os = System.getProperty("os.name").toLowerCase();

                    ProcessBuilder pb;
                    if (os.contains("win")) {
                        pb = new ProcessBuilder("explorer", configDir.toAbsolutePath().toString());
                    } else if (os.contains("mac")) {
                        pb = new ProcessBuilder("open", configDir.toAbsolutePath().toString());
                    } else {
                        pb = new ProcessBuilder("xdg-open", configDir.toAbsolutePath().toString());
                    }
                    pb.start();
                    logDirect("Папка с конфигурациями открыта!");
                } catch (IOException e) {
                    logDirect("Папка с конфигурациями не найдена! " + e.getMessage(), Formatting.RED);
                }
            }
            default -> {
                logDirectRaw(Text.literal(getLine()));
                logDirect("§f§lИСПОЛЬЗОВАНИЕ");
                logDirectRaw(Text.literal(getLine()));
                logDirect("§7> config load <name> §8- §fЗагружает конфиг.");
                logDirect("§7> config save <name> §8- §fСохраняет конфиг.");
                logDirect("§7> config list §8- §fВозвращает список конфигов");
                logDirect("§7> config dir §8- §fОткрывает папку с конфигами.");
                logDirectRaw(Text.literal(getLine()));
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, String[] args) {
        if (args.length == 1) {
            return new TabCompleteHelper()
                    .append("load", "save", "list", "dir")
                    .sortAlphabetically()
                    .filterPrefix(args[0])
                    .stream();
        }
        if (args.length == 2) {
            String action = args[0].toLowerCase();
            if (action.equals("load") || action.equals("save")) {
                return new TabCompleteHelper()
                        .append(getConfigs().toArray(new String[0]))
                        .filterPrefix(args[1])
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
                "Использование:",
                "> config load <name> - Загружает конфиг.",
                "> config save <name> - Сохраняет конфиг.",
                "> config list - Возвращает список конфигов",
                "> config dir - Открывает папку с конфигами."
        );
    }

    public List<String> getConfigs() {
        List<String> configs = new ArrayList<>();
        try {
            Path configDir = ConfigPath.getConfigDirectory();
            if (Files.exists(configDir)) {
                Files.list(configDir)
                        .filter(path -> path.toString().endsWith(".json"))
                        .forEach(path -> {
                            String name = path.getFileName().toString();
                            configs.add(name.substring(0, name.length() - 5));
                        });
            }
        } catch (IOException ignored) {}
        return configs;
    }
}