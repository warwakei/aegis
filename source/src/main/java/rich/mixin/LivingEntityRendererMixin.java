package rich.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;
import rich.IMinecraft;
import rich.events.api.EventManager;
import rich.events.impl.EntityColorEvent;
import rich.modules.impl.combat.aura.AngleConnection;

@SuppressWarnings("unchecked")
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererMixin<S extends LivingEntityRenderState, M extends EntityModel<? super S>> implements IMinecraft {

    @Shadow
    @Nullable
    protected abstract RenderLayer getRenderLayer(S state, boolean showBody, boolean translucent, boolean showOutline);

    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/MathHelper;lerpAngleDegrees(FFF)F"))
    private float lerpAngleDegreesHook(float original, @Local(ordinal = 0, argsOnly = true) LivingEntity entity, @Local(ordinal = 0, argsOnly = true) float delta) {
        AngleConnection controller = AngleConnection.INSTANCE;

        if (entity.equals(mc.player) && controller.getCurrentAngle() != null && !(mc.currentScreen instanceof HandledScreen)) {
            float prevYaw = controller.getPreviousRotation().getYaw();
            float currentYaw = controller.getRotation().getYaw();
            return MathHelper.lerpAngleDegrees(delta, prevYaw, currentYaw);
        }

        return original;
    }

    @ModifyExpressionValue(method = "updateRenderState(Lnet/minecraft/entity/LivingEntity;Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;F)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;getLerpedPitch(F)F"))
    private float getLerpedPitchHook(float original, @Local(ordinal = 0, argsOnly = true) LivingEntity entity, @Local(ordinal = 0, argsOnly = true) float delta) {
        AngleConnection controller = AngleConnection.INSTANCE;

        if (entity.equals(mc.player) && controller.getCurrentAngle() != null && !(mc.currentScreen instanceof HandledScreen)) {
            float prevPitch = controller.getPreviousRotation().getPitch();
            float currentPitch = controller.getRotation().getPitch();
            return MathHelper.lerp(delta, prevPitch, currentPitch);
        }

        return original;
    }

    @Redirect(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/entity/LivingEntityRenderer;getRenderLayer(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;ZZZ)Lnet/minecraft/client/render/RenderLayer;"))
    private RenderLayer renderLayerHook(LivingEntityRenderer<?, ?, ?> instance, LivingEntityRenderState state, boolean showBody, boolean translucent, boolean showOutline) {
        if (!translucent && state.width == 0.6F) {
            EntityColorEvent event = new EntityColorEvent(-1);
            EventManager.callEvent(event);
            if (event.isCancelled()) {
                translucent = true;
            }
        }
        return this.getRenderLayer((S) state, showBody, translucent, showOutline);
    }

    @ModifyArg(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitModel(Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;IIILnet/minecraft/client/texture/Sprite;ILnet/minecraft/client/render/command/ModelCommandRenderer$CrumblingOverlayCommand;)V"), index = 6)
    private int modifyColor(int color, @Local(argsOnly = true) S renderState) {
        if (renderState.invisibleToPlayer) {
            EntityColorEvent event = new EntityColorEvent(color);
            EventManager.callEvent(event);
            return event.getColor();
        }
        return color;
    }
}