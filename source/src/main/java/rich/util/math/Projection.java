package rich.util.math;

import lombok.experimental.UtilityClass;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.Frustum;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4d;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4d;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import rich.IMinecraft;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.MathAngle;
import rich.util.render.Render3D;

@UtilityClass
public class Projection implements IMinecraft {

    private static final double NEAR_PLANE = 0.05;

    public Vec3d worldSpaceToScreenSpace(Vec3d pos) {
        Camera camera = mc.getEntityRenderDispatcher().camera;
        if (camera == null) return Vec3d.ZERO;

        int displayHeight = mc.getWindow().getHeight();
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        Vector3f target = new Vector3f();

        double deltaX = pos.x - camera.getCameraPos().x;
        double deltaY = pos.y - camera.getCameraPos().y;
        double deltaZ = pos.z - camera.getCameraPos().z;

        Vector4f transformedCoordinates = new Vector4f((float) deltaX, (float) deltaY, (float) deltaZ, 1.0F);
        transformedCoordinates.mul(Render3D.lastWorldSpaceMatrix);

        Matrix4f matrixProj = new Matrix4f(Render3D.lastProjMat);
        Matrix4f matrixModel = new Matrix4f(Render3D.lastModMat);

        matrixProj.mul(matrixModel).project(transformedCoordinates.x(), transformedCoordinates.y(), transformedCoordinates.z(), viewport, target);

        return new Vec3d(
                target.x / mc.getWindow().getScaleFactor(),
                (displayHeight - target.y) / mc.getWindow().getScaleFactor(),
                target.z
        );
    }

    private Matrix4d toMatrix4d(Matrix4f mat) {
        Matrix4d result = new Matrix4d();
        result.m00(mat.m00()).m01(mat.m01()).m02(mat.m02()).m03(mat.m03());
        result.m10(mat.m10()).m11(mat.m11()).m12(mat.m12()).m13(mat.m13());
        result.m20(mat.m20()).m21(mat.m21()).m22(mat.m22()).m23(mat.m23());
        result.m30(mat.m30()).m31(mat.m31()).m32(mat.m32()).m33(mat.m33());
        return result;
    }

    private double getViewZ(Vec3d pos, Vec3d cameraPos) {
        double deltaX = pos.x - cameraPos.x;
        double deltaY = pos.y - cameraPos.y;
        double deltaZ = pos.z - cameraPos.z;

        Matrix4d worldSpace = toMatrix4d(Render3D.lastWorldSpaceMatrix);
        Vector4d view = new Vector4d(deltaX, deltaY, deltaZ, 1.0);
        worldSpace.transform(view);

        return -view.z;
    }

    private ClipResult worldSpaceToClipSpaceDouble(Vec3d pos, Vec3d cameraPos) {
        double deltaX = pos.x - cameraPos.x;
        double deltaY = pos.y - cameraPos.y;
        double deltaZ = pos.z - cameraPos.z;

        Matrix4d worldSpace = toMatrix4d(Render3D.lastWorldSpaceMatrix);
        Vector4d view = new Vector4d(deltaX, deltaY, deltaZ, 1.0);
        worldSpace.transform(view);

        Matrix4d proj = toMatrix4d(Render3D.lastProjMat);
        Matrix4d model = toMatrix4d(Render3D.lastModMat);
        Matrix4d combined = new Matrix4d(proj).mul(model);

        double clipX = combined.m00() * view.x + combined.m10() * view.y + combined.m20() * view.z + combined.m30() * view.w;
        double clipY = combined.m01() * view.x + combined.m11() * view.y + combined.m21() * view.z + combined.m31() * view.w;
        double clipZ = combined.m02() * view.x + combined.m12() * view.y + combined.m22() * view.z + combined.m32() * view.w;
        double clipW = combined.m03() * view.x + combined.m13() * view.y + combined.m23() * view.z + combined.m33() * view.w;

        return new ClipResult(clipX, clipY, clipZ, clipW, -view.z);
    }

    private Vec3d clipToScreenDouble(ClipResult clip, int[] viewport, int displayHeight, double scale) {
        double w = clip.w;
        if (Math.abs(w) < 1e-14) {
            return new Vec3d(viewport[2] / scale / 2.0, displayHeight / scale / 2.0, 0);
        }

        double invW = 1.0 / w;
        double ndcX = clip.x * invW;
        double ndcY = clip.y * invW;
        double ndcZ = clip.z * invW;

        double screenX = (ndcX * 0.5 + 0.5) * viewport[2] / scale;
        double screenY = (displayHeight - (ndcY * 0.5 + 0.5) * viewport[3]) / scale;
        double screenZ = ndcZ * 0.5 + 0.5;

        return new Vec3d(screenX, screenY, w > 0 ? screenZ : -1);
    }

