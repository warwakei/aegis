package fun.aegis.utils.features.aura.warp;

import fun.aegis.utils.features.aura.utils.MathAngle;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.client.managers.event.EventManager;
import fun.aegis.utils.client.managers.event.types.EventType;
import fun.aegis.features.module.Module;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.math.task.TaskPriority;
import fun.aegis.utils.math.task.TaskProcessor;
import fun.aegis.Aegis;
import fun.aegis.events.packet.PacketEvent;
import fun.aegis.events.player.*;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class TurnsConnection implements QuickImports {
    public static TurnsConnection INSTANCE = new TurnsConnection();

    TurnsConstructor lastRotationPlan;
    final TaskProcessor<TurnsConstructor> rotationPlanTaskProcessor = new TaskProcessor<>();
    public Turns currentAngle;
    Turns previousAngle;
    Turns serverAngle = Turns.DEFAULT;
    Turns fakeAngle;


    public TurnsConnection() {
        Aegis.getInstance().getEventManager().register(this);
    }

    public void setRotation(Turns value) {
        if (value == null) {
            this.previousAngle = this.currentAngle != null ? this.currentAngle : MathAngle.cameraAngle();
        } else {
            this.previousAngle = this.currentAngle;
        }

        this.currentAngle = value;
    }

    public Turns getRotation() {
        return currentAngle != null ? currentAngle : MathAngle.cameraAngle();
    }

    public Turns getFakeRotation() {
        if (fakeAngle != null) {
            return fakeAngle;
        }

        return currentAngle != null ? currentAngle : previousAngle != null ? previousAngle : MathAngle.cameraAngle();
    }
    public void setFakeRotation(Turns angle) {
        this.fakeAngle = angle;
    }
    public Turns getPreviousRotation() {
        return currentAngle != null && previousAngle != null ? previousAngle : new Turns(mc.player.prevYaw, mc.player.prevPitch);
    }

    public Turns getMoveRotation() {
        TurnsConstructor rotationPlan = getCurrentRotationPlan();
        return currentAngle != null && rotationPlan != null && rotationPlan.isMoveCorrection() ? currentAngle : MathAngle.cameraAngle();
    }

    public TurnsConstructor getCurrentRotationPlan() {
        return rotationPlanTaskProcessor.fetchActiveTaskValue() != null ? rotationPlanTaskProcessor.fetchActiveTaskValue() : lastRotationPlan;
    }

    public void rotateTo(Turns.VecRotation vecRotation, LivingEntity entity, int reset, TurnsConfig configurable, TaskPriority taskPriority, Module provider) {
        rotateTo(configurable.createRotationPlan(vecRotation.getAngle(), vecRotation.getVec(), entity, reset), taskPriority, provider);
    }

    public void rotateTo(Turns angle, int reset, TurnsConfig configurable, TaskPriority taskPriority, Module provider) {
        rotateTo(configurable.createRotationPlan(angle,angle.toVector(),null, reset), taskPriority, provider);
    }

    public void rotateTo(Turns angle, TurnsConfig configurable, TaskPriority taskPriority, Module provider) {
        rotateTo(configurable.createRotationPlan(angle,angle.toVector(),null,1), taskPriority, provider);
    }

    public void rotateTo(TurnsConstructor plan, TaskPriority taskPriority, Module provider) {
        rotationPlanTaskProcessor.addTask(new TaskProcessor.Task<>(1, taskPriority.getPriority(), provider, plan));
    }


    public void update() {
        TurnsConstructor activePlan = getCurrentRotationPlan();
        if (activePlan == null) return;

        Turns clientAngle = MathAngle.cameraAngle();

        if (lastRotationPlan != null) {
            double differenceFromCurrentToPlayer = computeRotationDifference(serverAngle, clientAngle);
            if (activePlan.getTicksUntilReset() <= rotationPlanTaskProcessor.tickCounter && differenceFromCurrentToPlayer < activePlan.getResetThreshold()) {
                setRotation(null);
                lastRotationPlan = null;
                rotationPlanTaskProcessor.tickCounter = 0;
                return;
            }
        }

        Turns newAngle = activePlan.nextRotation(currentAngle != null ? currentAngle : clientAngle, rotationPlanTaskProcessor.fetchActiveTaskValue() == null).adjustSensitivity();

        setRotation(newAngle);

        lastRotationPlan = activePlan;
        rotationPlanTaskProcessor.tick(1);
    }


    public static double computeRotationDifference(Turns a, Turns b) {
        return Math.hypot(Math.abs(computeAngleDifference(a.getYaw(), b.getYaw())), Math.abs(a.getPitch() - b.getPitch()));
    }

    public static float computeAngleDifference(float a, float b) {
        return MathHelper.wrapDegrees(a - b);
    }


    private Vec3d fixVelocity(Vec3d currVelocity, Vec3d movementInput, float speed) {
        if (currentAngle != null) {
            float yaw = currentAngle.getYaw();
            double d = movementInput.lengthSquared();

            if (d < 1.0E-7) {
                return Vec3d.ZERO;
            } else {
                Vec3d vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).multiply(speed);

                float f = MathHelper.sin(yaw * 0.017453292f);
                float g = MathHelper.cos(yaw * 0.017453292f);

                return new Vec3d(vec3d.getX() * g - vec3d.getZ() * f, vec3d.getY(), vec3d.getZ() * g + vec3d.getX() * f);
            }
        }
        return currVelocity;
    }

    public void clear() {
        rotationPlanTaskProcessor.activeTasks.clear();
    }

    @EventHandler
    public void onPlayerVelocityStrafe(PlayerVelocityStrafeEvent e) {
        TurnsConstructor currentRotationPlan = getCurrentRotationPlan();
        if (currentRotationPlan != null && currentRotationPlan.isMoveCorrection()) {
            e.setVelocity(fixVelocity(e.getVelocity(), e.getMovementInput(), e.getSpeed()));
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        EventManager.callEvent(new RotationUpdateEvent(EventType.PRE));
        update();
        EventManager.callEvent(new RotationUpdateEvent(EventType.POST));
    }

    @EventHandler

    public void onPacket(PacketEvent event) {
        if (!event.isCancelled()) switch (event.getPacket()) {
            case PlayerMoveC2SPacket player when player.changesLook() -> serverAngle = new Turns(player.getYaw(1), player.getPitch(1));
            case PlayerPositionLookS2CPacket player -> serverAngle = new Turns(player.change().yaw(), player.change().pitch());
            default -> {}
        }
    }
}

