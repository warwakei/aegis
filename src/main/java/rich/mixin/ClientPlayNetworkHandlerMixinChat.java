package rich.mixin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ChatMessageS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.util.repository.ignore.IgnoreUtils;

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
            }
        }
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
