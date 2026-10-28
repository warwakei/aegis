package rich.modules.impl.movement;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.util.math.Vec3d;
import rich.events.api.EventHandler;
import rich.events.impl.PacketEvent;
import rich.events.impl.TickEvent;
import rich.modules.impl.combat.Aura;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.Instance;
import rich.util.movement.MovementUtil;
import rich.util.timer.StopWatch;

import java.util.Random;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ElytraMotion extends ModuleStructure {

    public static ElytraMotion getInstance() {
        return Instance.get(ElytraMotion.class);
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void applySmoothVelocity(Vec3d targetVel) {
        if (smoothMotion.isValue()) {
            float smooth = (float) smoothingFactor.getValue();
            smoothedVelocity = MovementUtil.applySmoothVelocity(smoothedVelocity, targetVel, smooth);
            mc.player.setVelocity(smoothedVelocity);
        } else {
            mc.player.setVelocity(targetVel);
        }
    }

    StopWatch timer = new StopWatch();
    @NonFinal
    Vec3d targetPosition = null;
    @NonFinal
    Random random = new Random();
    @NonFinal double rotationAngle = 0.0;

    private final SliderSettings hoverHeight = new SliderSettings("Высота зависания", "Высота для зависания над целью")
            .setValue(2.5f).range(0.5f, 5.0f);

    private final SliderSettings approachSpeed = new SliderSettings("Скорость приближения", "Насколько быстро приближаться к цели")
            .setValue(0.3f).range(0.1f, 1.0f);

    private final BooleanSetting smoothMotion = new BooleanSetting("Плавное движение", "Плавная интерполяция движения")
            .setValue(true);

    private final SliderSettings smoothingFactor = new SliderSettings("Коэффициент сглаживания", "Фактор сглаживания движения")
            .setValue(0.3f).range(0.01f, 0.99f);

    private final BooleanSetting autoHover = new BooleanSetting("Авто зависание", "Зависать когда не атакуем")
            .setValue(true);

    @NonFinal
    private Vec3d smoothedVelocity = Vec3d.ZERO;

    public ElytraMotion() {
        super("ElytraMotion", "Elytra Motion", ModuleCategory.MOVEMENT);
        settings(hoverHeight, approachSpeed, smoothMotion, smoothingFactor, autoHover);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (!state || mc.player == null || mc.world == null || !mc.player.isGliding()) {
            smoothedVelocity = Vec3d.ZERO;
            return;
        }

        Aura aura = Instance.get(Aura.class);
        if (aura != null && aura.isState()) {
            handleAuraMotion(aura);
        } else if (autoHover.isValue()) {
            handleHoverMotion();
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleAuraMotion(Aura aura) {
        if (aura.target == null) return;

        double distance = mc.player.distanceTo(aura.target);
        double attackRange = aura.getAttackrange().getValue() - 1F;

        if (distance > attackRange) {
            Vec3d direction = aura.target.getEntityPos().subtract(mc.player.getEntityPos()).normalize();
            Vec3d targetVel = direction.multiply(approachSpeed.getValue() * 2);
            applySmoothVelocity(targetVel);
        } else if (distance < attackRange - 2) {
            Vec3d direction = mc.player.getEntityPos().subtract(aura.target.getEntityPos()).normalize();
            Vec3d targetVel = direction.multiply(approachSpeed.getValue());
            applySmoothVelocity(targetVel);
        } else {
            applySmoothVelocity(new Vec3d(0, 0.02, 0));
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleHoverMotion() {
        double targetY = mc.player.getY() + hoverHeight.getValue();
        if (mc.player.getY() < targetY) {
            applySmoothVelocity(new Vec3d(0, 0.05, 0));
        } else if (mc.player.getY() > targetY) {
            applySmoothVelocity(new Vec3d(0, -0.05, 0));
        } else {
            applySmoothVelocity(new Vec3d(0, 0, 0));
        }
    }



    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        super.deactivate();
        smoothedVelocity = Vec3d.ZERO;
    }
}