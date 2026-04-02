package fun.aegis.utils.math.calc;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

import static fun.aegis.utils.display.interfaces.QuickImports.mc;

public class CalcVector {

    public static Vec3d lerpPosition(Entity entity) {
        float tickDelta = mc.getRenderTickCounter().getTickDelta(true);
        return new Vec3d(
                entity.prevX + (entity.getX() - entity.prevX) * tickDelta,
                entity.prevY + (entity.getY() - entity.prevY) * tickDelta,
                entity.prevZ + (entity.getZ() - entity.prevZ) * tickDelta
        );
    }

}
