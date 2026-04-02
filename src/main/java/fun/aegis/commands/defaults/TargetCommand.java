package fun.aegis.commands.defaults;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.client.managers.api.command.Command;
import fun.aegis.utils.client.managers.api.command.argument.IArgConsumer;
import fun.aegis.utils.client.managers.api.command.datatypes.TabPlayerDataType;
import fun.aegis.utils.client.managers.api.command.datatypes.TargetDataType;
import fun.aegis.utils.client.managers.api.command.exception.CommandException;
import fun.aegis.utils.client.managers.api.command.helpers.Paginator;
import fun.aegis.utils.client.managers.api.command.helpers.TabCompleteHelper;
import fun.aegis.utils.client.managers.api.command.manager.ICommandManager;
import fun.aegis.common.repository.target.TargetRepository;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static fun.aegis.utils.client.managers.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

public class TargetCommand extends Command implements QuickImports {

    private final TargetRepository repository = TargetRepository.getInstance();

    public TargetCommand() {
        super("target");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            handleListTargets(label, args);
            return;
        }

        String action = args.peekString().toLowerCase(Locale.US);

        switch (action) {
            case "add" -> {
                args.get();
                handleAddTarget(args);
            }
            case "remove" -> {
                args.get();
                handleRemoveTarget(args);
            }
            case "clear" -> {
                args.get();
                handleClearTargets(args);
            }
            case "list" -> {
                args.get();
                handleListTargets(label, args);
            }
            default -> handleAddTarget(args);
        }
    }

    private void handleAddTarget(IArgConsumer args) throws CommandException {
        args.requireMin(1);
        String name = args.getString();
        if (!repository.isTarget(name)) {
            repository.addTarget(name);
            logDirect("Игрок " + Formatting.GREEN + name + Formatting.GRAY + " добавлен в приоритетный список.", Formatting.GRAY);
        } else {
            logDirect("Игрок " + Formatting.RED + name + Formatting.GRAY + " уже в приоритетном списке.", Formatting.RED);
        }
    }

    private void handleRemoveTarget(IArgConsumer args) throws CommandException {
        args.requireMin(1);
        String name = args.getString();
        if (repository.isTarget(name)) {
            repository.removeTarget(name);
            logDirect("Игрок " + Formatting.RED + name + Formatting.GRAY + " удален из приоритетного списка.", Formatting.GRAY);
        } else {
            logDirect("Игрок " + Formatting.RED + name + Formatting.GRAY + " не найден в приоритетном списке.", Formatting.RED);
        }
    }

    private void handleClearTargets(IArgConsumer args) throws CommandException {
        args.requireMax(0);
        repository.clearTargets();
        logDirect("Приоритетный список целей очищен.", Formatting.GREEN);
    }

    private void handleListTargets(String label, IArgConsumer args) throws CommandException {
        args.requireMax(1);
        Paginator.paginate(args, new Paginator<>(repository.getTargets()),
                () -> logDirect("Приоритетный список целей:"),
                targetName -> Text.literal(Formatting.GRAY + "- " + Formatting.WHITE + targetName),
                FORCE_COMMAND_PREFIX + label + " list");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper().prepend("add", "remove", "list", "clear").filterPrefix(args.peekString()).addCommands((ICommandManager) mc.getNetworkHandler().getCommandSource()).stream();
        }
        if (args.hasAtMost(2) && args.peekString(0).equalsIgnoreCase("add")) {
            return args.tabCompleteDatatype(TabPlayerDataType.INSTANCE);
        }
        if (args.hasAtMost(2) && args.peekString(0).equalsIgnoreCase("remove")) {
            return args.tabCompleteDatatype(TargetDataType.INSTANCE);
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управляет приоритетными целями для Aura.";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Добавляет игроков в приоритетный список для модуля Aura.",
                "",
                "Использование:",
                "> target <ник> - Добавить игрока в приоритет.",
                "> target add <ник> - Добавить игрока в приоритет.",
                "> target remove <ник> - Удалить игрока из приоритета.",
                "> target list - Показать список приоритетных игроков.",
                "> target clear - Очистить список."
        );
    }
}
