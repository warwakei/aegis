package fun.aegis.utils.interactions.simulate;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import net.minecraft.world.World;
import fun.aegis.utils.display.interfaces.QuickImports;

public class ArrowSimulation implements QuickImports {
    public final ClientWorld world;
    public Vec3d pos;
    public Vec3d velocity;
    public final boolean collideEntities;
    public boolean inGround = false;

    public ArrowSimulation(ClientWorld world, Vec3d pos, Vec3d velocity) {
        this(world, pos, velocity, true);
    }

    public ArrowSimulation(ClientWorld world, Vec3d pos, Vec3d velocity, boolean collideEntities) {
        this.world = world;
        this.pos = pos;
        this.velocity = velocity;
        this.collideEntities = collideEntities;
    }

    public HitResult tick() {
        if (this.inGround) {
            return null;
        }

        Vec3d newPos = pos.add(velocity);

        double drag = isTouchingWater() ? 0.6 : 0.99;

        velocity = velocity.multiply(drag);

        velocity = new Vec3d(velocity.x, velocity.y - 0.05000000074505806, velocity.z);

        HitResult hitResult = updateCollision(pos, newPos);
        if (hitResult != null) {
            this.pos = hitResult.getPos();
            this.inGround = true;
            return hitResult;
        }

        pos = newPos;

        return null;
    }

    private HitResult updateCollision(Vec3d pos, Vec3d newPos) {
        World world = this.world;

        ArrowEntity arrowEntity = new ArrowEntity(this.world, this.pos.x, this.pos.y, this.pos.z, new ItemStack(Items.ARROW), null);

        HitResult blockHitResult = world.raycast(new RaycastContext(
                pos,
                newPos,
                RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE,
                arrowEntity
        ));

        if (this.collideEntities) {
            double size = 0.45;

            HitResult entityHitResult = ProjectileUtil.getEntityCollision(
                    this.world,
                    arrowEntity,
                    pos,
                    newPos,
                    new Box(-size, -size, -size, size, size, size).offset(pos).stretch(newPos.subtract(pos)).expand(1.0),
                    entity -> {
                        if (!entity.isSpectator() && entity.isAlive() && (entity.canHit() || entity != mc.player && entity == arrowEntity)) {
                            return !arrowEntity.isConnectedThroughVehicle(entity);
                        }
                        return false;
                    }
            );

            if (entityHitResult != null && entityHitResult.getType() != HitResult.Type.MISS) {
                return entityHitResult;
            }
        }

        if (blockHitResult != null && blockHitResult.getType() != HitResult.Type.MISS) {
            return blockHitResult;
        }

        return null;
    }

    private boolean isTouchingWater() {
        return false;
    }
}
