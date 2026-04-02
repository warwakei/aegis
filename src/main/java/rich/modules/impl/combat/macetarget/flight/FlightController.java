package rich.modules.impl.combat.macetarget.flight;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
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

    public FlightController(TargetPredictor predictor) {
        this.predictor = predictor;
    }

    public void setPredictionEnabled(boolean enabled) {
        this.predictionEnabled = enabled;
    }

    public void setHeight(float height) {
        this.height = height;
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

    public Angle calculateAngle(LivingEntity target, Stage stage) {
        if (target == null || mc.player == null) {
            return MathAngle.cameraAngle();
        }

        Vec3d targetPos = getTargetPosition(target, stage);

        switch (stage) {
            case FLYING_UP -> {
                Vec3d flyTarget = targetPos.add(0, height, 0);
                return MathAngle.fromVec3d(flyTarget.subtract(mc.player.getEyePos()));
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