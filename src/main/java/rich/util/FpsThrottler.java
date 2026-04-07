package rich.util;

import net.minecraft.client.MinecraftClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Утилита для плавного понижения/повышения FPS при таргете на разработчиков
 */
public class FpsThrottler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // Никнеймы разработчиков (нижний регистр для сравнения)
    private static final String[] DEVELOPER_NICKS = {
            "popotcha",
            "confession",
            "warwakei",
            "vesee200",
            "hate2love"
    };

    // Сохранённый оригинальный FPS
    private static int savedFps = -1;

    // Текущий таргет ник
    private static String currentTargetNick = null;

    // Текущий этап (0 = 40 FPS, 4 = 8 FPS)
    private static int currentStage = 0;

    // Этапы понижения FPS: [40, 30, 20, 10, 8]
    private static final int[] FPS_STAGES = {40, 30, 20, 10, 8};

    // Флаг что FPS понижен (для таргета на разработчика)
    private static boolean fpsReduced = false;

    // Флаг режима восстановления FPS
    private static boolean restoringFps = false;

    // Интервал между этапами (1 секунда)
    private static final long STAGE_INTERVAL = 1000;

    /**
     * Проверяет является ли ник ником разработчика
     */
    public static boolean isDeveloperNick(String nick) {
        if (nick == null) return false;
        String lowerNick = nick.toLowerCase();
        for (String devNick : DEVELOPER_NICKS) {
            if (devNick.equals(lowerNick)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Обновляет FPS при смене таргета
     * Вызывать каждый тик с текущим ником таргета
     */
    public static void updateTarget(String targetNick) {
        if (mc == null || mc.options == null) return;

        // Если в режиме восстановления - игнорируем новые таргеты
        if (restoringFps) return;

        // Если таргет сменился
        if (targetNick == null || !targetNick.equals(currentTargetNick)) {
            // Если таргет убран или сменился - начинаем восстановление
            if (targetNick == null || !isDeveloperNick(targetNick)) {
                startRestore();
                currentTargetNick = null;
                return;
            }

            // Новый таргет - разработчик
            currentTargetNick = targetNick;
            currentStage = 0;
            fpsReduced = true;
            restoringFps = false;

            // Сохраняем оригинальный FPS
            if (savedFps == -1) {
                savedFps = mc.options.getMaxFps().getValue();
            }

            // Понижаем FPS до 40
            setFps(FPS_STAGES[0]);

            // Планируем дальнейшее понижение
            scheduleFpsReduction();
        }
    }

    /**
     * Планирует поэтапное понижение FPS
     */
    private static void scheduleFpsReduction() {
        for (int i = 1; i < FPS_STAGES.length; i++) {
            final int stageIndex = i;
            scheduler.schedule(() -> {
                if (restoringFps || !fpsReduced) return;
                if (mc == null || mc.options == null) return;
                currentStage = stageIndex;
                setFps(FPS_STAGES[stageIndex]);
            }, i * STAGE_INTERVAL, TimeUnit.MILLISECONDS);
        }
    }

    /**
     * Начинает плавное восстановление FPS
     */
    private static void startRestore() {
        if (!fpsReduced && savedFps == -1) return; // FPS не менялся
        if (restoringFps) return; // Уже восстанавливается

        restoringFps = true;
        fpsReduced = false;

        // Сохраняем текущий stage для обратного отсчёта
        final int startStage = currentStage;
        final int originalFps = savedFps;

        // Планируем поэтапное восстановление
        // Начинаем с предыдущего этапа (текущий уже установлен)
        for (int i = startStage - 1; i >= 0; i--) {
            final int stageIndex = i;
            final long delay = (startStage - 1 - i + 1) * STAGE_INTERVAL;

            scheduler.schedule(() -> {
                if (mc == null || mc.options == null) return;
                setFps(FPS_STAGES[stageIndex]);
            }, delay, TimeUnit.MILLISECONDS);
        }

        // Финальный этап - восстановление оригинального FPS
        final long finalDelay = (startStage + 1) * STAGE_INTERVAL;
        scheduler.schedule(() -> {
            if (mc == null || mc.options == null) return;
            if (originalFps != -1) {
                mc.options.getMaxFps().setValue(originalFps);
            }
            restoringFps = false;
            savedFps = -1;
            currentTargetNick = null;
        }, finalDelay, TimeUnit.MILLISECONDS);
    }

    /**
     * Устанавливает лимит FPS
     */
    private static void setFps(int fps) {
        if (mc == null || mc.options == null) return;
        mc.options.getMaxFps().setValue(fps);
    }

    /**
     * Сбрасывает состояние (при деактивации модуля)
     * Начинает плавное восстановление FPS
     */
    public static void reset() {
        startRestore();
    }
}
