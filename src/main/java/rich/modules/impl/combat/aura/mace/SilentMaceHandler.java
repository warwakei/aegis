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
 * сайлент мейс для ауры
 * палим падение 9 блоков
 * свап булавы удар возврат
 * тики 0 палим 1 свап 2 бьем 3 возврат
 */
public class SilentMaceHandler {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // высота для вкл 9 блоков
    private static final double MIN_FALL_DISTANCE = 9.0;

    @Getter
    private MaceState state = MaceState.IDLE;

    private int maceInventorySlot = -1;    // булава в инте 9-35
    private int maceHotbarSlot = -1;       // булава в хотбаре 0-8
    private int originalSlot = -1;         // слот чела был
    private int currentHeldSlot = -1;      // щас в руке для возврата

    private long stateStartTime = 0;
    private LivingEntity target = null;

    // ник запомнили для разраба
    private boolean wasDeveloperTarget = false;

    /**
     * статусы автомата
     */
    public enum MaceState {
        IDLE,           // ждем
        SWAPPING,       // суем булаву
        ATTACKING,      // бьем булавой
        RESTORING       // меч назад
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

        // чек высоту падения
        double fallDistance = mc.player.fallDistance;
        if (fallDistance < MIN_FALL_DISTANCE) {
            return;
        }

        // чек что летим не стоим
        if (mc.player.isOnGround()) {
            return;
        }

        // чек дистанцию
        float distance = mc.player.distanceTo(currentTarget);
        if (distance > 6.0f) {
            return; // далеко не достать
        }

        // чек что не булава в руке
        ItemStack mainHand = mc.player.getMainHandStack();
        if (mainHand.getItem() == Items.MACE) {
            return; // уже с булавой скип
        }

        // ищем булаву
        int hotbarMace = InventoryUtils.findHotbarItem(Items.MACE);
        int invMace = -1;

        if (hotbarMace == -1) {
            // в хотбаре нет лезем в инт
            for (int i = 9; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.getItem() == Items.MACE) {
                    invMace = i;
                    break;
                }
            }
        }

        // нигде нет отмена
        if (hotbarMace == -1 && invMace == -1) {
            return;
        }

        // го свапать
        this.target = currentTarget;
        this.originalSlot = mc.player.getInventory().getSelectedSlot();
        this.currentHeldSlot = originalSlot;
        this.maceHotbarSlot = hotbarMace;
        this.maceInventorySlot = invMace;
        this.wasDeveloperTarget = true;

        // погнали в свап
        state = MaceState.SWAPPING;
        stateStartTime = System.currentTimeMillis();
    }

    /**
     * свап суем булаву
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

        // булава в хотбаре просто свитч
        if (maceHotbarSlot != -1) {
            mc.player.getInventory().setSelectedSlot(maceHotbarSlot);
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(maceHotbarSlot));
        } else if (maceInventorySlot != -1) {
            // булава в инте сайлент свап
            int wrappedSlot = InventoryUtils.wrapSlot(maceInventorySlot);
            int targetHotbarSlot = originalSlot + 36; // хотбар слот цели

            // 3 клика свап
            InventoryUtils.click(wrappedSlot, 0, SlotActionType.PICKUP);   // взяли
            InventoryUtils.click(targetHotbarSlot, 0, SlotActionType.PICKUP); // кинули в хотбар
            InventoryUtils.click(wrappedSlot, 0, SlotActionType.PICKUP);   // остаток назад

            InventoryUtils.closeScreen();

            // свитч на слот
            mc.player.getInventory().setSelectedSlot(originalSlot);
            mc.player.networkHandler.sendPacket(new UpdateSelectedSlotC2SPacket(originalSlot));
        }

        state = MaceState.ATTACKING;
        stateStartTime = System.currentTimeMillis();
    }

    /**
     * бьем булавой
     */
    private void handleAttacking() {
        // ждем тик свап встанет
        long elapsed = System.currentTimeMillis() - stateStartTime;
        if (elapsed < 50) {
            return;
        }

        if (mc.player == null || target == null || !target.isAlive()) {
            // цель слилась возврат меча
            state = MaceState.RESTORING;
            stateStartTime = System.currentTimeMillis();
            return;
        }

        // чек еще летим
        if (mc.player.isOnGround()) {
            // на земле все равно бьем если рядом
        }

        // лупим булавой
        performMaceAttack(target);

        // го возврат
        state = MaceState.RESTORING;
        stateStartTime = System.currentTimeMillis();
    }

    /**
     * удар булавой
     */
    private void performMaceAttack(LivingEntity target) {
        if (mc.player == null || mc.getNetworkHandler() == null) return;

        // шлем пакет удара
        mc.getNetworkHandler().sendPacket(
                PlayerInteractEntityC2SPacket.attack((Entity) target, mc.player.isSneaking())
        );

        // мах рукой
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    /**
     * возврат меча назад
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

        // булава была в инте вернуть все
        if (maceInventorySlot != -1) {
            int wrappedSlot = InventoryUtils.wrapSlot(maceInventorySlot);
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
     * сброс стейта
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
     * щас свапаем или нет
     */
    public boolean isActive() {
        return state != MaceState.IDLE;
    }

    /**
     * жесткий сброс при офф ауры
     */
    public void forceReset() {
        if (state != MaceState.IDLE && mc.player != null) {
            // слот назад
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
