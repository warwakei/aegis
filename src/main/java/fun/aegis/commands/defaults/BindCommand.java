package fun.aegis.commands.defaults;

import fun.aegis.utils.client.managers.api.command.exception.CommandNotEnoughArgumentsException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.lwjgl.glfw.GLFW;

import fun.aegis.Aegis;
import fun.aegis.utils.client.managers.api.command.Command;
import fun.aegis.utils.client.managers.api.command.argument.IArgConsumer;
import fun.aegis.utils.client.managers.api.command.datatypes.KeyDataType;
import fun.aegis.utils.client.managers.api.command.datatypes.ModuleDataType;
import fun.aegis.utils.client.managers.api.command.exception.CommandException;
import fun.aegis.utils.client.managers.api.command.helpers.Paginator;
import fun.aegis.utils.client.managers.api.command.helpers.TabCompleteHelper;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleProvider;
import fun.aegis.features.module.ModuleRepository;
import fun.aegis.utils.client.chat.StringHelper;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static fun.aegis.utils.client.managers.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BindCommand extends Command {
    ModuleProvider moduleProvider;
    ModuleRepository moduleRepository;

    public BindCommand(Aegis main) {
        super("bind");
        moduleRepository = main.getModuleRepository();
        moduleProvider = main.getModuleProvider();
    }


    @Override
    public void execute(java.lang.String label, IArgConsumer args) throws CommandException {
        java.lang.String action = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
        switch (action) {
            case "add":
                handleAddBind(args);
                break;
            case "remove":
                handleRemoveBind(args);
                break;
            case "list":
                handleListBinds(args, label);
                break;
            case "clear":
                handleClearBinds(args);
                break;
            case "set":
                handleSetBind(args);
                break;
        }
    }


    private void handleSetBind(IArgConsumer args) throws CommandException {
        args.requireMin(2);
        java.lang.String target = args.getString().toLowerCase(Locale.US);

        if (target.equals("clickgui")) {
            int key = args.getDatatypeFor(KeyDataType.INSTANCE).getValue();
            ClickGuiManager.setClickGuiKey(key);
            logDirect(Formatting.GREEN + "Клавиша для открытия ClickGUI изменена на: " + Formatting.RED + StringHelper.getBindName(key).toLowerCase());
        } else {
            throw new CommandException("Неизвестная цель для установки бинда: " + target);
        }
    }

    public class ClickGuiManager {
        public static int clickGuiKey = GLFW.GLFW_KEY_RIGHT_SHIFT;

        public static void setClickGuiKey(int key) {
            clickGuiKey = key;
        }

        public static int getClickGuiKey() {
            return clickGuiKey;
        }
    }

    private void handleAddBind(IArgConsumer args) throws CommandException {
        args.requireMin(2);
        java.lang.String moduleName = args.getString();
        int key = args.getDatatypeFor(KeyDataType.INSTANCE).getValue();

        Module module = moduleProvider.get(moduleName);
        module.setKey(key);

        logDirect(Formatting.GREEN +
                "Модуль " + Formatting.RED
                + moduleName + Formatting.GREEN
                + " привязан к кнопке " + Formatting.RED
                + StringHelper.getBindName(key).toLowerCase());
    }


    private void handleRemoveBind(IArgConsumer args) throws CommandException {
        args.requireMax(1);
        java.lang.String moduleName = args.getString();
        Module module = moduleProvider.get(moduleName);
        module.setKey(GLFW.GLFW_KEY_UNKNOWN);
        logDirect(Formatting.GREEN + "Бинд для модуля " + Formatting.RED + moduleName + Formatting.GREEN + " был успешно удален!");
    }


    private void handleListBinds(IArgConsumer args, java.lang.String label) throws CommandException {
        args.requireMax(1);
        List<Module> filtredList = moduleRepository.modules()
                .stream()
                .filter(module -> module.getKey() != -1)
                .toList();

        Paginator.paginate(
                args, new Paginator<>(filtredList),
                () -> logDirect("Список модулей:"),
                module -> {
                    java.lang.String names = module.getName();
                    java.lang.String keys = StringHelper.getBindName(module.getKey()).toLowerCase();
                    return Text.literal(Formatting.GRAY + "Название: " + Formatting.WHITE + names)
                            .append(Text.literal(Formatting.GRAY + " Клавиша: " + Formatting.WHITE + keys));
                },
                FORCE_COMMAND_PREFIX + label);
    }


    private void handleClearBinds(IArgConsumer args) throws CommandException {
        args.requireMax(1);
        moduleRepository.modules().forEach(function -> function.setKey(GLFW.GLFW_KEY_UNKNOWN));
        logDirect("Все бинды модулей были удалены.", Formatting.GREEN);
    }

    @Override
    public Stream<java.lang.String> tabComplete(java.lang.String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper()
                    .sortAlphabetically()
                    .prepend("add", "remove", "list", "clear", "set")
                    .filterPrefix(args.getString())
                    .stream();
        } else {
            java.lang.String arg = args.getString();
            if (arg.equalsIgnoreCase("add")) {
                if (args.hasExactly(1)) {
                    return args.tabCompleteDatatype(ModuleDataType.INSTANCE);
                } else if (args.hasExactly(2)) {
                    return args.tabCompleteDatatype(KeyDataType.INSTANCE);
                }
            } else if (arg.equalsIgnoreCase("set")) {
                if (args.hasExactly(1)) {
                    return Stream.of("clickgui").filter(s -> {
                        try {
                            return s.startsWith(args.getString().toLowerCase(Locale.US));
                        } catch (CommandNotEnoughArgumentsException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } else if (args.hasExactly(2)) {
                    return args.tabCompleteDatatype(KeyDataType.INSTANCE);
                }
            }
        }
        return Stream.empty();
    }

    @Override
    public java.lang.String getShortDesc() {
        return "Управление биндами для модулей и GUI.";
    }

    @Override
    public List<java.lang.String> getLongDesc() {
        return Arrays.asList(
                "Эта команда позволяет управлять биндами для модулей и GUI, которые будут активироваться при нажатии определённых клавиш.",
                "",
                "Использование:",
                "> bind add <module> <key> - Привязывает модуль к указанной клавише.",
                "> bind remove <module> - Удаляет привязку модуля.",
                "> bind list - Показывает список всех текущих биндов модулей.",
                "> bind clear - Удаляет все бинды модулей.",
                "> bind set clickgui <key> - Изменяет клавишу для открытия ClickGUI."
        );
    }
}
