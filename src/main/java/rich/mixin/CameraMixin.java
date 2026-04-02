package rich.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.events.api.EventManager;
import rich.events.impl.CameraEvent;
import rich.events.impl.CameraPositionEvent;
import rich.modules.impl.combat.aura.Angle;


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
    private void updateHook(World area, Entity focusedEntity, boolean thirdPerson, boolean inverseView, float tickProgress, CallbackInfo ci) {
        CameraEvent event = new CameraEvent(false, 4, new Angle(yaw, pitch));
        EventManager.callEvent(event);
        Angle angle = event.getAngle();
        if (event.isCancelled() && focusedEntity instanceof ClientPlayerEntity player && !player.isSleeping() && thirdPerson) {
            float pitch = inverseView ? -angle.getPitch() : angle.getPitch();
            float yaw = angle.getYaw() - (inverseView ? 180 : 0);
            float distance = event.getDistance();
            setRotation(yaw, pitch);
            moveBy(event.isCameraClip() ? -distance : -clipToSpace(distance), 0.0F, 0.0F);
            ci.cancel();
        }
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