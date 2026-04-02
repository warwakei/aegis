package rich.modules.impl.movement;

import antidaunleak.api.annotation.Native;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import rich.events.api.EventHandler;
import rich.events.impl.InputEvent;
import rich.events.impl.TickEvent;
import rich.modules.impl.combat.Aura;
import rich.modules.impl.combat.AutoTotem;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.*;
import rich.util.Instance;

public class TargetStrafe extends ModuleStructure {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public SelectSetting mode = new SelectSetting("Режим", "Тип стрейфа")
            .value("Matrix", "Grim")
            .selected("Matrix");

    SelectSetting type = new SelectSetting("Точка ходьбы", "Выбирете точку куда будет идти стрейф")
            .value("Cube", "Center", "Circle")
            .selected("Cube").visible(() -> mode.isSelected("Grim"));

    SelectSetting typeMatrix = new SelectSetting("Точка для обхода", "Выберите точку обхода в режиме Matrix")
            .value("Cube", "Circle")
            .selected("Circle")
            .visible(() -> mode.isSelected("Matrix"));

    SliderSettings grimRadius = new SliderSettings("Радиус обхода", "Радиус обхода вокруг цели")
            .setValue(0.87F).range(0.1F, 1.5F).visible(() -> mode.isSelected("Grim") && (type.isSelected("Cube") || type.isSelected("Circle")));

    MultiSelectSetting setting = new MultiSelectSetting("Настройки", "Позволяет настроить работу стрейфов")
            .value("Auto Jump", "Only Key Pressed", "In front of the target", "Direction Mode")
            .selected("Auto Jump");

    SelectSetting directionMode = new SelectSetting("Направление", "Выберите направление обхода")
            .value("Clockwise", "Counterclockwise", "Random")
            .selected("Clockwise")
            .visible(() -> setting.isSelected("Direction Mode"));

    SliderSettings radius = new SliderSettings("Радиус", "Радиус обхода вокруг цели")
            .setValue(2.5F).range(0.1F, 7F).visible(() -> mode.isSelected("Matrix"));
    SliderSettings speed = new SliderSettings("Скорость", "Скорость стрейфа")
            .setValue(0.3F).range(0.1F, 1F).visible(() -> mode.isSelected("Matrix"));

    private int grimPointIndex = 0;

    public TargetStrafe() {
        super("TargetStrafe", "Target Strafe", ModuleCategory.MOVEMENT);
        settings(mode, type, typeMatrix, grimRadius, radius, speed, setting, directionMode);
    }

    public static TargetStrafe getInstance() {
        return Instance.get(TargetStrafe.class);
    }

