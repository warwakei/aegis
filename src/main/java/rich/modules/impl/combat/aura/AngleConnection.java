package rich.modules.impl.combat.aura;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import rich.IMinecraft;
import rich.Initialization;
import rich.events.api.EventHandler;
import rich.events.api.EventManager;
import rich.events.api.types.EventType;
import rich.events.impl.PacketEvent;
import rich.events.impl.PlayerVelocityStrafeEvent;
import rich.events.impl.RotationUpdateEvent;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.util.math.TaskPriority;
import rich.util.math.TaskProcessor;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AngleConnection implements IMinecraft {
    public static AngleConnection INSTANCE = new AngleConnection();

    AngleConstructor lastRotationPlan;
    final TaskProcessor<AngleConstructor> rotationPlanTaskProcessor = new TaskProcessor<>();
    public Angle currentAngle;
    Angle previousAngle;
    Angle serverAngle = Angle.DEFAULT;
    Angle fakeAngle;
    boolean returning = false;


    public AngleConnection() {
        Initialization.getInstance().getManager().getEventManager().register(this);
    }

    public void setRotation(Angle value) {
        if (value == null) {
            this.previousAngle = this.currentAngle != null ? this.currentAngle : MathAngle.cameraAngle();
        } else {
            this.previousAngle = this.currentAngle;
        }

        this.currentAngle = value;
    }

    public Angle getRotation() {
        return currentAngle != null ? currentAngle : MathAngle.cameraAngle();
    }

    public Angle getFakeRotation() {
        if (fakeAngle != null) {
            return fakeAngle;
        }

        return currentAngle != null ? currentAngle : previousAngle != null ? previousAngle : MathAngle.cameraAngle();
    }

    public void setFakeRotation(Angle angle) {
        this.fakeAngle = angle;
    }

    public Angle getPreviousRotation() {
        return currentAngle != null && previousAngle != null ? previousAngle : new Angle(mc.player.lastYaw, mc.player.lastPitch);
    }

    public Angle getMoveRotation() {
        AngleConstructor rotationPlan = getCurrentRotationPlan();
        return currentAngle != null && rotationPlan != null && rotationPlan.isMoveCorrection() ? currentAngle : MathAngle.cameraAngle();
    }

    public AngleConstructor getCurrentRotationPlan() {
        return rotationPlanTaskProcessor.fetchActiveTaskValue() != null ? rotationPlanTaskProcessor.fetchActiveTaskValue() : lastRotationPlan;
    }

    public void rotateTo(Angle.VecRotation vecRotation, LivingEntity entity, int reset, AngleConfig configurable, TaskPriority taskPriority, ModuleStructure provider) {
        rotateTo(configurable.createRotationPlan(vecRotation.getAngle(), vecRotation.getVec(), entity, reset), taskPriority, provider);
    }

    public void rotateTo(Angle angle, int reset, AngleConfig configurable, TaskPriority taskPriority, ModuleStructure provider) {
        rotateTo(configurable.createRotationPlan(angle, angle.toVector(), null, reset), taskPriority, provider);
    }

    public void rotateTo(Angle angle, AngleConfig configurable, TaskPriority taskPriority, ModuleStructure provider) {
        rotateTo(configurable.createRotationPlan(angle, angle.toVector(), null, 1), taskPriority, provider);
    }

    public void rotateTo(AngleConstructor plan, TaskPriority taskPriority, ModuleStructure provider) {
        returning = false;
        rotationPlanTaskProcessor.addTask(new TaskProcessor.Task<>(1, taskPriority.getPriority(), provider, plan));
    }


    public void update() {
        AngleConstructor activePlan = getCurrentRotationPlan();

        if (activePlan == null) {
            if (currentAngle != null && returning) {
                Angle cameraAngle = MathAngle.cameraAngle();
                double diff = computeRotationDifference(currentAngle, cameraAngle);

                if (diff < 0.5) {
                    setRotation(null);
                    lastRotationPlan = null;
                    returning = false;
                } else {
                    float speed = 0.25f;
                    float distanceFactor = Math.min(1.0f, (float) diff / 30.0f);
                    speed = speed + (0.4f * distanceFactor);

                    float yawDiff = MathHelper.wrapDegrees(cameraAngle.getYaw() - currentAngle.getYaw());
                    float newYaw = currentAngle.getYaw() + yawDiff * speed;
                    float newPitch = MathHelper.lerp(speed, currentAngle.getPitch(), cameraAngle.getPitch());

                    setRotation(new Angle(newYaw, newPitch).adjustSensitivity());
                }
            }
            return;
        }

        returning = false;

        Angle clientAngle = MathAngle.cameraAngle();

        if (lastRotationPlan != null) {
            double differenceFromCurrentToPlayer = computeRotationDifference(serverAngle, clientAngle);
            if (activePlan.getTicksUntilReset() <= rotationPlanTaskProcessor.tickCounter && differenceFromCurrentToPlayer < activePlan.getResetThreshold()) {
                setRotation(null);
                lastRotationPlan = null;
                rotationPlanTaskProcessor.tickCounter = 0;
                return;
            }
        }

        Angle newAngle = activePlan.nextRotation(currentAngle != null ? currentAngle : clientAngle, rotationPlanTaskProcessor.fetchActiveTaskValue() == null).adjustSensitivity();

        setRotation(newAngle);

        lastRotationPlan = activePlan;
        rotationPlanTaskProcessor.tick(1);
    }


    public static double computeRotationDifference(Angle a, Angle b) {
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

    public void startReturning() {
//        clear();
//        lastRotationPlan = null;
//        rotationPlanTaskProcessor.tickCounter = 0;
//        returning = true;
    }

    public void reset() {
//        clear();
        currentAngle = null;
        previousAngle = null;
        fakeAngle = null;
        lastRotationPlan = null;
        rotationPlanTaskProcessor.tickCounter = 0;
//        returning = false;
    }

    @EventHandler
    public void onPlayerVelocityStrafe(PlayerVelocityStrafeEvent e) {
        AngleConstructor currentRotationPlan = getCurrentRotationPlan();
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
            case PlayerMoveC2SPacket player when player.changesLook() ->
                    serverAngle = new Angle(player.getYaw(1), player.getPitch(1));
            case PlayerPositionLookS2CPacket player ->
                    serverAngle = new Angle(player.change().yaw(), player.change().pitch());
            default -> {
            }
        }
    }
}