    public static double getDistanceToGround() {
        if (mc.player == null || mc.world == null) return 256;
        for (double y = mc.player.getY(); y > 0; y -= 0.1) {
            if (!mc.world.getBlockState(mc.player.getBlockPos().down((int) ((mc.player.getY() - y) + 1))).isAir()) {
                return mc.player.getY() - y;
            }
        }
        return 256;
    }

    public Vec3d interpolate(Entity entity) {
        float tickDelta = mc.getRenderTickCounter().getTickProgress(true);
        return entity.getLerpedPos(tickDelta);
    }

    public Vec3d interpolate(Entity entity, float tickDelta) {
        return entity.getLerpedPos(tickDelta);
    }

    public Vector4d getVector4D(Entity ent) {
        return getVector4D(ent, mc.getRenderTickCounter().getTickProgress(true));
    }

    public Vector4d getVector4D(Entity ent, float tickDelta) {
        if (ent == null) return null;
        Camera camera = mc.getEntityRenderDispatcher().camera;
        if (camera == null) return null;

        Vec3d cameraPos = camera.getCameraPos();
        Vec3d interp = ent.getLerpedPos(tickDelta);
        Vec3d entityPos = ent.getEntityPos();
        Box box = ent.getBoundingBox().offset(interp.subtract(entityPos));

        Vec3d boxCenter = box.getCenter();

        double centerViewZ = getViewZ(boxCenter, cameraPos);
        if (centerViewZ < -5.0) {
            return null;
        }

        int displayHeight = mc.getWindow().getHeight();
        int[] viewport = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, viewport);
        double scale = mc.getWindow().getScaleFactor();

        double screenW = mc.getWindow().getScaledWidth();
        double screenH = mc.getWindow().getScaledHeight();

        Vec3d[] corners = new Vec3d[]{
                new Vec3d(box.minX, box.minY, box.minZ),
                new Vec3d(box.minX, box.minY, box.maxZ),
                new Vec3d(box.maxX, box.minY, box.minZ),
                new Vec3d(box.maxX, box.minY, box.maxZ),
                new Vec3d(box.minX, box.maxY, box.minZ),
                new Vec3d(box.minX, box.maxY, box.maxZ),
                new Vec3d(box.maxX, box.maxY, box.minZ),
                new Vec3d(box.maxX, box.maxY, box.maxZ)
        };

        int[][] edges = {
                {0, 1}, {0, 2}, {1, 3}, {2, 3},
                {4, 5}, {4, 6}, {5, 7}, {6, 7},
                {0, 4}, {1, 5}, {2, 6}, {3, 7}
        };

        ClipResult[] clipResults = new ClipResult[8];
        for (int i = 0; i < 8; i++) {
            clipResults[i] = worldSpaceToClipSpaceDouble(corners[i], cameraPos);
            if (clipResults[i] == null) return null;
        }

        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double maxX = -Double.MAX_VALUE;
        double maxY = -Double.MAX_VALUE;

        int visibleCount = 0;

        for (int i = 0; i < 8; i++) {
            ClipResult clip = clipResults[i];
            if (clip.viewZ > NEAR_PLANE) {
                visibleCount++;
                Vec3d screen = clipToScreenDouble(clip, viewport, displayHeight, scale);
                double px = clampScreenX(screen.x, screenW);
                double py = clampScreenY(screen.y, screenH);

                minX = Math.min(minX, px);
                minY = Math.min(minY, py);
                maxX = Math.max(maxX, px);
                maxY = Math.max(maxY, py);
            }
        }

        for (int[] edge : edges) {
            ClipResult c0 = clipResults[edge[0]];
            ClipResult c1 = clipResults[edge[1]];

            boolean v0 = c0.viewZ > NEAR_PLANE;
            boolean v1 = c1.viewZ > NEAR_PLANE;

            if (v0 != v1) {
                double denom = c1.viewZ - c0.viewZ;
                if (Math.abs(denom) < 1e-14) continue;

                double t = (NEAR_PLANE - c0.viewZ) / denom;
                t = Math.max(0.0, Math.min(1.0, t));

                ClipResult clipped = new ClipResult(
                        c0.x + t * (c1.x - c0.x),
                        c0.y + t * (c1.y - c0.y),
                        c0.z + t * (c1.z - c0.z),
                        c0.w + t * (c1.w - c0.w),
                        NEAR_PLANE
                );

                Vec3d screen = clipToScreenDouble(clipped, viewport, displayHeight, scale);
                double px = clampScreenX(screen.x, screenW);
                double py = clampScreenY(screen.y, screenH);

                minX = Math.min(minX, px);
                minY = Math.min(minY, py);
                maxX = Math.max(maxX, px);
                maxY = Math.max(maxY, py);
            }
        }

