package fun.aegis.mixins.player.input;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import fun.aegis.utils.features.aura.warp.Turns;
import net.minecraft.client.input.KeyboardInput;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import fun.aegis.utils.client.managers.event.EventManager;
import fun.aegis.utils.interactions.inv.InventoryFlowManager;
import fun.aegis.events.player.InputEvent;
import fun.aegis.utils.features.aura.warp.TurnsConstructor;
import fun.aegis.utils.features.aura.warp.TurnsConnection;

@Mixin(KeyboardInput.class)
public class KeyboardInputMixin extends InputMixin {

    @ModifyExpressionValue(method = "tick", at = @At(value = "NEW", target = "(ZZZZZZZ)Lnet/minecraft/util/PlayerInput;"))
    private PlayerInput tickHook(PlayerInput original) {
        InputEvent event = new InputEvent(original);
        EventManager.callEvent(event);
        InventoryFlowManager.input(event);
        return transformInput(event.getInput());
    }

    @Unique
    private PlayerInput transformInput(PlayerInput input) {
        TurnsConnection rotationController = TurnsConnection.INSTANCE;
        Turns angle = rotationController.getCurrentAngle();
        TurnsConstructor configurable = rotationController.getCurrentRotationPlan();

        if (mc.player == null || angle == null || configurable == null || !(configurable.isMoveCorrection() && configurable.isFreeCorrection())) {
            return input;
        }

        float deltaYaw = mc.player.getYaw() - angle.getYaw();
        float z = KeyboardInput.getMovementMultiplier(input.forward(), input.backward());
        float x = KeyboardInput.getMovementMultiplier(input.left(), input.right());
        float newX = x * MathHelper.cos(deltaYaw * 0.017453292f) - z * MathHelper.sin(deltaYaw * 0.017453292f);
        float newZ = z * MathHelper.cos(deltaYaw * 0.017453292f) + x * MathHelper.sin(deltaYaw * 0.017453292f);
        int movementSideways = Math.round(newX), movementForward = Math.round(newZ);

        return new PlayerInput(movementForward > 0F, movementForward < 0F, movementSideways > 0F, movementSideways < 0F, input.jump(), input.sneak(), input.sprint());
    }
}
