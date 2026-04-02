package fun.aegis.commands.defaults;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import fun.aegis.utils.client.managers.api.command.Command;
import fun.aegis.utils.client.managers.api.command.argument.IArgConsumer;
import fun.aegis.utils.client.managers.api.command.exception.CommandException;
import fun.aegis.utils.client.chat.ChatMessage;
import fun.aegis.Aegis;
import fun.aegis.features.impl.render.BlockESP;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class BlockESPCommand extends Command {
    public BlockESPCommand() {
        super("blockesp", "Управляет функцией Block ESP", "blockesp");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            sendUsage();
            return;
        }
        BlockESP blockESP = (BlockESP) Aegis.getInstance().getModuleRepository().modules().stream()
                .filter(module -> module instanceof BlockESP)
                .findFirst()
                .orElse(null);
        if (blockESP == null) {
            ChatMessage.brandmessage("Модуль Block ESP не найден");
            return;
        }
        String subCommand = args.getString().toLowerCase();
        switch (subCommand) {
            case "add":
                if (args.hasAny()) {
                    String blockToAdd = args.getString();
                    Block block = Registries.BLOCK.get(Identifier.tryParse(blockToAdd));
                    if (block != null && block != Blocks.AIR) {
                        String registryName = Registries.BLOCK.getId(block).toString();
                        if (blockESP.getBlocksToHighlight().add(registryName)) {
                            ChatMessage.brandmessage("Добавлен блок " + registryName + " в Block ESP");
                        } else {
                            ChatMessage.brandmessage("Блок " + registryName + " уже есть в Block ESP");
                        }
                    } else {
                        ChatMessage.brandmessage("Блок " + blockToAdd + " не найден");
                    }
                } else {
                    ChatMessage.brandmessage("Укажите блок для добавления: .blockesp add <block>");
                }
                break;
            case "remove":
                if (args.hasAny()) {
                    String blockToRemove = args.getString();
                    Block block = Registries.BLOCK.get(Identifier.tryParse(blockToRemove));
                    if (block != null && block != Blocks.AIR) {
                        String registryName = Registries.BLOCK.getId(block).toString();
                        if (blockESP.getBlocksToHighlight().remove(registryName)) {
                            ChatMessage.brandmessage("Удалён блок " + registryName + " из Block ESP");
                        } else {
                            ChatMessage.brandmessage("Блок " + blockToRemove + " не найден в Block ESP");
                        }
                    } else {
                        ChatMessage.brandmessage("Блок " + blockToRemove + " не найден");
                    }
                } else {
                    ChatMessage.brandmessage("Укажите блок для удаления: .blockesp remove <block>");
                }
                break;
            case "clear":
                blockESP.getBlocksToHighlight().clear();
                ChatMessage.brandmessage("Список Block ESP очищен");
                break;
            case "list":
                ChatMessage.brandmessage("Список всех блоков в Minecraft:");
                Registries.BLOCK.getIds().stream()
                        .map(Identifier::toString)
                        .sorted()
                        .forEach(blockName -> ChatMessage.brandmessage("- " + blockName));
                break;
            default:
                sendUsage();
        }
    }

    public void sendUsage() {
        ChatMessage.helpmessage("Пример использования команды .blockesp:");
        ChatMessage.brandmessage(".blockesp add <block> - Добавить блок в Block ESP (например: .blockesp add minecraft:diamond_ore)");
        ChatMessage.brandmessage(".blockesp remove <block> - Удалить блок из Block ESP (например: .blockesp remove minecraft:diamond_ore)");
        ChatMessage.brandmessage(".blockesp clear - Очистить список Block ESP");
        ChatMessage.brandmessage(".blockesp list - Показать все блоки в Minecraft");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            return Stream.of("add", "remove", "clear", "list");
        }

        if (args.hasExactlyOne()) {
            String partial = args.peekString().toLowerCase();
            return Stream.of("add", "remove", "clear", "list")
                    .filter(cmd -> cmd.startsWith(partial));
        }

        String subCommand = args.getString().toLowerCase();

        if (("add".equals(subCommand) || "remove".equals(subCommand)) && args.hasExactlyOne()) {
            String partial = args.peekString().toLowerCase();
            return Registries.BLOCK.getIds().stream()
                    .map(Identifier::toString)
                    .filter(blockName -> blockName.toLowerCase().startsWith(partial));
        }

        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управляет функцией Block ESP.";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Управляет подсветкой блоков в модуле Block ESP.",
                "",
                "Использование:",
                ".blockesp add <block> - Добавить блок в список подсветки.",
                ".blockesp remove <block> - Удалить блок из списка подсветки.",
                ".blockesp clear - Очистить список подсвечиваемых блоков.",
                ".blockesp list - Показать все доступные блоки."
        );
    }
}
