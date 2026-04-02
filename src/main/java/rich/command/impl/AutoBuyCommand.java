package rich.command.impl;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rich.command.Command;
import rich.command.helpers.TabCompleteHelper;
import rich.modules.impl.misc.AutoBuy;
import rich.screens.clickgui.impl.autobuy.items.ItemRegistry;
import rich.screens.clickgui.impl.autobuy.manager.AutoBuyManager;
import rich.util.config.impl.autobuyconfig.AutoBuyConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static rich.command.impl.HelpCommand.getLine;

public class AutoBuyCommand extends Command {

    public AutoBuyCommand() {
        super("autobuy", "Управление конфигурацией автобая", "ab");
    }

    @Override
    public void execute(String label, String[] args) {
        String arg = args.length > 0 ? args[0].toLowerCase(Locale.US) : "";

        switch (arg) {
            case "load" -> {
                try {
                    AutoBuyConfig.getInstance().load();
                    ItemRegistry.reloadSettings();
                    logDirect("Конфигурация автобая загружена!", Formatting.GREEN);
                } catch (Exception e) {
                    logDirect("Ошибка при загрузке конфигурации: " + e.getMessage(), Formatting.RED);
                }
            }
            case "save" -> {
                try {
                    AutoBuyConfig.getInstance().save();
                    logDirect("Конфигурация автобая сохранена!", Formatting.GREEN);
                } catch (Exception e) {
                    logDirect("Ошибка при сохранении конфигурации: " + e.getMessage(), Formatting.RED);
                }
            }
            case "reset" -> {
                try {
                    AutoBuyConfig.getInstance().reset();
                    ItemRegistry.clearCache();
                    ItemRegistry.ensureSettingsLoaded();
                    logDirect("Конфигурация автобая сброшена!", Formatting.GREEN);
                } catch (Exception e) {
                    logDirect("Ошибка при сбросе конфигурации: " + e.getMessage(), Formatting.RED);
                }
            }
            case "status" -> {
                int totalItems = ItemRegistry.getAllItems().size();
                long enabledItems = ItemRegistry.getAllItems().stream().filter(i -> i.isEnabled()).count();
                AutoBuy autoBuyModule = AutoBuy.getInstance();
                boolean moduleActive = autoBuyModule != null && autoBuyModule.isState();
                boolean buttonEnabled = AutoBuyManager.getInstance().isEnabled();

                String currentStatus;
                if (!moduleActive) {
                    currentStatus = "§cOFF";
                } else if (buttonEnabled) {
                    currentStatus = "§aON";
                } else {
                    currentStatus = "§ePAUSE";
                }

                logDirectRaw(Text.literal(getLine()));
                logDirect("§f§lСТАТУС АВТОБАЯ");
                logDirectRaw(Text.literal(getLine()));
                logDirect("§7Статус: " + currentStatus);
                logDirect("§7Модуль: " + (moduleActive ? "§aВключен" : "§cВыключен"));
                logDirect("§7Кнопка: " + (buttonEnabled ? "§aВключена" : "§cВыключена"));
                logDirect("§7Активных предметов: §b" + enabledItems + "§7/§b" + totalItems);

                if (autoBuyModule != null && moduleActive) {
                    int clients = autoBuyModule.getNetworkManager().getConnectedClientCount();
                    int inAuction = autoBuyModule.getNetworkManager().getClientsInAuctionCount();
                    boolean connected = autoBuyModule.getNetworkManager().isConnectedToServer();
                    boolean isServer = autoBuyModule.getNetworkManager().isServerRunning();

                    if (isServer) {
                        logDirect("§7Режим: §bПокупающий (Сервер)");
                        logDirect("§7Подключённых клиентов: §b" + clients);
                        logDirect("§7В аукционе: §b" + inAuction);
                    } else {
                        logDirect("§7Режим: §bПроверяющий (Клиент)");
                        logDirect("§7Подключение к серверу: " + (connected ? "§aДа" : "§cНет"));
                    }
                }
                logDirectRaw(Text.literal(getLine()));
            }
            default -> {
                logDirectRaw(Text.literal(getLine()));
                logDirect("§f§lАВТОБАЙ");
                logDirectRaw(Text.literal(getLine()));
                logDirect("§7> autobuy load §8- §fЗагружает конфигурацию");
                logDirect("§7> autobuy save §8- §fСохраняет конфигурацию");
                logDirect("§7> autobuy reset §8- §fСбрасывает конфигурацию");
                logDirect("§7> autobuy status §8- §fПоказывает статус автобая");
                logDirectRaw(Text.literal(getLine()));
            }
        }
    }

    @Override
    public Stream<String> tabComplete(String label, String[] args) {
        if (args.length == 1) {
            return new TabCompleteHelper()
                    .append("load", "save", "reset", "status")
                    .sortAlphabetically()
                    .filterPrefix(args[0])
                    .stream();
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управление конфигурацией автобая";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда для управления конфигурацией автобая",
                "Использование:",
                "> autobuy load - Загружает конфигурацию из файла",
                "> autobuy save - Сохраняет текущую конфигурацию",
                "> autobuy reset - Сбрасывает конфигурацию к значениям по умолчанию",
                "> autobuy status - Показывает текущий статус автобая"
        );
    }
}