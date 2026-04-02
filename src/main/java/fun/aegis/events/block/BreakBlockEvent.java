package fun.aegis.events.block;

import net.minecraft.util.math.BlockPos;
import fun.aegis.utils.client.managers.event.events.Event;

public record BreakBlockEvent(BlockPos blockPos) implements Event {}
