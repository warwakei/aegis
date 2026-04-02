package fun.aegis.utils.features.aura.rotations.constructor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.features.aura.warp.Turns;

@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public abstract class RotateConstructor implements QuickImports {
    String name;

    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle) {
        return limitAngleChange(currentAngle, targetAngle, null, null);
    }

    public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d) {
        return limitAngleChange(currentAngle, targetAngle, vec3d, null);
    }

    public abstract Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity);

    public abstract Vec3d randomValue();
}
