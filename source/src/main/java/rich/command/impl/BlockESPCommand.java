package rich.command.impl;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import rich.Initialization;
import rich.command.Command;
import rich.command.CommandManager;
import rich.command.helpers.Paginator;
import rich.command.helpers.TabCompleteHelper;
import rich.modules.impl.render.BlockESP;
import rich.util.config.impl.blockesp.BlockESPConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static rich.command.impl.HelpCommand.getLine;

public class BlockESPCommand extends Command {

    public BlockESPCommand() {
        super("blockesp", "Управление блоками для BlockESP", "besp");
    }

    @Override
    public void execute(String label, String[] args) {
        CommandManager manager = CommandManager.getInstance();
        BlockESPConfig config = BlockESPConfig.getInstance();

        String action = args.length > 0 ? args[0].toLowerCase(Locale.US) : "help";

        switch (action) {
            case "add" -> {
                if (args.length < 2) {
                    logDirect("Использование: blockesp add <block>", Formatting.RED);
                    logDirect("Пример: blockesp add minecraft:diamond_ore", Formatting.RED);
                    return;
                }

                String blockId = args[1].toLowerCase();
                if (!blockId.contains(":")) {
                    blockId = "minecraft:" + blockId;
                }

                Block block = Registries.BLOCK.get(Identifier.tryParse(blockId));
                if (block == null || block == Blocks.AIR) {
                    logDirect(String.format("Блок %s не найден!", args[1]), Formatting.RED);
                    return;
                }

                String registryName = Registries.BLOCK.getId(block).toString();

                if (config.hasBlock(registryName)) {
                    logDirect(String.format("Блок %s уже в списке!", registryName), Formatting.RED);
                    return;
                }

                config.addBlockAndSave(registryName);
                syncWithModule();
                logDirect(String.format("§aБлок §f%s §aдобавлен в BlockESP!", registryName), Formatting.GREEN);
            }
            case "remove", "del", "delete" -> {
                if (args.length < 2) {
                    logDirect("Использование: blockesp remove <block>", Formatting.RED);
                    return;
                }

                String blockId = args[1].toLowerCase();
                if (!blockId.contains(":")) {
                    blockId = "minecraft:" + blockId;
                }

                Block block = Registries.BLOCK.get(Identifier.tryParse(blockId));
                String registryName = block != null ? Registries.BLOCK.getId(block).toString() : blockId;

                if (!config.hasBlock(registryName)) {
                    logDirect(String.format("Блок %s не найден в списке!", registryName), Formatting.RED);
                    return;
                }

                config.removeBlockAndSave(registryName);
                syncWithModule();
                logDirect(String.format("Блок %s удален из BlockESP!", registryName), Formatting.GREEN);
            }
            case "clear" -> {
                int count = config.size();
                config.clearAndSave();
                syncWithModule();
                logDirect(String.format("Список BlockESP очищен! Удалено: %d", count), Formatting.GREEN);
            }
            case "list" -> {
                int page = 1;
                if (args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {}
                }

                List<String> blocks = config.getBlockList();

                if (blocks.isEmpty()) {
                    logDirect("Список BlockESP пуст!", Formatting.RED);
                    return;
                }

                Paginator<String> paginator = new Paginator<>(blocks);
                paginator.setPage(page);

                paginator.display(
                        () -> {
                            logDirectRaw(Text.literal(getLine()));
                            logDirect("§f§lБЛОКИ BLOCKESP §7(" + blocks.size() + ")");
                            logDirectRaw(Text.literal(getLine()));
                        },
                        blockName -> {
                            String shortName = blockName.replace("minecraft:", "");

                            MutableText component = Text.literal("  §6● §f" + shortName)
                                    .append(Text.literal(" §8(" + blockName + ")"));

                            MutableText hoverText = Text.literal("§7Нажмите чтобы удалить §f" + shortName);
                            String removeCommand = manager.getPrefix() + "blockesp remove " + blockName;

                            component.setStyle(component.getStyle()
                                    .withHoverEvent(new HoverEvent.ShowText(hoverText))
                                    .withClickEvent(new ClickEvent.RunCommand(removeCommand)));

                            return component;
                        },
                        manager.getPrefix() + label + " list"
                );
            }
            case "blocks", "allblocks" -> {
                int page = 1;
                if (args.length > 1) {
                    try {
                        page = Integer.parseInt(args[1]);
                    } catch (NumberFormatException ignored) {}
                }

                List<String> allBlocks = Registries.BLOCK.getIds().stream()
                        .map(Identifier::toString)
                        .sorted()
                        .toList();

                Paginator<String> paginator = new Paginator<>(allBlocks, 15);
                paginator.setPage(page);

                paginator.display(
                        () -> {
                            logDirectRaw(Text.literal(getLine()));
                            logDirect("§f§lВСЕ БЛОКИ §7(" + allBlocks.size() + ")");
                            logDirectRaw(Text.literal(getLine()));
                        },
                        blockName -> {
                            boolean inList = config.hasBlock(blockName);
                            String prefix = inList ? "§a✓" : "§8○";

                            MutableText component = Text.literal("  " + prefix + " §f" + blockName.replace("minecraft:", ""));

                            String command = inList
                                    ? manager.getPrefix() + "blockesp remove " + blockName
                                    : manager.getPrefix() + "blockesp add " + blockName;

                            MutableText hoverText = Text.literal(inList
                                    ? "§7Нажмите чтобы удалить"
                                    : "§7Нажмите чтобы добавить");

                            component.setStyle(component.getStyle()
                                    .withHoverEvent(new HoverEvent.ShowText(hoverText))
                                    .withClickEvent(new ClickEvent.RunCommand(command)));

                            return component;
                        },
                        manager.getPrefix() + label + " blocks"
                );
            }
            default -> {
                logDirectRaw(Text.literal(getLine()));
                logDirect("§f§lBLOCKESP");
                logDirectRaw(Text.literal(getLine()));
                logDirect("§7> blockesp add <block> §8- §fДобавить блок");
                logDirect("§7> blockesp remove <block> §8- §fУдалить блок");
                logDirect("§7> blockesp list §8- §fПоказать добавленные блоки");
                logDirect("§7> blockesp clear §8- §fОчистить список");
                logDirect("§7> blockesp blocks §8- §fПоказать все блоки игры");
                logDirectRaw(Text.literal(getLine()));
                logDirect("§7Примеры:");
                logDirect("§8> §fblockesp add diamond_ore");
                logDirect("§8> §fblockesp add minecraft:ancient_debris");
                logDirectRaw(Text.literal(getLine()));
            }
        }
    }

