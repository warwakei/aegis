package rich.netpanel.loggers;

import net.minecraft.network.packet.Packet;

/**
 * Packet logger — logs sent and received packets.
 */
public class PacketLogger {

    private static final LogBuffer BUFFER = new LogBuffer(500);
    private static volatile boolean loggingSend = true;
    private static volatile boolean loggingReceive = true;

    public static void logSend(Packet<?> packet) {
        if (!loggingSend) return;
        String packetName = packet.getClass().getSimpleName();
        BUFFER.add("SEND", packetName);
    }

    public static void logReceive(Packet<?> packet) {
        if (!loggingReceive) return;
        String packetName = packet.getClass().getSimpleName();
        BUFFER.add("RECV", packetName);
    }

    public static void logSendDetailed(Packet<?> packet, String detail) {
        if (!loggingSend) return;
        String packetName = packet.getClass().getSimpleName();
        BUFFER.add("SEND", packetName + " — " + detail);
    }

    public static void logReceiveDetailed(Packet<?> packet, String detail) {
        if (!loggingReceive) return;
        String packetName = packet.getClass().getSimpleName();
        BUFFER.add("RECV", packetName + " — " + detail);
    }

    public static void setLoggingSend(boolean value) {
        loggingSend = value;
    }

    public static void setLoggingReceive(boolean value) {
        loggingReceive = value;
    }

    public static boolean isLoggingSend() {
        return loggingSend;
    }

    public static boolean isLoggingReceive() {
        return loggingReceive;
    }

    public static LogBuffer getBuffer() {
        return BUFFER;
    }
}
