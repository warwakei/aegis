package rich.modules.impl.player;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import rich.events.api.EventHandler;
import rich.events.impl.*;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SelectSetting;

import java.util.Comparator;
import java.util.Objects;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoTool extends ModuleStructure {

    final SelectSetting modeSetting = new SelectSetting("Режим", "Способ смены инструмента")
            .value("Silent", "Legit")
            .selected("Silent");

    final BooleanSetting swapBack = new BooleanSetting("Возврат", "Возвращать инструмент после лома")
            .setValue(true);

    final BooleanSetting fullInventory = new BooleanSetting("Вне хотбара", "Искать инструмент во всём инвентаре")
            .setValue(true);

    final BooleanSetting stopSprint = new BooleanSetting("Стоп спринт", "Останавливать спринт при ломке (Legit режим)")
            .setValue(true);

    final BooleanSetting antiBreak = new BooleanSetting("Анти ломка", "Не менять инструмент когда блок вот-вот сломается")
            .setValue(true);

    final BooleanSetting preferDurability = new BooleanSetting("По долговечности", "Предпочитать инструмент с большей прочностью")
            .setValue(false);

    long lastSwapTime = 0;
    long lastBreakTime = 0;

    ItemStack originalStack = null;
    ItemStack toolStack = null;
    int originalSlotIndex = -1;
    int toolSlotIndex = -1;
    BlockPos lastBreakPos = null;

    boolean isActive = false;
    Slot swapBackSlot = null;

    public AutoTool() {
        super("AutoTool", "Auto Tool", ModuleCategory.PLAYER);
        settings(modeSetting, swapBack, fullInventory, stopSprint, antiBreak, preferDurability);
    }

    @Override
    public void activate() {
        resetState();
    }

    @Override
    public void deactivate() {
        resetState();
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean isSilentMode() {
        return modeSetting.getSelected().equals("Silent");
    }

    private void resetState() {
        lastSwapTime = 0;
        lastBreakTime = 0;
        originalStack = null;
        toolStack = null;
        originalSlotIndex = -1;
        toolSlotIndex = -1;
        lastBreakPos = null;
        isActive = false;
        swapBackSlot = null;
    }

    private void clearRenderState() {
        originalStack = null;
        toolStack = null;
        originalSlotIndex = -1;
        toolSlotIndex = -1;
    }

    @EventHandler
    public void onHeldItemUpdate(HeldItemUpdateEvent e) {
        if (!isSilentMode()) return;
        if (!isActive) return;
        if (originalStack == null || mc.player == null) return;

        if (mc.player.getInventory().getSelectedSlot() != originalSlotIndex) {
            clearRenderState();
            return;
        }

        e.setMainHand(originalStack);
    }

    @EventHandler
    public void onItemRenderer(ItemRendererEvent e) {
        if (!isSilentMode()) return;
        if (!isActive) return;
        if (originalStack == null) return;
        if (e.getHand() != Hand.MAIN_HAND) return;
        if (!Objects.equals(mc.player, e.getPlayer())) return;

        if (mc.player.getInventory().getSelectedSlot() != originalSlotIndex) {
            clearRenderState();
            return;
        }

        e.setStack(originalStack);
    }

    @EventHandler
    public void onHotbarItemRender(HotbarItemRenderEvent e) {
        if (!isSilentMode()) return;
        if (!isActive) return;
        if (originalStack == null || mc.player == null) return;

        int currentSelectedSlot = mc.player.getInventory().getSelectedSlot();

        if (currentSelectedSlot != originalSlotIndex) {
            clearRenderState();
            return;
        }

        if (e.getHotbarIndex() == originalSlotIndex) {
            e.setStack(originalStack);
        }

        if (toolSlotIndex != -1 && toolSlotIndex != originalSlotIndex && e.getHotbarIndex() == toolSlotIndex) {
            e.setStack(toolStack);
        }
    }

    @EventHandler
    public void onHotBarUpdate(HotBarUpdateEvent e) {
        if (isActive && isSilentMode()) {
            e.cancel();
        }
    }

    @EventHandler
    public void onHotBarScroll(HotBarScrollEvent e) {
        if (isActive && isSilentMode()) {
            e.cancel();
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void onBlockBreaking(BlockBreakingEvent e) {
        if (mc.player == null || mc.world == null) return;

        lastBreakTime = System.currentTimeMillis();
        lastBreakPos = e.blockPos();

        if (mc.player.isCreative()) return;
        if (isActive) return;
        if (!hasSwapCooldownPassed()) return;

        // Анти-ломка: не меняем инструмент если блок почти сломан
        if (antiBreak.isValue()) {
            return;
        }

        Slot bestSlot = findBestTool(lastBreakPos);
        Slot mainHandSlot = getMainHandSlot();

        if (bestSlot == null || mainHandSlot == null) return;
        if (bestSlot.id == mainHandSlot.id) return;

        int selectedSlot = mc.player.getInventory().getSelectedSlot();
        int bestToolHotbarIndex = bestSlot.id - 36;

        if (isSilentMode()) {
            originalStack = mc.player.getInventory().getStack(selectedSlot).copy();
            toolStack = mc.player.getInventory().getStack(bestToolHotbarIndex).copy();
            originalSlotIndex = selectedSlot;
            toolSlotIndex = bestToolHotbarIndex;
        }

        swapBackSlot = bestSlot;

        if (isSilentMode()) {
            swapToHandSilent(bestSlot);
        } else {
            swapToHand(bestSlot);
        }

        isActive = true;
        lastSwapTime = System.currentTimeMillis();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (!isActive) {
            if (originalStack != null) {
                clearRenderState();
            }
            return;
        }

        if (isSilentMode()) {
            int currentSlot = mc.player.getInventory().getSelectedSlot();
            if (originalSlotIndex != -1 && currentSlot != originalSlotIndex) {
                forceReset();
                return;
            }
        }

        if (!hasSwapCooldownPassed()) return;

        boolean breakingStopped = hasBreakingCooldownPassed();

        if (breakingStopped) {
            if (swapBackSlot != null && swapBack.isValue()) {
                if (isSilentMode()) {
                    swapToHandSilent(swapBackSlot);
                } else {
                    swapToHand(swapBackSlot);
                }
            }

            clearRenderState();
            isActive = false;
            swapBackSlot = null;
            lastSwapTime = System.currentTimeMillis();
        }
    }

    private void forceReset() {
        clearRenderState();
        isActive = false;
        swapBackSlot = null;
    }

    private boolean hasSwapCooldownPassed() {
        return System.currentTimeMillis() - lastSwapTime >= 50;
    }

    private boolean hasBreakingCooldownPassed() {
        return System.currentTimeMillis() - lastBreakTime >= 100;
    }

    private Slot getMainHandSlot() {
        if (mc.player == null) return null;
        int selectedSlot = mc.player.getInventory().getSelectedSlot();
        return mc.player.playerScreenHandler.getSlot(36 + selectedSlot);
    }

    private void swapToHand(Slot slot) {
        if (mc.player == null || mc.interactionManager == null || slot == null) return;
        int hotbarSlot = mc.player.getInventory().getSelectedSlot();
        
        // Останавливаем спринт если нужно
        if (stopSprint.isValue() && mc.player.isSprinting()) {
            mc.player.setSprinting(false);
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.STOP_SPRINTING));
        }
        
        mc.interactionManager.clickSlot(
                mc.player.playerScreenHandler.syncId,
                slot.id,
                hotbarSlot,
                SlotActionType.SWAP,
                mc.player
        );
    }

    private void swapToHandSilent(Slot slot) {
        if (mc.player == null || slot == null) return;

        int currentSlot = mc.player.getInventory().getSelectedSlot();
        int targetSlot = slot.id - 36;

        if (targetSlot < 0 || targetSlot > 8) return;
        if (targetSlot == currentSlot) return;

        // Отправляем пакет смены слота
        mc.getNetworkHandler().sendPacket(new UpdateSelectedSlotC2SPacket(targetSlot));
        
        // Обновляем selectedSlot локально через setSelectedSlot для корректной работы
        mc.player.getInventory().setSelectedSlot(targetSlot);
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private Slot findBestTool(BlockPos blockPos) {
        if (mc.player == null || mc.world == null || blockPos == null) return getMainHandSlot();

        BlockState state = mc.world.getBlockState(blockPos);
        if (state.isAir()) return getMainHandSlot();

        Slot mainHandSlot = getMainHandSlot();
        float currentSpeed = mainHandSlot != null ? mainHandSlot.getStack().getMiningSpeedMultiplier(state) : 1.0f;

        int startSlot = fullInventory.isValue() ? 9 : 36;
        int endSlot = 45;

        var slots = mc.player.playerScreenHandler.slots.stream()
                .filter(slot -> slot.id >= startSlot && slot.id < endSlot)
                .filter(slot -> !slot.getStack().isEmpty())
                .filter(slot -> slot.getStack().getMiningSpeedMultiplier(state) > 1.0f)
                .toList();

        if (slots.isEmpty()) return mainHandSlot;

        Slot bestSlot;
        if (preferDurability.isValue()) {
            bestSlot = slots.stream()
                    .max(Comparator.comparingInt(slot -> getDurability(slot)))
                    .orElse(mainHandSlot);
        } else {
            bestSlot = slots.stream()
                    .max(Comparator.comparingDouble(slot -> slot.getStack().getMiningSpeedMultiplier(state)))
                    .orElse(mainHandSlot);
        }

        if (bestSlot != null && bestSlot.getStack().getMiningSpeedMultiplier(state) > currentSpeed) {
            return bestSlot;
        }

        return mainHandSlot;
    }

    private int getDurability(Slot slot) {
        ItemStack stack = slot.getStack();
        if (stack.getMaxDamage() == 0) return Integer.MAX_VALUE;
        return stack.getMaxDamage() - stack.getDamage();
    }
}