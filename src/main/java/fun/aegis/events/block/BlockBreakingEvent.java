package fun.aegis.events.block;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import fun.aegis.utils.client.managers.event.events.Event;

public record BlockBreakingEvent(BlockPos blockPos, Direction direction) implements Event {}
