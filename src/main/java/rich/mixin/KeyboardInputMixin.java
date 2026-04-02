package rich.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import rich.events.api.EventManager;
import rich.events.impl.InputEvent;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.impl.combat.aura.AngleConstructor;

import static rich.IMinecraft.mc;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {

    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/util/PlayerInput;"))
    private PlayerInput tickHook(PlayerInput original) {
        InputEvent event = new InputEvent(original);
        EventManager.callEvent(event);
        return transformInput(event.getInput());
    }

    @Unique
    private PlayerInput transformInput(PlayerInput input) {
        AngleConnection rotationController = AngleConnection.INSTANCE;
        Angle angle = rotationController.getCurrentAngle();
        AngleConstructor configurable = rotationController.getCurrentRotationPlan();

        if (mc.player == null || angle == null || configurable == null ||
                !(configurable.isMoveCorrection() && configurable.isFreeCorrection())) {
            return input;
        }

        float deltaYaw = mc.player.getYaw() - angle.getYaw();
        float z = KeyboardInput.getMovementMultiplier(input.forward(), input.backward());
        float x = KeyboardInput.getMovementMultiplier(input.left(), input.right());
        float newX = x * MathHelper.cos(deltaYaw * 0.017453292f) - z * MathHelper.sin(deltaYaw * 0.017453292f);
        float newZ = z * MathHelper.cos(deltaYaw * 0.017453292f) + x * MathHelper.sin(deltaYaw * 0.017453292f);
        int movementSideways = Math.round(newX);
        int movementForward = Math.round(newZ);

        return new PlayerInput(
                movementForward > 0F,
                movementForward < 0F,
                movementSideways > 0F,
                movementSideways < 0F,
                input.jump(),
                input.sneak(),
                input.sprint());
    }
}