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
            .value("Игроки", "Мобы", "Животные", "Друзья", "Стойки для брони", "BW Тиммейты", "Креатив", "Инвизы", "Голые инвизы", "Фаерболлы")
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

    private final SilentMaceHandler silentMaceHandler = new SilentMaceHandler();
    private final ShieldBreakerHandler shieldBreakerHandler = new ShieldBreakerHandler();

    public Aura() {
        super("Aura", ModuleCategory.COMBAT);
        settings(mode, attackrange, lookrange, options, targetType, moveFix, resetSprintMode, checkCrit, smartCrits, mode1_8, cpsSetting, silentMace, elytraRotationMode, heightRandom, fakeRotation, fakeRotationAmount, autoFlyme, moveFixInFly, shieldBreaker, multiTarget);
    }

    @NonFinal
    public static LivingEntity target;

    @NonFinal
    public LivingEntity lastTarget;

    // Multi-Target система
    @NonFinal
    private java.util.List<LivingEntity> availableTargets = new java.util.ArrayList<>();
    @NonFinal
    private int currentTargetIndex = 0;
    @NonFinal
    private long lastAttackTime = 0;
    @NonFinal
    private long lastTargetSwitchTime = 0;

    // AutoFlyme - отслеживание двойного нажатия пробела
    @NonFinal
    private long lastJumpPressTime = 0;
    @NonFinal
    private boolean flymeCommandSent = false; // чтобы не спамить команду каждый тик

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
        shieldBreakerHandler.forceReset();
        // Сброс AutoFlyme
        lastJumpPressTime = 0;
        flymeCommandSent = false;
        
        // Сброс Multi-Target
        availableTargets.clear();
        currentTargetIndex = 0;
        lastAttackTime = 0;
        lastTargetSwitchTime = 0;
    }

    @EventHandler
    private void tick(TickEvent event) {
        // Silent Mace handler
        if (silentMace.isValue()) {
            silentMaceHandler.onTick(target);
        }

        // Shield Breaker handler
        if (shieldBreaker.isValue()) {
            shieldBreakerHandler.onTick(target);
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
        
        // Приоритетная обработка фаерболлов
        handleFireballs();
        
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
                // Если Silent Mace активен и сейчас атакует булавой — пропускаем обычную атаку
                if (silentMace.isValue() && silentMaceHandler.isActive()) {
                    return;
                }
                // Если Shield Breaker активен и сейчас атакует топором — пропускаем обычную атаку
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

    // Для отслеживания круга - если долго преследуем но дистанция не уменьшается
    private Vec3d lastOrbitCheckPos = null;
    private long orbitStartTime = 0;
    private static final long ORBIT_BREAK_TIME = 800; // Через 800ms разрываем круг

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

            Vec3d ourPos = mc.player.getEntityPos();
            Vec3d targetPos = target.getEntityPos();
            double currentDistance = ourPos.distanceTo(targetPos);

            // DETECT ORBIT: если мы преследуем цель но дистанция НЕ уменьшается
            boolean breakingOrbit = false;
            if (lastOrbitCheckPos != null) {
                double lastDistance = lastOrbitCheckPos.distanceTo(targetPos);
                long orbitTime = System.currentTimeMillis() - orbitStartTime;
                
                // Если дистанция не уменьшается уже 800ms+ и цель кружится
                if (orbitTime > ORBIT_BREAK_TIME && Math.abs(currentDistance - lastDistance) < 0.5) {
                    // Проверяем что цель реально кружится (перпендикулярная скорость)
                    Vec3d toTarget = targetPos.subtract(ourPos);
                    if (toTarget.horizontalLength() > 1.0) {
                        Vec3d toTargetNorm = new Vec3d(toTarget.x, 0, toTarget.z).normalize();
                        Vec3d velNorm = new Vec3d(targetVelocity.x, 0, targetVelocity.z).normalize();
                        double crossProduct = Math.abs(toTargetNorm.x * velNorm.z - toTargetNorm.z * velNorm.x);
                        double radialSpeed = Math.abs(toTargetNorm.dotProduct(velNorm));
                        
                        // Цель кружится и мы не приближаемся — РАЗРЫВАЕМ КРУГ
                        if (crossProduct > radialSpeed * 1.2) {
                            breakingOrbit = true;
                        }
                    }
                }
            }

            // Если разрываем круг - летим НАПРЯМУЮ к цели без предсказания
            Vec3d predictedPos;
            if (breakingOrbit) {
                // Летим ПРЯМО к цели - никакого предсказания
                predictedPos = targetPos;
                // Сбрасываем отслеживание
                lastOrbitCheckPos = null;
                orbitStartTime = 0;
            } else {
                // Обычное предсказание
                double leadMultiplier = 1.0 + (targetSpeed * 0.6);
                predictedPos = targetPos.add(targetVelocity.multiply(leadTicks * leadMultiplier));
                
                // Обновляем отслеживание круга
                lastOrbitCheckPos = targetPos;
                if (orbitStartTime == 0) orbitStartTime = System.currentTimeMillis();
            }

            computedPoint = predictedPos.add(0, target.getHeight() / 2, 0);

            // Расширенный хитбокс для более стабильных попаданий
            double expandAmount = breakingOrbit ? 0.5 : 0.35; // Ещё больше при разрыве круга
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

        // ===== AutoFlyme Logic =====
        handleAutoFlyme();

        // ===== MoveFix Logic =====
        // Если moveFixInFly выключен и игрок в полёте - не применяем коррекцию движения
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
            // Сфокусированная коррекция - движение строго по направлению взгляда
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
            // Свободная коррекция - минимальные изменения движения
            // Оставляем движение как есть, только слегка корректируем
            return; // Не изменяем движение
        }
        
        if (moveFix.isSelected("Таргет")) {
            Vec3d playerPos = mc.player.getEntityPos();
            Vec3d targetPos = target.getEntityPos();

            // ИСПРАВЛЕНИЕ: В полёте учитываем высоту цели
            Vec3d moveTarget;
            if (isInFly) {
                // В полёте - летим к цели с учетом высоты
                moveTarget = targetPos;
            } else {
                // На земле - игнорируем высоту цели
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

            // ИСПРАВЛЕНИЕ: В полёте НЕ обнуляем Y координату
            if (!isInFly) {
                // На земле - используем Y игрока
                moveTargetVec = new Vec3d(moveTargetVec.x, playerPos.y, moveTargetVec.z);
            }
            // В полёте - используем Y цели (moveTargetVec уже содержит правильную высоту)

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
     * AutoFlyme - при двойном нажатии пробела в прыжке без флая прописывает /flyme
     * Работает только для серверов Jenro (funnymc.su и подобные)
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void handleAutoFlyme() {
        if (!autoFlyme.isValue()) {
            flymeCommandSent = false; // сброс при выключенной настройке
            return;
        }

        // Если у игрока уже есть флай (ability) - ничего не делаем
        if (mc.player.getAbilities().flying) {
            flymeCommandSent = false; // сброс, можно будет снова активировать если флай отключат
            return;
        }

        // Проверяем что игрок нажимает пробел
        boolean jumpPressed = mc.options.jumpKey.isPressed();
        long currentTime = System.currentTimeMillis();

        if (jumpPressed) {
            // Проверяем двойное нажатие (в пределах 500мс между нажатиями)
            if (currentTime - lastJumpPressTime < 500 && lastJumpPressTime > 0) {
                // Проверяем что игрок в прыжке (не на земле)
                boolean inJump = !mc.player.isOnGround() && mc.player.fallDistance < 0.1;

                if (inJump && !flymeCommandSent) {
                    // Пропишем /flyme
                    mc.player.networkHandler.sendChatCommand("flyme");
                    flymeCommandSent = true;
                }
            }
            lastJumpPressTime = currentTime;
        } else {
            // Если пробел отпущен и флай не включился - сбрасываем flymeCommandSent
            // чтобы можно было попробовать снова
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
        
        // Multi-Target логика
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
        // Получаем всех доступных целей в радиусе поиска
        java.util.List<LivingEntity> allTargets = new java.util.ArrayList<>();
        java.util.List<LivingEntity> attackableTargets = new java.util.ArrayList<>();
        float attackRange = attackrange.getValue();
        
        for (net.minecraft.entity.Entity entity : mc.world.getEntities()) {
            if (entity instanceof LivingEntity living && entity != mc.player) {
                float distance = mc.player.distanceTo(living);
                
                if (distance <= range && filter.isValid(living)) {
                    // Проверяем FOV если нужно
                    if (dynamicFov < 360) {
                        Vec3d playerPos = mc.player.getEyePos();
                        Vec3d targetPos = living.getEyePos();
                        Vec3d direction = targetPos.subtract(playerPos).normalize();
                        
                        float playerYaw = mc.player.getYaw();
                        float targetYaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90F;
                        float angleDiff = Math.abs(MathHelper.wrapDegrees(targetYaw - playerYaw));
                        
                        if (angleDiff > dynamicFov / 2F) continue;
                    }
                    
                    // Проверяем стены если нужно
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
                    
                    // Отдельно считаем цели в радиусе атаки
                    if (distance <= attackRange) {
                        attackableTargets.add(living);
                    }
                }
            }
        }
        
        // Если целей нет - сбрасываем
        if (allTargets.isEmpty()) {
            availableTargets.clear();
            currentTargetIndex = 0;
            return null;
        }
        
        // Сортируем цели по дистанции (ближайшие первые)
        allTargets.sort((a, b) -> Float.compare(mc.player.distanceTo(a), mc.player.distanceTo(b)));
        
        // Если список целей изменился - обновляем
        if (!availableTargets.equals(allTargets)) {
            LivingEntity currentTarget = availableTargets.isEmpty() ? null : 
                (currentTargetIndex < availableTargets.size() ? availableTargets.get(currentTargetIndex) : null);
            
            availableTargets = new java.util.ArrayList<>(allTargets);
            
            // Пытаемся сохранить текущую цель если она ещё доступна
            if (currentTarget != null && availableTargets.contains(currentTarget)) {
                currentTargetIndex = availableTargets.indexOf(currentTarget);
            } else {
                currentTargetIndex = 0;
            }
        }
        
        // Если в радиусе атаки только одна цель - не переключаемся
        if (attackableTargets.size() <= 1) {
            // Выбираем ближайшую цель (первую в отсортированном списке)
            currentTargetIndex = 0;
        }
        
        // Проверяем что индекс валидный
        if (currentTargetIndex >= availableTargets.size()) {
            currentTargetIndex = 0;
        }
        
        return availableTargets.get(currentTargetIndex);
    }
    
    /**
     * Переключает на следующую цель в Multi-Target режиме
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
     * Уведомляет Multi-Target систему о том что произошла атака
     */
    public void notifyAttackExecuted() {
        if (multiTarget.isValue() && availableTargets.size() > 1) {
            long currentTime = System.currentTimeMillis();
            
            // Переключаем цель если прошло достаточно времени (100мс)
            if (currentTime - lastTargetSwitchTime >= 100) {
                lastAttackTime = currentTime;
                lastTargetSwitchTime = currentTime;
                switchToNextTarget();
            }
        }
    }
}