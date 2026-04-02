package fun.aegis.mixins.player.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fun.aegis.utils.client.managers.event.EventManager;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.features.aura.warp.TurnsConnection;
import fun.aegis.events.player.BoundingBoxControlEvent;
import fun.aegis.events.player.PlayerVelocityStrafeEvent;
import fun.aegis.events.block.PushEvent;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin implements QuickImports {

    @Inject(method = "isPushedByFluids", at = @At("HEAD"), cancellable = true)
    public void isPushedByFluids(CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == mc.player) {
            PushEvent event = new PushEvent(PushEvent.Type.WATER);
            EventManager.callEvent(event);
            if (event.isCancelled()) cir.setReturnValue(false);
        }
    }

    @Shadow private Box boundingBox;
    @Shadow public float yaw;

    @Unique
    private final MinecraftClient client = MinecraftClient.getInstance();

    @Redirect(method = "updateVelocity", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;movementInputToVelocity(Lnet/minecraft/util/math/Vec3d;FF)Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d hookVelocity(Vec3d movementInput, float speed, float yaw) {
        if ((Object) this == mc.player) {
            PlayerVelocityStrafeEvent event = new PlayerVelocityStrafeEvent(movementInput, speed, yaw, Entity.movementInputToVelocity(movementInput, speed, yaw));
            EventManager.callEvent(event);
            return event.getVelocity();
        }
        return Entity.movementInputToVelocity(movementInput, speed, yaw);
    }

    @Inject(method = "getBoundingBox", at = @At("HEAD"), cancellable = true)
    public final void getBoundingBox(CallbackInfoReturnable<Box> cir) {
        BoundingBoxControlEvent event = new BoundingBoxControlEvent(boundingBox, (Entity) (Object) this);
        EventManager.callEvent(event);
        cir.setReturnValue(event.getBox());
    }
    @ModifyVariable(
            method = "getRotationVector(FF)Lnet/minecraft/util/math/Vec3d;",
            at = @At("HEAD"),
            ordinal = 0,
            argsOnly = true
    )
    private float modifyPitch(float pitch) {
        if ((Object) this instanceof ClientPlayerEntity && TurnsConnection.INSTANCE.getCurrentAngle() !=null) {
            return TurnsConnection.INSTANCE.getCurrentAngle().getPitch();
        }
        return pitch;
    }
    @ModifyExpressionValue(method = "move", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/Entity;isControlledByPlayer()Z"))
    public boolean isControlledByPlayerHook(boolean original) {
        if ((Object) this == mc.player) return false;
        return original;
    }
}
