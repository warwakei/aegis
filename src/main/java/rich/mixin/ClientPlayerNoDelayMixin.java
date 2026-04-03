package rich.mixin;

import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.modules.impl.player.NoDelay;

@Mixin(ClientPlayerEntity.class)
public class ClientPlayerNoDelayMixin {

    @Shadow
    public int jumpingCooldown;

    /**
     * Сброс кулдауна прыжка
     */
    @Inject(method = "tickMovement", at = @At("HEAD"))
    private void onTickMovement(CallbackInfo ci) {
        NoDelay noDelay = NoDelay.getInstance();
        if (noDelay != null && noDelay.isState() && noDelay.ignoreSetting.isSelected("Прыжок")) {
            jumpingCooldown = 0;
        }
    }
}
