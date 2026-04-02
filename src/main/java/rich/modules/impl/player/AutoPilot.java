package rich.modules.impl.player;

import antidaunleak.api.annotation.Native;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.AngleConfig;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.util.Instance;
import rich.util.math.TaskPriority;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class AutoPilot extends ModuleStructure {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public ItemEntity target;

    private float lastYaw, lastPitch;

    private float targetYaw, targetPitch;

    Angle rot = new Angle(0, 0);

    public AutoPilot() {
        super("AutoPilot", "Auto Pilot", ModuleCategory.MISC);
    }

    public static AutoPilot getInstance() {
        return Instance.get(AutoPilot.class);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.getNetworkHandler() == null) {
            target = null;
            return;
        }

        target = findTarget();

        if (target != null) {
            processRotation();
        } else {
            lastYaw = mc.player.getYaw();
            lastPitch = mc.player.getPitch();
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void processRotation() {
        double dx = target.getEntityPos().getX() - mc.player.getEntityPos().getX();
        double dy = (target.getEntityPos().getY()) - (mc.player.getEntityPos().getY() + mc.player.getEyeHeight(mc.player.getPose()));
        double dz = target.getEntityPos().getZ() - mc.player.getEntityPos().getZ();

        targetYaw = (float) (Math.atan2(dz, dx) * 180.0 / Math.PI - 90.0);
        targetPitch = (float) (-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 180.0 / Math.PI);

        float maxRotation = 1024;

        float yawDiff = MathHelper.wrapDegrees(targetYaw - lastYaw);
        float yawStep = MathHelper.clamp(yawDiff, -maxRotation, maxRotation);
        lastYaw += yawStep;

        float pitchDiff = MathHelper.wrapDegrees(targetPitch - lastPitch);
        float pitchStep = MathHelper.clamp(pitchDiff, -maxRotation, maxRotation);
        lastPitch += pitchStep;
        mc.player.setYaw(lastYaw);
        mc.player.setPitch(lastPitch);
        rot.setYaw(lastYaw);
        rot.setPitch(lastPitch);
        AngleConnection.INSTANCE.rotateTo(rot, AngleConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private ItemEntity findTarget() {
        List<ItemEntity> items = mc.world.getEntitiesByClass(ItemEntity.class,
                        mc.player.getBoundingBox().expand(50.0),
                        e -> e.isAlive() && isValidItem(e))
                .stream()
                .sorted(Comparator.comparingDouble(e -> mc.player.squaredDistanceTo(e)))
                .collect(Collectors.toList());
        return items.isEmpty() ? null : items.get(0);
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean isValidItem(ItemEntity item) {
        var stack = item.getStack();
        return stack.getItem() == Items.SPAWNER ||
                stack.getItem() == Items.PLAYER_HEAD ||
                stack.getItem() == Items.ENCHANTED_GOLDEN_APPLE ||
                stack.getItem().toString().contains("_spawn_egg");
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        target = null;
        if (mc.player != null) {
            mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.Full(
                    mc.player.getEntityPos().getX(),
                    mc.player.getEntityPos().getY(),
                    mc.player.getEntityPos().getZ(),
                    mc.player.getYaw(),
                    mc.player.getPitch(),
                    mc.player.isOnGround(),
                    false
            ));
        }
    }
}