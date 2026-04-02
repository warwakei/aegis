package fun.aegis.features.impl.movement;

import fun.aegis.events.player.TickEvent;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.interactions.simulate.Simulations;
import fun.aegis.utils.math.time.StopWatch;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.Vec3d;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Jesus extends Module {

    private final SelectSetting mode = new SelectSetting("Режим", "Выберите режим передвижения по воде")
            .value("Matrix")
            .selected("Matrix");

    private final StopWatch timer = new StopWatch();

    @NonFinal
    private boolean isMoving;

    private final float melonBallSpeed = 0.47F;

    public Jesus() {
        super("Jesus", ModuleCategory.MOVEMENT);
        setup(mode);
    }

    @Override
    public void deactivate() {
        if (mode.isSelected("Matrix")) {

        }
    }

    @EventHandler
    public void tick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (!mode.isSelected("Matrix")) return;

        if (mc.player.isTouchingWater() || mc.player.isInLava()) {
            StatusEffectInstance speedEffect = mc.player.getStatusEffect(StatusEffects.SPEED);
            StatusEffectInstance slowEffect = mc.player.getStatusEffect(StatusEffects.SLOWNESS);
            ItemStack offHandItem = mc.player.getOffHandStack();

            String itemName = offHandItem.getName().getString();
            float appliedSpeed = 0F;

            if (itemName.contains("Ломтик Дыни") && speedEffect != null && speedEffect.getAmplifier() == 2) {
                appliedSpeed = 0.4283F * 1.15F;
            } else {
                if (speedEffect != null) {
                    if (speedEffect.getAmplifier() == 2) {
                        appliedSpeed = melonBallSpeed * 1.15F;
                    } else if (speedEffect.getAmplifier() == 1) {
                        appliedSpeed = melonBallSpeed;
                    }
                } else {
                    appliedSpeed = melonBallSpeed * 0.68F;
                }
            }

            if (slowEffect != null) {
                appliedSpeed *= 0.85f;
            }

            isMoving = mc.options.forwardKey.isPressed()
                    || mc.options.backKey.isPressed()
                    || mc.options.leftKey.isPressed()
                    || mc.options.rightKey.isPressed();

            if (isMoving) {
                Vec3d velocity = mc.player.getVelocity();
                mc.player.setVelocity(
                        velocity.x,
                        velocity.y,
                        velocity.z
                );
                Simulations.setVelocity(appliedSpeed);
            } else {
                mc.player.setVelocity(0.0, mc.player.getVelocity().y, 0.0);
            }

            double yMotion = mc.options.jumpKey.isPressed() ? 0.019 : 0.003;
            mc.player.setVelocity(mc.player.getVelocity().x, yMotion, mc.player.getVelocity().z);
        }
    }
}
