package fun.aegis.events.block;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import fun.aegis.utils.client.managers.event.events.Event;

@Getter
@Setter
public class BlockCollisionEvent implements Event {
    private BlockPos blockPos;
    private BlockState state;

    public BlockCollisionEvent(BlockPos blockPos, BlockState state) {
        this.blockPos = blockPos;
        this.state = state;
    }
}
