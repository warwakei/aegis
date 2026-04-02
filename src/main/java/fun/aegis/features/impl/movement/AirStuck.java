package fun.aegis.features.impl.movement;

import antidaunleak.api.annotation.Native;
import fun.aegis.events.packet.PacketEvent;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.utils.client.managers.event.EventHandler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AirStuck extends Module {

    public AirStuck() {
        super("AirStuck", "Air Stuck", ModuleCategory.MOVEMENT);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onPacket(PacketEvent e) {
        if (mc.player == null || mc.world == null) return;

        mc.player.setVelocity(0, 0, 0);
        mc.player.setMovementSpeed(0);

        switch (e.getPacket()) {
            case PlayerMoveC2SPacket move -> e.cancel();
            default -> {}
        }

    }
}
