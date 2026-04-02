package rich.modules.impl.combat.macetarget.flight;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.MathAngle;
import rich.modules.impl.combat.macetarget.prediction.TargetPredictor;
import rich.modules.impl.combat.macetarget.state.MaceState.Stage;

@Getter
public class FlightController {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    private final TargetPredictor predictor;
    private boolean predictionEnabled = false;
    private float height = 30.0f;
    private boolean smartPathEnabled = false;

    public FlightController(TargetPredictor predictor) {
        this.predictor = predictor;
    }

    public void setPredictionEnabled(boolean enabled) {
        this.predictionEnabled = enabled;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public void setSmartPathEnabled(boolean enabled) {
        this.smartPathEnabled = enabled;
    }

    public Vec3d getTargetPosition(LivingEntity target, Stage stage) {
        if (target == null) {
            return Vec3d.ZERO;
        }

        if (predictionEnabled && predictor.isMoving()) {
            return predictor.getPredictedPosition(target, stage);
        }

        return target.getEyePos();
    }

    /**
     * Проверяет есть ли блок в указанной позиции
     */
    private boolean hasBlockAt(Vec3d pos) {
        if (mc.world == null) return false;
        BlockPos blockPos = BlockPos.ofFloored(pos);
        var blockState = mc.world.getBlockState(blockPos);
        return !blockState.isAir() && blockState.getCollisionShape(mc.world, blockPos).isEmpty() == false;
    }

    /**
     * Ищет безопасную Y позицию для атаки, обходя препятствия сверху
     */
    private Vec3d findSmartPathPosition(LivingEntity target, Stage stage) {
        Vec3d basePos = getTargetPosition(target, stage);
        
        if (!smartPathEnabled) {
            return basePos;
        }

        switch (stage) {
            case FLYING_UP -> {
                // Проверяем есть ли блок над целевой позицией
                Vec3d flyTarget = basePos.add(0, height, 0);
                
                // Сканируем сверху вниз в поисках свободного места
                for (int offset = 0; offset <= 10; offset++) {
                    Vec3d checkPos = basePos.add(0, height - offset, 0);
                    if (!hasBlockAt(checkPos)) {
                        // Проверяем что есть место для игрока (2 блока высоты)
                        boolean hasSpace = true;
                        for (int h = 0; h < 2; h++) {
                            if (hasBlockAt(checkPos.add(0, h, 0))) {
                                hasSpace = false;
                                break;
                            }
                        }
                        if (hasSpace) {
                            return basePos.add(0, height - offset, 0);
                        }
                    }
                }
                
                // Если не нашли место сверху, пробуем обойти сбоку
                for (int radius = 1; radius <= 5; radius++) {
                    for (int x = -radius; x <= radius; x++) {
                        for (int z = -radius; z <= radius; z++) {
                            if (x == 0 && z == 0) continue;
                            Vec3d sidePos = basePos.add(x, height, z);
                            if (!hasBlockAt(sidePos)) {
                                boolean hasSpace = true;
                                for (int h = 0; h < 2; h++) {
                                    if (hasBlockAt(sidePos.add(0, h, 0))) {
                                        hasSpace = false;
                                        break;
                                    }
                                }
                                if (hasSpace) {
                                    return basePos.add(x, height, z);
                                }
                            }
                        }
                    }
                }
                
                return flyTarget;
            }
            case TARGETTING, ATTACKING -> {
                // При атаке проверяем нет ли блока между нами и целью
                if (mc.world != null && mc.player != null) {
                    Vec3d start = mc.player.getEyePos();
                    Vec3d end = basePos;
                    
                    // Raycast для проверки препятствий
                    var hitResult = mc.world.raycast(
                        new net.minecraft.world.RaycastContext(
                            start, end,
                            net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                            net.minecraft.world.RaycastContext.FluidHandling.NONE,
                            mc.player
                        )
                    );
                    
                    // Если попали в блок до цели - смещаемся вверх
                    if (hitResult.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
                        return basePos.add(0, 1, 0);
                    }
                }
                return basePos;
            }
            default -> {
                return basePos;
            }
        }
    }

    public Angle calculateAngle(LivingEntity target, Stage stage) {
        if (target == null || mc.player == null) {
            return MathAngle.cameraAngle();
        }

        Vec3d targetPos = findSmartPathPosition(target, stage);

        switch (stage) {
            case FLYING_UP -> {
                return MathAngle.fromVec3d(targetPos.subtract(mc.player.getEyePos()));
            }
            case TARGETTING, ATTACKING -> {
                return MathAngle.fromVec3d(targetPos.subtract(mc.player.getEyePos()));
            }
            default -> {
                return MathAngle.cameraAngle();
            }
        }
    }
}