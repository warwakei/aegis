package rich.modules.impl.combat.aura.mace;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import rich.util.inventory.InventoryUtils;

/**
 * Silent Mace Handler для Aura
 * 
 * Логика работы:
 * - Отслеживает fallDistance >= 9 блоков
 * - Находит булаву в хотбаре или инвентаре
 * - Свапает булаву в слот → атакует → возвращает меч
 * 
 * Тайминги (по тикам):
 * - Тик 0: обнаружено падение, найден слот булавы
 * - Тик 1: свап булавы в слот (3 клика PICKUP)
 * - Тик 2: атака булавой (пакет)
 * - Тик 3: возврат меча обратно
 */
public class SilentMaceHandler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // Минимальная высота падения для активации
    private static final double MIN_FALL_DISTANCE = 9.0;

    @Getter
    private MaceState state = MaceState.IDLE;

    private int maceInventorySlot = -1;    // Слот булавы в инвентаре (9-35)
    private int maceHotbarSlot = -1;       // Слот булавы в хотбаре (0-8)
    private int originalSlot = -1;         // Оригинальный слот игрока
    private int currentHeldSlot = -1;      // Текущий слот в руке (для возврата)

    private long stateStartTime = 0;
    private LivingEntity target = null;

    // Запоминанный ник для проверки разработчика
    private boolean wasDeveloperTarget = false;

    /**
     * Состояния автомата
     */
    public enum MaceState {
        IDLE,           // Ожидание
        SWAPPING,       // Свап булавы в слот
        ATTACKING,      // Атака булавой
        RESTORING       // Возврат меча
    }

    /**
     * Вызывается каждый тик из Aura
     */
    public void onTick(LivingEntity currentTarget) {
        if (mc.player == null || mc.world == null) {
            reset();
            return;
        }

        switch (state) {
            case IDLE -> handleIdle(currentTarget);
            case SWAPPING -> handleSwapping();
            case ATTACKING -> handleAttacking();
            case RESTORING -> handleRestoring();
        }
    }

    /**
     * Состояние IDLE: проверяем нужно ли активировать булаву
     */
    private void handleIdle(LivingEntity currentTarget) {
        // Проверяем условия для активации
        if (currentTarget == null || !currentTarget.isAlive()) {
            return;
        }

        // Проверяем высоту падения
        double fallDistance = mc.player.fallDistance;
        if (fallDistance < MIN_FALL_DISTANCE) {
            return;
        }

        // Проверяем что игрок НЕ на земле (падает)
        if (mc.player.isOnGround()) {
            return;
        }

        // Проверяем дистанцию до цели
        float distance = mc.player.distanceTo(currentTarget);
        if (distance > 6.0f) {
            return; // Цель слишком далеко
        }

        // Проверяем что в руке НЕ булава
        ItemStack mainHand = mc.player.getMainHandStack();
        if (mainHand.getItem() == Items.MACE) {
            return; // Уже с булавой, не нужно свапать
        }

        // Ищем булаву
        int hotbarMace = InventoryUtils.findHotbarItem(Items.MACE);
        int invMace = -1;

        if (hotbarMace == -1) {
            // Булавы нет в хотбаре — ищем в инвентаре
            for (int i = 9; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.getItem() == Items.MACE) {
                    invMace = i;
                    break;
                }
            }
        }

        // Если булавы нет нигде — отмена
        if (hotbarMace == -1 && invMace == -1) {
            return;
        }

        // Инициализируем свап
        this.target = currentTarget;
        this.originalSlot = mc.player.getInventory().getSelectedSlot();
        this.currentHeldSlot = originalSlot;
        this.maceHotbarSlot = hotbarMace;
        this.maceInventorySlot = invMace;
        this.wasDeveloperTarget = true;

        // Переходим в состояние свапа
        state = MaceState.SWAPPING;
        stateStartTime = System.currentTimeMillis();
    }

    /**
     * Состояние SWAPPING: свапаем булаву в слот
     */
    private void handleSwapping() {
        // Ждём 1 тик (50ms) перед свапом чтобы убедиться что всё стабильно
        long elapsed = System.currentTimeMillis() - stateStartTime;
        if (elapsed < 50) {
            return;
        }

        if (mc.player == null) {
            reset();
            return;
        }

        // Если булава уже в хотбаре — просто свитчим
        if (maceHotbarSlot != -1) {
            mc.player.getInventory().setSelectedSlot(maceHotbarSlot);
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(maceHotbarSlot));
        } else if (maceInventorySlot != -1) {
            // Булава в инвентаре — делаем silent swap
            int wrappedSlot = InventoryUtils.wrapSlot(maceInventorySlot);
            int targetHotbarSlot = originalSlot + 36; // Целевой слот хотбара в экранных координатах

            // 3 клика PICKUP для свапа
            InventoryUtils.click(wrappedSlot, 0, SlotActionType.PICKUP);   // взяли булаву
            InventoryUtils.click(targetHotbarSlot, 0, SlotActionType.PICKUP); // положили в хотбар
            InventoryUtils.click(wrappedSlot, 0, SlotActionType.PICKUP);   // вернули остаток

            InventoryUtils.closeScreen();

            // Теперь свитчим на этот слот
            mc.player.getInventory().setSelectedSlot(originalSlot);
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
        }

        state = MaceState.ATTACKING;
        stateStartTime = System.currentTimeMillis();
    }

    /**
     * Состояние ATTACKING: атакуем булавой
     */
    private void handleAttacking() {
        // Ждём 1 тик (50ms) чтобы свап успел примениться
        long elapsed = System.currentTimeMillis() - stateStartTime;
        if (elapsed < 50) {
            return;
        }

        if (mc.player == null || target == null || !target.isAlive()) {
            // Цель пропала — возвращаем меч
            state = MaceState.RESTORING;
            stateStartTime = System.currentTimeMillis();
            return;
        }

        // Проверяем что всё ещё падаем
        if (mc.player.isOnGround()) {
            // Уже на земле — всё равно атакуем если цель рядом
        }

        // Атакуем булавой
        performMaceAttack(target);

        // Переходим к возврату
        state = MaceState.RESTORING;
        stateStartTime = System.currentTimeMillis();
    }

    /**
     * Выполняет атаку булавой
     */
    private void performMaceAttack(LivingEntity target) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        // Отправляем пакет атаки
        mc.getNetworkHandler().sendPacket(
                PlayerInteractEntityC2SPacket.attack((Entity) target, mc.player.isSneaking())
        );

        // Анимация руки
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    /**
     * Состояние RESTORING: возвращаем меч обратно
     */
    private void handleRestoring() {
        // Ждём 1 тик (50ms) чтобы атака ушла на сервер
        long elapsed = System.currentTimeMillis() - stateStartTime;
        if (elapsed < 50) {
            return;
        }

        if (mc.player == null) {
            reset();
            return;
        }

        // Если булава была в инвентаре — нужно вернуть всё обратно
        if (maceInventorySlot != -1) {
            int wrappedSlot = InventoryUtils.wrapSlot(maceInventorySlot);
            int sourceHotbarSlot = originalSlot + 36;

            // Обратный свап
            InventoryUtils.click(sourceHotbarSlot, 0, SlotActionType.PICKUP);
            InventoryUtils.click(wrappedSlot, 0, SlotActionType.PICKUP);
            InventoryUtils.click(sourceHotbarSlot, 0, SlotActionType.PICKUP);

            InventoryUtils.closeScreen();
        }

        // Возвращаем оригинальный слот
        mc.player.getInventory().setSelectedSlot(originalSlot);
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));

        // Сброс
        reset();
    }

    /**
     * Сброс состояния
     */
    public void reset() {
        state = MaceState.IDLE;
        maceInventorySlot = -1;
        maceHotbarSlot = -1;
        originalSlot = -1;
        currentHeldSlot = -1;
        target = null;
        wasDeveloperTarget = false;
        stateStartTime = 0;
    }

    /**
     * Возвращает true если сейчас активен процесс свапа булавы
     */
    public boolean isActive() {
        return state != MaceState.IDLE;
    }

    /**
     * Принудительный сброс (при деактивации Aura)
     */
    public void forceReset() {
        if (state != MaceState.IDLE && mc.player != null) {
            // Возвращаем оригинальный слот
            if (originalSlot != -1) {
                mc.player.getInventory().setSelectedSlot(originalSlot);
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
            }

            // Закрываем инвентарь если открыт
            if (InventoryUtils.isScreenOpen()) {
                InventoryUtils.closeScreen();
            }
        }
        reset();
    }
}
