package fun.aegis.mixins.client.screen.mainmenu;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.CubeMapRenderer;
import net.minecraft.client.gui.RotatingCubeMapRenderer;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import fun.aegis.utils.client.managers.event.EventManager;
import fun.aegis.events.chat.ChatEvent;
import fun.aegis.display.screens.clickgui.MenuScreen;
import fun.aegis.display.screens.mainmenu.MainMenu;

@Mixin(Screen.class)
public class ScreenMixin {
    private static final CubeMapRenderer CUSTOM_PANORAMA_RENDERER = new CubeMapRenderer(Identifier.of("minecraft", "panorama/panorama"));
    private static final RotatingCubeMapRenderer CUSTOM_ROTATING_PANORAMA_RENDERER = new RotatingCubeMapRenderer(CUSTOM_PANORAMA_RENDERER);

    @Inject(at = @At(value = "INVOKE", target = "Lorg/slf4j/Logger;error(Ljava/lang/String;Ljava/lang/Object;)V", remap = false, ordinal = 1), method = "handleTextClick", cancellable = true)
    public void handleCustomClickEvent(Style style, CallbackInfoReturnable<Boolean> cir) {
        ClickEvent clickEvent = style.getClickEvent();
        if (clickEvent == null) {
            return;
        }
        EventManager.callEvent(new ChatEvent(clickEvent.getValue(), false));
        cir.setReturnValue(true);
        cir.cancel();
    }

    @Inject(method = "renderBackground", at = @At("HEAD"), cancellable = true)
    private void disableBackgroundBlurAndDimming(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if ((Object) this instanceof MenuScreen || (Object) this instanceof MainMenu) {
            ci.cancel();
        }
    }

    @Inject(method = "renderPanoramaBackground", at = @At("HEAD"), cancellable = true)
    private void renderCustomPanoramaBackground(DrawContext context, float delta, CallbackInfo ci) {
        if ((Object) this instanceof MainMenu) {
            ci.cancel();
        } else {
            CUSTOM_ROTATING_PANORAMA_RENDERER.render(context, ((Screen)(Object)this).width, ((Screen)(Object)this).height, 1.0F, delta);
            ci.cancel();
        }
    }
}
