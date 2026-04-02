package fun.aegis.mixins.player.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import fun.aegis.features.impl.combat.Aura;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;
import fun.aegis.utils.client.managers.event.EventManager;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.events.render.EntityColorEvent;
import fun.aegis.utils.features.aura.warp.TurnsConnection;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin implements QuickImports {
    @Shadow @Nullable protected abstract RenderLayer getRenderLayer(LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline);

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;"))
    private RenderLayer renderHook(LivingEntityRenderer instance, LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline) {
        if (!translucent && state.width == 0.6F) {
            EntityColorEvent event = new EntityColorEvent(-1);
            EventManager.callEvent(event);
            if (event.isCancelled()) translucent = true;
        }
        return this.getRenderLayer(state, showBody, translucent, showOutline);
    }

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/model/EntityModel;render(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;III)V"))
    private void renderModelHook(EntityModel<?> instance, MatrixStack matrixStack, VertexConsumer vertexConsumer, int i, int j, int l, @Local(ordinal = 0, argsOnly = true) LivingEntityRenderState renderState) {
        EntityColorEvent event = new EntityColorEvent(l);
        if (renderState.invisibleToPlayer) EventManager.callEvent(event);
        instance.render(matrixStack, vertexConsumer, i, j, event.getColor());
    }

    @ModifyExpressionValue(
            method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F")
    )
    private float lerpAngleDegreesHook(
            float original,
            @Local(ordinal = 0, argsOnly = true) LivingEntity entity,
            @Local(ordinal = 0, argsOnly = true) float delta
    ) {
        TurnsConnection controller = TurnsConnection.INSTANCE;
        Aura aura = Aura.getInstance();

        if (entity.equals(mc.player) && controller.getPreviousRotation().getYaw() != mc.player.getYaw() && controller.getFakeRotation().getYaw() != mc.player.getYaw() && !(mc.currentScreen instanceof HandledScreen)) {
            boolean isLony = Aura.fakeRotate;

            float prevYaw = isLony ? controller.getFakeRotation().getYaw() : controller.getPreviousRotation().getYaw();
            float currentYaw = isLony ? controller.getFakeRotation().getYaw() : controller.getRotation().getYaw();

            if (Aura.getInstance().getTarget() == null) {
                prevYaw = controller.getPreviousRotation().getYaw();
                currentYaw = controller.getRotation().getYaw();
            }

            return MathHelper.lerp(delta, prevYaw, currentYaw);
        }

        return original;
    }

    @ModifyExpressionValue(
            method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F")
    )
    private float getLerpedPitchHook(
            float original,
            @Local(ordinal = 0, argsOnly = true) LivingEntity entity,
            @Local(ordinal = 0, argsOnly = true) float delta
    ) {
        TurnsConnection controller = TurnsConnection.INSTANCE;
        Aura aura = Aura.getInstance();

        if (entity.equals(mc.player) && controller.getPreviousRotation().getPitch() != mc.player.getPitch() && controller.getFakeRotation().getPitch() != mc.player.getPitch() && !(mc.currentScreen instanceof HandledScreen)) {
            boolean isLony = Aura.fakeRotate;

            float prevPitch = isLony ? controller.getFakeRotation().getPitch() : controller.getPreviousRotation().getPitch();
            float currentPitch = isLony ? controller.getFakeRotation().getPitch() : controller.getRotation().getPitch();

            if (Aura.getInstance().getTarget() == null ) {
                prevPitch = controller.getPreviousRotation().getPitch();
                currentPitch = controller.getRotation().getPitch();
            }


            return MathHelper.lerp(delta, prevPitch, currentPitch);
        }

        return original;
    }

}
