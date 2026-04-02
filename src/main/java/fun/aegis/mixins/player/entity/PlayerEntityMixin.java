package fun.aegis.mixins.player.entity;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fun.aegis.utils.client.managers.event.EventManager;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.events.player.KeepSprintEvent;
import fun.aegis.events.player.PlayerTravelEvent;
import fun.aegis.events.player.SwimmingEvent;
import fun.aegis.utils.features.aura.warp.TurnsConnection;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin implements QuickImports {

    @Inject(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;setSprinting(Z)V", shift = At.Shift.AFTER))
    public void attackHook(CallbackInfo callbackInfo) {
        EventManager.callEvent(new KeepSprintEvent());
    }

    @ModifyExpressionValue(method = "attack", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getYaw()F"))
    private float hookFixRotation(float original) {
        return TurnsConnection.INSTANCE.getMoveRotation().getYaw();
    }

    @ModifyExpressionValue(method = "travel", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/PlayerEntity;getRotationVector()Lnet/minecraft/util/math/Vec3d;"))
    public Vec3d travelHook(Vec3d vec3d) {
        SwimmingEvent event = new SwimmingEvent(vec3d);
        EventManager.callEvent(event);
        return event.getVector();
    }

    @Inject(method = "travel", at = @At("HEAD"), cancellable = true)
    private void onTravelPre(Vec3d movementInput, CallbackInfo ci) {
        if (mc.player == null)
            return;

        PlayerTravelEvent event = new PlayerTravelEvent(movementInput, true);
        EventManager.callEvent(event);

        if (event.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "travel", at = @At("RETURN"), cancellable = true)
    private void onTravelPost(Vec3d movementInput, CallbackInfo ci) {
        if (mc.player == null)
            return;

        PlayerTravelEvent event = new PlayerTravelEvent(movementInput, false);
        EventManager.callEvent(event);
        if (event.isCancelled()) {

        }
    }

}
