package rich.modules.impl.movement;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.Vec3d;
import rich.events.api.EventHandler;
import rich.events.impl.FireworkEvent;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class SuperFireWork extends ModuleStructure {
    SelectSetting modeSetting = new SelectSetting("Режим", "Выберите тип режима")
            .value("BravoHvH", "ReallyWorld", "PulseHVH", "Custom", "Soft");

    SliderSettings customSpeedSetting = new SliderSettings("Скорость", "Скорость для Custom режима")
            .range(1.5f, 3f)
            .setValue(1.963f)
            .visible(() -> modeSetting.isSelected("Custom"));

    SliderSettings speedYSetting = new SliderSettings("Скорость Y", "Вертикальная скорость (меньше = плавнее)")
            .range(0.5f, 2.5f)
            .setValue(1.5f);

    SliderSettings smoothingFactor = new SliderSettings("Плавность", "Сглаживание скорости (0.1 = плавно, 1.0 = резко)")
            .range(0.1f, 1.0f)
            .setValue(0.6f);

    BooleanSetting nearBoostSetting = new BooleanSetting("Буст рядом", "Увеличивать скорость когда игрок рядом")
            .setValue(true);

    BooleanSetting smoothVelocity = new BooleanSetting("Плавная скорость", "Плавно интерполировать скорость")
            .setValue(true);

    BooleanSetting adaptiveSpeed = new BooleanSetting("Адаптивная скорость", "Автоматически подстраивать скорость под FPS")
            .setValue(true);

    @NonFinal
    private Vec3d lastVelocity = Vec3d.ZERO;
    @NonFinal
    private double baseSpeedMultiplier = 1.0;

    public SuperFireWork() {
        super("SuperFireWork", "Super FireWork", ModuleCategory.MOVEMENT);
        settings(modeSetting, customSpeedSetting, speedYSetting, smoothingFactor, nearBoostSetting, smoothVelocity, adaptiveSpeed);
    }

    @Override
    public void deactivate() {
        lastVelocity = Vec3d.ZERO;
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onFirework(FireworkEvent e) {
        if (mc.player == null || !mc.player.isGliding()) return;

        updateAdaptiveSpeed();

        float yaw = AngleConnection.INSTANCE.getRotation().getYaw() % 360f;
        if (yaw < 0) yaw += 360f;

        if (modeSetting.isSelected("ReallyWorld")) {
            handleReallyWorldMode(e, yaw);
        } else if (modeSetting.isSelected("BravoHvH")) {
            handleBravoHvHMode(e, yaw);
        } else if (modeSetting.isSelected("PulseHVH")) {
            handlePulseHVHMode(e, yaw);
        } else if (modeSetting.isSelected("Custom")) {
            handleCustomMode(e, yaw);
        } else if (modeSetting.isSelected("Soft")) {
            handleSoftMode(e, yaw);
        }
    }

    private void updateAdaptiveSpeed() {
        if (!adaptiveSpeed.isValue()) {
            baseSpeedMultiplier = 1.0;
            return;
        }

        int fps = mc.getCurrentFps();
        if (fps < 30) {
            baseSpeedMultiplier = 0.7;
        } else if (fps < 60) {
            baseSpeedMultiplier = 0.85;
        } else if (fps > 144) {
            baseSpeedMultiplier = 1.05;
        } else {
            baseSpeedMultiplier = 1.0;
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleReallyWorldMode(FireworkEvent e, float yaw) {
        float[] diagonals = {45f, 135f, 225f, 315f};
        float closestDiff = 180f;

        for (float d : diagonals) {
            float diff = Math.abs(yaw - d);
            diff = Math.min(diff, 360f - diff);
            if (diff < closestDiff) closestDiff = diff;
        }

        double speedXZ = 1.5;
        double speedY = 1.5;

        if (closestDiff <= 4) {
            speedXZ = 2.2;
        } else if (closestDiff <= 8) {
            speedXZ = 2.06;
        } else if (closestDiff <= 12) {
            speedXZ = 1.98;
        } else if (closestDiff <= 16) {
            speedXZ = 1.87;
        } else if (closestDiff <= 20) {
            speedXZ = 1.8;
        } else if (closestDiff <= 24) {
            speedXZ = 1.74;
        } else if (closestDiff <= 28) {
            speedXZ = 1.7;
        } else if (closestDiff <= 32) {
            speedXZ = 1.65;
        } else if (closestDiff <= 36) {
            speedXZ = 1.63;
        } else {
            speedXZ = 1.61;
            speedY = 1.61;
        }

        applyFireworkVelocity(e, speedXZ, speedY);
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleBravoHvHMode(FireworkEvent e, float yaw) {
        boolean isDiagonal = checkDiagonal(yaw, 16f);
        boolean nearPlayer = checkNearPlayer(4f);

        double speedXZ;
        double speedY = 1.66;

        if (isDiagonal) {
            speedXZ = 1.963;
        } else if (nearBoostSetting.isValue() && nearPlayer) {
            speedXZ = 1.82;
            speedY = 1.67;
        } else {
            speedXZ = 1.675;
        }

        applyFireworkVelocity(e, speedXZ, speedY);
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handlePulseHVHMode(FireworkEvent e, float yaw) {
        boolean isDiagonal = checkDiagonal(yaw, 16f);
        boolean nearPlayer = checkNearPlayer(5f);

        double speedXZ;
        double speedY = 1.66;

        if (isDiagonal) {
            speedXZ = 1.963;
        } else if (nearBoostSetting.isValue() && nearPlayer) {
            speedXZ = 1.82;
            speedY = 1.67;
        } else {
            speedXZ = 1.675;
        }

        applyFireworkVelocity(e, speedXZ, speedY);
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleCustomMode(FireworkEvent e, float yaw) {
        boolean isDiagonal = checkDiagonal(yaw, 16f);
        boolean nearPlayer = checkNearPlayer(5f);

        double speedXZ;
        double speedY = speedYSetting.getValue();

        if (isDiagonal) {
            speedXZ = customSpeedSetting.getValue();
        } else if (nearBoostSetting.isValue() && nearPlayer) {
            speedXZ = customSpeedSetting.getValue() - 0.1f;
            speedY = speedYSetting.getValue() + 0.1f;
        } else {
            speedXZ = 1.675;
        }

        applyFireworkVelocity(e, speedXZ, speedY);
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleSoftMode(FireworkEvent e, float yaw) {
        boolean isDiagonal = checkDiagonal(yaw, 20f);
        boolean nearPlayer = checkNearPlayer(6f);

        double speedXZ = 1.45;
        double speedY = speedYSetting.getValue() * 0.8;

        if (isDiagonal) {
            speedXZ = 1.65;
        } else if (nearBoostSetting.isValue() && nearPlayer) {
            speedXZ = 1.55;
            speedY = speedYSetting.getValue() * 0.9;
        }

        applyFireworkVelocity(e, speedXZ, speedY);
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean checkDiagonal(float yaw, float threshold) {
        for (float d : new float[]{45f, 135f, 225f, 315f}) {
            float diff = Math.abs(yaw - d);
            diff = Math.min(diff, 360f - diff);
            if (diff <= threshold) {
                return true;
            }
        }
        return false;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean checkNearPlayer(float distance) {
        if (!nearBoostSetting.isValue() || mc.world == null) return false;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            if (player.distanceTo(mc.player) <= distance) {
                return true;
            }
        }
        return false;
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void applyFireworkVelocity(FireworkEvent e, double speedXZ, double speedY) {
        Vec3d rotationVector = AngleConnection.INSTANCE.getMoveRotation().toVector();
        Vec3d currentVelocity = e.getVector();

        speedXZ *= baseSpeedMultiplier;
        speedY *= baseSpeedMultiplier;

        Vec3d targetVelocity = new Vec3d(
                rotationVector.x * speedXZ,
                rotationVector.y * speedY,
                rotationVector.z * speedXZ
        );

        Vec3d finalVelocity;
        if (smoothVelocity.isValue()) {
            double factor = smoothingFactor.getValue();
            finalVelocity = new Vec3d(
                    lerp(lastVelocity.x, targetVelocity.x, factor),
                    lerp(lastVelocity.y, targetVelocity.y, factor),
                    lerp(lastVelocity.z, targetVelocity.z, factor)
            );
            lastVelocity = finalVelocity;
        } else {
            finalVelocity = targetVelocity;
        }

        e.setVector(finalVelocity);
    }

    private double lerp(double start, double end, double factor) {
        return start + (end - start) * factor;
    }
}