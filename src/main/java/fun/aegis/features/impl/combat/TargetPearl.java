package fun.aegis.features.impl.combat;

import antidaunleak.api.annotation.Native;
import fun.aegis.utils.features.aura.warp.Turns;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import fun.aegis.utils.interactions.inv.InventoryFlowManager;
import fun.aegis.utils.interactions.inv.InventoryTask;
import fun.aegis.utils.interactions.simulate.PlayerSimulation;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Vec3d;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.client.managers.event.types.EventType;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.BindSetting;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.common.repository.friend.FriendUtils;
import fun.aegis.utils.math.time.StopWatch;
import fun.aegis.utils.math.task.TaskPriority;
import fun.aegis.utils.math.script.Script;
import fun.aegis.events.player.EntitySpawnEvent;
import fun.aegis.events.player.PostMotionEvent;
import fun.aegis.events.player.RotationUpdateEvent;
import fun.aegis.utils.features.aura.utils.MathAngle;
import fun.aegis.utils.features.aura.warp.TurnsConfig;
import fun.aegis.utils.features.aura.warp.TurnsConnection;
import fun.aegis.utils.features.aura.rotations.impl.SnapAngle;
import fun.aegis.features.impl.render.ProjectilePrediction;
import java.util.Comparator;
import java.util.Objects;
import java.util.stream.IntStream;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetPearl extends Module {
    StopWatch stopWatch = new StopWatch();
    StopWatch pearlThrowCooldown = new StopWatch();
    Script script = new Script();
    int originalSlot = -1;
    Vec3d lastEnemyPearlPos = null;
    long lastEnemyPearlTime = 0;

    SelectSetting modeSetting = new SelectSetting("Mode", "When will target pearl work")
            .value("Bind", "Always").selected("Always");

    SelectSetting targetSetting = new SelectSetting("Targets", "Targets for which pearls will be thrown")
            .value("Aura Target", "All").selected("Aura Target");

    BindSetting throwSetting = new BindSetting("Throw","Throw Key").visible(()-> modeSetting.isSelected("Bind"));

    SliderSettings distanceSetting = new SliderSettings("Distance", "Target Pearl Trigger Distance")
            .setValue(10).range(5, 15);

    public TargetPearl() {
        super("TargetPearl","Target Pearl", ModuleCategory.COMBAT);
        setup(modeSetting, targetSetting, throwSetting, distanceSetting);
    }

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent e) {
        if (e.getEntity() instanceof EnderPearlEntity pearl) {
            net.minecraft.entity.Entity ownerEntity = pearl.getOwner();
            LivingEntity owner = ownerEntity instanceof LivingEntity ? (LivingEntity) ownerEntity : null;
            if (owner == null) {
                LivingEntity closestPlayer = mc.world.getPlayers().stream()
                        .filter(p -> p.distanceTo(pearl) <= 3)
                        .min(Comparator.comparingDouble(p -> p.distanceTo(pearl)))
                        .orElse(null);
                if (closestPlayer != null) {
                    pearl.setOwner(closestPlayer);
                }
            }
            
            if (owner != null && !owner.equals(mc.player) && !FriendUtils.isFriend(owner)) {
                LivingEntity target = Aura.getInstance().getLastTarget();
                if (targetSetting.isSelected("All") || (target != null && target.equals(owner))) {
                    lastEnemyPearlPos = pearl.getPos();
                    lastEnemyPearlTime = System.currentTimeMillis();
                    throwPearlToLocation(pearl.getPos());
                }
            }
        }
    }

    private void throwPearlToLocation(Vec3d targetLocation) {
        if (mc.player == null) return;
        
        Slot pearlSlot = InventoryTask.getSlot(Items.ENDER_PEARL);
        if (pearlSlot == null) return;
        
        // Не кидаем если уже недавно кидали
        if (!pearlThrowCooldown.finished(300)) return;
        
        originalSlot = mc.player.getInventory().selectedSlot;
        
        ProjectilePrediction prediction = ProjectilePrediction.getInstance();
        Vec3d eyePos = mc.player.getEyePos();
        
        // Ищем оптимальный угол для броска
        IntStream.range(-89, 89).mapToObj(pitch -> {
            float yaw = MathAngle.fromVec3d(targetLocation.subtract(eyePos)).getYaw();
            return new Turns(yaw, pitch);
        }).filter(angle -> {
            HitResult result = prediction.checkTrajectory(angle.toVector(), new EnderPearlEntity(mc.world, mc.player, pearlSlot.getStack()), 1.5);
            return result != null && result.getPos().distanceTo(targetLocation) <= 2.0F;
        }).max(Comparator.comparingDouble(Turns::getPitch)).ifPresent(angle -> {
            // Наводимся на позицию
            TurnsConnection.INSTANCE.rotateTo(new Turns.VecRotation(angle, angle.toVector()), mc.player, 1, new TurnsConfig(new SnapAngle(), true, true), TaskPriority.HIGH_IMPORTANCE_3, this);
            InventoryFlowManager.unPressMoveKeys();
            
            // Выполняем бросок
            script.cleanup().addTickStep(0, () -> {
                InventoryTask.swapAndUse(Items.ENDER_PEARL, angle, false);
                script.cleanup().addTickStep(2, () -> {
                    // Возвращаем слот
                    InventoryTask.switchTo(originalSlot);
                    InventoryFlowManager.enableMoveKeys();
                    pearlThrowCooldown.reset();
                });
            });
        });
    }

    @EventHandler
    public void onPostMotion(PostMotionEvent e) {
        script.update();
    }
}
