package rich.modules.impl.movement;

import antidaunleak.api.annotation.Native;
import rich.IMinecraft;
import rich.events.api.EventHandler;
import rich.events.impl.KeyEvent;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BindSetting;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.Instance;
import rich.util.sounds.SoundManager;
import rich.util.timer.StopWatch;

import java.util.Random;

public class ElytraTarget extends ModuleStructure implements IMinecraft {

    public static ElytraTarget getInstance() {
        return Instance.get(ElytraTarget.class);
    }

    public SliderSettings elytraFindRange = new SliderSettings("Дистанция наводки", "Дальность поиска цели во время полета на элитре")
            .setValue(32).range(6F, 64F);

    public SliderSettings elytraForward = new SliderSettings("Значение перегона", "заебался")
            .setValue(3).range(0F, 6F);

    final BindSetting forward = new BindSetting("Кнопка вкл/выкл перегона", "");

    public final BooleanSetting doubleSneak = new BooleanSetting("Двойной шифт", "Автоматически нажимать шифт 2 раза при перекритовке для сброса спринта противника")
            .setValue(true);

    public final SliderSettings doubleSneakDistance = new SliderSettings("Дистанция шифта", "Максимальная дистанция для активации двойного шифта")
            .setValue(4.5f).range(3.0F, 6.0F);

    public final BooleanSetting antiPredict = new BooleanSetting("Анти-предсказание", "Случайное лёгкое смещение позиции чтобы враг не мог стабильно таргетить")
            .setValue(false);

    public final SliderSettings antiPredictAmount = new SliderSettings("Сила анти-предсказания", "Насколько сильно смещаться")
            .setValue(0.15f).range(0.05F, 0.4F)
            .visible(() -> antiPredict.isValue());

    public static boolean shouldElytraTarget = false;

    // Состояние двойного шифта (public для доступа из StrikeManager)
    final StopWatch sneakTimer = new StopWatch();
    int sneakCount = 0;
    boolean sneakSequenceActive = false;
    public long lastTradeHitTime = 0;
    public int consecutiveTradeHits = 0;

    public ElytraTarget() {
        super("ElytraTarget", "Elytra Target", ModuleCategory.MOVEMENT);
        settings(elytraFindRange, elytraForward, forward, doubleSneak, doubleSneakDistance, antiPredict, antiPredictAmount);
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void onEventKey(KeyEvent e) {
        if (e.isKeyDown(forward.getKey())) {
            shouldElytraTarget = !shouldElytraTarget;
            SoundManager.playSound(shouldElytraTarget ? SoundManager.MODULE_ENABLE : SoundManager.MODULE_DISABLE, 1, 1.0f);
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void onTick(TickEvent e) {
        if (!isState() || mc.player == null || mc.world == null) {
            resetSneakState();
            return;
        }

        if (!doubleSneak.isValue()) {
            resetSneakState();
            return;
        }

        // Проверяем условия для перекритовки
        rich.modules.impl.combat.Aura aura = rich.modules.impl.combat.Aura.getInstance();
        if (aura == null || rich.modules.impl.combat.Aura.target == null || !rich.modules.impl.combat.Aura.target.isAlive()) {
            resetSneakState();
            return;
        }

        double distance = mc.player.distanceTo(rich.modules.impl.combat.Aura.target);
        boolean isCloseRange = distance <= doubleSneakDistance.getValue();
        boolean bothGliding = mc.player.isGliding() && rich.modules.impl.combat.Aura.target.isGliding();

        if (!isCloseRange || !bothGliding || !shouldElytraTarget) {
            resetSneakState();
            return;
        }

        // Активируем двойной шифт после 3+ попаданий в перекритовке
        if (consecutiveTradeHits >= 3 && !sneakSequenceActive) {
            startDoubleSneak();
        }

        // Выполняем последовательность двойного шифта
        if (sneakSequenceActive) {
            executeDoubleSneak();
        }

        // Анти-предсказание — случайное лёгкое смещение позиции
        if (antiPredict.isValue() && shouldElytraTarget && mc.player.isGliding()) {
            applyAntiPrediction();
        }
    }

    private void startDoubleSneak() {
        sneakSequenceActive = true;
        sneakCount = 0;
        sneakTimer.reset();
    }

    private void executeDoubleSneak() {
        if (mc.player == null) {
            resetSneakState();
            return;
        }

        // Первый шифт
        if (sneakCount == 0 && sneakTimer.finished(0)) {
            mc.player.setSneaking(true);
            sneakCount = 1;
            sneakTimer.reset();
            return;
        }

        // Отпускаем шифт
        if (sneakCount == 1 && sneakTimer.finished(50)) {
            mc.player.setSneaking(false);
            sneakCount = 2;
            sneakTimer.reset();
            return;
        }

        // Второй шифт
        if (sneakCount == 2 && sneakTimer.finished(80)) {
            mc.player.setSneaking(true);
            sneakCount = 3;
            sneakTimer.reset();
            return;
        }

        // Отпускаем шифт второй раз — завершение
        if (sneakCount == 3 && sneakTimer.finished(130)) {
            mc.player.setSneaking(false);
            resetSneakState();
            consecutiveTradeHits = 0; // Сбрасываем счётчик
        }
    }

    private void resetSneakState() {
        sneakSequenceActive = false;
        sneakCount = 0;
        // Не сбрасываем sneakTimer здесь чтобы не прерывать текущую анимацию
    }

    /**
     * Анти-предсказание — случайное лёгкое смещение позиции
     * Делает игрока непредсказуемым для вражеского авто-таргета
     */
    private final Random antiPredictRandom = new Random();
    private long lastAntiPredictTime = 0;
    private static final long ANTI_PREDICT_INTERVAL = 200; // Обновление каждые 200мс

    private void applyAntiPrediction() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastAntiPredictTime < ANTI_PREDICT_INTERVAL) return;
        lastAntiPredictTime = currentTime;

        float amount = antiPredictAmount.getValue();

        // Случайное смещение по X и Z (очень маленькое)
        double offsetX = (antiPredictRandom.nextDouble() - 0.5) * 2 * amount;
        double offsetZ = (antiPredictRandom.nextDouble() - 0.5) * 2 * amount;
        double offsetY = (antiPredictRandom.nextDouble() - 0.5) * amount * 0.5; // Ещё меньше по Y

        // Применяем как лёгкий толчок к velocity
        mc.player.addVelocity(offsetX * 0.1, offsetY * 0.1, offsetZ * 0.1);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        resetSneakState();
        consecutiveTradeHits = 0;
        lastTradeHitTime = 0;
    }
}