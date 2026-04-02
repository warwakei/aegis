package rich.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.util.config.impl.proxy.ProxyConfig;
import rich.util.proxy.GuiProxy;
import rich.util.proxy.ProxyServer;

@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenOpenMixin {

    @Inject(method = "init()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerScreen;updateButtonActivationStates()V"))
    public void multiplayerGuiOpen(CallbackInfo ci) {
        MultiplayerScreen ms = (MultiplayerScreen) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();

        ProxyConfig config = ProxyConfig.getInstance();

        String buttonText;
        if (config.isProxyEnabled() && !config.getDefaultProxy().isEmpty()) {
            buttonText = "§aПрокси: Активен";
        } else {
            buttonText = "§7Proxy";
        }

        ProxyServer.proxyMenuButton = ButtonWidget.builder(Text.literal(buttonText), (buttonWidget) -> {
            MinecraftClient.getInstance().setScreen(new GuiProxy(ms));
        }).dimensions(5, 5, 100, 20).build();

        IScreen si = (IScreen) ms;
        si.getDrawables().add(ProxyServer.proxyMenuButton);
        si.getSelectables().add(ProxyServer.proxyMenuButton);
        si.getChildren().add(ProxyServer.proxyMenuButton);
    }
}