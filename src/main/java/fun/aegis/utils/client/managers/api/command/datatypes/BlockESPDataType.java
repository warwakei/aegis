package fun.aegis.utils.client.managers.api.command.datatypes;

import net.minecraft.block.Block;
import fun.aegis.utils.client.managers.api.command.exception.CommandException;
import fun.aegis.utils.client.managers.api.command.helpers.TabCompleteHelper;
import fun.aegis.Aegis;

import java.util.List;
import java.util.stream.Stream;

public enum BlockESPDataType implements IDatatypeFor<Block> {
    INSTANCE;

    @Override
    public Stream<String> tabComplete(IDatatypeContext datatypeContext) throws CommandException {
        Stream<String> blocks = getBlocks().stream().map(b -> b.getName().getString().replace(" ", "_"));
        String context = datatypeContext.getConsumer().getString();
        return new TabCompleteHelper().append(blocks).filterPrefix(context).sortAlphabetically().stream();
    }

    @Override
    public Block get(IDatatypeContext datatypeContext) throws CommandException {
        String text = datatypeContext.getConsumer().getString();
        return getBlocks().stream().filter(s -> s.getName().getString().replace(" ", "_").equalsIgnoreCase(text)).findFirst().orElse(null);
    }

    private List<? extends Block> getBlocks() {
        return Aegis.getInstance().getBoxESPRepository().blocks.keySet().stream().toList();
    }
}
