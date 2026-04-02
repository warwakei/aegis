package fun.aegis.mixins.player.camera;

import fun.aegis.utils.features.aura.warp.Turns;
import fun.aegis.utils.interactions.simulate.Simulations;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fun.aegis.utils.client.managers.event.EventManager;
import fun.aegis.utils.features.aura.warp.TurnsConnection;
import fun.aegis.utils.features.aura.warp.TurnsConstructor;
import fun.aegis.events.render.CameraEvent;
import fun.aegis.events.render.CameraPositionEvent;

@Mixin(Camera.class)
public abstract class CameraMixin {

    @Shadow private Vec3d pos;

    @Shadow @Final private BlockPos.Mutable blockPos;

    @Shadow public abstract void setRotation(float yaw, float pitch);

    @Shadow protected abstract void moveBy(float f, float g, float h);

    @Shadow protected abstract float clipToSpace(float f);
    @Shadow private float yaw;
    @Shadow private float pitch;


    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V", shift = At.Shift.AFTER), cancellable = true)
    private void updateHook(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        CameraEvent event = new CameraEvent(false, 4, new Turns(yaw, pitch));
        EventManager.callEvent(event);
        Turns angle = event.getAngle();
        if (event.isCancelled() && focusedEntity instanceof ClientPlayerEntity player && !player.isSleeping() && thirdPerson) {
            float pitch = inverseView ? -angle.getPitch() : angle.getPitch();
            float yaw = angle.getYaw() - (inverseView ? 180 : 0);
            float distance = event.getDistance();
            setRotation(yaw, pitch);
            moveBy(event.isCameraClip() ? -distance : -clipToSpace(distance), 0.0F, 0.0F);
            ci.cancel();
        }
    }
    @Inject(method = "update", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/Camera;setPos(DDD)V", shift = At.Shift.AFTER))
    private void injectQuickPerspectiveSwap(BlockView area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickDelta, CallbackInfo ci) {
        TurnsConnection rotationController = TurnsConnection.INSTANCE;
        TurnsConstructor rotationPlan = rotationController.getCurrentRotationPlan();
        Turns previousAngle = rotationController.getPreviousAngle();
        Turns currentAngle = rotationController.getCurrentAngle();

        boolean shouldModifyRotation = rotationPlan != null && rotationPlan.isChangeLook();

        if (currentAngle == null || previousAngle == null || !shouldModifyRotation) {
            return;
        }

        this.setRotation(
                (float) MathHelper.lerp(Simulations.kizdamati() == 1488 ? tickDelta : Simulations.kizdamati(), previousAngle.getYaw(), currentAngle.getYaw()),
                (float) MathHelper.lerp(Simulations.kizdamati() == 1488 ? tickDelta : Simulations.kizdamati(), previousAngle.getPitch(), currentAngle.getPitch())
        );
    }

    @Inject(method = "setPos(Lnet/minecraft/util/math/Vec3d;)V", at = @At("HEAD"), cancellable = true)
    private void posHook(Vec3d pos, CallbackInfo ci) {
        CameraPositionEvent event = new CameraPositionEvent(pos);
        EventManager.callEvent(event);
        this.pos = pos = event.getPos();
        blockPos.set(pos.x,pos.y,pos.z);
        ci.cancel();
    }
}
