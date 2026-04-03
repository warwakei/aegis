package rich.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.modules.impl.player.NoDelay;

@Mixin(MinecraftClient.class)
public class MinecraftClientNoDelayMixin {

    @Shadow
    public ClientPlayerEntity player;

    @Shadow
    public ClientPlayerInteractionManager interactionManager;

    @Shadow
    public int itemUseCooldown;

    /**
     * Сброс кулдауна правого клика (item use)
     */
    @Inject(method = "doItemUse", at = @At("HEAD"))
    private void onDoItemUse(CallbackInfo ci) {
        if (player == null) return;
        NoDelay noDelay = NoDelay.getInstance();
        if (noDelay != null && noDelay.isState() && noDelay.ignoreSetting.isSelected("Правый клик")) {
            itemUseCooldown = 0;
        }
    }
}
