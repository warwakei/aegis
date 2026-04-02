package fun.aegis.commands.defaults;

import fun.aegis.utils.client.managers.api.command.Command;
import fun.aegis.utils.client.managers.api.command.argument.IArgConsumer;
import fun.aegis.utils.client.managers.api.command.datatypes.StaffDataType;
import fun.aegis.utils.client.managers.api.command.datatypes.TabPlayerDataType;
import fun.aegis.utils.client.managers.api.command.exception.CommandException;
import fun.aegis.utils.client.managers.api.command.helpers.Paginator;
import fun.aegis.utils.client.managers.api.command.helpers.TabCompleteHelper;
import fun.aegis.common.repository.staff.StaffRepository;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static fun.aegis.utils.client.managers.api.command.IBaritoneChatControl.FORCE_COMMAND_PREFIX;

public class StaffCommand extends Command {

    public StaffCommand() {
        super("staff");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        String action = args.hasAny() ? args.getString().toLowerCase(Locale.US) : "list";
        
        switch (action) {
            case "add" -> {
                args.requireMin(1);
                String name = args.getString();
                if (!StaffRepository.isStaff(name)) {
                    StaffRepository.addStaff(name);
                    save(); // Сохраняем изменения
                    logDirect("Вы успешно добавили " + Formatting.GREEN + name + Formatting.GRAY + " в список персонала!");
                } else {
                    logDirect(Formatting.RED + name + " уже есть в списке персонала!", Formatting.RED);
                }
            }
            case "remove" -> {
                args.requireMin(1);
                String name = args.getString();
                if (StaffRepository.isStaff(name)) {
                    StaffRepository.removeStaff(name);
                    save(); // Сохраняем изменения
                    logDirect("Вы успешно удалили " + Formatting.RED + name + Formatting.GRAY + " из списка персонала!");
                } else {
                    logDirect(Formatting.RED + name + " не найден в списке персонала", Formatting.RED);
                }
            }
            case "list" -> {
                args.requireMax(1);
                Paginator.paginate(
                        args, new Paginator<>(StaffRepository.getStaff()),
                        () -> logDirect("Список персонала:"),
                        staff -> {
                            MutableText namesComponent = Text.literal(staff.getName());
                            namesComponent.setStyle(namesComponent.getStyle().withColor(Formatting.WHITE));
                            return namesComponent;
                        },
                        FORCE_COMMAND_PREFIX + label
                );
            }
            case "clear" -> {
                args.requireMax(0);
                StaffRepository.clear();
                save(); // Сохраняем изменения
                logDirect("Список персонала очищен.");
            }
            default -> logDirect("Неизвестная подкоманда. Используйте: add, remove, list, clear");
        }
    }

    private void save() {
        try {
            // Используем рефлексию для вызова приватного метода save() в StaffRepository
            Method saveMethod = StaffRepository.class.getDeclaredMethod("save");
            saveMethod.setAccessible(true);
            saveMethod.invoke(null);
        } catch (ReflectiveOperationException e) {
            // Оборачиваем проверяемое исключение в непроверяемое, чтобы не изменять сигнатуру execute()
            throw new RuntimeException("Ошибка при сохранении списка персонала через рефлексию", e);
        }
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper().sortAlphabetically().prepend("add", "remove", "list", "clear").filterPrefix(args.getString()).stream();
        }

        if (args.hasAtMost(2)) {
            String arg = args.peekString(0).toLowerCase(Locale.US);
            if (arg.equals("add")) {
                return args.tabCompleteDatatype(TabPlayerDataType.INSTANCE);
            } else if (arg.equals("remove")) {
                return args.tabCompleteDatatype(StaffDataType.INSTANCE);
            }
        }

        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управление списком персонала.";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Эта команда позволяет управлять списком персонала.",
                "",
                "Использование:",
                "> staff add <ник> - Добавляет игрока в список персонала.",
                "> staff remove <ник> - Удаляет игрока из списка персонала.",
                "> staff list - Показывает список персонала.",
                "> staff clear - Очищает список персонала."
        );
    }
}
