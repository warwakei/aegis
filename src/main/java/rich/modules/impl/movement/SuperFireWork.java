package rich.modules.impl.movement;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
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

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SuperFireWork extends ModuleStructure {
    SelectSetting modeSetting = new SelectSetting("Режим", "Выберите тип режима")
            .value("BravoHvH", "ReallyWorld", "PulseHVH", "Custom");

    SliderSettings customSpeedSetting = new SliderSettings("Скорость", "Скорость для Custom режима")
            .range(1.5f, 3f)
            .setValue(1.963f)
            .visible(() -> modeSetting.isSelected("Custom"));

    BooleanSetting nearBoostSetting = new BooleanSetting("", "");

    public SuperFireWork() {
        super("SuperFireWork", "Super FireWork", ModuleCategory.MOVEMENT);
        settings(modeSetting, customSpeedSetting);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onFirework(FireworkEvent e) {
        if (mc.player == null || !mc.player.isGliding()) return;

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
        double speedY = 1.66;

        if (isDiagonal) {
            speedXZ = customSpeedSetting.getValue();
        } else if (nearBoostSetting.isValue() && nearPlayer) {
            speedXZ = customSpeedSetting.getValue() - 0.1f;
            speedY = 1.67;
        } else {
            speedXZ = 1.675;
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

        e.setVector(currentVelocity.add(
                rotationVector.x * 0.1 + (rotationVector.x * speedXZ - currentVelocity.x) * 0.5,
                rotationVector.y * 0.1 + (rotationVector.y * speedY - currentVelocity.y) * 0.5,
                rotationVector.z * 0.1 + (rotationVector.z * speedXZ - currentVelocity.z) * 0.5
        ));
    }
}