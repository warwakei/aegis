package rich.modules.impl.misc;

import antidaunleak.api.annotation.Native;
import rich.events.api.EventHandler;
import rich.events.impl.RotationUpdateEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.math.TaskPriority;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.AngleConfig;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.impl.combat.aura.impl.LinearConstructor;

public class Instant180 extends ModuleStructure {

    private final SliderSettings rotationSpeed = new SliderSettings("Скорость", "Скорость поворота (градусов за тик)")
            .range(1.0f, 180.0f)
            .setValue(180.0f);

    private final BooleanSetting silent = new BooleanSetting("Тихо", "Не показывать поворот другим игрокам")
            .setValue(false);

    private float startYaw = 0;
    private float targetYaw = 0;
    private boolean rotating = false;
    private int rotationTicks = 0;

    public Instant180() {
        super("Instant180", "Instant 180", ModuleCategory.MISC);
        settings(rotationSpeed, silent);
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        if (mc.player == null) {
            setState(false);
            return;
        }

        startYaw = mc.player.getYaw();
        targetYaw = startYaw + 180.0f;

        // Нормализуем угол (-180 до 180)
        targetYaw = normalizeAngle(targetYaw);

        rotating = true;
        rotationTicks = 0;
    }

    @Override
    public void deactivate() {
        rotating = false;
        AngleConnection.INSTANCE.startReturning();
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onRotationUpdate(RotationUpdateEvent event) {
        if (!rotating || mc.player == null) {
            return;
        }

        float speed = rotationSpeed.getValue();
        float currentYaw = mc.player.getYaw();

        // Вычисляем кратчайший путь поворота
        float delta = targetYaw - currentYaw;
        delta = normalizeAngle(delta);

        if (Math.abs(delta) <= speed) {
            // Достигли цели
            rotateTo(targetYaw);
            rotating = false;
            setState(false);
            return;
        }

        // Поворачиваем на заданный угол
        float newYaw = currentYaw + Math.signum(delta) * speed;
        rotateTo(newYaw);
        rotationTicks++;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void rotateTo(float yaw) {
        Angle angle = new Angle(yaw, mc.player.getPitch());
        Angle.VecRotation rotation = new Angle.VecRotation(angle, angle.toVector());
        AngleConfig config = new AngleConfig(new LinearConstructor(), true, silent.isValue());
        AngleConnection.INSTANCE.rotateTo(rotation, null, 1, config, TaskPriority.HIGH_IMPORTANCE_1, this);
    }

    /**
     * Нормализует угол в диапазон [-180, 180]
     */
    private float normalizeAngle(float angle) {
        angle = angle % 360;
        if (angle > 180) {
            angle -= 360;
        } else if (angle < -180) {
            angle += 360;
        }
        return angle;
    }
}
