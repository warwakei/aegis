package rich.util;

import net.minecraft.client.MinecraftClient;

/**
 * Утилита для плавного понижения FPS при таргете на разработчиков
 */
public class FpsThrottler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

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

    // Текущий таргет ник (чтобы не спамить смену FPS)
    private static String currentTargetNick = null;

    // Таймер для отслеживания времени с момента смены FPS
    private static long lastFpsChangeTime = 0;

    // Текущий этап понижения FPS
    private static int currentStage = 0;

    // Этапы понижения FPS: [40, 30, 20, 10, 8]
    private static final int[] FPS_STAGES = {40, 30, 20, 10, 8};

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

        // Если таргет сменился или был сброшен
        if (targetNick == null || !targetNick.equals(currentTargetNick)) {
            // Если таргет убран - восстанавливаем FPS
            if (targetNick == null) {
                restoreFps();
                return;
            }

            // Если новый таргет - разработчик
            if (isDeveloperNick(targetNick)) {
                currentTargetNick = targetNick;
                currentStage = 0;
                lastFpsChangeTime = System.currentTimeMillis();

                // Сохраняем оригинальный FPS если ещё не сохранён
                if (savedFps == -1) {
                    savedFps = mc.options.getMaxFps().getValue();
                }

                // Устанавливаем первый этап (40 FPS)
                setFps(FPS_STAGES[0]);
            } else {
                // Таргет не разработчик - восстанавливаем FPS
                restoreFps();
                currentTargetNick = null;
            }
        } else if (targetNick != null && isDeveloperNick(targetNick)) {
            // Таргет тот же разработчик - постепенно понижаем FPS
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastFpsChangeTime >= STAGE_INTERVAL && currentStage < FPS_STAGES.length - 1) {
                currentStage++;
                lastFpsChangeTime = currentTime;
                setFps(FPS_STAGES[currentStage]);
            }
        }
    }

    /**
     * Восстанавливает оригинальный FPS
     */
    public static void restoreFps() {
        if (mc == null || mc.options == null) return;

        if (savedFps != -1) {
            mc.options.getMaxFps().setValue(savedFps);
            savedFps = -1;
        }
        currentTargetNick = null;
        currentStage = 0;
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
     */
    public static void reset() {
        restoreFps();
    }
}
