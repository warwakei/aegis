package rich.modules.impl.combat.aura;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import rich.modules.impl.combat.aura.impl.LinearConstructor;
import rich.modules.impl.combat.aura.impl.RotateConstructor;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AngleConfig {
    public static AngleConfig DEFAULT = new AngleConfig(new LinearConstructor(), true, true);
    public static boolean moveCorrection, freeCorrection;
    RotateConstructor angleSmooth;
    int resetThreshold = 1;

    public AngleConfig(boolean moveCorrection, boolean freeCorrection) {
        this(new LinearConstructor(), moveCorrection, freeCorrection);
    }

    public AngleConfig(boolean moveCorrection) {
        this(new LinearConstructor(), moveCorrection, true);
    }

    public AngleConfig(RotateConstructor angleSmooth, boolean moveCorrection, boolean freeCorrection) {
        this.angleSmooth = angleSmooth;
        this.moveCorrection = moveCorrection;
        this.freeCorrection = freeCorrection;
    }

    public AngleConstructor createRotationPlan(Angle angle, Vec3d vec, Entity entity, int reset) {
        return new AngleConstructor(angle, vec, entity, angleSmooth, reset, resetThreshold, moveCorrection, freeCorrection);
    }

    public AngleConstructor createRotationPlan(Angle angle, Vec3d vec, Entity entity, boolean moveCorrection, boolean freeCorrection) {
        return new AngleConstructor(angle, vec, entity, angleSmooth, 1, resetThreshold, moveCorrection, freeCorrection);
    }
}