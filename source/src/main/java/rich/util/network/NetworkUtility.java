package rich.util.network;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.PendingUpdateManager;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.hit.BlockHitResult;
import rich.IMinecraft;
import rich.mixin.ClientConnectionAccessor;
import rich.mixin.IClientWorld;
import rich.modules.impl.combat.aura.Angle;
import rich.util.timer.TimerUtil;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@UtilityClass
public class NetworkUtility implements IMinecraft {
    private boolean shouldTriggerEvent = true;
    private boolean serverSprinting = false;

    @Getter
    private float tpsFactor = 0;
    private int received = 0;
    private long lastReceive = 0;
    private TimerUtil tpsTimer = new TimerUtil();

    public void pauseEvents() {
        shouldTriggerEvent = false;
    }

    public void resumeEvents() {
        shouldTriggerEvent = true;
    }

    public boolean shouldTriggerEvent() {
        return shouldTriggerEvent;
    }

    public void updateServerSprint(boolean sprint) {
        serverSprinting = sprint;
    }

    public boolean serverSprinting() {
        return serverSprinting;
    }

    public void sendWithoutEvent(Runnable runnable) {
        pauseEvents();
        runnable.run();
        resumeEvents();
    }

    public void sendWithoutEvent(Packet<?> packet) {
        pauseEvents();
        send(packet);
        resumeEvents();
    }

    public void send(Packet<?> packet) {
        if (mc.getNetworkHandler() == null) return;

        if (packet instanceof ClickSlotC2SPacket click) {
            mc.interactionManager.clickSlot(click.syncId(), click.slot(), click.button(), click.actionType(), mc.player);
        } else {
            mc.getNetworkHandler().sendPacket(packet);
        }
    }

    public void sendInputPacket(boolean forward, boolean backward, boolean left, boolean right, boolean jump, boolean sneak, boolean sprint) {
        PlayerInput input = new PlayerInput(forward, backward, left, right, jump, sneak, sprint);
        mc.getNetworkHandler().sendPacket(new PlayerInputC2SPacket(input));
    }

    public void sendOnlySneak(boolean sneak) {
        PlayerInput playerInput = mc.player.input.playerInput;
        sendInputPacket(playerInput.forward(), playerInput.backward(), playerInput.left(), playerInput.right(), playerInput.jump(), sneak, playerInput.sprint());
    }

    public void sendUse(Hand hand) {
        sendUse(hand, new Angle(mc.player.getYaw(), mc.player.getPitch()));
    }

    public void sendUse(Hand hand, Angle angle) {
        try (PendingUpdateManager pendingUpdateManager = ((IClientWorld)mc.world).client$pending().incrementSequence()) {
            int i = pendingUpdateManager.getSequence();
            PlayerInteractItemC2SPacket packet = new PlayerInteractItemC2SPacket(hand, i, angle.getYaw(), angle.getPitch());
            NetworkUtility.send(packet);
        }
    }

    public void sendUse(Hand hand, BlockHitResult hitResult) {
        try (PendingUpdateManager pendingUpdateManager = ((IClientWorld)mc.world).client$pending().incrementSequence()) {
            int i = pendingUpdateManager.getSequence();
            PlayerInteractBlockC2SPacket packet = new PlayerInteractBlockC2SPacket(hand, hitResult, i);
            NetworkUtility.send(packet);
        }
    }

    public boolean is(String server) {
        return mc.getNetworkHandler() != null && mc.getNetworkHandler().getServerInfo() != null && mc.getNetworkHandler().getServerInfo().address.contains(server);
    }

    public void handleCPacket(Packet<?> packet) {
        if (packet instanceof PlayerMoveC2SPacket e) {
            PlayerState.lastGround = e.isOnGround();
            PlayerState.lastVertical = mc.player.verticalCollision;
        }
    }

    public void handleSPacket(Packet<?> packet) {
        if (packet instanceof WorldTimeUpdateS2CPacket e) {
            lastReceive = System.currentTimeMillis();
        }
    }

    public void handlePacket(Packet<?> packet) {
        if (!(mc.getNetworkHandler() instanceof ClientPlayNetworkHandler net)) return;
        if (mc.isOnThread()) {
            ClientConnectionAccessor.handlePacket(packet, net);
        } else {
            mc.execute(() -> ClientConnectionAccessor.handlePacket(packet, net));
        }
    }

    @UtilityClass
    public class PlayerState {
        public boolean lastGround = false, lastVertical = false;
        public int lastTp = 0;
    }

    public UUID offlineUUID(String name) {
        return UUID.nameUUIDFromBytes(("OfflinePlayer:" + name).getBytes(StandardCharsets.UTF_8));
    }
}