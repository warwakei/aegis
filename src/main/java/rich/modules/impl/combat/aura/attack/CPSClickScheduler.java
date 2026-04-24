package rich.modules.impl.combat.aura.attack;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;

import java.util.Random;

/**
 * Менеджер CPS очереди для 1.8 режима
 * Улучшенный алгоритм для обхода античитов до 13+ CPS
 */
public class CPSClickScheduler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Random random = new Random();

    @Getter
    private int cps = 8;

    // Счётчики для очереди
    private int clicksInQueue = 0;
    private int queueSize = 0;

    // Тайминги
    private long lastClickTime = 0;
    private long lastBurstTime = 0;
    private long queueStartTime = 0;

    // Состояние
    private boolean queueActive = false;
    private int ticksInQueue = 0;
    
    // Система рандомных дропов CPS
    private long lastCpsDropTime = 0;
    private int cpsDropAmount = 0;
    private boolean inCpsDrop = false;
    private long cpsDropDuration = 0;
    
    // Человечные паттерны
    private long lastHumanPauseTime = 0;
    private boolean inHumanPause = false;
    private long humanPauseDuration = 0;

    /**
     * Обновляет CPS из настроек
     */
    public void updateCPS(int newCps) {
        this.cps = Math.max(2, Math.min(40, newCps));
    }

    /**
     * Начинает новую очередь кликов с улучшенным алгоритмом
     */
    public void startQueue(int cps) {
        // Применяем рандомные дропы CPS
        int effectiveCps = applyHumanCpsVariations(cps);
        
        this.cps = Math.max(2, Math.min(40, effectiveCps));
        this.queueSize = this.cps;
        this.clicksInQueue = 0;
        this.queueActive = true;
        this.ticksInQueue = 0;
        this.queueStartTime = System.currentTimeMillis();
        
        // Проверяем нужна ли человечная пауза
        checkForHumanPause();
    }
    
    /**
     * Применяет человечные вариации CPS (дропы на -1, -2, -3)
     */
    private int applyHumanCpsVariations(int baseCps) {
        long currentTime = System.currentTimeMillis();
        
        // Проверяем активен ли дроп
        if (inCpsDrop) {
            if (currentTime - lastCpsDropTime >= cpsDropDuration) {
                inCpsDrop = false;
                cpsDropAmount = 0;
            } else {
                return Math.max(2, baseCps - cpsDropAmount);
            }
        }
        
        // Увеличил шанс дропов и частоту (20% каждые 1.5-4 секунды)
        if (currentTime - lastCpsDropTime >= 1500 + random.nextInt(2500)) {
            if (random.nextFloat() < 0.2f) {
                // Рандомный дроп на 1-4 CPS (увеличил максимум)
                cpsDropAmount = 1 + random.nextInt(4);
                cpsDropDuration = 600 + random.nextInt(1800); // 0.6-2.4 секунды
                inCpsDrop = true;
                lastCpsDropTime = currentTime;
                return Math.max(2, baseCps - cpsDropAmount);
            }
        }
        
        return baseCps;
    }
    
    /**
     * Проверяет нужна ли человечная пауза
     */
    private void checkForHumanPause() {
        long currentTime = System.currentTimeMillis();
        
        // Шанс на микропаузу (8% каждые 3-7 секунд)
        if (currentTime - lastHumanPauseTime >= 3000 + random.nextInt(4000)) {
            if (random.nextFloat() < 0.08f) {
                humanPauseDuration = 150 + random.nextInt(300); // 150-450мс пауза
                inHumanPause = true;
                lastHumanPauseTime = currentTime;
            }
        }
    }

    /**
     * Проверяет можно ли сделать быстрый бурст клик
     */
    public boolean shouldDoFastSecondClick() {
        if (!queueActive || clicksInQueue <= 0) {
            return false;
        }
        
        // Логика: если CPS четное, выше 11 и цельное число
        if (cps > 11 && cps % 2 == 0) {
            int interval = cps / 2; // 12÷2=6, 14÷2=7, 16÷2=8, etc
            
            // Каждый N-й клик делаем даблклик, но с рандомными пропусками
            if (clicksInQueue % interval == 0) {
                // 15% шанс пропустить бурст для человечности
                if (random.nextFloat() < 0.15f) {
                    return false;
                }
                
                // Проверяем что прошло достаточно времени с последнего бурста
                long timeSinceLastBurst = System.currentTimeMillis() - lastBurstTime;
                return timeSinceLastBurst >= (40 + random.nextInt(30)); // 40-70мс рандом
            }
        }
        
        return false;
    }

    /**
     * Возвращает задержку до второго клика в бурсте (в один тик)
     */
    public int getSecondClickDelay() {
        // Для даблклика в один тик - рандомная задержка с микровариациями
        int baseDelay = 1 + random.nextInt(4); // 1-4мс базовая
        
        // Иногда добавляем микрозадержку для реализма (20% шанс)
        if (random.nextFloat() < 0.2f) {
            baseDelay += random.nextInt(3); // +0-2мс
        }
        
        return baseDelay;
    }

    /**
     * Отмечает что быстрый клик был использован
     */
    public void useFastClick() {
        lastBurstTime = System.currentTimeMillis();
    }

    /**
     * Улучшенная проверка можно ли кликать с адаптивными интервалами
     */
    public boolean shouldClick() {
        if (!queueActive || mc.player == null) {
            return false;
        }
        
        // Проверяем человечную паузу
        if (inHumanPause) {
            if (System.currentTimeMillis() - lastHumanPauseTime >= humanPauseDuration) {
                inHumanPause = false;
            } else {
                return false; // Блокируем клики во время паузы
            }
        }

        long elapsed = System.currentTimeMillis() - queueStartTime;
        double expectedClicks = (elapsed / 1000.0) * cps;

        // Если мы отстаём от графика - можно кликать
        boolean shouldClick = clicksInQueue < expectedClicks;

        // Адаптивный минимальный интервал с большей рандомизацией
        long timeSinceLastClick = System.currentTimeMillis() - lastClickTime;
        long minInterval = calculateHumanInterval();

        if (timeSinceLastClick < minInterval) {
            shouldClick = false;
        }

        return shouldClick;
    }

    /**
     * Вычисляет человечный интервал между кликами с большей рандомизацией
     */
    private long calculateHumanInterval() {
        // Базовый интервал зависит от CPS
        long baseInterval = 1000 / cps;
        
        // Более агрессивная рандомизация ±40%
        double randomFactor = 0.6 + (random.nextDouble() * 0.8);
        
        // Добавляем микроджиттеры
        int microJitter = random.nextInt(15) - 7; // ±7мс
        
        long adaptiveInterval = (long) (baseInterval * randomFactor) + microJitter;
        
        // Иногда добавляем случайные задержки (5% шанс)
        if (random.nextFloat() < 0.05f) {
            adaptiveInterval += 20 + random.nextInt(40); // +20-60мс
        }
        
        // Минимум 15мс для предотвращения детекции
        return Math.max(15, adaptiveInterval);
    }

    /**
     * Регистрирует сделанный клик
     */
    public void registerClick(boolean isFastClick) {
        clicksInQueue++;
        lastClickTime = System.currentTimeMillis();
        ticksInQueue++;

        // Проверяем окончание очереди
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
     * Сбрасывает состояние
     */
    public void reset() {
        endQueue();
        lastClickTime = 0;
        lastBurstTime = 0;
        queueStartTime = 0;
        ticksInQueue = 0;
        
        // Сбрасываем человечные паттерны
        lastCpsDropTime = 0;
        cpsDropAmount = 0;
        inCpsDrop = false;
        cpsDropDuration = 0;
        lastHumanPauseTime = 0;
        inHumanPause = false;
        humanPauseDuration = 0;
    }

    /**
     * Возвращает количество кликов сделанных в текущей очереди
     */
    public int getClicksMade() {
        return clicksInQueue;
    }

    /**
     * Возвращает количество оставшихся кликов в очереди
     */
    public int getClicksRemaining() {
        return Math.max(0, queueSize - clicksInQueue);
    }

    /**
     * Проверяет активна ли очередь
     */
    public boolean isQueueActive() {
        return queueActive;
    }

    /**
     * Возвращает прогресс очереди (0.0 - 1.0)
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
