package fun.aegis.features.impl.movement;

import antidaunleak.api.annotation.Native;
import fun.aegis.features.impl.combat.Aura;
import fun.aegis.events.packet.PacketEvent;
import fun.aegis.utils.interactions.inv.InventoryTask;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.item.Items;
import net.minecraft.util.math.Vec3d;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.events.player.TickEvent;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.math.time.StopWatch;

import java.util.Random;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ElytraMotion extends Module {
    public static Fly getInstance() {
        return Instance.get(Fly.class);
    }
    @NonFinal StopWatch timer = new StopWatch();
    @NonFinal Vec3d targetPosition = null;
    @NonFinal
    Random random = new Random();
    @NonFinal double rotationAngle = 0.0;
    public ElytraMotion() {
        super("ElytraMotion", "Elytra Motion", ModuleCategory.MOVEMENT);
        setup();
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (!state || mc.player == null || mc.world == null || !mc.player.isGliding()) return;

        Aura aura = Instance.get(Aura.class);

//        if (timer.every(500)) {
//            InventoryTask.swapAndUse(Items.FIREWORK_ROCKET);
//        }

        if (aura.isState()) {
            if (aura.isState() && aura.getTarget() !=null && mc.player.distanceTo(aura.getTarget()) < aura.getAttackRange().getValue() - 1F) {
                mc.player.setVelocity(0, 0.02, 0);
            }
        }
    }


    @EventHandler
    public void onPacket(PacketEvent e) {
        Aura aura = Instance.get(Aura.class);
        if (aura.isState() && aura.getTarget() != null && mc.player.distanceTo(aura.getTarget()) < aura.getAttackRange().getValue() - 1F) {
            switch (e.getPacket()) {
                default -> {
                }
            }
        }
    }

    @Override
    public void deactivate() {
        super.deactivate();
    }
}
