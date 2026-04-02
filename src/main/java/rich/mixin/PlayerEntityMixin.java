package rich.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import rich.IMinecraft;
import rich.events.api.EventManager;
import rich.events.impl.PlayerTravelEvent;
import rich.events.impl.PushEvent;
import rich.events.impl.SwimmingEvent;
import rich.modules.impl.combat.aura.AngleConnection;


@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements IMinecraft {

    @Inject(method = "isPushedByFluids", at = @At("HEAD"), cancellable = true)
    public void isPushedByFluids(CallbackInfoReturnable<Boolean> cir) {
        PushEvent event = new PushEvent(PushEvent.Type.WATER);
        EventManager.callEvent(event);
        if (event.isCancelled()) cir.setReturnValue(false);
    }

    @ModifyExpressionValue(method = "knockbackTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F"))
    private float hookKnockbackRotation(float original) {
        if ((Object) this == mc.player && AngleConnection.INSTANCE.getMoveRotation() != null) {
            return AngleConnection.INSTANCE.getMoveRotation().getYaw();
        }
        return original;
    }

    @ModifyExpressionValue(method = "doSweepingAttack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F"))
    private float hookSweepRotation(float original) {
        if ((Object) this == mc.player && AngleConnection.INSTANCE.getMoveRotation() != null) {
            return AngleConnection.INSTANCE.getMoveRotation().getYaw();
        }
        return original;
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravelPre(Vec3d movementInput, CallbackInfo ci) {
        if (mc.player == null) return;
        PlayerTravelEvent event = new PlayerTravelEvent(movementInput, true);
        EventManager.callEvent(event);
        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @ModifyExpressionValue(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d travelHook(Vec3d vec3d) {
        SwimmingEvent event = new SwimmingEvent(vec3d);
        EventManager.callEvent(event);
        return event.getVector();
    }

    @Inject(method = "travel", at = @At("RETURN"))
    private void onTravelPost(Vec3d movementInput, CallbackInfo ci) {
        if (mc.player == null) return;
        PlayerTravelEvent event = new PlayerTravelEvent(movementInput, false);
        EventManager.callEvent(event);
    }
}