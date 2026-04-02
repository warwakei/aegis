package rich.mixin;

import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.TntEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rich.modules.impl.render.Esp;

@Mixin(EntityRenderer.class)
public class EntityRendererMixin<T extends Entity, S extends EntityRenderState> {

    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void onRenderLabelIfPresent(S state, MatrixStack matrices, OrderedRenderCommandQueue queue, CameraRenderState cameraRenderState, CallbackInfo ci) {
        Esp esp = Esp.getInstance();
        if (esp != null && esp.isState()) {
            if (state.entityType == EntityType.PLAYER && esp.entityType.isSelected("Player")) {
                ci.cancel();
            }
            if (state.entityType == EntityType.ITEM && esp.entityType.isSelected("Item")) {
                ci.cancel();
            }
        }
    }

    @Inject(method = "getDisplayName", at = @At("HEAD"), cancellable = true)
    private void hookNametag(T entity, CallbackInfoReturnable<Text> cir) {
        Esp esp = Esp.getInstance();
        if (esp != null && esp.isState()) {
            if (entity instanceof PlayerEntity && esp.entityType.isSelected("Player")) {
                cir.setReturnValue(null);
            }

            if (entity instanceof ItemEntity && esp.entityType.isSelected("Item")) {
                cir.setReturnValue(null);
            }
        }
    }
}