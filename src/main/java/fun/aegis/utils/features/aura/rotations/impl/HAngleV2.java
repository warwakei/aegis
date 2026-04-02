package fun.aegis.utils.features.aura.rotations.impl;

import fun.aegis.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.aegis.utils.features.aura.utils.MathAngle;
import fun.aegis.utils.features.aura.warp.Turns;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.security.SecureRandom;

public class HAngleV2 extends RotateConstructor {
     private final SecureRandom secureRandom = new SecureRandom();
     private final double phi = 1.618033988749895;

     public HAngleV2() {
          super("HvH V2");
     }

     @Override
     public Turns limitAngleChange(Turns currentAngle, Turns targetAngle, Vec3d vec3d, Entity entity) {
          if (entity instanceof net.minecraft.entity.LivingEntity target) {
               Vec3d targetPos = target.getPos().add(0, target.getHeight() * 0.75, 0);
               targetAngle = MathAngle.calculateAngle(targetPos);
          }

          Turns angleDelta = MathAngle.calculateDelta(currentAngle, targetAngle);
          float yawDelta = angleDelta.getYaw();
          float pitchDelta = angleDelta.getPitch();

          // Динамическая скорость вращения на основе Золотого Сечения
          double seed = (System.currentTimeMillis() % 2000) / 2000.0;
          float chaoticSpeedYaw = (float) ((seed * phi) % 1.0 * 30.0);
          float chaoticSpeedPitch = (float) ((seed * (phi * phi)) % 1.0 * 20.0);

          float speedYaw = 35.0f + chaoticSpeedYaw;
          float speedPitch = 20.0f + chaoticSpeedPitch;

          float moveYaw = MathHelper.clamp(yawDelta, -speedYaw, speedYaw);
          float movePitch = MathHelper.clamp(pitchDelta, -speedPitch, speedPitch);

          float finalYaw = currentAngle.getYaw() + moveYaw;
          float finalPitch = MathHelper.clamp(currentAngle.getPitch() + movePitch, -90F, 90F);

          // Апериодический джиттер (Золотое Сечение)
          float rotationDifference = (float) Math.hypot(yawDelta, pitchDelta);
          if (rotationDifference > 0.4f) {
               double time = (System.currentTimeMillis() % 10000) / 1000.0;
               double jitterX = Math.sin(time * phi) * 0.15;
               double jitterY = Math.cos(time * (phi * phi)) * 0.15;

               finalYaw += (float) jitterX;
               finalPitch = MathHelper.clamp(finalPitch + (float) jitterY, -90F, 90F);
          }

          Turns result = new Turns(finalYaw, finalPitch);
          return result.adjustSensitivity();
     }

     @Override
     public Vec3d randomValue() {
          return new Vec3d(0, 0, 0);
     }
}
