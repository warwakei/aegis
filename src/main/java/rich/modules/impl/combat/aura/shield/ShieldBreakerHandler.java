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
 * щиты ломаем топором
 * палим щит свап удар возврат
 * тики 0 палим 1 свап 2 бьем 3 возврат
 */
public class ShieldBreakerHandler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    @Getter
    private ShieldBreakerState state = ShieldBreakerState.IDLE;

    private int axeInventorySlot = -1;    // топор в инте 9-35
    private int axeHotbarSlot = -1;       // топор в хотбаре 0-8
    private int originalSlot = -1;         // слот чела был

    private long stateStartTime = 0;
    private LivingEntity target = null;

    /**
     * статусы автомата
     */
    public enum ShieldBreakerState {
        IDLE,           // ждем
        SWAPPING,       // суем топор
        ATTACKING,      // бьем топором
        RESTORING       // шмот назад
    }

    /**
     * тик из ауры
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
     * идле чекаем пора ли
     */
    private void handleIdle(LivingEntity currentTarget) {
        // чек пора ли вкл
        if (currentTarget == null || !currentTarget.isAlive()) {
            return;
        }

        // чек щит у цели
        if (!currentTarget.isUsingItem()) {
            return;
        }

        ItemStack activeItem = currentTarget.getActiveItem();
        if (activeItem.isEmpty() || activeItem.getItem() != Items.SHIELD) {
            return;
        }

        // чек дистанцию в радиусе ауры
        float distance = mc.player.distanceTo(currentTarget);
        if (distance > 6.0f) {
            return; // далеко не достать
        }

        // чек что не топор в руке
        ItemStack mainHand = mc.player.getMainHandStack();
        if (isAxe(mainHand)) {
            return; // уже с топором скип
        }

        // ищем топор везде
        int hotbarAxe = findHotbarAxe();
        int invAxe = -1;

        if (hotbarAxe == -1) {
            // в хотбаре нет лезем в инт
            for (int i = 9; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (isAxe(stack)) {
                    invAxe = i;
                    break;
                }
            }
        }

        // нигде нет отмена
        if (hotbarAxe == -1 && invAxe == -1) {
            return;
        }

        // го свапать
        this.target = currentTarget;
        this.originalSlot = mc.player.getInventory().getSelectedSlot();
        this.axeHotbarSlot = hotbarAxe;
        this.axeInventorySlot = invAxe;

        // погнали в свап
        state = ShieldBreakerState.SWAPPING;
        stateStartTime = System.currentTimeMillis();
    }

    /**
     * свап суем топор
     */
    private void handleSwapping() {
        // ждем тик чтоб стабильно
        long elapsed = System.currentTimeMillis() - stateStartTime;
        if (elapsed < 50) {
            return;
        }

        if (mc.player == null) {
            reset();
            return;
        }

        // топор в хотбаре просто свитч
        if (axeHotbarSlot != -1) {
            mc.player.getInventory().setSelectedSlot(axeHotbarSlot);
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(axeHotbarSlot));
        } else if (axeInventorySlot != -1) {
            // топор в инте сайлент свап
            int wrappedSlot = InventoryUtils.wrapSlot(axeInventorySlot);
            int targetHotbarSlot = originalSlot + 36; // хотбар слот цели

            // 3 клика свап
            InventoryUtils.click(wrappedSlot, 0, SlotActionType.PICKUP);     // взяли
            InventoryUtils.click(targetHotbarSlot, 0, SlotActionType.PICKUP); // кинули в хотбар
            InventoryUtils.click(wrappedSlot, 0, SlotActionType.PICKUP);     // остаток назад

            InventoryUtils.closeScreen();

            // свитч на слот
            mc.player.getInventory().setSelectedSlot(originalSlot);
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
        }

        state = ShieldBreakerState.ATTACKING;
        stateStartTime = System.currentTimeMillis();
    }

    /**
     * бьем топором
     */
    private void handleAttacking() {
        // ждем тик свап встанет
        long elapsed = System.currentTimeMillis() - stateStartTime;
        if (elapsed < 50) {
            return;
        }

        if (mc.player == null || target == null || !target.isAlive()) {
            // цель слилась возврат шмота
            state = ShieldBreakerState.RESTORING;
            stateStartTime = System.currentTimeMillis();
            return;
        }

        // лупим топором
        performAxeAttack(target);

        // го возврат
        state = ShieldBreakerState.RESTORING;
        stateStartTime = System.currentTimeMillis();
    }

    /**
     * удар топором
     */
    private void performAxeAttack(LivingEntity target) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        // шлем пакет удара
        mc.getNetworkHandler().sendPacket(
                PlayerInteractEntityC2SPacket.attack((Entity) target, mc.player.isSneaking())
        );

        // мах рукой
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    /**
     * возврат шмота назад
     */
    private void handleRestoring() {
        // ждем тик удар долетит
        long elapsed = System.currentTimeMillis() - stateStartTime;
        if (elapsed < 50) {
            return;
        }

        if (mc.player == null) {
            reset();
            return;
        }

        // топор был в инте вернуть все
        if (axeInventorySlot != -1) {
            int wrappedSlot = InventoryUtils.wrapSlot(axeInventorySlot);
            int sourceHotbarSlot = originalSlot + 36;

            // обратный свап
            InventoryUtils.click(sourceHotbarSlot, 0, SlotActionType.PICKUP);
            InventoryUtils.click(wrappedSlot, 0, SlotActionType.PICKUP);
            InventoryUtils.click(sourceHotbarSlot, 0, SlotActionType.PICKUP);

            InventoryUtils.closeScreen();
        }

        // Возвращаем оригинальный слот
        mc.player.getInventory().setSelectedSlot(originalSlot);
        mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));

        // сброс
        reset();
    }

    /**
     * чек топор или нет
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
     * топор в хотбаре ищем
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
     * сброс стейта
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
     * щас свапаем или нет
     */
    public boolean isActive() {
        return state != ShieldBreakerState.IDLE;
    }

    /**
     * жесткий сброс при офф ауры
     */
    public void forceReset() {
        if (state != ShieldBreakerState.IDLE && mc.player != null) {
            // слот назад верни
            if (originalSlot != -1) {
                mc.player.getInventory().setSelectedSlot(originalSlot);
                mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
            }

            // инт закрываем если открыт
            if (InventoryUtils.isScreenOpen()) {
                InventoryUtils.closeScreen();
            }
        }
        reset();
    }
}
