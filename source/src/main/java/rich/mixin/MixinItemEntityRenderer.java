package rich.mixin;

import net.minecraft.client.item.ItemModelManager;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.ItemEntityRenderer;
import net.minecraft.client.render.entity.state.ItemEntityRenderState;
import net.minecraft.client.render.entity.state.ItemStackEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.random.Random;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.modules.impl.render.ItemPhysic;

import java.util.WeakHashMap;

@Mixin(ItemEntityRenderer.class)
public abstract class MixinItemEntityRenderer {

    @Unique
    private static final WeakHashMap<ItemEntityRenderState, Boolean> groundStateMap = new WeakHashMap<>();

    @Unique
    private ItemEntityRenderState currentState = null;

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/ItemEntity;Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;F)V", at = @At("HEAD"))
    private void captureGroundState(ItemEntity entity, ItemEntityRenderState state, float tickDelta, CallbackInfo ci) {
        groundStateMap.put(state, entity.isOnGround());
    }

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;translate(FFF)V", ordinal = 0))
    private void redirectTranslate(MatrixStack matrices, float x, float y, float z, ItemEntityRenderState state, MatrixStack matricesArg, OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
        currentState = state;

        ItemPhysic itemPhysic = ItemPhysic.getInstance();

        if (itemPhysic != null && itemPhysic.isState() && itemPhysic.mode.isSelected("Обычная")) {
            Box box = state.itemRenderState.getModelBoundingBox();
            float f = -((float) box.minY) + 0.0625F;
            matrices.translate(x, f, z);
        } else {
            matrices.translate(x, y, z);
        }
    }

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/ItemEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/ItemEntityRenderer;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;ILnet/minecraft/client/render/entity/state/ItemStackEntityRenderState;Lnet/minecraft/util/math/random/Random;Lnet/minecraft/util/math/Box;)V"))
    private void redirectRender(MatrixStack matrices, OrderedRenderCommandQueue queue, int light, ItemStackEntityRenderState stackState, Random random, Box box) {
        ItemPhysic itemPhysic = ItemPhysic.getInstance();

        if (itemPhysic != null && itemPhysic.isState() && itemPhysic.mode.isSelected("Обычная") && currentState != null) {
            float age = currentState.age;
            float offset = currentState.uniqueOffset;

            boolean isOnGround = groundStateMap.getOrDefault(currentState, false);

            float rotation = ItemEntity.getRotation(age, offset);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotation(-rotation));

            if (isOnGround) {
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
                float yOffset = (float) box.getLengthY() / 2.0f;
                matrices.translate(0, -yOffset + 0.0625f, 0);
            } else {
                float spinSpeed = 15.0f;
                float itemRotation = (age * spinSpeed + offset * 360.0f) % 360.0f;
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(itemRotation));
            }
        }

        ItemEntityRenderer.render(matrices, queue, light, stackState, random, box);
    }
}