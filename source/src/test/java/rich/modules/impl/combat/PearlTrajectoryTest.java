package rich.modules.impl.combat;

import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import org.junit.Test;

import java.util.function.BiFunction;

import static org.junit.Assert.*;

public class PearlTrajectoryTest {
    private static PearlTrajectory.Landing floor(Vec3d from, Vec3d to) {
        if (from.y < 0 || to.y > 0) return null;
        Vec3d contact = from.lerp(to, from.y / (from.y - to.y));
        return new PearlTrajectory.Landing(contact, BlockPos.ofFloored(contact.add(0, -0.01, 0)));
    }

    private static Vec3d launch(PearlTrajectory.Aim aim, Vec3d movement) {
        return Vec3d.fromPolar(aim.pitch(), aim.yaw()).multiply(1.5).add(movement);
    }

    @Test public void compensatesForDifferentOriginAndMovement() {
        var enemy = PearlTrajectory.predict(new Vec3d(0, 1.5, 0), new Vec3d(1.1, 0.4, 0.2), p -> false, PearlTrajectoryTest::floor);
        assertNotNull(enemy);
        var origin = new Vec3d(-4, 1.5, 4);
        var movement = new Vec3d(0.18, 0, 0.12);
        var aim = PearlTrajectory.solve(origin, movement, enemy, p -> false, PearlTrajectoryTest::floor);
        assertNotNull(aim);
        var own = PearlTrajectory.predict(origin, launch(aim, movement), p -> false, PearlTrajectoryTest::floor);
        assertNotNull(own);
        assertTrue(own.position().distanceTo(enemy.position()) < 0.9);
    }

    @Test public void usesHigherArcWhenLowThrowHitsWall() {
        var target = new PearlTrajectory.Landing(new Vec3d(20, 0, 0.5), new BlockPos(20, -1, 0));
        var origin = new Vec3d(0, 1.5, 0.5);
        BiFunction<Vec3d, Vec3d, PearlTrajectory.Landing> walls = (from, to) -> {
            if (from.x < 10 && to.x >= 10) {
                Vec3d contact = from.lerp(to, (10 - from.x) / (to.x - from.x));
                if (contact.y >= 0 && contact.y < 3) return new PearlTrajectory.Landing(contact, BlockPos.ofFloored(contact));
            }
            return floor(from, to);
        };
        var aim = PearlTrajectory.solve(origin, Vec3d.ZERO, target, p -> false, walls);
        assertNotNull(aim);
        assertTrue(aim.pitch() < -35);
        assertTrue(PearlTrajectory.predict(origin, launch(aim, Vec3d.ZERO), p -> false, walls)
                .position().distanceTo(target.position()) < 0.9);
    }

    @Test public void doesNotThrowIfTargetIsUnreachable() {
        var target = new PearlTrajectory.Landing(new Vec3d(100, 0, 0), new BlockPos(100, -1, 0));
        assertNull(PearlTrajectory.solve(new Vec3d(0, 1.5, 0), Vec3d.ZERO, target, p -> false, PearlTrajectoryTest::floor));
    }

    @Test public void waterDragShortensThePredictedThrow() {
        var origin = new Vec3d(0, 1.5, 0);
        var velocity = new Vec3d(1.5, 0.25, 0);
        var air = PearlTrajectory.predict(origin, velocity, p -> false, PearlTrajectoryTest::floor);
        var water = PearlTrajectory.predict(origin, velocity, p -> true, PearlTrajectoryTest::floor);
        assertNotNull(air);
        assertNotNull(water);
        assertTrue(water.position().x < air.position().x / 2);
    }
}
