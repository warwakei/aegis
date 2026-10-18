package rich.modules.impl.combat.aura;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import rich.IMinecraft;
import rich.modules.impl.combat.aura.impl.RotateConstructor;

@Setter
@Getter
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AngleConstructor implements IMinecraft {
    Angle angle;
    Vec3d vec3d;
    Entity entity;
    RotateConstructor angleSmooth;
    int ticksUntilReset;
    float resetThreshold;
    public boolean moveCorrection;
    @Getter(AccessLevel.PUBLIC)
    public boolean freeCorrection;
    public boolean changeLook = false;


    public Angle nextRotation(Angle fromAngle, boolean isResetting) {
        if (isResetting) {
            return angleSmooth.limitAngleChange(fromAngle, MathAngle.fromVec2f(mc.player.getRotationClient()));
        }
        return angleSmooth.limitAngleChange(fromAngle, angle, vec3d, entity);
    }
}