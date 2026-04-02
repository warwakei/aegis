package fun.aegis.features.impl.movement;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.util.math.Vec3d;

import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.events.player.FireworkEvent;
import fun.aegis.utils.features.aura.warp.TurnsConnection;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SuperFireWork extends Module {
    SelectSetting modeSetting = new SelectSetting("Режим", "Выберите тип режима")
            .value("Grim", "BravoHvH", "Custom");

    SliderSettings speedSetting = new SliderSettings("Скорость", "Скорость полета фейерверка")
            .range(1f, 50f)
            .setValue(20f)
            .visible(() -> modeSetting.isSelected("Custom"));

    public SuperFireWork() {
        super("SuperFireWork", "Super FireWork", ModuleCategory.MOVEMENT);
        setup(modeSetting, speedSetting);
    }


    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onFirework(FireworkEvent e) {
        if (modeSetting.isSelected("Grim")) {
            int ff = TurnsConnection.INSTANCE.getRotation().getYaw() > 0F ? 45 : -45;
            double acceleration = Math.abs((TurnsConnection.INSTANCE.getRotation().getYaw() + ff) % 90 - ff) / 45, boost = 1 + (0.3 * acceleration * acceleration);
            boolean yAcceleration = Math.abs(TurnsConnection.INSTANCE.getMoveRotation().getPitch()) > 60;
            Vec3d vec3d = e.getVector();
            e.setVector(new Vec3d(vec3d.x * boost, yAcceleration ? vec3d.y * boost : vec3d.y, vec3d.z * boost));
        } else if (modeSetting.isSelected("Custom")) {
            int ff = TurnsConnection.INSTANCE.getRotation().getYaw() > 0F ? 45 : -45;
            double acceleration = Math.abs((TurnsConnection.INSTANCE.getRotation().getYaw() + ff) % 90 - ff) / 45;
            double rotationBoost = 1 + (0.3 * acceleration * acceleration);
            boolean yAcceleration = Math.abs(TurnsConnection.INSTANCE.getMoveRotation().getPitch()) > 60;

            Vec3d direction = TurnsConnection.INSTANCE.getMoveRotation().toVector();
            float speed = speedSetting.getValue() / 20f;

            double finalSpeed = speed * rotationBoost;
            e.setVector(new Vec3d(
                    direction.x * finalSpeed,
                    yAcceleration ? direction.y * finalSpeed : direction.y * speed,
                    direction.z * finalSpeed
            ));
        } else if (modeSetting.isSelected("BravoHvH")) {
            int ff = TurnsConnection.INSTANCE.getRotation().getYaw() > 0F ? 45 : -45;
            double yaw = TurnsConnection.INSTANCE.getRotation().getYaw();

            double accelerationNear45 = Math.abs((yaw + ff) % 90 - ff) / 45;

            double strongMultiplier = 0.26;
            double weakMultiplier = strongMultiplier / 2.2;
            double strongBoost = strongMultiplier * accelerationNear45 * accelerationNear45;

            double yawNormalized = Math.abs(yaw % 90) / 90.0;
            double weakBoost = weakMultiplier * yawNormalized * yawNormalized;

            double boost = 0.95 + strongBoost + weakBoost;

            boolean yAcceleration = Math.abs(TurnsConnection.INSTANCE.getMoveRotation().getPitch()) > 60;
            Vec3d vec3d = e.getVector();
            e.setVector(new Vec3d(vec3d.x * boost, yAcceleration ? vec3d.y * boost : vec3d.y, vec3d.z * boost));
        }
    }
}
