package fun.aegis.utils.features.aura.rotations.impl;

import fun.aegis.features.impl.combat.Aura;
import fun.aegis.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.aegis.utils.features.aura.utils.MathAngle;
import fun.aegis.utils.features.aura.warp.Turns;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

public class HAngleV2X extends RotateConstructor {
     private final SecureRandom random = new SecureRandom();
     private final double PHI = 1.618033988749895;

     private float noiseOffset = 0;
     private final float[] p = new float[512];

     private Turns lastTargetRot;
     private final List<Vec3d> pathPoints = new ArrayList<>();
     private double staticChaosX = 0.5;

     public HAngleV2X() {
          super("HvH V2X");
          initPerlin();
     }

     private void initPerlin() {
          for (int i = 0; i < 256; i++)
               p[i] = p[256 + i] = random.nextFloat();
     }

     @Override
     public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
          if (!(entity instanceof LivingEntity target))
               return targetAngle;

          Vec3d bestPoint = scanHitbox(target);
          targetAngle = MathAngle.calculateAngle(bestPoint);

          Turns smoothedAngle = applyBezierSmoothing(currentAngle, targetAngle);

          float noise = getNoise(noiseOffset += 0.05f * PHI);
          smoothedAngle.setYaw(smoothedAngle.getYaw() + noise * 0.15f);
          smoothedAngle.setPitch(MathHelper.clamp(smoothedAngle.getPitch() + noise * 0.12f, -90F, 90F));

          return validateRotations(currentAngle, smoothedAngle.adjustSensitivity());
     }

     private Vec3d scanHitbox(LivingEntity target) {
          Box box = target.getBoundingBox();
          
          double p = 0.15;
          double minX = box.minX + (box.maxX - box.minX) * p;
          double maxX = box.maxX - (box.maxX - box.minX) * p;
          double minY = box.minY + (box.maxY - box.minY) * p;
          double maxY = box.maxY - (box.maxY - box.minY) * p;
          double minZ = box.minZ + (box.maxZ - box.minZ) * p;
          double maxZ = box.maxZ - (box.maxZ - box.minZ) * p;

          Vec3d[] points = {
                    new Vec3d(target.getX(), minY + (maxY - minY) * 0.85, target.getZ()),
                    new Vec3d(target.getX(), minY + (maxY - minY) * 0.5, target.getZ()),
                    new Vec3d(minX, minY + (maxY - minY) * 0.6, target.getZ()),
                    new Vec3d(maxX, minY + (maxY - minY) * 0.6, target.getZ()),
                    target.getPos().add(0, target.getHeight() * 0.75, 0)
          };

          for (Vec3d point : points) {
               if (isVisible(point))
                    return point;
          }
          return points[1];
     }

     private boolean isVisible(Vec3d end) {
          if (mc.player == null || mc.world == null) return false;
          Vec3d start = mc.player.getEyePos();
          return mc.world.raycast(new RaycastContext(start, end, RaycastContext.ShapeType.COLLIDER,
                    RaycastContext.FluidHandling.NONE, mc.player)).getType() == HitResult.Type.MISS;
     }

     private Turns applyBezierSmoothing(Turns current, Turns target) {
          if (lastTargetRot == null)
               lastTargetRot = current;

          float diff = (float) Math.hypot(MathHelper.wrapDegrees(target.getYaw() - current.getYaw()),
                    target.getPitch() - current.getPitch());

          float t = MathHelper.clamp(0.38f + (diff / 180f) * 0.32f, 0, 1);

          double r = 3.9 + (System.currentTimeMillis() % 10 / 100.0);
          staticChaosX = r * staticChaosX * (1 - staticChaosX);
          if (Double.isNaN(staticChaosX) || Double.isInfinite(staticChaosX)) {
               staticChaosX = 0.5;
          }
          float chaosMod = (float) (0.85 + staticChaosX * 0.3);
          t = MathHelper.clamp(t * chaosMod, 0.1f, 0.95f);

          float yawDelta = MathHelper.wrapDegrees(target.getYaw() - current.getYaw());
          float wrappedTargetYaw = current.getYaw() + yawDelta;

          float jitterStrength = (float) (2.0f + Math.sin(System.currentTimeMillis() / 200.0) * PHI);
          float controlYaw = current.getYaw() + yawDelta * 0.5f + (getNoise(noiseOffset) * jitterStrength);

          float yaw = bezier(current.getYaw(), controlYaw, wrappedTargetYaw, t);
          float pitch = bezier(current.getPitch(),
                    current.getPitch() + (target.getPitch() - current.getPitch()) * 0.45f,
                    target.getPitch(), t);

          lastTargetRot = target;
          return new Turns(yaw, pitch);
     }

     private float bezier(float p0, float p1, float p2, float t) {
          return (1 - t) * (1 - t) * p0 + 2 * (1 - t) * t * p1 + t * t * p2;
     }

     private float getNoise(float x) {
          int X = (int) Math.floor(x) & 255;
          x -= Math.floor(x);
          float u = x * x * x * (x * (x * 6 - 15) + 10);
          int nextIdx = (X + 1) & 255;
          return MathHelper.lerp(u, p[X], p[nextIdx]);
     }

     private Turns validateRotations(Turns current, Turns next) {
          float yawDelta = Math.abs(MathHelper.wrapDegrees(next.getYaw() - current.getYaw()));

          float maxStep = 58.0f;

          Aura aura = Aura.getInstance();
          if (aura != null && aura.getTarget() != null && mc.player != null && mc.player.distanceTo(aura.getTarget()) < 1.2) {
               maxStep = 42.0f;
          }

          if (yawDelta > maxStep) {
               float correction = maxStep / yawDelta;
               next.setYaw(current.getYaw() + (MathHelper.wrapDegrees(next.getYaw() - current.getYaw()) * correction));
          }

          return next.adjustSensitivity();
     }

     @Override
     public Vec3d randomValue() {
          return new Vec3d(0, 0, 0);
     }
}
