package rich.modules.impl.combat.aura.shield;

import lombok.Getter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import rich.util.inventory.InventoryUtils;

/**
 * Shield Breaker Handler для Aura
 *
 * Логика работы:
 * - Детектит что цель использует щит (isUsingItem + Items.SHIELD)
 * - Находит топор в хотбаре или инвентаре
 * - Свапает топор в слот → атакует → возвращает оригинальный предмет
 *
 * Тайминги (по тикам):
 * - Тик 0: цель подняла щит, найден слот топора
 * - Тик 1: свап топора в слот
 * - Тик 2: атака топором (пакет)
 * - Тик 3: возврат оригинального предмета
 */
public class ShieldBreakerHandler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    @Getter
    private ShieldBreakerState state = ShieldBreakerState.IDLE;

    private int axeInventorySlot = -1;    // Слот топора в инвентаре (9-35)
    private int axeHotbarSlot = -1;       // Слот топора в хотбаре (0-8)
    private int originalSlot = -1;         // Оригинальный слот игрока

    private long stateStartTime = 0;
    private LivingEntity target = null;

    /**
     * Состояния автомата
     */
    public enum ShieldBreakerState {
        IDLE,           // Ожидание
        SWAPPING,       // Свап топора в слот
        ATTACKING,      // Атака топором
        RESTORING       // Возврат оригинального предмета
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
     * Состояние IDLE: проверяем нужно ли активировать Shield Breaker
     */
    private void handleIdle(LivingEntity currentTarget) {
        // Проверяем условия для активации
        if (currentTarget == null || !currentTarget.isAlive()) {
            return;
        }

        // Проверяем что цель использует щит
        if (!currentTarget.isUsingItem()) {
            return;
        }

        ItemStack activeItem = currentTarget.getActiveItem();
        if (activeItem.isEmpty() || activeItem.getItem() != Items.SHIELD) {
            return;
        }

        // Проверяем дистанцию до цели (в радиусе атаки Aura + небольшой запас)
        float distance = mc.player.distanceTo(currentTarget);
        if (distance > 6.0f) {
            return; // Цель слишком далеко
        }

        // Проверяем что в руке НЕ топор (ищем любой топор: WOODEN, STONE, IRON, GOLDEN, NETHERITE)
        ItemStack mainHand = mc.player.getMainHandStack();
        if (isAxe(mainHand)) {
            return; // Уже с топором, не нужно свапать
        }

        // Ищем топор в хотбаре и инвентаре
        int hotbarAxe = findHotbarAxe();
        int invAxe = -1;

        if (hotbarAxe == -1) {
            // Топора нет в хотбаре — ищем в инвентаре
            for (int i = 9; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (isAxe(stack)) {
                    invAxe = i;
                    break;
                }
            }
        }

        // Если топора нет нигде — отмена
        if (hotbarAxe == -1 && invAxe == -1) {
            return;
        }

        // Инициализируем свап
        this.target = currentTarget;
        this.originalSlot = mc.player.getInventory().getSelectedSlot();
        this.axeHotbarSlot = hotbarAxe;
        this.axeInventorySlot = invAxe;

        // Переходим в состояние свапа
        state = ShieldBreakerState.SWAPPING;
        stateStartTime = System.currentTimeMillis();
    }

    /**
     * Состояние SWAPPING: свапаем топор в слот
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

        // Если топор уже в хотбаре — просто свитчим
        if (axeHotbarSlot != -1) {
            mc.player.getInventory().setSelectedSlot(axeHotbarSlot);
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(axeHotbarSlot));
        } else if (axeInventorySlot != -1) {
            // Топор в инвентаре — делаем silent swap
            int wrappedSlot = InventoryUtils.wrapSlot(axeInventorySlot);
            int targetHotbarSlot = originalSlot + 36; // Целевой слот хотбара в экранных координатах

            // 3 клика PICKUP для свапа
            InventoryUtils.click(wrappedSlot, 0, SlotActionType.PICKUP);     // взяли топор
            InventoryUtils.click(targetHotbarSlot, 0, SlotActionType.PICKUP); // положили в хотбар
            InventoryUtils.click(wrappedSlot, 0, SlotActionType.PICKUP);     // вернули остаток

            InventoryUtils.closeScreen();

            // Теперь свитчим на этот слот
            mc.player.getInventory().setSelectedSlot(originalSlot);
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
        }

        state = ShieldBreakerState.ATTACKING;
        stateStartTime = System.currentTimeMillis();
    }

    /**
     * Состояние ATTACKING: атакуем топором
     */
    private void handleAttacking() {
        // Ждём 1 тик (50ms) чтобы свап успел примениться
        long elapsed = System.currentTimeMillis() - stateStartTime;
        if (elapsed < 50) {
            return;
        }

        if (mc.player == null || target == null || !target.isAlive()) {
            // Цель пропала — возвращаем предмет
            state = ShieldBreakerState.RESTORING;
            stateStartTime = System.currentTimeMillis();
            return;
        }

        // Атакуем топором
        performAxeAttack(target);

        // Переходим к возврату
        state = ShieldBreakerState.RESTORING;
        stateStartTime = System.currentTimeMillis();
    }

    /**
     * Выполняет атаку топором
     */
    private void performAxeAttack(LivingEntity target) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        // Отправляем пакет атаки
        mc.getNetworkHandler().sendPacket(
                PlayerInteractEntityC2SPacket.attack((Entity) target, mc.player.isSneaking())
        );

        // Анимация руки
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    /**
     * Состояние RESTORING: возвращаем оригинальный предмет обратно
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

        // Если топор был в инвентаре — нужно вернуть всё обратно
        if (axeInventorySlot != -1) {
            int wrappedSlot = InventoryUtils.wrapSlot(axeInventorySlot);
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
     * Проверяет является ли предмет топором
     */
    private boolean isAxe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        var item = stack.getItem();
        return item == Items.WOODEN_AXE
                || item == Items.STONE_AXE
                || item == Items.IRON_AXE
                || item == Items.GOLDEN_AXE
                || item == Items.NETHERITE_AXE;
    }

    /**
     * Ищет топор в хотбаре
     */
    private int findHotbarAxe() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isAxe(stack)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Сброс состояния
     */
    public void reset() {
        state = ShieldBreakerState.IDLE;
        axeInventorySlot = -1;
        axeHotbarSlot = -1;
        originalSlot = -1;
        target = null;
        stateStartTime = 0;
    }

    /**
     * Возвращает true если сейчас активен процесс свапа топора
     */
    public boolean isActive() {
        return state != ShieldBreakerState.IDLE;
    }

    /**
     * Принудительный сброс (при деактивации Aura)
     */
    public void forceReset() {
        if (state != ShieldBreakerState.IDLE && mc.player != null) {
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
