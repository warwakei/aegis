package rich.modules.impl.combat;

import antidaunleak.api.annotation.Native;
import lombok.Getter;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import rich.Initialization;
import rich.events.api.EventHandler;
import rich.events.api.types.EventType;
import rich.events.impl.PacketEvent;
import rich.events.impl.RotationUpdateEvent;
import rich.events.impl.TickEvent;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.impl.combat.aura.MathAngle;
import rich.modules.impl.combat.aura.attack.StrikerConstructor;
import rich.modules.impl.combat.aura.impl.LinearConstructor;
import rich.modules.impl.combat.aura.impl.RotateConstructor;
import rich.modules.impl.combat.aura.target.MultiPoint;
import rich.modules.impl.combat.aura.target.TargetFinder;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.MultiSelectSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.Instance;
import rich.util.string.PlayerInteractionHelper;

import java.util.Objects;

public class TriggerBot extends ModuleStructure {
    private static final float RANGE_MARGIN = 0.253F;
    private final TargetFinder targetSelector = new TargetFinder();
    private final MultiPoint pointFinder = new MultiPoint();
    public LivingEntity target;

    public SliderSettings attackRange = new SliderSettings("Дистанция удара", "Дальность атаки до цели")
            .setValue(3).range(1F, 6F);

    MultiSelectSetting targetType = new MultiSelectSetting("Тип таргета", "Фильтрует список целей по типу")
            .value("Игроки", "Мобы", "Животные", "Стойки для брони")
            .selected("Игроки", "Мобы", "Животные");

    public MultiSelectSetting attackSetting = new MultiSelectSetting("Настройки", "Параметры атаки")
            .value("Только криты", "Рандомизация крита", "Бить сквозь стены")
            .selected("Только криты");

    @Getter
    public SelectSetting sprintReset = new SelectSetting("Сброс спринта", "Выбор сброса спринта перед ударом")
            .value("Легитный", "Интенсивный")
            .selected("Легитный");

    @Getter
    public BooleanSetting smartCrits = new BooleanSetting("Умные криты", "Атака на земле когда кулдаун готов")
            .setValue(true)
            .visible(() -> attackSetting.isSelected("Только криты"));

    public TriggerBot() {
        super("TriggerBot", "Trigger Bot", ModuleCategory.COMBAT);
        settings(attackRange, targetType, attackSetting, sprintReset, smartCrits);
    }

    public static TriggerBot getInstance() {
        return Instance.get(TriggerBot.class);
    }

    @Override
    public void deactivate() {
        target = null;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private LivingEntity updateTarget() {
        TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(targetType.getSelected());
        float range = attackRange.getValue() + RANGE_MARGIN;
        targetSelector.searchTargets(mc.world.getEntities(), range, 360, attackSetting.isSelected("Бить сквозь стены"));
        targetSelector.validateTarget(filter::isValid);
        return targetSelector.getCurrentTarget();
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (PlayerInteractionHelper.nullCheck()) return;

        switch (e.getType()) {
            case EventType.PRE -> target = updateTarget();
            case EventType.POST -> {
                if (target != null) {
                    Initialization.getInstance().getManager().getAttackPerpetrator().performTriggerAttack(getConfig(), this);
                }
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    public StrikerConstructor.AttackPerpetratorConfigurable getConfig() {
        float baseRange = attackRange.getValue() + RANGE_MARGIN;

        Pair<Vec3d, Box> pointData = pointFinder.computeVector(
                target,
                baseRange,
                AngleConnection.INSTANCE.getRotation(),
                getSmoothMode().randomValue(),
                attackSetting.isSelected("Бить сквозь стены")
        );

        Vec3d computedPoint = pointData.getLeft();
        Box hitbox = pointData.getRight();

        Angle angle = MathAngle.fromVec3d(computedPoint.subtract(Objects.requireNonNull(mc.player).getEyePos()));

        return new StrikerConstructor.AttackPerpetratorConfigurable(
                target,
                angle,
                baseRange,
                attackSetting.getSelected(),
                null,
                hitbox
        );
    }

    public RotateConstructor getSmoothMode() {
        return new LinearConstructor();
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    public boolean isResetSprintLegit() {
        return sprintReset.isSelected("Легитный");
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    public boolean isResetSprintPacket() {
        return sprintReset.isSelected("Интенсивный");
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    public boolean isOnlyCrits() {
        return attackSetting.isSelected("Только криты");
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    public boolean isRandomizeCrit() {
        return attackSetting.isSelected("Рандомизация крита");
    }

    @EventHandler
    public void tick(TickEvent e) {}

    @EventHandler
    public void onPacket(PacketEvent e) {}
}