package rich.command.impl;

import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;
import rich.Initialization;
import rich.command.Command;
import rich.command.CommandManager;
import rich.command.helpers.Paginator;
import rich.command.helpers.TabCompleteHelper;
import rich.modules.module.ModuleRepository;
import rich.modules.module.ModuleStructure;
import rich.util.config.ConfigSystem;
import rich.util.config.impl.bind.BindConfig;
import rich.util.string.KeyHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static rich.command.impl.HelpCommand.getLine;

public class BindCommand extends Command {

    public BindCommand() {
        super("bind", "Управление биндами модулей", "b");
    }

    @Override
    public void execute(String label, String[] args) {
        CommandManager manager = CommandManager.getInstance();
        ModuleRepository repository = getModuleRepository();

        if (repository == null) {
            logDirect("Репозиторий модулей не найден!", Formatting.RED);
            return;
        }

        String action = args.length > 0 ? args[0].toLowerCase(Locale.US) : "list";

        switch (action) {
            case "add" -> {
                if (args.length < 3) {
                    logDirect("Использование: bind add <module> <key>", Formatting.RED);
                    return;
                }

                String moduleName = args[1];
                String keyName = args[2];

                ModuleStructure module = findModule(repository, moduleName);
                if (module == null) {
                    logDirect(String.format("Модуль %s не найден!", moduleName), Formatting.RED);
                    return;
                }

                int key = KeyHelper.getKeyCode(keyName);
                if (key == -1) {
                    logDirect(String.format("Неизвестная клавиша: %s", keyName), Formatting.RED);
                    return;
                }

                module.setKey(key);
                ConfigSystem.getInstance().save();

                logDirect(String.format("§aМодуль §f%s §aпривязан к клавише §f%s",
                        module.getName(), KeyHelper.getKeyName(key).toLowerCase()), Formatting.GREEN);
            }
            case "remove", "del", "delete" -> {
                if (args.length < 2) {
                    logDirect("Использование: bind remove <module>", Formatting.RED);
                    return;
                }

                String moduleName = args[1];
                ModuleStructure module = findModule(repository, moduleName);

                if (module == null) {
                    logDirect(String.format("Модуль %s не найден!", moduleName), Formatting.RED);
                    return;
                }

                module.setKey(GLFW.GLFW_KEY_UNKNOWN);
                ConfigSystem.getInstance().save();

                logDirect(String.format("Бинд для модуля %s удален!", module.getName()), Formatting.GREEN);
            }
            case "clear" -> {
                int count = 0;
                for (ModuleStructure module : repository.modules()) {
                    if (module.getKey() != GLFW.GLFW_KEY_UNKNOWN) {
                        module.setKey(GLFW.GLFW_KEY_UNKNOWN);
                        count++;
                    }
                }
                ConfigSystem.getInstance().save();
                logDirect(String.format("Все бинды модулей удалены! Удалено: %d", count), Formatting.GREEN);
            }
            case "set" -> {
                if (args.length < 3) {
                    logDirect("Использование: bind set <target> <key>", Formatting.RED);
                    logDirect("Доступные цели: Bind", Formatting.RED);
                    return;
                }

                String target = args[1].toLowerCase(Locale.US);
                String keyName = args[2];

                int key = KeyHelper.getKeyCode(keyName);
                if (key == -1) {
                    logDirect(String.format("Неизвестная клавиша: %s", keyName), Formatting.RED);
                    return;
                }

                if (target.equals("Bind")) {
                    BindConfig.getInstance().setKeyAndSave(key);
                    logDirect(String.format("§aКлавиша для Bind изменена на: §f%s",
                            KeyHelper.getKeyName(key).toLowerCase()), Formatting.GREEN);
                } else {
                    logDirect(String.format("Неизвестная цель: %s", target), Formatting.RED);
                }
            }
            case "list" -> {
                int page = 1;
                if (args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {}
                }

                List<ModuleStructure> boundModules = repository.modules().stream()
                        .filter(m -> m.getKey() != GLFW.GLFW_KEY_UNKNOWN && m.getKey() != -1)
                        .collect(Collectors.toList());

                if (boundModules.isEmpty()) {
                    logDirect("Нет модулей с биндами!", Formatting.RED);
                    return;
                }

                Paginator<ModuleStructure> paginator = new Paginator<>(boundModules);
                paginator.setPage(page);

                paginator.display(
                        () -> {
                            logDirectRaw(Text.literal(getLine()));
                            logDirect("§f§lСПИСОК БИНДОВ §7(" + boundModules.size() + ")");
                            logDirectRaw(Text.literal(getLine()));
                        },
                        module -> {
                            String name = module.getName();
                            String keyName = KeyHelper.getKeyName(module.getKey()).toLowerCase();

                            MutableText component = Text.literal("  §b● §f" + name)
                                    .append(Text.literal(" §8[§7" + keyName + "§8]"));

                            MutableText hoverText = Text.literal("§7Нажмите чтобы удалить бинд для §f" + name);
                            String removeCommand = manager.getPrefix() + "bind remove " + name;

                            component.setStyle(component.getStyle()
                                    .withHoverEvent(new HoverEvent.ShowText(hoverText))
                                    .withClickEvent(new ClickEvent.RunCommand(removeCommand)));

                            return component;
                        },
                        manager.getPrefix() + label + " list"
                );
            }
            default -> {
                logDirectRaw(Text.literal(getLine()));
                logDirect("§f§lУПРАВЛЕНИЕ БИНДАМИ");
                logDirectRaw(Text.literal(getLine()));
                logDirect("§7> bind add <module> <key> §8- §fПривязать модуль к клавише");
                logDirect("§7> bind remove <module> §8- §fУдалить бинд модуля");
                logDirect("§7> bind list §8- §fПоказать список биндов");
                logDirect("§7> bind clear §8- §fУдалить все бинды");
                logDirect("§7> bind set Bind <key> §8- §fИзменить клавишу Bind");
                logDirectRaw(Text.literal(getLine()));
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, String[] args) {
        ModuleRepository repository = getModuleRepository();

        if (args.length == 1) {
            return new TabCompleteHelper()
                    .append("add", "remove", "list", "clear", "set")
                    .sortAlphabetically()
                    .filterPrefix(args[0])
                    .stream();
        }
        if (args.length == 2) {
            String action = args[0].toLowerCase();
            if (action.equals("add")) {
                return new TabCompleteHelper()
                        .append(getModuleNames(repository))
                        .filterPrefix(args[1])
                        .stream();
            }
            if (action.equals("remove") || action.equals("del") || action.equals("delete")) {
                return new TabCompleteHelper()
                        .append(getBoundModuleNames(repository))
                        .filterPrefix(args[1])
                        .stream();
            }
            if (action.equals("set")) {
                return new TabCompleteHelper()
                        .append("Bind")
                        .filterPrefix(args[1])
                        .stream();
            }
        }
        if (args.length == 3) {
            String action = args[0].toLowerCase();
            if (action.equals("add") || action.equals("set")) {
                return new TabCompleteHelper()
                        .append(KeyHelper.getAllKeyNames())
                        .filterPrefix(args[2])
                        .stream();
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управление биндами модулей";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда для управления биндами модулей и GUI",
                "Использование:",
                "> bind add <module> <key> - Привязать модуль к клавише",
                "> bind remove <module> - Удалить бинд модуля",
                "> bind list - Показать список биндов",
                "> bind clear - Удалить все бинды",
                "> bind set Bind <key> - Изменить клавишу Bind"
        );
    }

    private ModuleRepository getModuleRepository() {
        Initialization instance = Initialization.getInstance();
        if (instance != null && instance.getManager() != null) {
            return instance.getManager().getModuleRepository();
        }
        return null;
    }

    private ModuleStructure findModule(ModuleRepository repository, String name) {
        return repository.modules().stream()
                .filter(m -> m.getName().equalsIgnoreCase(name))
                .findFirst()
                .orElse(null);
    }

    private String[] getModuleNames(ModuleRepository repository) {
        if (repository == null) return new String[0];
        return repository.modules().stream()
                .map(ModuleStructure::getName)
                .toArray(String[]::new);
    }

    private String[] getBoundModuleNames(ModuleRepository repository) {
        if (repository == null) return new String[0];
        return repository.modules().stream()
                .filter(m -> m.getKey() != GLFW.GLFW_KEY_UNKNOWN && m.getKey() != -1)
                .map(ModuleStructure::getName)
                .toArray(String[]::new);
    }
}