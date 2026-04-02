package fun.aegis.utils.interactions.simulate.interfaces;

import net.minecraft.util.math.Vec3d;

public interface Simulation {
    Vec3d pos();

    void tick();
}