    private boolean isAutoTotemBlocking() {
        AutoTotem autoTotem = Instance.get(AutoTotem.class);
        if (autoTotem == null) return false;
        if (!autoTotem.isState()) return false;
        return autoTotem.getExecutor().isBlocking() || autoTotem.getExecutor().isRunning();
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onInput(InputEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (isAutoTotemBlocking()) return;

        LivingEntity target = Aura.target;
        if (target == null || !target.isAlive()) return;

        if (!mode.isSelected("Grim")) return;

        if (setting.isSelected("Only Key Pressed")) {
            if (!mc.options.forwardKey.isPressed() &&
                    !mc.options.backKey.isPressed() &&
                    !mc.options.leftKey.isPressed() &&
                    !mc.options.rightKey.isPressed()) {
                return;
            }
        }

        Vec3d nextPoint = calculateGrimNextPoint(target);
        applyGrimMovement(event, nextPoint);
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private Vec3d calculateGrimNextPoint(LivingEntity target) {
        Vec3d playerPos = mc.player.getEntityPos();
        Vec3d targetPos = target.getEntityPos();
        double r = grimRadius.getValue();

        int directionMultiplier = getDirectionMultiplier();

        if (setting.isSelected("In front of the target")) {
            return calculateFrontPoint(target, targetPos, r, directionMultiplier);
        } else {
            return calculateNormalPoint(playerPos, targetPos, r, directionMultiplier);
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private Vec3d calculateFrontPoint(LivingEntity target, Vec3d targetPos, double r, int directionMultiplier) {
        float targetYaw = target.getYaw();

        if (type.isSelected("Center")) {
            return targetPos.add(
                    -Math.sin(Math.toRadians(targetYaw)) * r * directionMultiplier,
                    0,
                    Math.cos(Math.toRadians(targetYaw)) * r * directionMultiplier);
        } else {
            double offset = Math.cos(System.currentTimeMillis() / 500.0) * r * directionMultiplier;
            return targetPos.add(
                    -Math.sin(Math.toRadians(targetYaw)) * r + Math.cos(Math.toRadians(targetYaw)) * offset,
                    0,
                    Math.cos(Math.toRadians(targetYaw)) * r + Math.sin(Math.toRadians(targetYaw)) * offset
            );
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private Vec3d calculateNormalPoint(Vec3d playerPos, Vec3d targetPos, double r, int directionMultiplier) {
        if (type.isSelected("Cube")) {
            Vec3d[] points = new Vec3d[]{
                    new Vec3d(targetPos.x - r, playerPos.y, targetPos.z - r),
                    new Vec3d(targetPos.x - r, playerPos.y, targetPos.z + r),
                    new Vec3d(targetPos.x + r, playerPos.y, targetPos.z + r),
                    new Vec3d(targetPos.x + r, playerPos.y, targetPos.z - r)
            };

            if (playerPos.distanceTo(points[grimPointIndex]) < 0.5) {
                grimPointIndex = (grimPointIndex + directionMultiplier + points.length) % points.length;
            }

            return points[grimPointIndex];
        } else if (type.isSelected("Circle")) {
            double baseAngle = (System.currentTimeMillis() % 3600L) / 3600.0 * 4 * Math.PI;
            double angle = directionMultiplier > 0 ? baseAngle : (2 * Math.PI - baseAngle);

            return new Vec3d(
                    targetPos.x + Math.cos(angle) * r,
                    playerPos.y,
                    targetPos.z + Math.sin(angle) * r
            );
        } else {
            return new Vec3d(targetPos.x, playerPos.y, targetPos.z);
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void applyGrimMovement(InputEvent event, Vec3d nextPoint) {
        Vec3d playerPos = mc.player.getEntityPos();
        Vec3d direction = nextPoint.subtract(playerPos).normalize();

        float yaw = AngleConnection.INSTANCE.getRotation().getYaw();
        float movementAngle = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90F;
        float angleDiff = MathHelper.wrapDegrees(movementAngle - yaw);

        boolean forward = false, back = false, left = false, right = false;

        if (angleDiff >= -22.5 && angleDiff < 22.5) {
            forward = true;
        } else if (angleDiff >= 22.5 && angleDiff < 67.5) {
            forward = true; right = true;
        } else if (angleDiff >= 67.5 && angleDiff < 112.5) {
            right = true;
        } else if (angleDiff >= 112.5 && angleDiff < 157.5) {
            back = true; right = true;
        } else if (angleDiff >= -67.5 && angleDiff < -22.5) {
            forward = true; left = true;
        } else if (angleDiff >= -112.5 && angleDiff < -67.5) {
            left = true;
        } else if (angleDiff >= -157.5 && angleDiff < -112.5) {
            back = true; left = true;
        } else {
            back = true;
        }

        event.setDirectionalLow(forward, back, left, right);

        if (setting.isSelected("Auto Jump") && mc.player.isOnGround()) {
            event.setJumping(true);
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (isAutoTotemBlocking()) return;

        LivingEntity target = Aura.target;
        if (target == null || !target.isAlive()) return;

        if (!mode.isSelected("Matrix")) return;

        if (setting.isSelected("Only Key Pressed")) {
            if (!mc.options.forwardKey.isPressed() &&
                    !mc.options.backKey.isPressed() &&
                    !mc.options.leftKey.isPressed() &&
                    !mc.options.rightKey.isPressed()) {
                return;
            }
        }

        if (setting.isSelected("Auto Jump") && mc.player.isOnGround()) {
            mc.player.jump();
        }

        processMatrixStrafe(target);
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void processMatrixStrafe(LivingEntity target) {
        Vec3d playerPos = mc.player.getEntityPos();
        Vec3d targetPos = target.getEntityPos();
        double r = radius.getValue();

        int directionMultiplier = getDirectionMultiplier();

        if (setting.isSelected("In front of the target")) {
            processMatrixFrontStrafe(target, playerPos, targetPos, r, directionMultiplier);
            return;
        }

        if (typeMatrix.isSelected("Cube")) {
            processMatrixCubeStrafe(playerPos, targetPos, r, directionMultiplier);
        } else if (typeMatrix.isSelected("Circle")) {
            processMatrixCircleStrafe(playerPos, targetPos, r, directionMultiplier);
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private int getDirectionMultiplier() {
        int directionMultiplier = 1;
        if (setting.isSelected("Direction Mode")) {
            if (directionMode.isSelected("Counterclockwise")) {
                directionMultiplier = -1;
            } else if (directionMode.isSelected("Random")) {
                long time = System.currentTimeMillis() / 3000;
                directionMultiplier = (time % 2 == 0) ? 1 : -1;
            }
        }
        return directionMultiplier;
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void processMatrixFrontStrafe(LivingEntity target, Vec3d playerPos, Vec3d targetPos, double r, int directionMultiplier) {
        float targetYaw = target.getYaw();
        double x = targetPos.x - Math.sin(Math.toRadians(targetYaw)) * r * directionMultiplier;
        double z = targetPos.z + Math.cos(Math.toRadians(targetYaw)) * r * directionMultiplier;

        float yaw = (float) Math.toDegrees(Math.atan2(z - playerPos.z, x - playerPos.x)) - 90F;
        double motionSpeed = speed.getValue();
        mc.player.setVelocity(-Math.sin(Math.toRadians(yaw)) * motionSpeed,
                mc.player.getVelocity().y,
                Math.cos(Math.toRadians(yaw)) * motionSpeed);
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void processMatrixCubeStrafe(Vec3d playerPos, Vec3d targetPos, double r, int directionMultiplier) {
        Vec3d[] points = new Vec3d[]{
                new Vec3d(targetPos.x - r, playerPos.y, targetPos.z - r),
                new Vec3d(targetPos.x - r, playerPos.y, targetPos.z + r),
                new Vec3d(targetPos.x + r, playerPos.y, targetPos.z + r),
                new Vec3d(targetPos.x + r, playerPos.y, targetPos.z - r)
        };

        if (playerPos.distanceTo(points[grimPointIndex]) < 0.5) {
            grimPointIndex = (grimPointIndex + directionMultiplier + points.length) % points.length;
        }

        Vec3d nextPoint = points[grimPointIndex];
        Vec3d dirVec = nextPoint.subtract(playerPos).normalize();

        float yaw = (float) Math.toDegrees(Math.atan2(dirVec.z, dirVec.x)) - 90F;
        double motionSpeed = speed.getValue();

        mc.player.setVelocity(-Math.sin(Math.toRadians(yaw)) * motionSpeed,
                mc.player.getVelocity().y,
                Math.cos(Math.toRadians(yaw)) * motionSpeed);
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void processMatrixCircleStrafe(Vec3d playerPos, Vec3d targetPos, double r, int directionMultiplier) {
        double angle = Math.atan2(playerPos.z - targetPos.z, playerPos.x - targetPos.x);
        angle += directionMultiplier * speed.getValue() / Math.max(playerPos.distanceTo(targetPos), r);

        double x = targetPos.x + r * Math.cos(angle);
        double z = targetPos.z + r * Math.sin(angle);

        float yaw = (float) Math.toDegrees(Math.atan2(z - playerPos.z, x - playerPos.x)) - 90F;
        double motionSpeed = speed.getValue();

        mc.player.setVelocity(-Math.sin(Math.toRadians(yaw)) * motionSpeed,
                mc.player.getVelocity().y,
                Math.cos(Math.toRadians(yaw)) * motionSpeed);
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        super.activate();
        grimPointIndex = 0;
    }
}