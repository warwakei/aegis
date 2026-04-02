package fun.aegis.utils.client.managers.api.command.datatypes;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.registry.Registries;
import fun.aegis.utils.client.managers.api.command.exception.CommandException;
import fun.aegis.utils.client.managers.api.command.helpers.TabCompleteHelper;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

public enum BlockDataType implements IDatatypeFor<Block> {
    INSTANCE;

    @Override
    public Stream<String> tabComplete(IDatatypeContext ctx) throws CommandException {
        Stream<String> ways = streamBlocks().map(s -> s.getName().getString().replace(" ", "_"));
        String context = ctx.getConsumer().getString();
        return new TabCompleteHelper().append(ways).filterPrefix(context).sortAlphabetically().stream();
    }

    @Override
    public Block get(IDatatypeContext datatypeContext) throws CommandException {
        return findBlock(datatypeContext.getConsumer().getString()).orElse(null);
    }

    public Optional<Block> findBlock(String text) {
        return streamBlocks().filter(s -> s.getName().getString().replace(" ", "_").equalsIgnoreCase(text)).findFirst();
    }

    public Stream<Block> streamBlocks() {
        return Registries.BLOCK.stream().filter(this::blackList);
    }

    public boolean blackList(Block block) {
        return !List.of(Blocks.AIR, Blocks.CAVE_AIR, Blocks.VOID_AIR, Blocks.WATER).contains(block);
    }
}
