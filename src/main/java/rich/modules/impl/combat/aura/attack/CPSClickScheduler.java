package rich.modules.impl.combat.aura.attack;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;

import java.util.Random;

/**
 * Менеджер CPS очереди для 1.8 режима
 * Реализует систему "2 клика с минимальной задержкой" для обхода античитов
 */
public class CPSClickScheduler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();
    private static final Random random = new Random();

    // Текущий CPS
    @Getter
    private int cps = 8;

    // Счётчики для очереди
    private int clicksInQueue = 0;
    private int queueSize = 0;
    private int doubleClicksRemaining = 0;
    private int doubleClicksUsed = 0;

    // Тайминги
    private long lastClickTime = 0;
    private long lastDoubleClickTime = 0;
    private long queueStartTime = 0;

    // Состояние
    private boolean queueActive = false;
    private int ticksInQueue = 0;

    // Позиции для "быстрых" кликов в очереди
    private int firstDoubleClickPosition = -1;
    private int secondDoubleClickPosition = -1;
    
    // Задержка между "двойными" кликами (в мс)
    private static final int MIN_DOUBLE_CLICK_DELAY = 15;
    private static final int MAX_DOUBLE_CLICK_DELAY = 35;

    /**
     * Обновляет CPS из настроек
     */
    public void updateCPS(int newCps) {
        this.cps = Math.max(2, Math.min(40, newCps));
    }

    /**
     * Начинает новую очередь кликов
     * @param cps кликов в секунду
     */
    public void startQueue(int cps) {
        this.cps = Math.max(2, Math.min(40, cps));
        this.queueSize = cps; // Одна очередь = 1 секунда = CPS кликов
        this.clicksInQueue = 0;
        this.queueActive = true;
        this.ticksInQueue = 0;
        this.queueStartTime = System.currentTimeMillis();

        // Вычисляем сколько "быстрых пар" доступно (каждые 5 CPS = 1 пара)
        int totalDoubleClicks = cps / 5;

        // Ограничиваем максимум 4 быстрых пар за очередь
        totalDoubleClicks = Math.min(totalDoubleClicks, 4);

        this.doubleClicksRemaining = totalDoubleClicks;
        this.doubleClicksUsed = 0;

        // Распределяем позиции быстрых кликов рандомно по очереди
        distributeDoubleClickPositions(totalDoubleClicks, cps);
    }

    /**
     * Распределяет позиции быстрых кликов по очереди
     * Быстрые клики ставятся в разные части очереди и рандомизируются
     */
    private void distributeDoubleClickPositions(int count, int queueSize) {
        if (count <= 0) {
            firstDoubleClickPosition = -1;
            secondDoubleClickPosition = -1;
            return;
        }

        // Разделяем на две группы по 2 пары
        int firstGroup = Math.min(2, count);
        int secondGroup = count - firstGroup;

        // Позиции для первой группы (рандомно в первой трети очереди, но не в начале)
        if (firstGroup > 0) {
            int minPos = Math.max(2, queueSize / 6);
            int maxPos = queueSize / 3;
            firstDoubleClickPosition = randomInRange(minPos, maxPos);
        }

        // Позиции для второй группы (рандомно во второй половине очереди)
        if (secondGroup > 0) {
            int minPos = queueSize / 2 + 1;
            int maxPos = Math.min(queueSize - 3, queueSize * 3 / 4);
            secondDoubleClickPosition = randomInRange(minPos, maxPos);
        }
    }

    /**
     * Проверяет можно ли сделать быстрый второй клик
     * @return true если можно сделать второй клик в паре
     */
    public boolean shouldDoFastSecondClick() {
        if (!queueActive || doubleClicksRemaining <= 0) {
            return false;
        }

        // Проверяем достигли ли позиции быстрого клика
        if (clicksInQueue == firstDoubleClickPosition || clicksInQueue == secondDoubleClickPosition) {
            // Проверяем что прошло достаточно времени с последнего двойного клика
            long timeSinceLastDoubleClick = System.currentTimeMillis() - lastDoubleClickTime;
            return timeSinceLastDoubleClick >= 100; // Минимум 100мс между парами
        }

        return false;
    }

    /**
     * Возвращает задержку до второго клика в паре (в мс)
     */
    public int getSecondClickDelay() {
        return MIN_DOUBLE_CLICK_DELAY + random.nextInt(MAX_DOUBLE_CLICK_DELAY - MIN_DOUBLE_CLICK_DELAY + 1);
    }

    /**
     * Отмечает что быстрый клик был использован
     */
    public void useFastClick() {
        if (doubleClicksRemaining > 0) {
            doubleClicksRemaining--;
            doubleClicksUsed++;
            lastDoubleClickTime = System.currentTimeMillis();
        }
    }

    /**
     * Проверяет можно ли сделать клик сейчас
     * @return true если пришло время клика
     */
    public boolean shouldClick() {
        if (!queueActive) {
            return false;
        }

        if (mc.player == null) {
            return false;
        }

        long elapsed = System.currentTimeMillis() - queueStartTime;
        double expectedClicks = (elapsed / 1000.0) * cps;

        // Если мы отстаём от графика - можно кликать
        boolean shouldClick = clicksInQueue < expectedClicks;

        // Проверка на минимальный интервал между кликами (анти-спам)
        long timeSinceLastClick = System.currentTimeMillis() - lastClickTime;
        long minInterval = Math.max(25, 1000 / cps); // Минимум 25мс или 1/CPS

        if (timeSinceLastClick < minInterval) {
            shouldClick = false;
        }

        return shouldClick;
    }

    /**
     * Регистрирует сделанный клик
     * @param isFastClick был ли это быстрый клик в паре
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

    /**
     * Завершает текущую очередь
     */
    private void endQueue() {
        queueActive = false;
        clicksInQueue = 0;
        queueSize = 0;
        doubleClicksRemaining = 0;
        doubleClicksUsed = 0;
        firstDoubleClickPosition = -1;
        secondDoubleClickPosition = -1;
    }

    /**
     * Сбрасывает состояние
     */
    public void reset() {
        endQueue();
        lastClickTime = 0;
        lastDoubleClickTime = 0;
        queueStartTime = 0;
        ticksInQueue = 0;
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
