package fun.aegis.features.impl.combat;

import antidaunleak.api.annotation.Native;
import fun.aegis.events.player.MotionEvent;
import fun.aegis.events.player.TickEvent;
import fun.aegis.events.render.DrawEvent;
import fun.aegis.events.render.WorldRenderEvent;
import fun.aegis.features.impl.movement.ElytraTarget;
import fun.aegis.features.impl.movement.TargetStrafe;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.geometry.Render3D;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.features.aura.point.MultiPoint;
import fun.aegis.utils.features.aura.rotations.constructor.LinearConstructor;
import fun.aegis.utils.features.aura.rotations.constructor.RotateConstructor;
import fun.aegis.utils.features.aura.rotations.impl.*;
import fun.aegis.utils.features.aura.utils.MathAngle;
import fun.aegis.utils.features.aura.warp.TurnsConfig;
import fun.aegis.utils.features.aura.warp.Turns;
import fun.aegis.utils.features.aura.warp.TurnsConnection;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import fun.aegis.utils.interactions.inv.InventoryToolkit;
import net.minecraft.item.ItemStack;
import net.minecraft.item.MaceItem;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.ClientStatusC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerRespawnS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.util.Pair;
import net.minecraft.util.math.*;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.client.managers.event.types.EventType;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.*;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.math.task.TaskPriority;
import fun.aegis.Aegis;
import fun.aegis.events.packet.PacketEvent;
import fun.aegis.events.player.RotationUpdateEvent;
import fun.aegis.display.hud.Notifications;
import fun.aegis.utils.features.aura.striking.StrikeManager;
import fun.aegis.utils.features.aura.striking.StrikerConstructor;
import fun.aegis.utils.features.aura.target.TargetFinder;
import fun.aegis.features.impl.render.Hud;
import fun.aegis.utils.math.calc.Calculate;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Aura extends Module {

    private static final float RANGE_MARGIN = 0.253F;

    private static final Map<String, Integer> LEGIT_SPRINT_MAP = Map.of(
            "Matrix", 1,
            "HolyWorld", 2);

    public static Aura getInstance() {
        return Instance.get(Aura.class);
    }

    TargetFinder targetSelector = new TargetFinder();
    MultiPoint pointFinder = new MultiPoint();

    @NonFinal
    LivingEntity target, lastTarget;

    @NonFinal
    long shiftTapEndTime = 0;

    public static boolean fakeRotate;

    @NonFinal
    @Getter
    public static float legitSprintNeed;

    @NonFinal
    private int originalSlot = -1;

    @NonFinal
    private int maceSwitchTimer = 0;

    @NonFinal
    private FovCircleRenderer fovCircleRenderer;

    @NonFinal
    private Random random = new Random();

    SelectSetting aimMode = new SelectSetting("Наводка", "Выберите тип наводки")
            .value("None", "Legit Snap", "ReallyWorld", "HolyWorld", "HvH", "HvH V2", "HvH V2X", "Matrix")
            .selected("Legit Snap");

    MultiSelectSetting targetType = new MultiSelectSetting("Тип таргета", "Фильтрует весь список целей по типу")
            .value("Players", "Mobs", "Animals", "Friends", "Armor Stand")
            .selected("Players", "Mobs", "Animals");

    SliderSettings attackRange = new SliderSettings("Дистанция удара", "Дальность атаки до цели")
            .setValue(3).range(1F, 6F);

    SliderSettings lookRange = new SliderSettings("Дополнительная дистанция поиска", "Диапазон поиска до цели")
            .setValue(1.5f).range(0F, 2F);

    SliderSettings fov = new SliderSettings("FOV", "Угол обзора килауры")
            .range(1.0f, 360.0f).setValue(90.0f);

    MultiSelectSetting attackSetting = new MultiSelectSetting("Настройки", "Позволяет настроить работу функции")
            .value("Only Critical", "Break Shield", "UnPress Shield", "No Attack When Eat", "Ignore The Walls",
                    "Fake Lag", "Hit Chance")
            .selected("Only Critical", "Break Shield");

    SliderSettings hitChance = new SliderSettings("Шанс удара в %", "Шанс удара по цели")
            .setValue(100).range(1F, 100F).visible(() -> attackSetting.isSelected("Hit Chance"));

    SelectSetting correctionType = new SelectSetting("Коррекции движения", "Выбор коррекции движения игрока")
            .value("Free", "Focused", "Target", "Not visible").selected("Free");

    SelectSetting sprintReset = new SelectSetting("Сброс спринта", "Выбор сброса спринта перед ударом")
            .value("Legit", "Packet").selected("Legit");

    BooleanSetting mode18 = new BooleanSetting("1.8 Режим", "Включить режим атаки как в 1.8")
            .setValue(false);

    BooleanSetting smartCrits = new BooleanSetting("Удары на земле", "Криты только при нажатии пробела")
            .setValue(true).visible(() -> attackSetting.isSelected("Only Critical") && !mode18.isValue());

    SliderSettings cps = new SliderSettings("CPS", "Клики в секунду")
            .setValue(10).range(1F, 35F).visible(() -> mode18.isValue());

    SelectSetting clickType = new SelectSetting("Click Type", "Тип клика для 1.8 режима")
            .value("Normal", "Drag", "Butterfly", "Jitter-click")
            .selected("Normal").visible(() -> mode18.isValue());

    BooleanSetting autoMace = new BooleanSetting("AutoMace", "Автоматически бьет булавой на определенной высоте")
            .setValue(false);

    BooleanSetting fovCircleEnabled = new BooleanSetting("FOV Circle", "Отображать круг FOV для Legit Snap")
            .setValue(true);

    SliderSettings fovCircleThickness = new SliderSettings("Толщина круга", "Толщина линии FOV круга")
            .range(0.5f, 5.0f).setValue(1.5f).visible(() -> fovCircleEnabled.isValue());

    SelectSetting fovCircleColor = new SelectSetting("Цвет круга", "Цвет FOV круга")
            .value("White", "Red", "Green", "Blue", "Yellow", "Cyan", "Magenta", "Client Color")
            .selected("White").visible(() -> fovCircleEnabled.isValue());

    SliderSettings fovCircleAlpha = new SliderSettings("Прозрачность", "Прозрачность FOV круга")
            .range(0.0f, 255.0f).setValue(255.0f).visible(() -> fovCircleEnabled.isValue());

    SliderSettings maceHeight = new SliderSettings("Высота для AutoMace",
            "Высота для автоматического использования булавы")
            .setValue(50).range(3F, 200F).visible(() -> autoMace.isValue());

    BooleanSetting reachEnabled = new BooleanSetting("Reach", "Спуфит позицию для увеличения дистанции атаки")
            .setValue(false);

    SliderSettings reachSpoofDistance = new SliderSettings("Reach Distance", "Дистанция спуфа позиции")
            .setValue(3.3f).range(3.0F, 6F).visible(() -> reachEnabled.isValue());

    public Aura() {
        super("Aura", ModuleCategory.COMBAT);
        setup(
                aimMode,
                correctionType,
                sprintReset,

                targetType,

                attackRange,
                lookRange,
                fov,
                hitChance,

                attackSetting,
                smartCrits,
                mode18,
                cps,
                autoMace,
                maceHeight,
                reachEnabled,
                reachSpoofDistance,
                fovCircleEnabled,
                fovCircleThickness,
                fovCircleColor,
                fovCircleAlpha);
        fovCircleRenderer = new FovCircleRenderer();
        Aegis.getInstance().getEventManager().register(fovCircleRenderer);
    }

    @Override
    public void deactivate() {
        targetSelector.releaseTarget();
        target = null;
        lastTarget = null;
        packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
        packets.clear();

        originalSlot = -1;
        maceSwitchTimer = 0;

        TurnsConnection.INSTANCE.setRotation(null);
        TurnsConnection.INSTANCE.clear();

        super.deactivate();
    }

    private final List<Packet<?>> packets = new CopyOnWriteArrayList<>();
    private Box box;
    public static int tickStop = -1;

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e == null || e.getPacket() == null)
            return;

        if (e.getPacket() instanceof EntityStatusS2CPacket status && status.getStatus() == 30) {
            Entity entity = status.getEntity(mc.world);
            if (entity != null && entity.equals(target) && Hud.getInstance() != null
                    && Hud.getInstance().notificationSettings.isSelected("Break Shield")) {
                Notifications.getInstance()
                        .addList(Text.literal("Сломали щит игроку - ").append(entity.getDisplayName()), 5000);
            }
        }

        if (attackSetting.isSelected("Fake Lag") && target != null) {
            if (PlayerInteractionHelper.nullCheck())
                return;
            switch (e.getPacket()) {
                case PlayerRespawnS2CPacket respawn -> {
                    setState(false);
                    packets.clear();
                }
                case GameJoinS2CPacket join -> {
                    setState(false);
                    packets.clear();
                }
                case ClientStatusC2SPacket status when status.getMode()
                        .equals(ClientStatusC2SPacket.Mode.PERFORM_RESPAWN) -> {
                    setState(false);
                    packets.clear();
                }
                default -> {
                    if (e.isSend() && tickStop < 0) {
                        if (packets.size() < 256) {
                            packets.add(e.getPacket());
                            e.cancel();
                        }
                    }
                }
            }
        }
    }

    public class FovCircleRenderer implements QuickImports {
        private float currentScale = 1.0f;
        private float targetScale = 1.0f;
        private float cachedDynamicFov = 33;
        private float lastFinalRadius = 0;

        @EventHandler
        public void drawEvent(DrawEvent e) {
            if (mc.player == null || !aimMode.isSelected("Legit Snap") || !isState() || !fovCircleEnabled.isValue())
                return;

            if (mc.options.getPerspective().isFirstPerson()) {
                MatrixStack matrix = e.getDrawContext().getMatrices();
                float middleW = mc.getWindow().getScaledWidth() / 2f;
                float middleH = mc.getWindow().getScaledHeight() / 2f;

                double fov = mc.options.getFov().getValue();
                fov = MathHelper.clamp(fov, 30, 110);

                float baseRadius = (float) MathHelper.lerp((fov - 30.0) / 80.0, 106.5, 65.0);
                float fovScale = (float) (450.0 / fov);
                float dynamicRadius = baseRadius * fovScale;

                targetScale = mc.player.isSprinting() ? 0.9f : 1f;
                currentScale = Calculate.interpolateSmooth(2.5, currentScale, targetScale);

                float finalRadius = dynamicRadius * currentScale;
                lastFinalRadius = finalRadius;

                float baseThickness = fovCircleThickness.getValue();

                int circleColor = getCircleColor();

                arc.render(ShapeProperties
                        .create(matrix, middleW - finalRadius / 2f, middleH - finalRadius / 2f, finalRadius,
                                finalRadius)
                        .round(0.3F)
                        .thickness(baseThickness)
                        .end(360)
                        .color(circleColor)
                        .build());

                cachedDynamicFov = calculateDynamicFov();
            }
        }

        private int getCircleColor() {
            int alpha = (int) fovCircleAlpha.getValue();
            return switch (fovCircleColor.getSelected()) {
                case "Red" -> ColorAssist.getColor(255, 0, 0, alpha);
                case "Green" -> ColorAssist.getColor(0, 255, 0, alpha);
                case "Blue" -> ColorAssist.getColor(0, 0, 255, alpha);
                case "Yellow" -> ColorAssist.getColor(255, 255, 0, alpha);
                case "Cyan" -> ColorAssist.getColor(0, 255, 255, alpha);
                case "Magenta" -> ColorAssist.getColor(255, 0, 255, alpha);
                case "Client Color" -> ColorAssist.getClientColor();
                default -> ColorAssist.getColor(255, 255, 255, alpha);
            };
        }

        private float calculateDynamicFov() {
            if (mc.player == null || mc.getWindow() == null)
                return 33;

            double fov = mc.options.getFov().getValue();
            fov = MathHelper.clamp(fov, 30, 110);

            float screenWidth = mc.getWindow().getScaledWidth();
            float screenHeight = mc.getWindow().getScaledHeight();

            float circleRadiusInPixels = lastFinalRadius / 2f;

            double horizontalFovRadians = Math.toRadians(fov);

            double pixelsPerRadian = screenWidth / horizontalFovRadians;

            double circleFovRadians = circleRadiusInPixels / pixelsPerRadian;

            float circleFovDegrees = (float) Math.toDegrees(circleFovRadians);

            circleFovDegrees *= 2.0f;

            return MathHelper.clamp(circleFovDegrees, 36, 360);
        }

        public float getCachedDynamicFov() {
            return cachedDynamicFov;
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (box != null && attackSetting.isSelected("Fake Lag") && target != null) {
            Render3D.drawBox(box, ColorAssist.getClientColor(), 1);
        }
    }

    @EventHandler
    public void tick(TickEvent e) {
        if (mode18.isValue()) {
            attackSetting.getSelected().remove("Only Critical");
        }

        if (PlayerInteractionHelper.nullCheck())
            return;
        if (target == null)
            return;

        if (maceSwitchTimer > 0) {
            maceSwitchTimer--;
            if (maceSwitchTimer == 0 && originalSlot != -1) {
                InventoryToolkit.switchTo(originalSlot);
                originalSlot = -1;
            }
        }

        tickStop--;
        if (tickStop >= 0 && !packets.isEmpty() && attackSetting.isSelected("Fake Lag")) {
            if (mc.player != null) {
                box = mc.player.getBoundingBox();
            }
            packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
            packets.clear();
        }
        if (mc.player != null && mc.player.distanceTo(target) > attackRange.getValue()
                && attackSetting.isSelected("Fake Lag")) {
            packets.forEach(PlayerInteractionHelper::sendPacketWithOutEvent);
            packets.clear();
        }
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        switch (e.getType()) {
            case EventType.PRE -> {
                target = updateTarget();

                if (target != null && target.isAlive()) {
                    if (!aimMode.isSelected("None")) {
                        rotateToTarget(getConfig());
                    }
                    lastTarget = target;
                } else {
                    // Если нет цели - смотрим туда куда смотрит игрок
                    if (mc.player != null) {
                        Turns playerLook = new Turns(mc.player.getYaw(), mc.player.getPitch());
                        TurnsConnection.INSTANCE.setRotation(playerLook);
                    }
                }
            }
            case EventType.POST -> {
                if (target != null && target.isAlive()) {
                    handleAutoMace();

                    if (!autoMace.isValue() || mc.player == null
                            || mc.player.getY() - target.getY() < maceHeight.getValue()) {
                        if (aimMode.isSelected("None")) {
                            performTriggerAttack(getConfig());
                        } else {
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
        }
    }

    public static boolean shouldRotate;

    private LivingEntity updateTarget() {
        if (mc.world == null)
            return null;

        TargetFinder.EntityFilter filter = new TargetFinder.EntityFilter(targetType.getSelected());
        float range = attackRange.getValue() + RANGE_MARGIN
                + (mc.player != null && mc.player.isGliding() && ElytraTarget.getInstance() != null
                        && ElytraTarget.getInstance().isState() ? ElytraTarget.getInstance().elytraFindRange.getValue()
                                : lookRange.getValue());

        float dynamicFov = fov.getValue();
        if ((aimMode.isSelected("Legit Snap") || aimMode.isSelected("Snap")) && fovCircleRenderer != null) {
            dynamicFov = Math.min(fov.getValue(), fovCircleRenderer.getCachedDynamicFov());
        }

        targetSelector.searchTargets(mc.world.getEntities(), range, dynamicFov,
                attackSetting.isSelected("Ignore The Walls"));
        targetSelector.validateTarget(filter::isValid);
        return targetSelector.getCurrentTarget();
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void rotateToTarget(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (config == null || target == null || mc.player == null)
            return;

        Aegis aegis = Aegis.getInstance();
        if (aegis == null || aegis.getAttackPerpetrator() == null)
            return;

        StrikeManager attackHandler = aegis.getAttackPerpetrator().getAttackHandler();
        if (attackHandler == null)
            return;

        TurnsConnection controller = TurnsConnection.INSTANCE;
        if (controller == null || controller.getRotation() == null)
            return;

        Turns.VecRotation rotation = new Turns.VecRotation(config.getAngle(), config.getAngle().toVector());
        TurnsConfig rotationConfig = getRotationConfig();

        boolean elytraMode = mc.player.isGliding() && attackSetting.isSelected("Elytra possibilities");

        if (fakeRotate && target != null) {
            FakeAngle fake = new FakeAngle();
            Turns fakeRot = fake.limitAngleChange(controller.getRotation(), rotation.getAngle(), rotation.getVec(),
                    target);
            controller.setFakeRotation(fakeRot);
        }
        fakeRotate = false;
        switch (aimMode.getSelected()) {

            case "HolyWorld" -> {
                if (attackHandler.canAttack(config, 10) || !attackHandler.getAttackTimer().finished(150)) {
                    controller.rotateTo(rotation, target, 10, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
                }
            }

            case "Legit Snap" -> {
                if (attackHandler.canAttack(config, 1) || !attackHandler.getAttackTimer().finished(40)) {
                    controller.rotateTo(rotation, target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
                }
            }

            case "ReallyWorld", "Snap" -> {
                controller.rotateTo(rotation, target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
            }

            case "Matrix", "HvH", "HvH V2", "HvH V2X" -> {
                controller.rotateTo(rotation, target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
            }

        }
        ;

        if (shouldRotate && !aimMode.isSelected("TriggerBot")) {
            controller.rotateTo(rotation, target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
        }

        if (elytraMode && !aimMode.isSelected("TriggerBot")) {
            controller.rotateTo(rotation, target, 1, rotationConfig, TaskPriority.HIGH_IMPORTANCE_1, this);
        }
    }

    @NonFinal
    public boolean elytraStateForward = false;
    private boolean wasForwardPressed = false;

    public StrikerConstructor.AttackPerpetratorConfigurable getConfig() {
        if (target == null || mc.player == null)
            return null;

        float baseRange = attackRange.getValue() + RANGE_MARGIN;

        Pair<Vec3d, Box> pointData = pointFinder.computeVector(
                target,
                baseRange,
                TurnsConnection.INSTANCE.getRotation(),
                getSmoothMode().randomValue(),
                attackSetting.isSelected("Ignore The Walls"));

        if (pointData == null)
            return null;

        Vec3d computedPoint = pointData.getLeft();
        Box hitbox = pointData.getRight();

        if (computedPoint == null || hitbox == null)
            return null;

        if (mc.player.isGliding() && target.isGliding()) {
            Vec3d targetVelocity = target.getVelocity();
            if (targetVelocity == null)
                return null;

            double targetSpeed = targetVelocity.horizontalLength();

            float leadTicks = 0;
            if (ElytraTarget.shouldElytraTarget && ElytraTarget.getInstance() != null) {
                leadTicks = ElytraTarget.getInstance().elytraForward.getValue();
            }

            if (targetSpeed > 0.35) {

                Vec3d predictedPos = target.getPos().add(targetVelocity.multiply(leadTicks));
                computedPoint = predictedPos.add(0, target.getHeight() / 2, 0);

                hitbox = new Box(
                        predictedPos.x - target.getWidth() / 2,
                        predictedPos.y,
                        predictedPos.z - target.getWidth() / 2,
                        predictedPos.x + target.getWidth() / 2,
                        predictedPos.y + target.getHeight(),
                        predictedPos.z + target.getWidth() / 2);
            }
        }

        Turns angle = MathAngle.fromVec3d(computedPoint.subtract(Objects.requireNonNull(mc.player).getEyePos()));

        return new StrikerConstructor.AttackPerpetratorConfigurable(
                target,
                angle,
                baseRange,
                attackSetting.getSelected(),
                aimMode,
                hitbox);
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    public TurnsConfig getRotationConfig() {
        boolean visibleCorrection = !correctionType.isSelected("Not visible");
        boolean freeCorrection = !aimMode.isSelected("Legit") && correctionType.isSelected("Free");
        boolean targetCorrection = correctionType.isSelected("Target");

        if (TargetStrafe.getInstance() != null && TargetStrafe.getInstance().isState()
                && TargetStrafe.getInstance().mode.isSelected("Grim") && target != null) {
            freeCorrection = false;
            targetCorrection = false;
        }

        if (targetCorrection) {
            return new TurnsConfig(getSmoothMode(), true, false);
        }

        return new TurnsConfig(getSmoothMode(), visibleCorrection, freeCorrection);
    }

    @EventHandler
    public void onmotion(MotionEvent event) {

    }

    private void performTriggerAttack(StrikerConstructor.AttackPerpetratorConfigurable config) {
        if (config == null || mc.player == null || mc.interactionManager == null)
            return;

        Aegis aegis = Aegis.getInstance();
        if (aegis == null || aegis.getAttackPerpetrator() == null)
            return;

        StrikeManager attackHandler = aegis.getAttackPerpetrator().getAttackHandler();
        if (attackHandler == null)
            return;

        if (!attackHandler.canAttack(config, 0))
            return;

        Vec3d eyePos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(1.0F);
        Box targetBox = config.getBox();

        if (targetBox == null)
            return;

        if (targetBox.contains(eyePos)
                || targetBox.raycast(eyePos, eyePos.add(lookVec.multiply(config.getMaximumRange()))).isPresent()) {
            mc.interactionManager.attackEntity(mc.player, config.getTarget());
            mc.player.swingHand(Hand.MAIN_HAND);
        }
    }

    public RotateConstructor getSmoothMode() {
        if (mc.player == null)
            return new LinearConstructor();

        if (mc.player.isGliding() && attackSetting.isSelected("Elytra possibilities")
                && !aimMode.isSelected("Trigger Bot")) {
            return new LinearConstructor();
        }
        return switch (aimMode.getSelected()) {
            case "HolyWorld" -> new HWAngle();
            case "HvH" -> new HAngle();
            case "HvH V2" -> new HAngleV2();
            case "HvH V2X" -> new HAngleV2X();
            case "ReallyWorld" -> new RWAngle();
            case "Legit Snap" -> new SnapAngle();
            case "Matrix" -> new MatrixAdvancedPredictor();
            default -> new LinearConstructor();
        };
    }

    private int findMaceInInventory() {
        if (mc.player == null)
            return -1;

        // Check hotbar first (0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof MaceItem) {
                return i;
            }
        }

        // Check main inventory (9-35)
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof MaceItem) {
                return i;
            }
        }

        return -1;
    }

    private void handleAutoMace() {
        if (!autoMace.isValue() || target == null || mc.player == null)
            return;

        double playerY = mc.player.getY();
        double targetY = target.getY();
        double heightDiff = playerY - targetY;

        if (heightDiff >= maceHeight.getValue()) {
            int maceSlot = findMaceInInventory();
            if (maceSlot != -1) {
                if (originalSlot == -1) {
                    originalSlot = mc.player.getInventory().selectedSlot;
                }

                if (mc.player.getInventory().selectedSlot != maceSlot) {
                    if (maceSlot < 9) {
                        InventoryToolkit.switchTo(maceSlot);
                    } else {
                        int hotbarSlot = originalSlot;
                        InventoryToolkit.quickMoveFromTo(maceSlot, hotbarSlot);
                        InventoryToolkit.switchTo(hotbarSlot);
                    }
                }

                if (mc.interactionManager != null) {
                    mc.interactionManager.attackEntity(mc.player, target);
                    mc.player.swingHand(Hand.MAIN_HAND);
                    maceSwitchTimer = 2;
                }
            }
        }
    }

    public float reach() {
        if (!reachEnabled.isValue() || target == null || mc.player == null || !target.isAlive()) {
            return attackRange.getValue() + RANGE_MARGIN;
        }

        float baseRange = attackRange.getValue() + RANGE_MARGIN;
        float spoofDistance = reachSpoofDistance.getValue();
        float distanceToTarget = mc.player.distanceTo(target);

        if (distanceToTarget <= baseRange + spoofDistance && distanceToTarget > baseRange) {
            Vec3d playerPos = mc.player.getPos();
            Vec3d targetPos = target.getPos();
            Vec3d direction = targetPos.subtract(playerPos).normalize();

            double moveDistance = distanceToTarget - baseRange + 0.1;
            Vec3d spoofedPos = playerPos.add(direction.multiply(moveDistance));

            if (mc.player.networkHandler != null) {
                PlayerInteractionHelper.sendPacketWithOutEvent(
                        new PlayerMoveC2SPacket.PositionAndOnGround(
                                spoofedPos.x,
                                spoofedPos.y,
                                spoofedPos.z,
                                mc.player.isOnGround(),
                                mc.player.horizontalCollision));
            }

            return baseRange + spoofDistance;
        }

        return baseRange;
    }

    public float getReachSpoofDistance() {
        return reachEnabled.isValue() ? reachSpoofDistance.getValue() : 0;
    }

    public BooleanSetting getMode18() {
        return mode18;
    }

    public SliderSettings getCps() {
        return cps;
    }

    public SelectSetting getClickType() {
        return clickType;
    }
}
