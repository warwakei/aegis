package fun.aegis.events.player;

import net.minecraft.util.math.Vec3d;
import fun.aegis.utils.client.managers.event.events.callables.EventCancellable;

public class PlayerTravelEvent extends EventCancellable {
    private Vec3d motion;
    private final boolean pre;

    public PlayerTravelEvent(Vec3d motion, boolean pre) {
        this.motion = motion;
        this.pre = pre;
    }

    public Vec3d getMotion() {
        return motion;
    }

    public void setMotion(Vec3d motion) {
        this.motion = motion;
    }

    public boolean isPre() {
        return pre;
    }
}
