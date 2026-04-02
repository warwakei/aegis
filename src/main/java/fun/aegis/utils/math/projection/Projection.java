package fun.aegis.utils.math.projection;

import fun.aegis.utils.features.aura.warp.Turns;
import fun.aegis.utils.math.calc.Calculate;
import lombok.experimental.UtilityClass;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.NotNull;
import org.joml.*;
import org.lwjgl.opengl.GL11;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.display.geometry.Render3D;
import fun.aegis.utils.features.aura.utils.MathAngle;

import java.lang.Math;

@UtilityClass
public class Projection implements QuickImports {

    public @NotNull Vec3d worldSpaceToScreenSpace(Vec3d pos) {
        Vector3f delta = pos.toVector3f();
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        Vector3f target = new Vector3f();

        Vector4f transformedCoordinates = new Vector4f(delta.x, delta.y, delta.z, 1.f).mul(Render3D.lastWorldSpaceMatrix.getPositionMatrix());
        Matrix4f matrixProj = new Matrix4f(Render3D.lastProjMat);
        matrixProj.project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);

        return new Vec3d(target.x / mc.getWindow().getScaleFactor(), (mc.getWindow().getHeight() - target.y) / mc.getWindow().getScaleFactor(), target.z);
    }

    public static double getDistanceToGround() {
        for (double y = mc.player.getY(); y > 0; y -= 0.1) {
            if (!mc.world.getBlockState(mc.player.getBlockPos().down((int) ((mc.player.getY() - y) + 1))).isAir()) {
                return mc.player.getY() - y;
            }
        }
        return 256;
    }

    public Vector4d getVector4D(Entity ent) {
        Vector4d position = null;
        if (ent != null) {
            for (Vec3d vector : getVec3ds(ent, Calculate.interpolate(ent))) {
                vector = worldSpaceToScreenSpace(new Vec3d(vector.x, vector.y, vector.z));
                if (vector.z > 0 && vector.z < 1) {
                    if (position == null) position = new Vector4d(vector.x, vector.y, vector.z, 0);
                    position.x = Math.min(vector.x, position.x);
                    position.y = Math.min(vector.y, position.y);
                    position.z = Math.max(vector.x, position.z);
                    position.w = Math.max(vector.y, position.w);
                }
            }
        }
        return position;
    }

    public @NotNull Vec3d[] getVec3ds(Entity ent, Vec3d pos) {
        Box axisAlignedBB2 = ent.getBoundingBox();
        Box axisAlignedBB = new Box(axisAlignedBB2.minX - ent.getX() + pos.x - 0.1F, axisAlignedBB2.minY - ent.getY() + pos.y - 0.1F, axisAlignedBB2.minZ - ent.getZ() + pos.z - 0.1F, axisAlignedBB2.maxX - ent.getX() + pos.x + 0.1F, axisAlignedBB2.maxY - ent.getY() + pos.y + 0.1F, axisAlignedBB2.maxZ - ent.getZ() + pos.z + 0.1F);
        return new Vec3d[]{new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.minZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.minX, axisAlignedBB.maxY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.minY, axisAlignedBB.maxZ), new Vec3d(axisAlignedBB.maxX, axisAlignedBB.maxY, axisAlignedBB.maxZ)};
    }

    public boolean canSee(Vec3d vec3d) {
        Camera camera = mc.getEntityRenderDispatcher().camera;
        Turns angle = MathAngle.fromVec3d(vec3d.subtract(camera.getPos()));
        return (Math.abs(MathHelper.wrapDegrees(angle.getYaw() - camera.getYaw())) < 90 && Math.abs(MathHelper.wrapDegrees(angle.getPitch() - camera.getPitch())) < 60) || canSee(new Box(BlockPos.ofFloored(vec3d)));
    }

    public boolean canSee(Box box) {
        Frustum frustum = mc.worldRenderer.frustum;
        return box != null && frustum != null && frustum.isVisible(box);
    }

    public boolean cantSee(Vector4d vec) {
       return vec == null || (vec.x < 0 && vec.z < 1) || (vec.y < 0 && vec.w < 1);
    }

    public double centerX(Vector4d vec) {
        return vec.x + (vec.z - vec.x) / 2;
    }
}
