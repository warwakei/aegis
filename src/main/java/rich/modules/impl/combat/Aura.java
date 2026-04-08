package rich.modules.impl.combat;

import antidaunleak.api.annotation.Native;
import lombok.Getter;
import lombok.experimental.NonFinal;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import rich.Initialization;
import rich.events.api.EventHandler;
import rich.events.api.types.EventType;
import rich.events.impl.InputEvent;
import rich.events.impl.RotationUpdateEvent;
import rich.events.impl.TickEvent;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.AngleConfig;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.impl.combat.aura.MathAngle;
import rich.modules.impl.combat.aura.attack.StrikeManager;
import rich.modules.impl.combat.aura.attack.StrikerConstructor;
import rich.modules.impl.combat.aura.impl.*;
import rich.modules.impl.combat.aura.impl.RotateConstructor;
import rich.modules.impl.combat.aura.mace.SilentMaceHandler;
import rich.modules.impl.combat.aura.rotations.*;
import rich.modules.impl.combat.aura.target.MultiPoint;
import rich.modules.impl.combat.aura.target.TargetFinder;
import rich.modules.impl.movement.ElytraTarget;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.*;
import rich.util.Instance;
import rich.util.FpsThrottler;
import rich.util.math.TaskPriority;

import java.util.Objects;

public class Aura extends ModuleStructure {

    @Native(type = Native.Type.VMProtectBeginUltra)
    public static Aura getInstance() {
        return Instance.get(Aura.class);
    }

    @Getter
    public final SelectSetting mode = new SelectSetting("Режим наводки", "Select aim mode")
            .value("Matrix", "FunTime Snap", "Snap", "SpookyTime", "Jenro")
            .selected("Matrix");

    private final SelectSetting moveFix = new SelectSetting("Коррекция движения", "Select move fix mode")
            .value("Сфокусированная", "Свободная", "Преследование", "Таргет", "Отключена")
            .selected("Focus");

    @Getter
    public final SliderSettings attackrange = new SliderSettings("Дистанция удара", "Set range value")
            .range(2.0f, 6.0f)
            .setValue(3.0f);

    private final SliderSettings lookrange = new SliderSettings("Дистанция поиска", "Set look range value")
            .range(0.0f, 10.0f)
            .setValue(1.5f);

    public final MultiSelectSetting options = new MultiSelectSetting("Настройки", "Select settings")
            .value("Бить сквозь стены", "Рандомизация крита", "Не бить если ешь", "Рандомизация высоты")
            .selected("Бить сквозь стены", "Рандомизация крита", "Не бить если ешь");

    private final MultiSelectSetting targetType = new MultiSelectSetting("Настройка целей", "Select target settings")
            .value("Игроки", "Мобы", "Животные", "Друзья", "Стойки для брони", "BW Тиммейты", "Креатив", "Инвизы", "Голые инвизы")
            .selected("Игроки", "Мобы", "Животные");

    @Getter
    private final SelectSetting resetSprintMode = new SelectSetting("Сброс спринта", "Reset sprint mode")
            .value("Легитный", "Пакетный")
            .selected("Легитный");

    @Getter
    private final BooleanSetting checkCrit = new BooleanSetting("Только криты", "Only critical hits")
            .setValue(true);

    // Настройки 1.8 режима (объявляем перед smartCrits чтобы избежать forward reference)
    @Getter
    private final BooleanSetting mode1_8 = new BooleanSetting("1.8 Режим", "Режим атаки как в версии 1.8 (CPS)")
            .setValue(false);

    @Getter
    private final BooleanSetting smartCrits = new BooleanSetting("Умные криты",
            "Smart crits - attack on ground when possible")
            .setValue(true)
            .visible(() -> checkCrit.isValue() && !mode1_8.isValue());

    @Getter
    private final SliderSettings cpsSetting = new SliderSettings("CPS", "Кликов в секунду (2-40). Каждые 5 CPS дают +1 двойной клик в очередь")
            .range(2, 40)
            .setValue(8)
            .visible(() -> mode1_8.isValue());

    @Getter
    private final BooleanSetting silentMace = new BooleanSetting("Silent Mace", "Авто-свап булавы при падении с 9+ блоков")
            .setValue(false);

