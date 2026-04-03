package rich.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.util.repository.ignore.IgnoreUtils;
import rich.netpanel.loggers.ChatBridge;

import java.lang.reflect.Method;
import java.util.UUID;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class ClientPlayNetworkHandlerMixinChat {

    @Inject(method = "onChatMessage", at = @At("HEAD"), cancellable = true)
    private void onChatMessage(ChatMessageS2CPacket packet, CallbackInfo ci) {
        UUID senderUuid = packet.sender();
        if (senderUuid != null) {
            String senderName = getSenderName(senderUuid);
            if (senderName != null && IgnoreUtils.isIgnore(senderName)) {
                ci.cancel();
                return;
            }
            // Log chat to NetPanel
            if (senderName != null) {
                logChatPacket(packet, senderName);
            }
        }
    }

    @Unique
    private void logChatPacket(ChatMessageS2CPacket packet, String senderName) {
        try {
            // Try to find a method that returns Text for the message content
            for (Method m : packet.getClass().getDeclaredMethods()) {
                if (Text.class.isAssignableFrom(m.getReturnType())) {
                    m.setAccessible(true);
                    Object result = m.invoke(packet);
                    if (result instanceof Text text) {
                        ChatBridge.logReceivedText(senderName, text);
                        return;
                    }
                }
            }
        } catch (Exception ignored) {}
        // Fallback
        ChatBridge.logReceived(senderName, "[chat message]");
    }

    @Unique
    private String getSenderName(UUID uuid) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() != null) {
            var entry = mc.getNetworkHandler().getPlayerListEntry(uuid);
            if (entry != null) {
                return entry.getProfile().name();
            }
        }
        return null;
    }
}
