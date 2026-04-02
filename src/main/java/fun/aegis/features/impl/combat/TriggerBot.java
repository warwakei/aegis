package fun.aegis.features.impl.combat;

import antidaunleak.api.annotation.Native;
import fun.aegis.events.player.RotationUpdateEvent;
import fun.aegis.events.player.TickEvent;
import fun.aegis.events.packet.PacketEvent;
import fun.aegis.events.render.WorldRenderEvent;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.*;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.client.managers.event.types.EventType;
import fun.aegis.utils.features.aura.striking.StrikeManager;
import fun.aegis.utils.features.aura.striking.StrikerConstructor;
import fun.aegis.utils.features.aura.target.TargetFinder;
import fun.aegis.utils.features.aura.warp.TurnsConnection;
import fun.aegis.utils.features.aura.point.MultiPoint;
import fun.aegis.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.aegis.utils.features.aura.rotations.constructor.LinearConstructor;
import fun.aegis.utils.features.aura.utils.MathAngle;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import fun.aegis.Aegis;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import java.util.Objects;

public class TriggerBot extends Module {

    private static final float RANGE_MARGIN = 0.253F;
    private final TargetFinder targetSelector = new TargetFinder();
    private final MultiPoint pointFinder = new MultiPoint();
    public LivingEntity target;

    public SliderSettings attackRange = new SliderSettings("Дистанция удара", "Дальность атаки до цели")
            .setValue(3).range(1F, 6F);

    MultiSelectSetting targetType = new MultiSelectSetting("Тип таргета", "Фильтрует список целей по типу")
            .value("Players", "Mobs", "Animals", "Friends", "Armor Stand")
            .selected("Players", "Mobs", "Animals");

    public MultiSelectSetting attackSetting = new MultiSelectSetting("Настройки", "Параметры атаки")
            .value("Only Critical", "Break Shield", "UnPress Shield", "No Attack When Eat", "Ignore The Walls", "Hit Chance")
            .selected("Only Critical", "Break Shield");

    public SliderSettings hitChance = new SliderSettings("Шанс удара в %", "Шанс удара по цели")
            .setValue(100).range(1F, 100F).visible(() -> attackSetting.isSelected("Hit Chance"));

    public SelectSetting sprintReset = new SelectSetting("Сброс спринта", "Выбор сброса спринта перед ударом")
            .value("Legit", "Packet").selected("Legit");

    public BooleanSetting smartCrits = new BooleanSetting("Удары на земле", "Криты только при нажатии пробела")
            .setValue(true).visible(() -> attackSetting.isSelected("Only Critical"));

    public TriggerBot() {
        super("TriggerBot", "Trigger Bot", ModuleCategory.COMBAT);
        setup(attackRange, targetType, attackSetting, hitChance, sprintReset, smartCrits);
    }
    public static TriggerBot getInstance() {
        return Instance.get(TriggerBot.class);
    }
    private LivingEntity updateTarget() {
        if (mc.world == null) return null;
        
        TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(targetType.getSelected());
        float range = attackRange.getValue() + RANGE_MARGIN;
        targetSelector.searchTargets(mc.world.getEntities(), range, 360, attackSetting.isSelected("Ignore The Walls"));
        targetSelector.validateTarget(filter::isValid);
        return targetSelector.getCurrentTarget();
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (PlayerInteractionHelper.nullCheck()) return;
        switch (e.getType()) {
            case EventType.PRE -> target = updateTarget();
            case EventType.POST -> {
                if (target != null && target.isAlive()) {
                    Aegis aegis = Aegis.getInstance();
                    if (aegis != null && aegis.getAttackPerpetrator() != null) {
                        StrikerConstructor.AttackPerpetratorConfigurable config = getConfig();
                        if (config != null) {
                            aegis.getAttackPerpetrator().performAttack(config);
                        }
                    }
                }
            }
        }
    }

    public StrikerConstructor.AttackPerpetratorConfigurable getConfig() {
        if (target == null || mc.player == null) return null;
        
        float baseRange = attackRange.getValue() + RANGE_MARGIN;
        Pair<Vec3d, Box> pointData = pointFinder.computeVector(
                target,
                baseRange,
                TurnsConnection.INSTANCE.getRotation(),
                getSmoothMode().randomValue(),
                attackSetting.isSelected("Ignore The Walls")
        );

        if (pointData == null) return null;
        
        Vec3d computedPoint = pointData.getLeft();
        Box hitbox = pointData.getRight();
        
        if (computedPoint == null || hitbox == null) return null;
        
        var angle = MathAngle.fromVec3d(computedPoint.subtract(Objects.requireNonNull(mc.player).getEyePos()));

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

    @EventHandler
    public void tick(TickEvent e) {}

    @EventHandler
    public void onPacket(PacketEvent e) {}

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {}
}
