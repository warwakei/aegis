package fun.aegis.mixins.game.block;

import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fun.aegis.utils.client.managers.event.EventManager;
import fun.aegis.events.block.BlockEntityProgressEvent;

@Mixin(BlockEntity.class)
public class BlockEntityMixin {

    @Inject(method = "<init>", at = @At("RETURN"))
    public void initHook(BlockEntityType<?> type, BlockPos pos, BlockState state, CallbackInfo ci) {
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        if (blockEntity != null) EventManager.callEvent(new BlockEntityProgressEvent(blockEntity, BlockEntityProgressEvent.Type.ADD));
    }

    @Inject(method = "markRemoved", at = @At(value = "HEAD"))
    private void markRemovedHook(CallbackInfo ci) {
        BlockEntity blockEntity = (BlockEntity) (Object) this;
        if (blockEntity != null) EventManager.callEvent(new BlockEntityProgressEvent(blockEntity, BlockEntityProgressEvent.Type.REMOVE));
    }
}
