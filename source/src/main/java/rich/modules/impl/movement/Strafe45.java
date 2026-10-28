package rich.modules.impl.movement;

import antidaunleak.api.annotation.Native;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.Instance;

import java.util.concurrent.ThreadLocalRandom;

/**
 * сайлент стрейф на 45 градусов
 * камера не дёргаеца магия
 * пакеты шлём сервер хавает
 */
public class Strafe45 extends ModuleStructure {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // поля читает миксин не трогать
    private boolean strafePending = false;
    private float strafeYaw = 0;
    private float strafePitch = 0;

    private boolean wasOnGround = true;
    private boolean isJumping = false;
    private int strafeCount = 0;
    private boolean strafeLeft = true;
    private long lastInterferenceTime = 0;
    private static final long INTERFERENCE_COOLDOWN = 280; // мс

    // флаг буста чекает миксин
    private boolean timerBoostActive = false;
    private boolean timerBoostNextTick = false; // буст на след тик

    // настройки
    private final SliderSettings strafesPerJump = new SliderSettings("Стрейфов за прыжок", "Количество 45° стрейфов за один прыжок")
            .range(1.0f, 5.0f)
            .setValue(3.0f);

    private final BooleanSetting timerBoost = new BooleanSetting("Timer Boost", "Ускорение тиков в момент стрейфа")
            .setValue(false);

    private final SliderSettings timerBoostAmount = new SliderSettings("Сила буста", "Множитель ускорения (0.1-1.3%)")
            .range(0.1f, 1.3f)
            .setValue(0.2f)
            .visible(() -> timerBoost.isValue());

    private final BooleanSetting timerBoostChain = new BooleanSetting("Chain Boost", "15% шанс буста следующего тика (x2)")
            .setValue(false)
            .visible(() -> timerBoost.isValue());

    public Strafe45() {
        super("45SP", "Silent 45° Strafe passive speed", ModuleCategory.MOVEMENT);
        settings(strafesPerJump, timerBoost, timerBoostAmount, timerBoostChain);
    }

    public static Strafe45 getInstance() {
        return Instance.get(Strafe45.class);
    }

