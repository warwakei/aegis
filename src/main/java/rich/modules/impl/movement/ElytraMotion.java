package rich.modules.impl.movement;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.util.math.Vec3d;
import rich.events.api.EventHandler;
import rich.events.impl.PacketEvent;
import rich.events.impl.TickEvent;
import rich.modules.impl.combat.Aura;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.util.Instance;
import rich.util.timer.StopWatch;

import java.util.Random;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ElytraMotion extends ModuleStructure {

    public static Fly getInstance() {
        return Instance.get(Fly.class);
    }
    @NonFinal

    StopWatch timer = new StopWatch();
    @NonFinal
    Vec3d targetPosition = null;
    @NonFinal
    Random random = new Random();
    @NonFinal double rotationAngle = 0.0;

    public ElytraMotion() {
        super("ElytraMotion", "Elytra Motion", ModuleCategory.MOVEMENT);
        this.settings();
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (!state || mc.player == null || mc.world == null || !mc.player.isGliding()) return;

        Aura aura = Instance.get(Aura.class);

        if (aura.isState()) {
            handleAuraMotion(aura);
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleAuraMotion(Aura aura) {
        if (aura.isState() && aura.target != null && mc.player.distanceTo(aura.target) < aura.getAttackrange().getValue() - 1F) {
            mc.player.setVelocity(0, 0.02, 0);
        }
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        Aura aura = Instance.get(Aura.class);
        if (aura.isState() && aura.target != null && mc.player.distanceTo(aura.target) < aura.getAttackrange().getValue() - 1F) {
            switch (e.getPacket()) {
                default -> {
                }
            }
        }
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        super.deactivate();
    }
}