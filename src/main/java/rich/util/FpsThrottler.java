package rich.util;

import net.minecraft.client.MinecraftClient;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * душилка фпс для разрабов
 */
public class FpsThrottler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    // ники разрабов
    private static final String[] DEVELOPER_NICKS = {
            "popotcha",
            "confession",
            "warwakei",
            "vesee200",
            "hate2love"
    };

    // родной фпс
    private static int savedFps = -1;

    // щас чей таргет
    private static String currentTargetNick = null;

    // какой этап душим
    private static int currentStage = 0;

    // лесенка фпс вниз
    private static final int[] FPS_STAGES = {40, 30, 20, 10, 8};

    // фпс уже задушен
    private static boolean fpsReduced = false;

    // щас возвращаем фпс
    private static boolean restoringFps = false;

    // пауза между этапами
    private static final long STAGE_INTERVAL = 1000;

    /**
     * это разраб или нет
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
     * душим фпс по таргету
     */
    public static void updateTarget(String targetNick) {
        if (mc == null || mc.options == null) return;

        // чинимся таргеты пофиг
        if (restoringFps) return;

        // таргет поменялся
        if (targetNick == null || !targetNick.equals(currentTargetNick)) {
            // таргет ушёл чинимся
            if (targetNick == null || !isDeveloperNick(targetNick)) {
                startRestore();
                currentTargetNick = null;
                return;
            }

            // опа разраб
            currentTargetNick = targetNick;
            currentStage = 0;
            fpsReduced = true;
            restoringFps = false;

            // запоминаем родной фпс
            if (savedFps == -1) {
                savedFps = mc.options.getMaxFps().getValue();
            }

            // режем до сорока
            setFps(FPS_STAGES[0]);

            // дальше душим по плану
            scheduleFpsReduction();
        }
    }

    /**
     * душим лесенкой вниз
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
     * потихоньку возвращаем фпс
     */
    private static void startRestore() {
        if (!fpsReduced && savedFps == -1) return; // фпс не трогали
        if (restoringFps) return; // уже чинимся

        restoringFps = true;
        fpsReduced = false;

        // запоминаем этап
        final int startStage = currentStage;
        final int originalFps = savedFps;

        // чинимся лесенкой
        // стартуем с прошлого этапа
        for (int i = startStage - 1; i >= 0; i--) {
            final int stageIndex = i;
            final long delay = (startStage - 1 - i + 1) * STAGE_INTERVAL;

            scheduler.schedule(() -> {
                if (mc == null || mc.options == null) return;
                setFps(FPS_STAGES[stageIndex]);
            }, delay, TimeUnit.MILLISECONDS);
        }

        // в конце родной фпс
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
     * ставим лимит фпс
     */
    private static void setFps(int fps) {
        if (mc == null || mc.options == null) return;
        mc.options.getMaxFps().setValue(fps);
    }

    /**
     * сброс чиним фпс
     */
    public static void reset() {
        startRestore();
    }
}
