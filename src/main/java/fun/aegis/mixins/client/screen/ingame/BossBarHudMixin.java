package fun.aegis.mixins.client.screen.ingame;

import net.minecraft.client.gui.hud.BossBarHud;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fun.aegis.features.impl.render.Hud;
import fun.aegis.display.hud.DynamicIsland;

@Mixin(BossBarHud.class)
public class BossBarHudMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private void render(CallbackInfo ci) {
        if (Hud.getInstance().isState() && Hud.getInstance().interfaceSettings.isSelected("Dynamic Island")) {
            ci.cancel();
        }
    }
}
