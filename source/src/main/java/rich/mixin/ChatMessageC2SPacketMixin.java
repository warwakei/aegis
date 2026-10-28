package rich.mixin;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.packet.c2s.play.ChatMessageC2SPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.events.api.EventManager;
import rich.events.impl.ChatEvent;

@Mixin(ChatMessageC2SPacket.class)
public class ChatMessageC2SPacketMixin {

    @Shadow @Mutable
    private String chatMessage;

    @Inject(method = "<init>(Lnet/minecraft/network/PacketByteBuf;)V", at = @At("TAIL"))
    private void onInit(PacketByteBuf buf, CallbackInfo ci) {
        if (this.chatMessage != null) {
            System.out.println("[DEBUG] ChatMessageC2SPacket создан с: '" + this.chatMessage + "'");
            
            ChatEvent event = new ChatEvent(this.chatMessage);
            EventManager.callEvent(event);
            
            if (!event.getMessage().equals(this.chatMessage)) {
                System.out.println("[DEBUG] Заменяем сообщение: '" + this.chatMessage + "' -> '" + event.getMessage() + "'");
                this.chatMessage = event.getMessage();
            }
        }
    }
}