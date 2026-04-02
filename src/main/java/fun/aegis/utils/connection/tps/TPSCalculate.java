package fun.aegis.utils.connection.tps;

import fun.aegis.Aegis;
import fun.aegis.events.packet.PacketEvent;
import fun.aegis.utils.client.managers.event.EventHandler;
import lombok.Getter;
import net.minecraft.network.packet.s2c.play.WorldTimeUpdateS2CPacket;
import net.minecraft.util.math.MathHelper;

@Getter
public class TPSCalculate {

    private float TPS = 20;
    private float adjustTicks = 0;
    private long timestamp;

    public TPSCalculate() {
        Aegis.getInstance().getEventManager().register(this);
    }

    @EventHandler
    private void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof WorldTimeUpdateS2CPacket) {
            updateTPS();
        }
    }

    private void updateTPS() {
        long delay = System.nanoTime() - timestamp;
        float maxTPS = 20;
        float rawTPS = maxTPS * (1e9f / delay);
        float boundedTPS = MathHelper.clamp(rawTPS, 0, maxTPS);
        TPS = (float) round(boundedTPS);
        adjustTicks = boundedTPS - maxTPS;
        timestamp = System.nanoTime();
    }

    public double round(double input) {
        return Math.round(input * 100.0) / 100.0;
    }
}