        if (visibleCount == 0 && minX == Double.MAX_VALUE) {
            return null;
        }

        if (maxX <= minX || maxY <= minY) return null;

        minX = Math.max(-screenW, minX);
        minY = Math.max(-screenH, minY);
        maxX = Math.min(screenW * 2, maxX);
        maxY = Math.min(screenH * 2, maxY);

        return new Vector4d(minX, minY, maxX, maxY);
    }

    private double clampScreenX(double x, double screenW) {
        return Math.max(-screenW * 2, Math.min(screenW * 3, x));
    }

    private double clampScreenY(double y, double screenH) {
        return Math.max(-screenH * 2, Math.min(screenH * 3, y));
    }

    private boolean isPointInFrontDouble(Vec3d point, Vec3d cameraPos, Camera camera) {
        double toPointX = point.x - cameraPos.x;
        double toPointY = point.y - cameraPos.y;
        double toPointZ = point.z - cameraPos.z;

        double yaw = camera.getYaw();
        double pitch = camera.getPitch();

        double yawRad = Math.toRadians(yaw);
        double pitchRad = Math.toRadians(pitch);

        double cosPitch = Math.cos(pitchRad);
        double lookX = -Math.sin(yawRad) * cosPitch;
        double lookY = -Math.sin(pitchRad);
        double lookZ = Math.cos(yawRad) * cosPitch;

        double dot = lookX * toPointX + lookY * toPointY + lookZ * toPointZ;

        return dot > -10.0;
    }

    public boolean canSee(Vec3d vec3d) {
        Camera camera = mc.gameRenderer.getCamera();
        if (camera == null) return false;
        Angle angle = MathAngle.fromVec3d(vec3d.subtract(camera.getCameraPos()));
        return (Math.abs(MathHelper.wrapDegrees(angle.getYaw() - camera.getYaw())) < 90 &&
                Math.abs(MathHelper.wrapDegrees(angle.getPitch() - camera.getPitch())) < 60) ||
                canSee(new Box(BlockPos.ofFloored(vec3d)));
    }

    public boolean canSee(Box box) {
        if (box == null || mc.gameRenderer == null) return false;

        Camera camera = mc.gameRenderer.getCamera();
        if (camera == null || !camera.isReady()) return false;

        Vec3d cameraPos = camera.getCameraPos();

        Matrix4f viewMatrix = new Matrix4f().rotation(camera.getRotation());
        Matrix4f projectionMatrix = mc.gameRenderer.getBasicProjectionMatrix(mc.options.getFov().getValue().floatValue());

        Frustum frustum = new Frustum(viewMatrix, projectionMatrix);
        frustum.setPosition(cameraPos.x, cameraPos.y, cameraPos.z);

        return frustum.isVisible(box);
    }

    public boolean cantSee(Vector4d vec) {
        if (vec == null) return true;

        double screenWidth = mc.getWindow().getScaledWidth();
        double screenHeight = mc.getWindow().getScaledHeight();

        if (Double.isNaN(vec.x) || Double.isNaN(vec.y) || Double.isNaN(vec.z) || Double.isNaN(vec.w)) return true;
        if (Double.isInfinite(vec.x) || Double.isInfinite(vec.y) || Double.isInfinite(vec.z) || Double.isInfinite(vec.w)) return true;

        if (vec.z < -screenWidth || vec.x > screenWidth * 2) return true;
        if (vec.w < -screenHeight || vec.y > screenHeight * 2) return true;

        return false;
    }

    public double centerX(Vector4d vec) {
        return vec.x + (vec.z - vec.x) / 2;
    }

    public boolean isInFrontOfCamera(Vec3d worldPos) {
        Camera camera = mc.gameRenderer.getCamera();
        if (camera == null || !camera.isReady()) return false;

        return isPointInFrontDouble(worldPos, camera.getCameraPos(), camera);
    }

    private record ClipResult(double x, double y, double z, double w, double viewZ) {}
}