    @Getter
    private final SelectSetting elytraRotationMode = new SelectSetting("Ротация для ElytraTarget", "Режим ротации при использовании ElytraTarget (Snap ротации не работают)")
            .value("Matrix", "SpookyTime", "Jenro")
            .selected("Matrix");

    @Getter
    private final SliderSettings heightRandom = new SliderSettings("Рандомизация высоты", "Смещение точки прицеливания по высоте (±значение)")
            .range(0.0f, 0.5f)
            .setValue(0.2f)
            .visible(() -> options.isSelected("Рандомизация высоты"));

    @Getter
    private final BooleanSetting fakeRotation = new BooleanSetting("Фейковая ротация", "Дёргать камеру в сторону от врага перед ударом (~130мс)")
            .setValue(false);

    @Getter
    private final SliderSettings fakeRotationAmount = new SliderSettings("Сила фейка", "Насколько сильно дёргать камеру")
            .range(1.0f, 15.0f)
            .setValue(5.0f)
            .visible(() -> fakeRotation.isValue());

    private final SilentMaceHandler silentMaceHandler = new SilentMaceHandler();

    public Aura() {
        super("Aura", ModuleCategory.COMBAT);
        settings(mode, attackrange, lookrange, options, targetType, moveFix, resetSprintMode, checkCrit, smartCrits, mode1_8, cpsSetting, silentMace, elytraRotationMode, heightRandom, fakeRotation, fakeRotationAmount);
    }

    @NonFinal
    public static LivingEntity target;

    @NonFinal
    public LivingEntity lastTarget;

    TargetFinder targetSelector = new TargetFinder();
    MultiPoint pointFinder = new MultiPoint();

    @Override
    public void deactivate() {
        AngleConnection.INSTANCE.clear();
        AngleConnection.INSTANCE.reset();
        Initialization.getInstance().getManager()
                .getAttackPerpetrator()
                .getAttackHandler()
                .resetPendingState();
        target = null;
        lastTarget = null;
        FpsThrottler.reset();
        silentMaceHandler.forceReset();
    }

