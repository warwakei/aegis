package rich.mixin;

import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.util.Instance;

/**
 * Миксин для скрытия сообщений "Машина занята..." когда JenroCasino включён
 */
@Mixin(ChatHud.class)
public class ChatHudMixin {

    @Inject(
        method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void onAddMessage(Text message, MessageSignatureData signatureData, MessageIndicator indicator, CallbackInfo ci) {
        if (message == null) return;

        String text = message.getString();
        if (text == null || text.isEmpty()) return;

        // Проверяем включён ли JenroCasino
        try {
            var jenroCasino = Instance.get(rich.modules.impl.misc.JenroCasino.class);
            if (jenroCasino != null && jenroCasino.isState()) {
                String clean = text.replaceAll("(?i)[§&][0-9a-fk-or]", "").trim();
                if (clean.contains("Машина занята")) {
                    ci.cancel();
                }
            }
        } catch (Exception ignored) {}
    }
}
