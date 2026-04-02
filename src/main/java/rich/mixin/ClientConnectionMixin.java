package rich.mixin;

import io.netty.channel.ChannelPipeline;
import io.netty.handler.proxy.Socks4ProxyHandler;
import io.netty.handler.proxy.Socks5ProxyHandler;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkSide;
import net.minecraft.network.handler.PacketSizeLogger;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import rich.events.api.EventManager;
import rich.events.impl.PacketEvent;
import rich.util.config.impl.proxy.ProxyConfig;
import rich.util.proxy.Proxy;

import java.net.InetSocketAddress;

@Mixin(ClientConnection.class)
public class ClientConnectionMixin {

    @Inject(method = "handlePacket", at = @At("HEAD"), cancellable = true)
    private static <T extends PacketListener> void handlePacketPre(Packet<T> packet, PacketListener listener, CallbackInfo info) {
        PacketEvent packetEvent = new PacketEvent(packet, PacketEvent.Type.RECEIVE);
        EventManager.callEvent(packetEvent);
        if (packetEvent.isCancelled()) {
            info.cancel();
        }
    }

    @Inject(method = "send(Lnet/minecraft/network/packet/Packet;)V", at = @At("HEAD"), cancellable = true)
    private void sendPre(Packet<?> packet, CallbackInfo info) {
        PacketEvent packetEvent = new PacketEvent(packet, PacketEvent.Type.SEND);
        EventManager.callEvent(packetEvent);
        if (packetEvent.isCancelled()) {
            info.cancel();
        }
    }

    @Inject(method = "addHandlers", at = @At("RETURN"))
    private static void addHandlersHook(ChannelPipeline pipeline, NetworkSide side, boolean local, PacketSizeLogger packetSizeLogger, CallbackInfo ci) {
        ProxyConfig config = ProxyConfig.getInstance();
        Proxy proxy = config.getDefaultProxy();

        if (proxy != null && config.isProxyEnabled() && !proxy.isEmpty() && side == NetworkSide.CLIENTBOUND && !local) {
            InetSocketAddress proxyAddress = new InetSocketAddress(proxy.getIp(), proxy.getPort());

            if (proxy.type == Proxy.ProxyType.SOCKS4) {
                pipeline.addFirst("rich_socks4_proxy", new Socks4ProxyHandler(proxyAddress, proxy.username));
            } else {
                pipeline.addFirst("rich_socks5_proxy", new Socks5ProxyHandler(proxyAddress, proxy.username, proxy.password));
            }

            config.setLastUsedProxy(new Proxy(proxy));
        }
    }
}