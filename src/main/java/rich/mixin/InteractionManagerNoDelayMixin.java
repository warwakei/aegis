package rich.mixin;

import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.modules.impl.player.NoDelay;

@Mixin(ClientPlayerInteractionManager.class)
public class InteractionManagerNoDelayMixin {

    @Shadow
    public int blockBreakingCooldown;

    /**
     * Сброс кулдауна ломания блоков
     */
    @Inject(method = "attackBlock", at = @At("HEAD"))
    private void onAttackBlock(CallbackInfo ci) {
        NoDelay noDelay = NoDelay.getInstance();
        if (noDelay != null && noDelay.isState() && noDelay.ignoreSetting.isSelected("Задержка ломания")) {
            blockBreakingCooldown = 0;
        }
    }

    /**
     * Сброс кулдауна при обновлении блока
     */
    @Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"))
    private void onUpdateBlockBreakingProgress(CallbackInfo ci) {
        NoDelay noDelay = NoDelay.getInstance();
        if (noDelay != null && noDelay.isState() && noDelay.ignoreSetting.isSelected("Задержка ломания")) {
            blockBreakingCooldown = 0;
        }
    }
}