    public boolean isStrafePending() { return strafePending; }
    public float getStrafeYaw()     { return strafeYaw; }
    public float getStrafePitch()   { return strafePitch; }
    public boolean isTimerBoostActive() { return timerBoostActive; }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        super.activate();
        wasOnGround = mc.player != null && mc.player.isOnGround();
        resetState();
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        super.deactivate();
        resetState();
    }

    private void resetState() {
        isJumping = false;
        strafeCount = 0;
        strafePending = false;
        timerBoostActive = false;
        timerBoostNextTick = false;
    }

    /**
     * считаем фазы стрейфов
     * хз как но работает
     */
    @Native(type = Native.Type.VMProtectBeginUltra)
    private double[] generatePhases(int count, boolean hasCeiling) {
        double[] phases = new double[count];
        double ceilingFactor = hasCeiling ? 0.7 : 1.0;

        for (int i = 0; i < count; i++) {
            // раскидываем ровно по дуге
            phases[i] = ((i + 1.0) / (count + 1.0)) * ceilingFactor;
        }

        return phases;
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        // чистим флаг каждый тик
        strafePending = false;
        timerBoostActive = false;

        // чейн буст с прошлого тика
        if (timerBoostNextTick) {
            timerBoostActive = true;
            timerBoostNextTick = false;
            applyTimerBoost(true); // двойной буст щас
        }

        boolean onGround = mc.player.isOnGround();
        boolean holdingW = mc.options.forwardKey.isPressed();
        boolean holdingSpace = mc.options.jumpKey.isPressed();
        boolean holdingA = mc.options.leftKey.isPressed();
        boolean holdingD = mc.options.rightKey.isPressed();

        // жмут а/д блочим на чутка
        if (holdingW && holdingSpace && (holdingA || holdingD)) {
            lastInterferenceTime = System.currentTimeMillis();
            resetState();
            return;
        }

        // кд после блочки
        if (System.currentTimeMillis() - lastInterferenceTime < INTERFERENCE_COOLDOWN) {
            resetState();
            return;
        }

        // только если жмём в
        if (!holdingW) {
            resetState();
            wasOnGround = onGround;
            return;
        }

        // автопрыг без задержки
        if (holdingSpace && onGround) {
            mc.player.jump();
            return;
        }

        // чекаем отрыв от земли
        if (wasOnGround && !onGround) {
            isJumping = true;
            strafeCount = 0;
            strafeLeft = true;
        }

        // стрейфим пока летим
        if (isJumping && !onGround) {
            double progress = getJumpProgress();
            boolean hasCeiling = isBlockAbove();

            int strafeCountSetting = (int) strafesPerJump.getValue();
            double[] phases = generatePhases(strafeCountSetting, hasCeiling);

            if (strafeCount < strafeCountSetting && progress >= phases[strafeCount]) {
                executeStrafe();
                strafeCount++;
            }
        }

        // приземлились
        if (!wasOnGround && onGround) {
            resetState();
        }

        wasOnGround = onGround;
    }

    /**
     * ставим стрейф в ожидание
     * тело и камера не паляца
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void executeStrafe() {
        if (mc.player == null) return;

        float baseYaw = mc.player.getYaw();
        float basePitch = mc.player.getPitch();

        // косим на 45 туда сюда
        strafeYaw = baseYaw + (strafeLeft ? 45.0f : -45.0f);
        strafePitch = basePitch;
        strafeLeft = !strafeLeft;
        strafePending = true;

        // толкаем в повёрнутую сторону
        double strafeRad = Math.toRadians(strafeYaw);
        double speed = 0.28;
        mc.player.setVelocity(
                -Math.sin(strafeRad) * speed,
                mc.player.getVelocity().y,
                Math.cos(strafeRad) * speed
        );

        // врубаем буст на тик
        if (timerBoost.isValue()) {
            timerBoostActive = true;
            applyTimerBoost();
        }
    }

    /**
     * бустим скорость тупо
     * @param doubled если тру то x2
     */
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void applyTimerBoost() {
        applyTimerBoost(false);
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void applyTimerBoost(boolean doubled) {
        float boostRange = timerBoostAmount.getValue(); // от 0.1 до 1.3

        // чейн то умножаем на два
        float effectiveBoost = doubled ? boostRange * 2.0f : boostRange;

        // рандомим чтоб не палилось
        float boost = effectiveBoost * (0.5f + ThreadLocalRandom.current().nextFloat() * 0.5f);

        // пихаем буст в скорость
        double velX = mc.player.getVelocity().x;
        double velZ = mc.player.getVelocity().z;
        double boostMultiplier = 1.0 + (boost / 100.0);

        mc.player.setVelocity(
                velX * boostMultiplier,
                mc.player.getVelocity().y,
                velZ * boostMultiplier
        );

        // 15 проц чейн на след тик
        if (!doubled && timerBoostChain.isValue() && !timerBoostNextTick) {
            if (ThreadLocalRandom.current().nextDouble(1.0) < 0.15) {
                timerBoostNextTick = true;
            }
        }
    }

    /**
     * прогресс прыжка по велосити
     * хз как но работает
     */
    @Native(type = Native.Type.VMProtectBeginUltra)
    private double getJumpProgress() {
        double velY = mc.player.getVelocity().y;

        if (velY > 0.38) return 0.08;
        if (velY > 0.30) return 0.18;
        if (velY > 0.20) return 0.28;
        if (velY > 0.10) return 0.38;
        if (velY > 0.0)  return 0.45;
        if (velY > -0.1) return 0.55;
        if (velY > -0.2) return 0.65;
        if (velY > -0.3) return 0.78;
        if (velY > -0.4) return 0.90;
        return 0.98;
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private boolean isBlockAbove() {
        if (mc.world == null || mc.player == null) return false;
        BlockPos head = mc.player.getBlockPos().up(1);
        BlockPos above = mc.player.getBlockPos().up(2);
        return mc.world.getBlockState(head).isSolid() ||
                mc.world.getBlockState(above).isSolid();
    }
}