    @EventHandler
    private void tick(TickEvent event) {
        // Silent Mace handler
        if (silentMace.isValue()) {
            silentMaceHandler.onTick(target);
        }

        // Проверяем FPS throttler для разработчиков
        if (target != null) {
            FpsThrottler.updateTarget(target.getName().getString());
        } else {
            FpsThrottler.updateTarget(null);
        }
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        // Обновляем настройки 1.8 режима
        update1_8Settings();
        
        switch (e.getType()) {
            case EventType.PRE -> {
                LivingEntity previousTarget = target;
                target = updateTarget();

                if (previousTarget != null && target == null) {
                    Initialization.getInstance().getManager()
                            .getAttackPerpetrator()
                            .getAttackHandler()
                            .resetPendingState();
                }

                boolean passed = false;
                if (mode.isSelected("FunTime Snap") || mode.isSelected("HolyWorld")) {
                    passed = true;
                }
                if (target != null && passed && target.distanceTo(mc.player) <= attackrange.getValue() + 0.25F) {
                    rotateToTarget(getConfig());
                    lastTarget = target;
                }
                if (target != null && !passed) {
                    rotateToTarget(getConfig());
                    lastTarget = target;
                }
            }
            case EventType.POST -> {
                // Если Silent Mace активен и сейчас атакует булавой — пропускаем обычную атаку
                if (silentMace.isValue() && silentMaceHandler.isActive()) {
                    return;
                }
                if (target != null) {
                    Initialization.getInstance().getManager().getAttackPerpetrator().performAttack(getConfig());
                }
            }
        }
    }
    
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void update1_8Settings() {
        // Обновляем StrikeManager настройками 1.8 режима
        StrikeManager strikeManager = Initialization.getInstance().getManager()
                .getAttackPerpetrator()
                .getStrikeManager();

        if (strikeManager != null) {
            strikeManager.set1_8Mode(mode1_8.isValue());
            strikeManager.updateCPS(cpsSetting.getInt(), cpsSetting);
        }

        // Автоматически отключаем криты если включен 1.8 режим
        if (mode1_8.isValue() && checkCrit.isValue()) {
            checkCrit.setValue(false);
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    public StrikerConstructor.AttackPerpetratorConfigurable getConfig() {
        float baseRange = attackrange.getValue();

        Pair<Vec3d, Box> pointData = pointFinder.computeVector(
                target,
                baseRange,
                AngleConnection.INSTANCE.getRotation(),
                getSmoothMode().randomValue(),
                options.isSelected("Бить сквозь стены"));

        Vec3d computedPoint = pointData.getLeft();
        Box hitbox = pointData.getRight();

        if (mc.player.isGliding() && target.isGliding()) {
            Vec3d targetVelocity = target.getVelocity();
            double targetSpeed = targetVelocity.horizontalLength();

            float leadTicks = 0;
            if (ElytraTarget.shouldElytraTarget && ElytraTarget.getInstance() != null
                    && ElytraTarget.getInstance().isState()) {
                leadTicks = ElytraTarget.getInstance().elytraForward.getValue();
            }

            // INTERCEPT PREDICTION — летим НАПЕРЕХВАТ цели, не кружимся вокруг
            Vec3d ourPos = mc.player.getEntityPos();
            Vec3d targetPos = target.getEntityPos();
            Vec3d toTarget = targetPos.subtract(ourPos);
            double distanceToTarget = toTarget.horizontalLength();
            
            // Определяем что цель кружится вокруг нас
            boolean targetIsOrbiting = false;
            if (targetSpeed > 0.3 && distanceToTarget > 1.0) {
                Vec3d toTargetNorm = new Vec3d(toTarget.x, 0, toTarget.z).normalize();
                Vec3d velNorm = new Vec3d(targetVelocity.x, 0, targetVelocity.z).normalize();
                // Cross product — перпендикулярная скорость
                double crossProduct = toTargetNorm.x * velNorm.z - toTargetNorm.z * velNorm.x;
                double radialSpeed = Math.abs(toTargetNorm.dotProduct(velNorm));
                double tangentialSpeed = Math.abs(crossProduct);
                // Если тангенциальная скорость > радиальной — цель кружится
                targetIsOrbiting = tangentialSpeed > radialSpeed * 1.5;
            }

            Vec3d predictedPos;
            if (targetIsOrbiting) {
                // Цель КРУЖИТСЯ — летим НАПЕРЕХВАТ по прямой (по хорде круга)
                // Не кружимся вместе с ней, а режем путь
                double ourSpeed = mc.player.getVelocity().horizontalLength();
                if (ourSpeed < 0.5) ourSpeed = 1.5;
                
                // Время до перехвата
                double timeToIntercept = distanceToTarget / Math.max(ourSpeed + targetSpeed, 0.5);
                timeToIntercept = Math.min(timeToIntercept, leadTicks);
                
                // Предсказываем где цель будет и летим ТУДА напрямую
                predictedPos = targetPos.add(targetVelocity.multiply(timeToIntercept));
                
                // Добавляем компенсацию направления — предсказываем ЧУТЬ дальше по ходу цели
                // чтобы не лететь в точку где она УЖЕ была
                double leadMultiplier = 1.0 + (targetSpeed * 0.8);
                predictedPos = targetPos.add(targetVelocity.multiply(leadTicks * leadMultiplier));
            } else {
                // Обычное линейное предсказание
                double leadMultiplier = 1.0 + (targetSpeed * 0.5);
                predictedPos = targetPos.add(targetVelocity.multiply(leadTicks * leadMultiplier));
            }

            computedPoint = predictedPos.add(0, target.getHeight() / 2, 0);

            // Расширенный хитбокс для более стабильных попаданий
            double expandAmount = 0.35;
            hitbox = new Box(
                    predictedPos.x - target.getWidth() / 2 - expandAmount,
                    predictedPos.y - expandAmount,
                    predictedPos.z - target.getWidth() / 2 - expandAmount,
                    predictedPos.x + target.getWidth() / 2 + expandAmount,
                    predictedPos.y + target.getHeight() + expandAmount,
                    predictedPos.z + target.getWidth() / 2 + expandAmount);
        }

        // Рандомизация высоты удара (±0.1-0.3)
        if (options.isSelected("Рандомизация высоты") && heightRandom.getValue() > 0) {
            double randomOffset = (Math.random() - 0.5) * 2 * heightRandom.getValue();
            computedPoint = new Vec3d(computedPoint.x, computedPoint.y + randomOffset, computedPoint.z);
        }

        Angle angle = MathAngle.fromVec3d(computedPoint.subtract(Objects.requireNonNull(mc.player).getEyePos()));
        return new StrikerConstructor.AttackPerpetratorConfigurable(
                target,
                angle,
                baseRange,
                options.getSelected(),
                mode,
                hitbox);
    }

    public AngleConfig getRotationConfig() {
        boolean visibleCorrection = !moveFix.isSelected("Отключена");
        boolean freeCorrection = moveFix.isSelected("Свободная");
        return new AngleConfig(getSmoothMode(), visibleCorrection, freeCorrection);
    }

    public AngleConfig getElytraRotationConfig() {
        boolean visibleCorrection = !moveFix.isSelected("Отключена");
        boolean freeCorrection = moveFix.isSelected("Свободная");
        
        RotateConstructor elytraMode = getElytraRotateConstructor();
        
        return new AngleConfig(elytraMode, visibleCorrection, freeCorrection);
    }

    private rich.modules.impl.combat.aura.impl.RotateConstructor getElytraRotateConstructor() {
        return switch (elytraRotationMode.getSelected()) {
            case "Matrix" -> new MatrixAngle();
            case "SpookyTime" -> new SPAngle();
            default -> new MatrixAngle();
        };
    }

    private void rotateToTarget(StrikerConstructor.AttackPerpetratorConfigurable config) {
        StrikeManager attackHandler = Initialization.getInstance().getManager().getAttackPerpetrator()
                .getAttackHandler();
        AngleConnection controller = AngleConnection.INSTANCE;
        Angle.VecRotation rotation = new Angle.VecRotation(config.getAngle(), config.getAngle().toVector());

        boolean elytraMode = mc.player.isGliding() && ElytraTarget.getInstance() != null
                && ElytraTarget.getInstance().isState();
        
        AngleConfig rotationConfig = elytraMode ? getElytraRotationConfig() : getRotationConfig();

        switch (mode.getSelected()) {

            case "FunTime Snap" -> {
                if (attackHandler.canAttack(config, 5)) {
                    controller.clear();
                    controller.rotateTo(rotation, target, 60, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
                }
            }

            case "Snap" -> {
                if (attackHandler.canAttack(config, 0)) {
                    controller.rotateTo(rotation, target, 0, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
                }
            }

            case "Matrix", "SpookyTime" -> {
                controller.rotateTo(rotation, target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
            }

        }

        if (elytraMode) {
            controller.rotateTo(rotation, target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
        }
    }

    @EventHandler
    public void onInput(InputEvent event) {
        if (mc.player == null || mc.world == null)
            return;

        PlayerInput input = event.getInput();
        if (input == null)
            return;

        if (!isState())
            return;

        if (target == null || !target.isAlive())
            return;

        boolean w = mc.options.forwardKey.isPressed();
        boolean s = mc.options.backKey.isPressed();
        boolean a = mc.options.leftKey.isPressed();
        boolean d = mc.options.rightKey.isPressed();

        if (moveFix.isSelected("Таргет")) {
            Vec3d playerPos = mc.player.getEntityPos();
            Vec3d targetPos = target.getEntityPos();

            Vec3d moveTarget = new Vec3d(targetPos.x, playerPos.y, targetPos.z);
            Vec3d dir = moveTarget.subtract(playerPos).normalize();

            float yaw = AngleConnection.INSTANCE.getRotation().getYaw();
            float moveAngle = (float) Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90F;
            float angleDiff = MathHelper.wrapDegrees(moveAngle - yaw);

            boolean forward = false, back = false, left = false, right = false;

            if (angleDiff >= -22.5 && angleDiff < 22.5) {
                forward = true;
            } else if (angleDiff >= 22.5 && angleDiff < 67.5) {
                forward = true;
                right = true;
            } else if (angleDiff >= 67.5 && angleDiff < 112.5) {
                right = true;
            } else if (angleDiff >= 112.5 && angleDiff < 157.5) {
                back = true;
                right = true;
            } else if (angleDiff >= -67.5 && angleDiff < -22.5) {
                forward = true;
                left = true;
            } else if (angleDiff >= -112.5 && angleDiff < -67.5) {
                left = true;
            } else if (angleDiff >= -157.5 && angleDiff < -112.5) {
                back = true;
                left = true;
            } else {
                back = true;
            }

            event.setDirectionalLow(forward, back, left, right);
            return;
        }

        if (moveFix.isSelected("Преследование")) {
            if (!w && !s && !a && !d)
                return;

            Vec3d playerPos = mc.player.getEntityPos();
            Box targetBox = target.getBoundingBox();
            Vec3d center = targetBox.getCenter();

            float targetYaw = target.getYaw();
            double rad = Math.toRadians(targetYaw);

            Vec3d forwardDir = new Vec3d(-Math.sin(rad), 0, Math.cos(rad)).normalize();
            Vec3d rightDir = new Vec3d(-forwardDir.z, 0, forwardDir.x).normalize();
            Vec3d leftDir = rightDir.multiply(-1);

            double halfWidth = target.getWidth() / 2.0;
            double offset = halfWidth + 0.1;

            Vec3d moveTargetVec = center;
            Vec3d offsetVec = Vec3d.ZERO;

            if (w)
                offsetVec = offsetVec.add(forwardDir);
            if (s)
                offsetVec = offsetVec.add(forwardDir.multiply(-1.0));
            if (a)
                offsetVec = offsetVec.add(leftDir);
            if (d)
                offsetVec = offsetVec.add(rightDir);

            if (offsetVec.lengthSquared() > 0) {
                offsetVec = offsetVec.normalize().multiply(offset);
                moveTargetVec = center.add(offsetVec);
            }

            moveTargetVec = new Vec3d(moveTargetVec.x, playerPos.y, moveTargetVec.z);
            Vec3d dir = moveTargetVec.subtract(playerPos).normalize();

            float yaw = AngleConnection.INSTANCE.getRotation().getYaw();
            float moveAngle = (float) Math.toDegrees(Math.atan2(dir.z, dir.x)) - 90F;
            float angleDiff = MathHelper.wrapDegrees(moveAngle - yaw);

            boolean forward = false, back = false, left = false, right = false;

            if (angleDiff >= -22.5 && angleDiff < 22.5) {
                forward = true;
            } else if (angleDiff >= 22.5 && angleDiff < 67.5) {
                forward = true;
                right = true;
            } else if (angleDiff >= 67.5 && angleDiff < 112.5) {
                right = true;
            } else if (angleDiff >= 112.5 && angleDiff < 157.5) {
                back = true;
                right = true;
            } else if (angleDiff >= -67.5 && angleDiff < -22.5) {
                forward = true;
                left = true;
            } else if (angleDiff >= -112.5 && angleDiff < -67.5) {
                left = true;
            } else if (angleDiff >= -157.5 && angleDiff < -112.5) {
                back = true;
                left = true;
            } else {
                back = true;
            }

            event.setDirectionalLow(forward, back, left, right);
        }
    }

    private LivingEntity updateTarget() {
        TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(targetType.getSelected());
        float range = attackrange.getValue() + 0.25F
                + (mc.player.isGliding() && ElytraTarget.getInstance() != null && ElytraTarget.getInstance().isState()
                        ? ElytraTarget.getInstance().elytraFindRange.getValue()
                        : lookrange.getValue());

        float dynamicFov = 360;

        targetSelector.searchTargets(mc.world.getEntities(), range, dynamicFov,
                options.isSelected("Бить сквозь стены"));
        targetSelector.validateTarget(filter::isValid);
        return targetSelector.getCurrentTarget();
    }

    public RotateConstructor getSmoothMode() {
        boolean elytraMode = mc.player.isGliding() && ElytraTarget.getInstance() != null && ElytraTarget.getInstance().isState();
        
        if (elytraMode) {
            return getElytraRotateConstructor();
        }
        
        return switch (mode.getSelected()) {
            case "FunTime Snap" -> new FTAngle();
            case "SpookyTime" -> new SPAngle();
            case "Jenro" -> new JenroAngle();
            case "Snap" -> new SnapAngle();
            case "Matrix" -> new MatrixAngle();
            default -> new LinearConstructor();
        };
    }
}