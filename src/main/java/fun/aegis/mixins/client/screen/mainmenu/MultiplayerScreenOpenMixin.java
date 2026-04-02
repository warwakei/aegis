package fun.aegis.mixins.client.screen.mainmenu;

import fun.aegis.features.impl.misc.SelfDestruct;
import fun.aegis.mixins.client.screen.IScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import fun.aegis.common.proxy.Config;
import fun.aegis.common.proxy.GuiProxy;
import fun.aegis.common.proxy.Proxy;
import fun.aegis.common.proxy.ProxyServer;

@Mixin(MultiplayerScreen.class)
public class MultiplayerScreenOpenMixin {
    @Inject(method = "init()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/screen/multiplayer/MultiplayerScreen;updateButtonActivationStates()V"))
    public void multiplayerGuiOpen(CallbackInfo ci) {
        if (SelfDestruct.unhooked) return;

        String playerName = MinecraftClient.getInstance().getSession().getUsername();
        if (!playerName.equals(Config.lastPlayerName)) {
            Config.lastPlayerName = playerName;
            if (Config.accounts.containsKey(playerName)) {
                ProxyServer.proxy = Config.accounts.get(playerName);
            } else {
                if (Config.accounts.containsKey("")) {
                    ProxyServer.proxy = Config.accounts.get("");
                } else {
                    ProxyServer.proxy = new Proxy();
                }
            }
        }

        MultiplayerScreen ms = (MultiplayerScreen) (Object) this;
        MinecraftClient client = MinecraftClient.getInstance();
        int screenWidth = client.getWindow().getScaledWidth();

        String buttonText;
        if (ProxyServer.proxyEnabled && ProxyServer.proxy != null && !ProxyServer.proxy.ipPort.isEmpty()) {
            buttonText = "Прокси: Активен";
        } else {
            buttonText = "Proxy";
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
