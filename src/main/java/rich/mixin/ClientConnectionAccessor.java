package rich.mixin;

import net.minecraft.network.ClientConnection;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientConnection.class)
public interface ClientConnectionAccessor {
    @Accessor("packetListener")
    PacketListener client$listener();

    @Invoker("handlePacket")
    static <T extends PacketListener> void handlePacket(Packet<T> packet, PacketListener listener) {
        throw new UnsupportedOperationException();
    }
}
