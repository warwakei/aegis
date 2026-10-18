package rich.modules.impl.combat;

import antidaunleak.api.annotation.Native;
import lombok.Getter;
import lombok.experimental.NonFinal;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
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
import rich.modules.impl.combat.aura.shield.ShieldBreakerHandler;
import rich.modules.impl.combat.aura.rotations.*;
import rich.modules.impl.combat.aura.target.MultiPoint;
import rich.modules.impl.combat.aura.target.TargetFinder;
import rich.modules.impl.movement.ElytraTarget;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.*;
import rich.util.Instance;
import rich.util.FpsThrottler;
import rich.util.player.PlayerSimulation;
import rich.util.math.TaskPriority;

import java.util.ArrayList;
import java.util.List;
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
            .value("Игроки", "Мобы", "Животные", "Друзья", "Стойки для брони", "BW Тиммейты", "Креатив", "Инвизы", "Голые инвизы", "Фаерболлы")
            .selected("Игроки", "Мобы", "Животные");

    @Getter
    private final SelectSetting resetSprintMode = new SelectSetting("Сброс спринта", "Reset sprint mode")
            .value("Легитный", "Пакетный")
            .selected("Легитный");

    @Getter
    private final BooleanSetting checkCrit = new BooleanSetting("Только криты", "Only critical hits")
            .setValue(true);

    // 1.8 настройки чтоб не крашило
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

    @Getter
    private final BooleanSetting autoFlyme = new BooleanSetting("AutoFlyme", "Двойной пробел в прыжке пропишет /flyme (для серверов Jenro)")
            .setValue(false);

    @Getter
    private final BooleanSetting moveFixInFly = new BooleanSetting("Коррекция в полёте", "Работает ли коррекция движения когда игрок в полёте (Fly / Elytra)")
            .setValue(false);

    @Getter
    private final BooleanSetting shieldBreaker = new BooleanSetting("ShieldBreaker", "Авто-свап топора при блокировании щитом цели (как Silent Mace)")
            .setValue(false);

    @Getter
    private final BooleanSetting multiTarget = new BooleanSetting("Multi-Target", "Автоматически переключается между целями после каждого удара")
            .setValue(false);

    /**
     * Глубина симуляции: на сколько тиков вперёд симулируем движение
     * (свой крит и предикт цели). Больше = умнее, но дороже для CPU.
     */
    @Getter
    public final SliderSettings simDepth = new SliderSettings("Глубина симуляции", "На сколько тиков вперёд симулировать (умнее при большом значении, но грузит ПК)")
            .range(2, 9)
            .setValue(4)
            .visible(() -> checkCrit.isValue());

    /** Сколько кандидатов точек прицела искать по хитбоксу (было захардкожено 11). */
    @Getter
    public final SliderSettings aimPoints = new SliderSettings("Точки прицела", "Сколько кандидатов точек наводки искать по хитбоксу")
            .range(9, 47)
            .setValue(11);

    /** Максимальное число дорогих валидаций целей за тик (было: все сущности мира). */
    @Getter
    public final SliderSettings targetBudget = new SliderSettings("Бюджет поиска", "Сколько ближайших целей проверять дорогим рейкастом за тик (×10)")
            .range(1, 4)
            .setValue(2);

    /** K вариантов движения цели для упреждающего прицела (0 = выкл). */
    @Getter
    public final SliderSettings moveVariants = new SliderSettings("Предикт цели", "Сколько вариантов движения цели симулировать для упреждения (0 = выкл)")
            .range(0, 8)
            .setValue(5);

    private final SilentMaceHandler silentMaceHandler = new SilentMaceHandler();
    private final ShieldBreakerHandler shieldBreakerHandler = new ShieldBreakerHandler();

    public Aura() {
        super("Aura", ModuleCategory.COMBAT);
        settings(mode, attackrange, lookrange, options, targetType, moveFix, resetSprintMode, checkCrit, smartCrits, mode1_8, cpsSetting, silentMace, elytraRotationMode, heightRandom, fakeRotation, fakeRotationAmount, autoFlyme, moveFixInFly, shieldBreaker, multiTarget, simDepth, aimPoints, targetBudget, moveVariants);
    }

    @NonFinal
    public static LivingEntity target;

    @NonFinal
    public LivingEntity lastTarget;

    // мультитаргет
    @NonFinal
    private java.util.List<LivingEntity> availableTargets = new java.util.ArrayList<>();
    @NonFinal
    private int currentTargetIndex = 0;
    @NonFinal
    private long lastAttackTime = 0;
    @NonFinal
    private long lastTargetSwitchTime = 0;

    // флайми дабл пробел палим
    @NonFinal
    private long lastJumpPressTime = 0;
    @NonFinal
    private boolean flymeCommandSent = false; // чтоб не спамило

    TargetFinder targetSelector = new TargetFinder();
    MultiPoint pointFinder = new MultiPoint();

    /**
     * Кэш тяжёлой части конфига (computeVector + предикт цели): getConfig()
     * дергается несколько раз за тик (PRE, POST, rotation-конструкторы) —
     * считаем один раз, остальным раздаём кэш. Инвалидация — в начале каждого PRE.
     */
    @NonFinal
    private Pair<Vec3d, Box> cachedPointData = null;
    @NonFinal
    private boolean pointCacheValid = false;

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
        shieldBreakerHandler.forceReset();
        // сброс флайми
        lastJumpPressTime = 0;
        flymeCommandSent = false;
        
        // сброс таргетов
        availableTargets.clear();
        currentTargetIndex = 0;
        lastAttackTime = 0;
        lastTargetSwitchTime = 0;

        // сброс кэша конфига
        cachedPointData = null;
        pointCacheValid = false;
    }

    @EventHandler
    private void tick(TickEvent event) {
        // сайлент мейс
        if (silentMace.isValue()) {
            silentMaceHandler.onTick(target);
        }

        // ломатель щитов
        if (shieldBreaker.isValue()) {
            shieldBreakerHandler.onTick(target);
        }

        // фпс троттлер чек
        if (target != null) {
            FpsThrottler.updateTarget(target.getName().getString());
        } else {
            FpsThrottler.updateTarget(null);
        }
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        // обновляем 1.8
        update1_8Settings();
        
        // сначала фаерболы
        handleFireballs();
        
        switch (e.getType()) {
            case EventType.PRE -> {
                // конфиг пересчитываем один раз за тик (на PRE)
                pointCacheValid = false;

                LivingEntity previousTarget = target;
                target = updateTarget();

                if (previousTarget != null && target == null) {
                    Initialization.getInstance().getManager()
                            .getAttackPerpetrator()
                            .getAttackHandler()
                            .resetPendingState();
                }

                boolean passed = false;
                if (mode.isSelected("FunTime Snap")) {
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
                // мейс бьет сам скипаем
                if (silentMace.isValue() && silentMaceHandler.isActive()) {
                    return;
                }
                // топор бьет сам скипаем
                if (shieldBreaker.isValue() && shieldBreakerHandler.isActive()) {
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
        // пихаем 1.8 в страйк
        StrikeManager strikeManager = Initialization.getInstance().getManager()
                .getAttackPerpetrator()
                .getStrikeManager();

        if (strikeManager != null) {
            strikeManager.set1_8Mode(mode1_8.isValue());
            strikeManager.updateCPS(cpsSetting.getInt(), cpsSetting);
        }

        // 1.8 криты вырубаем
        if (mode1_8.isValue() && checkCrit.isValue()) {
            checkCrit.setValue(false);
        }
    }

    // палим круги хз
    private Vec3d lastOrbitCheckPos = null;
    private long orbitStartTime = 0;
    private static final long ORBIT_BREAK_TIME = 800; // через 800мс рвем круг

    @Native(type = Native.Type.VMProtectBeginMutation)
    public StrikerConstructor.AttackPerpetratorConfigurable getConfig() {
        float baseRange = attackrange.getValue();

        Pair<Vec3d, Box> pointData = acquirePointData(baseRange);

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

            Vec3d ourPos = mc.player.getEntityPos();
            Vec3d targetPos = target.getEntityPos();
            double currentDistance = ourPos.distanceTo(targetPos);

            // палим что кружит
            boolean breakingOrbit = false;
            if (lastOrbitCheckPos != null) {
                double lastDistance = lastOrbitCheckPos.distanceTo(targetPos);
                long orbitTime = System.currentTimeMillis() - orbitStartTime;
                
                // дистанция стоит кружит
                if (orbitTime > ORBIT_BREAK_TIME && Math.abs(currentDistance - lastDistance) < 0.5) {
                    // чек реально ли кружит
                    Vec3d toTarget = targetPos.subtract(ourPos);
                    if (toTarget.horizontalLength() > 1.0) {
                        Vec3d toTargetNorm = new Vec3d(toTarget.x, 0, toTarget.z).normalize();
                        Vec3d velNorm = new Vec3d(targetVelocity.x, 0, targetVelocity.z).normalize();
                        double crossProduct = Math.abs(toTargetNorm.x * velNorm.z - toTargetNorm.z * velNorm.x);
                        double radialSpeed = Math.abs(toTargetNorm.dotProduct(velNorm));
                        
                        // кружит рвем круг
                        if (crossProduct > radialSpeed * 1.2) {
                            breakingOrbit = true;
                        }
                    }
                }
            }

            // рвем летим прямо
            Vec3d predictedPos;
            if (breakingOrbit) {
                // прямо на цель
                predictedPos = targetPos;
                // сброс слежки
                lastOrbitCheckPos = null;
                orbitStartTime = 0;
            } else {
                // обычный предикт
                double leadMultiplier = 1.0 + (targetSpeed * 0.6);
                predictedPos = targetPos.add(targetVelocity.multiply(leadTicks * leadMultiplier));
                
                // слежку круга обновили
                lastOrbitCheckPos = targetPos;
                if (orbitStartTime == 0) orbitStartTime = System.currentTimeMillis();
            }

            computedPoint = predictedPos.add(0, target.getHeight() / 2, 0);

            // хитбокс шире чтоб попадало
            double expandAmount = breakingOrbit ? 0.5 : 0.35; // при рве еще шире
            hitbox = new Box(
                    predictedPos.x - target.getWidth() / 2 - expandAmount,
                    predictedPos.y - expandAmount,
                    predictedPos.z - target.getWidth() / 2 - expandAmount,
                    predictedPos.x + target.getWidth() / 2 + expandAmount,
                    predictedPos.y + target.getHeight() + expandAmount,
                    predictedPos.z + target.getWidth() / 2 + expandAmount);
        }

        // рандомим высоту удара
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

    /**
     * Тяжёлая точка прицела — раз за тик.
     * Упреждающий режим (computeVectorSmart): кандидаты по хитбоксу + предсказанные
     * позиции цели (K вариантов симуляции движения). Для элитры — дешёвый классический.
     */
    private Pair<Vec3d, Box> acquirePointData(float baseRange) {
        if (pointCacheValid && cachedPointData != null) {
            return cachedPointData;
        }

        Vec3d targetVelocity = measureTargetVelocity(target);

        pointFinder.updateOffset(isElytraAim() ? Vec3d.ZERO : targetVelocity);

        if (isElytraAim()) {
            // элитра: точное позиционирование по орбите, упреждение считается отдельно
            cachedPointData = pointFinder.computeVector(
                    target,
                    baseRange,
                    AngleConnection.INSTANCE.getRotation(),
                    targetVelocity,
                    options.isSelected("Бить сквозь стены"),
                    11);
        } else {
            cachedPointData = pointFinder.computeVectorSmart(
                    target,
                    baseRange,
                    AngleConnection.INSTANCE.getRotation(),
                    targetVelocity,
                    options.isSelected("Бить сквозь стены"),
                    aimPoints.getInt(),
                    computePredictedCenters(target));
        }

        pointCacheValid = true;
        return cachedPointData;
    }

    private static boolean isElytraAim() {
        return mc.player != null && target != null && mc.player.isGliding() && target.isGliding();
    }

    private static Vec3d measureTargetVelocity(LivingEntity entity) {
        if (entity == null) return Vec3d.ZERO;
        return entity.getEntityPos().subtract(new Vec3d(entity.lastX, entity.lastY, entity.lastZ));
    }

    /**
     * K симуляций движения цели (варианты: по инерции / вперёд / назад / стрейфы / стоп)
     * на глубину по слайдеру «Глубина симуляции». Возвращает центральные точки
     * предсказанных позиций с весами (инерционный сценарий весомее).
     */
    private List<Pair<Vec3d, Double>> computePredictedCenters(LivingEntity aimTarget) {
        if (aimTarget == null || moveVariants.getInt() == 0 || !(aimTarget instanceof PlayerEntity player)) {
            return List.of();
        }

        int horizon = simDepth.getInt();
        if (horizon < 2 || player == mc.player) {
            return List.of();
        }

        List<Pair<Vec3d, Double>> centers = new ArrayList<>();
        int count = Math.min(moveVariants.getInt(), 8);
        double halfHeight = player.getHeight() / 2.0;

        for (int k = 0; k < count; k++) {
            try {
                PlayerSimulation sim = PlayerSimulation.fromOtherPlayer(
                        player,
                        k == 0
                                ? PlayerSimulation.SimulatedPlayerInput.guessInput(player)
                                : new PlayerSimulation.SimulatedPlayerInput(variantInput(k, player)));

                for (int i = 0; i < horizon; i++) {
                    sim.tick();
                }

                centers.add(new Pair<>(sim.pos.add(0, halfHeight, 0), k == 0 ? 1.5 : 1.0));
            } catch (Exception ignored) {
                // симуляция опциональна — не роняем прицел
            }
        }

        return centers;
    }

    /** Вариант движения противника относительно его текущего взгляда. */
    private static PlayerInput variantInput(int variant, PlayerEntity player) {
        boolean jump = !player.isOnGround();
        boolean sneak = player.isSneaking();
        return switch (variant) {
            case 1 -> new PlayerInput(true, false, false, false, jump, sneak, true);     // вперёд
            case 2 -> new PlayerInput(false, true, false, false, false, sneak, false);   // назад
            case 3 -> new PlayerInput(false, false, true, false, false, sneak, true);    // лево
            case 4 -> new PlayerInput(false, false, false, true, false, sneak, true);    // право
            case 5 -> new PlayerInput(true, false, true, false, jump, sneak, true);      // вперёд-лево
            case 6 -> new PlayerInput(true, false, false, true, jump, sneak, true);      // вперёд-право
            case 7 -> new PlayerInput(false, false, false, false, false, sneak, false);  // стоп
            default -> new PlayerInput(false, false, false, false, jump, sneak, false);
        };
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
            case "Jenro" -> new JenroAngle();
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

            case "Matrix", "SpookyTime", "Jenro" -> {
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

        // флайми логика
        handleAutoFlyme();

        // мувфикс логика
        // в полете без фикса скип
        boolean isInFly = mc.player.getAbilities().flying || mc.player.isGliding();
        if (isInFly && !moveFixInFly.isValue()) {
            return;
        }

        if (target == null || !target.isAlive())
            return;

        boolean w = mc.options.forwardKey.isPressed();
        boolean s = mc.options.backKey.isPressed();
        boolean a = mc.options.leftKey.isPressed();
        boolean d = mc.options.rightKey.isPressed();

        if (moveFix.isSelected("Сфокусированная")) {
            // жостко по взгляду
            float yaw = AngleConnection.INSTANCE.getRotation().getYaw();
            
            boolean forward = false, back = false, left = false, right = false;
            
            if (w) forward = true;
            if (s) back = true;
            if (a) left = true;
            if (d) right = true;
            
            event.setDirectionalLow(forward, back, left, right);
            return;
        }
        
        if (moveFix.isSelected("Свободная")) {
            // почти не трогаем двиг
            // двиг как есть
            return; // двиг не трогаем
        }
        
        if (moveFix.isSelected("Таргет")) {
            Vec3d playerPos = mc.player.getEntityPos();
            Vec3d targetPos = target.getEntityPos();

            // в полете чекаем высоту
            Vec3d moveTarget;
            if (isInFly) {
                // летим с высотой
                moveTarget = targetPos;
            } else {
                // на земле высоту пофиг
                moveTarget = new Vec3d(targetPos.x, playerPos.y, targetPos.z);
            }
            
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

            // в полете Y не трогаем
            if (!isInFly) {
                // на земле свой Y
                moveTargetVec = new Vec3d(moveTargetVec.x, playerPos.y, moveTargetVec.z);
            }
            // в полете Y цели

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

    /**
     * дабл пробел жмет flyme
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void handleAutoFlyme() {
        if (!autoFlyme.isValue()) {
            flymeCommandSent = false; // вырубили сброс
            return;
        }

        // флай уже есть скип
        if (mc.player.getAbilities().flying) {
            flymeCommandSent = false; // сброс ждем откл флая
            return;
        }

        // чек жмет ли пробел
        boolean jumpPressed = mc.options.jumpKey.isPressed();
        long currentTime = System.currentTimeMillis();

        if (jumpPressed) {
            // чек дабл клик пробела
            if (currentTime - lastJumpPressTime < 500 && lastJumpPressTime > 0) {
                // чек что в прыжке
                boolean inJump = !mc.player.isOnGround() && mc.player.fallDistance < 0.1;

                if (inJump && !flymeCommandSent) {
                    // пишем flyme
                    mc.player.networkHandler.sendChatCommand("flyme");
                    flymeCommandSent = true;
                }
            }
            lastJumpPressTime = currentTime;
        } else {
            // пробел отпустил сброс
            // чтоб снова пробовать
            if (flymeCommandSent && !mc.player.getAbilities().flying) {
                flymeCommandSent = false;
            }
        }
    }

    private LivingEntity updateTarget() {
        if (mc.player == null || mc.world == null) return null;
        
        TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(targetType.getSelected());
        float range = attackrange.getValue() + 0.25F
                + (mc.player.isGliding() && ElytraTarget.getInstance() != null && ElytraTarget.getInstance().isState()
                        ? ElytraTarget.getInstance().elytraFindRange.getValue()
                        : lookrange.getValue());

        float dynamicFov = 360;

        targetSelector.searchTargets(mc.world.getEntities(), range, dynamicFov,
                options.isSelected("Бить сквозь стены"));
        targetSelector.validateTarget(filter::isValid);
        
        // мультитаргет логика
        if (multiTarget.isValue()) {
            return updateMultiTarget(filter, range, dynamicFov);
        }
        
        return targetSelector.getCurrentTarget();
    }
    
    private void handleFireballs() {
        if (!targetType.getSelected().contains("Фаерболлы")) return;
        
        float range = attackrange.getValue() + lookrange.getValue();
        
        for (net.minecraft.entity.Entity entity : mc.world.getEntities()) {
            if (entity instanceof net.minecraft.entity.projectile.FireballEntity fireball) {
                float distance = mc.player.distanceTo(fireball);
                
                if (distance <= range && distance > 0.5f) {
                    Angle fireballAngle = MathAngle.calculateAngle(fireball.getEyePos());
                    AngleConnection.INSTANCE.setRotation(fireballAngle);
                    
                    if (mc.player.getAttackCooldownProgress(0.5f) >= 0.9f) {
                        mc.interactionManager.attackEntity(mc.player, fireball);
                        mc.player.swingHand(net.minecraft.util.Hand.MAIN_HAND);
                    }
                    break;
                }
            }
        }
    }
    
    private LivingEntity updateMultiTarget(TargetFinder.EntityFilter filter, float range, float dynamicFov) {
        // собираем все цели
        java.util.List<LivingEntity> allTargets = new java.util.ArrayList<>();
        java.util.List<LivingEntity> attackableTargets = new java.util.ArrayList<>();
        float attackRange = attackrange.getValue();
        
        for (net.minecraft.entity.Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity living && entity != mc.player) {
                float distance = mc.player.distanceTo(living);
                
                if (distance <= range && filter.isValid(living)) {
                    // чек фова
                    if (dynamicFov < 360) {
                        Vec3d playerPos = mc.player.getEyePos();
                        Vec3d targetPos = living.getEyePos();
                        Vec3d direction = targetPos.subtract(playerPos).normalize();
                        
                        float playerYaw = mc.player.getYaw();
                        float targetYaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90F;
                        float angleDiff = Math.abs(MathHelper.wrapDegrees(targetYaw - playerYaw));
                        
                        if (angleDiff > dynamicFov / 2F) continue;
                    }
                    
                    // чек стены
                    if (!options.isSelected("Бить сквозь стены")) {
                        net.minecraft.util.hit.HitResult raycast = mc.world.raycast(new net.minecraft.world.RaycastContext(
                            mc.player.getEyePos(),
                            living.getEyePos(),
                            net.minecraft.world.RaycastContext.ShapeType.COLLIDER,
                            net.minecraft.world.RaycastContext.FluidHandling.NONE,
                            mc.player
                        ));
                        
                        if (raycast.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
                            continue;
                        }
                    }
                    
                    allTargets.add(living);
                    
                    // считаем кого бить
                    if (distance <= attackRange) {
                        attackableTargets.add(living);
                    }
                }
            }
        }
        
        // нет целей сброс
        if (allTargets.isEmpty()) {
            availableTargets.clear();
            currentTargetIndex = 0;
            return null;
        }
        
        // сортим по дистанции
        allTargets.sort((a, b) -> Float.compare(mc.player.distanceTo(a), mc.player.distanceTo(b)));
        
        // список поменялся обновляем
        if (!availableTargets.equals(allTargets)) {
            LivingEntity currentTarget = availableTargets.isEmpty() ? null : 
                (currentTargetIndex < availableTargets.size() ? availableTargets.get(currentTargetIndex) : null);
            
            availableTargets = new java.util.ArrayList<>(allTargets);
            
            // держим старую цель
            if (currentTarget != null && availableTargets.contains(currentTarget)) {
                currentTargetIndex = availableTargets.indexOf(currentTarget);
            } else {
                currentTargetIndex = 0;
            }
        }
        
        // одна цель не свитчим
        if (attackableTargets.size() <= 1) {
            // берем ближайшую
            currentTargetIndex = 0;
        }
        
        // чек индекс норм
        if (currentTargetIndex >= availableTargets.size()) {
            currentTargetIndex = 0;
        }
        
        return availableTargets.get(currentTargetIndex);
    }
    
    /**
     * свитч на след цель
     */
    private void switchToNextTarget() {
        if (availableTargets.size() > 1) {
            currentTargetIndex = (currentTargetIndex + 1) % availableTargets.size();
        }
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
    
    /**
     * говорим что ударили
     */
    public void notifyAttackExecuted() {
        if (multiTarget.isValue() && availableTargets.size() > 1) {
            long currentTime = System.currentTimeMillis();
            
            // прошло 100мс свитчим
            if (currentTime - lastTargetSwitchTime >= 100) {
                lastAttackTime = currentTime;
                lastTargetSwitchTime = currentTime;
                switchToNextTarget();
            }
        }
    }
}