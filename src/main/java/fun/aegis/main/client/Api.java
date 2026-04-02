package fun.aegis.main.client;

import net.minecraft.client.MinecraftClient;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.Identifier;


public interface Api {

    MinecraftClient mc = MinecraftClient.getInstance();


    static void sendPacket(Packet<?> packet) {
        if (mc.getNetworkHandler() == null) return;

        mc.getNetworkHandler().sendPacket(packet);
    }

}
