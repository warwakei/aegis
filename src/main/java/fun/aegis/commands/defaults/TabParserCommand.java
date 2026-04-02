package fun.aegis.commands.defaults;

import fun.aegis.features.impl.misc.TabParser;
import fun.aegis.utils.client.managers.api.command.Command;
import fun.aegis.utils.client.managers.api.command.argument.IArgConsumer;
import fun.aegis.utils.client.managers.api.command.exception.CommandException;
import fun.aegis.utils.client.chat.ChatMessage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class TabParserCommand extends Command {
    public TabParserCommand() {
        super("tabparser");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            sendUsage();
            return;
        }

        String subCommand = args.getString().toLowerCase();

        if (subCommand.equals("dir")) {
            File outputFile = new File(net.minecraft.client.MinecraftClient.getInstance().runDirectory, "tabparser_results.txt");

            if (!outputFile.exists()) {
                ChatMessage.brandmessage("Файл с результатами парсинга не найден! Сначала запустите модуль Tab Parser.");
                return;
            }

            try {
                String os = System.getProperty("os.name").toLowerCase();
                if (os.contains("win")) {
                    Runtime.getRuntime().exec("explorer /select," + outputFile.getAbsolutePath());
                } else if (os.contains("mac")) {
                    Runtime.getRuntime().exec("open -R " + outputFile.getAbsolutePath());
                } else if (os.contains("nix") || os.contains("nux")) {
                    Runtime.getRuntime().exec("xdg-open " + outputFile.getParent());
                }
                ChatMessage.brandmessage("Файл открыт: " + outputFile.getName());
            } catch (IOException e) {
                ChatMessage.brandmessage("Ошибка при открытии файла: " + e.getMessage());
            }
        } else {
            sendUsage();
        }
    }

    public void sendUsage() {
        ChatMessage.helpmessage("Пример использования команды .tabparser:");
        ChatMessage.brandmessage(".tabparser dir - Открыть файл с результатами парсинга");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            return Stream.of("dir");
        }

        if (args.hasExactlyOne()) {
            String partial = args.peekString().toLowerCase();
            return Stream.of("dir").filter(cmd -> cmd.startsWith(partial));
        }

        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управляет функцией Tab Parser.";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Управляет модулем парсинга игроков с донатами.",
                "",
                "Использование:",
                ".tabparser dir - Открыть файл с результатами парсинга."
        );
    }
}
