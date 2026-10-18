package rich.modules.impl.combat.aura.attack;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;

import java.util.Random;

/**
 * цпс очередь для 1.8
 * костыль под античит до 13+ цпс
 */
public class CPSClickScheduler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Random random = new Random();

    @Getter
    private int cps = 8;

    // счет очередь
    private int clicksInQueue = 0;
    private int queueSize = 0;

    // тайминги
    private long lastClickTime = 0;
    private long lastBurstTime = 0;
    private long queueStartTime = 0;

    // статус
    private boolean queueActive = false;
    private int ticksInQueue = 0;
    
    // рандом дропы цпс
    private long lastCpsDropTime = 0;
    private int cpsDropAmount = 0;
    private boolean inCpsDrop = false;
    private long cpsDropDuration = 0;
    
    // типа человек паттерны
    private long lastHumanPauseTime = 0;
    private boolean inHumanPause = false;
    private long humanPauseDuration = 0;

    /**
     * цпс из настроек
     */
    public void updateCPS(int newCps) {
        this.cps = Math.max(2, Math.min(40, newCps));
    }

    /**
     * новая очередь кликов
     */
    public void startQueue(int cps) {
        // дропы цпс под чела
        int effectiveCps = applyHumanCpsVariations(cps);
        
        this.cps = Math.max(2, Math.min(40, effectiveCps));
        this.queueSize = this.cps;
        this.clicksInQueue = 0;
        this.queueActive = true;
        this.ticksInQueue = 0;
        this.queueStartTime = System.currentTimeMillis();
        
        // чек пауза нужна ли
        checkForHumanPause();
    }
    
    /**
     * дропы цпс -1 -2 -3
     */
    private int applyHumanCpsVariations(int baseCps) {
        long currentTime = System.currentTimeMillis();
        
        // чек дроп активен ли
        if (inCpsDrop) {
            if (currentTime - lastCpsDropTime >= cpsDropDuration) {
                inCpsDrop = false;
                cpsDropAmount = 0;
            } else {
                return Math.max(2, baseCps - cpsDropAmount);
            }
        }
        
        // дропы чаще 20 проц
        if (currentTime - lastCpsDropTime >= 1500 + random.nextInt(2500)) {
            if (random.nextFloat() < 0.2f) {
                // дроп на 1-4 цпс
                cpsDropAmount = 1 + random.nextInt(4);
                cpsDropDuration = 600 + random.nextInt(1800); // 0.6-2.4 сек
                inCpsDrop = true;
                lastCpsDropTime = currentTime;
                return Math.max(2, baseCps - cpsDropAmount);
            }
        }
        
        return baseCps;
    }
    
    /**
     * чек микропауза нужна ли
     */
    private void checkForHumanPause() {
        long currentTime = System.currentTimeMillis();
        
        // шанс микропаузы 8 проц
        if (currentTime - lastHumanPauseTime >= 3000 + random.nextInt(4000)) {
            if (random.nextFloat() < 0.08f) {
                humanPauseDuration = 150 + random.nextInt(300); // 150-450мс стоим
                inHumanPause = true;
                lastHumanPauseTime = currentTime;
            }
        }
    }

    /**
     * можно ли даблклик бурст
     */
    public boolean shouldDoFastSecondClick() {
        if (!queueActive || clicksInQueue <= 0) {
            return false;
        }
        
        // логика чет цпс выше 11
        if (cps > 11 && cps % 2 == 0) {
            int interval = cps / 2; // 12 делим 6 и тд
            
            // каждый N даблклик рандом скип
            if (clicksInQueue % interval == 0) {
                // 15 проц скип для человечности
                if (random.nextFloat() < 0.15f) {
                    return false;
                }
                
                // чек прошло ли время с бурста
                long timeSinceLastBurst = System.currentTimeMillis() - lastBurstTime;
                return timeSinceLastBurst >= (40 + random.nextInt(30)); // 40-70мс рандом
            }
        }
        
        return false;
    }

    /**
     * задержка второго клика в тике
     */
    public int getSecondClickDelay() {
        // даблклик в тик рандом микрозадержка
        int baseDelay = 1 + random.nextInt(4); // 1-4мс база
        
        // иногда микро задержка для реализма
        if (random.nextFloat() < 0.2f) {
            baseDelay += random.nextInt(3); // плюс 0-2мс
        }
        
        return baseDelay;
    }

    /**
     * фастклик юзнули
     */
    public void useFastClick() {
        lastBurstTime = System.currentTimeMillis();
    }

    /**
     * можно ли кликать адаптив
     */
    public boolean shouldClick() {
        if (!queueActive || mc.player == null) {
            return false;
        }
        
        // чек пауза чела
        if (inHumanPause) {
            if (System.currentTimeMillis() - lastHumanPauseTime >= humanPauseDuration) {
                inHumanPause = false;
            } else {
                return false; // пауза клики стоп
            }
        }

        long elapsed = System.currentTimeMillis() - queueStartTime;
        double expectedClicks = (elapsed / 1000.0) * cps;

        // отстали от графика жмем
        boolean shouldClick = clicksInQueue < expectedClicks;

        // интервал с рандомом побольше
        long timeSinceLastClick = System.currentTimeMillis() - lastClickTime;
        long minInterval = calculateHumanInterval();

        if (timeSinceLastClick < minInterval) {
            shouldClick = false;
        }

        return shouldClick;
    }

    /**
     * интервал как у чела рандом
     */
    private long calculateHumanInterval() {
        // база от цпс
        long baseInterval = 1000 / cps;
        
        // рандом пожестче плюс минус 40
        double randomFactor = 0.6 + (random.nextDouble() * 0.8);
        
        // микроджиттер
        int microJitter = random.nextInt(15) - 7; // плюс минус 7мс
        
        long adaptiveInterval = (long) (baseInterval * randomFactor) + microJitter;
        
        // иногда рандом задержка 5 проц
        if (random.nextFloat() < 0.05f) {
            adaptiveInterval += 20 + random.nextInt(40); // плюс 20-60мс
        }
        
        // минимум 15мс чтоб не палить
        return Math.max(15, adaptiveInterval);
    }

    /**
     * клик засчитан
     */
    public void registerClick(boolean isFastClick) {
        clicksInQueue++;
        lastClickTime = System.currentTimeMillis();
        ticksInQueue++;

        // чек очередь кончилась
        if (clicksInQueue >= queueSize) {
            endQueue();
        }
    }

    private void endQueue() {
        queueActive = false;
        clicksInQueue = 0;
        queueSize = 0;
    }

    /**
     * сброс всего
     */
    public void reset() {
        endQueue();
        lastClickTime = 0;
        lastBurstTime = 0;
        queueStartTime = 0;
        ticksInQueue = 0;
        
        // сброс паттернов чела
        lastCpsDropTime = 0;
        cpsDropAmount = 0;
        inCpsDrop = false;
        cpsDropDuration = 0;
        lastHumanPauseTime = 0;
        inHumanPause = false;
        humanPauseDuration = 0;
    }

    /**
     * скока кликнули в очереди
     */
    public int getClicksMade() {
        return clicksInQueue;
    }

    /**
     * скока осталось кликнуть
     */
    public int getClicksRemaining() {
        return Math.max(0, queueSize - clicksInQueue);
    }

    /**
     * очередь жива или нет
     */
    public boolean isQueueActive() {
        return queueActive;
    }

    /**
     * прогресс очереди 0-1
     */
    public double getQueueProgress() {
        if (queueSize <= 0) return 0.0;
        return Math.min(1.0, (double) clicksInQueue / queueSize);
    }

    private int randomInRange(int min, int max) {
        if (min >= max) return min;
        return min + random.nextInt(max - min + 1);
    }
}
