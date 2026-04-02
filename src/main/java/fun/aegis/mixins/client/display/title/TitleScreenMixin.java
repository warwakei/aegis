package fun.aegis.mixins.client.display.title;

import fun.aegis.display.screens.mainmenu.MainMenu;
import fun.aegis.features.impl.misc.SelfDestruct;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public class TitleScreenMixin {
    @Unique
    private static boolean settingMainMenu = false;
    
    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void replaceTitleScreen(Screen screen, CallbackInfo ci) {
        if (SelfDestruct.unhooked) return;
        if (settingMainMenu) return;

        MinecraftClient client = (MinecraftClient)(Object)this;
        
        if (screen instanceof TitleScreen) {
            settingMainMenu = true;
            client.setScreen(MainMenu.INSTANCE);
            settingMainMenu = false;
            ci.cancel();
        }
    }
}