    private void syncWithModule() {
        BlockESP module = getBlockESPModule();
        if (module != null) {
            module.getBlocksToHighlight().clear();
            module.getBlocksToHighlight().addAll(BlockESPConfig.getInstance().getBlocks());
        }
    }

    private BlockESP getBlockESPModule() {
        if (Initialization.getInstance() == null || Initialization.getInstance().getManager() == null) {
            return null;
        }
        return Initialization.getInstance().getManager().getModuleRepository().modules().stream()
                .filter(m -> m instanceof BlockESP)
                .map(m -> (BlockESP) m)
                .findFirst()
                .orElse(null);
    }

    @Override
    public Stream<String> tabComplete(String label, String[] args) {
        if (args.length == 1) {
            return new TabCompleteHelper()
                    .append("add", "remove", "list", "clear", "blocks")
                    .sortAlphabetically()
                    .filterPrefix(args[0])
                    .stream();
        }
        if (args.length == 2) {
            String action = args[0].toLowerCase();
            if (action.equals("add")) {
                return Registries.BLOCK.getIds().stream()
                        .map(Identifier::toString)
                        .filter(name -> name.toLowerCase().contains(args[1].toLowerCase()))
                        .limit(50);
            }
            if (action.equals("remove") || action.equals("del") || action.equals("delete")) {
                return new TabCompleteHelper()
                        .append(BlockESPConfig.getInstance().getBlockList().toArray(new String[0]))
                        .filterPrefix(args[1])
                        .stream();
            }
        }
        return Stream.empty();
    }

    @Override
    public String getShortDesc() {
        return "Управление блоками для BlockESP";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Команда для управления списком блоков BlockESP",
                "Использование:",
                "> blockesp add <block> - Добавить блок в список",
                "> blockesp remove <block> - Удалить блок из списка",
                "> blockesp list - Показать добавленные блоки",
                "> blockesp clear - Очистить список",
                "> blockesp blocks - Показать все блоки игры"
        );
    }
}