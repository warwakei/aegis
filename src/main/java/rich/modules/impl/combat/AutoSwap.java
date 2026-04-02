package rich.modules.impl.combat;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import rich.events.api.EventHandler;
import rich.events.impl.InputEvent;
import rich.events.impl.KeyEvent;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BindSetting;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.inventory.InventoryUtils;
import rich.util.inventory.SwapExecutor;
import rich.util.inventory.SwapSettings;
import rich.util.string.chat.ChatMessage;

import java.util.Comparator;
import java.util.function.Predicate;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoSwap extends ModuleStructure {

    final SelectSetting modeSetting = new SelectSetting("Режим", "Способ свапа")
            .value("Instant", "Legit", "Custom")
            .selected("Legit");

    final BindSetting swapBind = new BindSetting("Кнопка свапа", "Кнопка для обмена предметов");

    final SelectSetting firstItem = new SelectSetting("Основной предмет", "Первый предмет")
            .value("Totem of Undying", "Player Head", "Golden Apple", "Shield");

    final SelectSetting secondItem = new SelectSetting("Вторичный предмет", "Второй предмет")
            .value("Totem of Undying", "Player Head", "Golden Apple", "Shield");

    final BooleanSetting stopMovement = new BooleanSetting("Стоп движение", "Остановить WASD")
            .setValue(true)
            .visible(() -> modeSetting.getSelected().equals("Custom"));

    final BooleanSetting stopSprint = new BooleanSetting("Стоп спринт", "Остановить спринт")
            .setValue(true)
            .visible(() -> modeSetting.getSelected().equals("Custom"));

    final BooleanSetting closeInventory = new BooleanSetting("Закрыть инвентарь", "Отправить пакет закрытия")
            .setValue(true)
            .visible(() -> modeSetting.getSelected().equals("Custom"));

    final SliderSettings preStopDelayMin = new SliderSettings("До стопа (мин)", "Мс до остановки")
            .range(0, 300).setValue(0)
            .visible(() -> modeSetting.getSelected().equals("Custom"));

    final SliderSettings preStopDelayMax = new SliderSettings("До стопа (макс)", "Мс до остановки")
            .range(0, 300).setValue(50)
            .visible(() -> modeSetting.getSelected().equals("Custom"));

    final SliderSettings waitStopDelayMin = new SliderSettings("Ожидание стопа (мин)", "Мс ожидания остановки")
            .range(0, 500).setValue(50)
            .visible(() -> modeSetting.getSelected().equals("Custom"));

    final SliderSettings waitStopDelayMax = new SliderSettings("Ожидание стопа (макс)", "Мс ожидания остановки")
            .range(0, 500).setValue(150)
            .visible(() -> modeSetting.getSelected().equals("Custom"));

    final SliderSettings preSwapDelayMin = new SliderSettings("До свапа (мин)", "Мс перед свапом")
            .range(0, 300).setValue(20)
            .visible(() -> modeSetting.getSelected().equals("Custom"));

    final SliderSettings preSwapDelayMax = new SliderSettings("До свапа (макс)", "Мс перед свапом")
            .range(0, 300).setValue(100)
            .visible(() -> modeSetting.getSelected().equals("Custom"));

    final SliderSettings postSwapDelayMin = new SliderSettings("После свапа (мин)", "Мс после свапа")
            .range(0, 300).setValue(20)
            .visible(() -> modeSetting.getSelected().equals("Custom"));

    final SliderSettings postSwapDelayMax = new SliderSettings("После свапа (макс)", "Мс после свапа")
            .range(0, 300).setValue(80)
            .visible(() -> modeSetting.getSelected().equals("Custom"));

    final SliderSettings resumeDelayMin = new SliderSettings("Восстановление (мин)", "Мс до восстановления")
            .range(0, 300).setValue(50)
            .visible(() -> modeSetting.getSelected().equals("Custom"));

    final SliderSettings resumeDelayMax = new SliderSettings("Восстановление (макс)", "Мс до восстановления")
            .range(0, 300).setValue(150)
            .visible(() -> modeSetting.getSelected().equals("Custom"));

    final SliderSettings velocityThreshold = new SliderSettings("Порог скорости", "Порог для определения остановки")
            .range(0.0001f, 0.05f).setValue(0.001f)
            .visible(() -> modeSetting.getSelected().equals("Custom"));

    final SwapExecutor executor = new SwapExecutor();

    public AutoSwap() {
        super("AutoSwap", "Auto Swap", ModuleCategory.COMBAT);
        settings(
                modeSetting, swapBind, firstItem, secondItem,
                stopMovement, stopSprint, closeInventory,
                preStopDelayMin, preStopDelayMax,
                waitStopDelayMin, waitStopDelayMax,
                preSwapDelayMin, preSwapDelayMax,
                postSwapDelayMin, postSwapDelayMax,
                resumeDelayMin, resumeDelayMax,
                velocityThreshold
        );
    }

    @EventHandler
    public void onKey(KeyEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (mc.currentScreen != null) return;
        if (!e.isKeyDown(swapBind.getKey())) return;
        if (executor.isRunning()) return;

        Slot hotbarSlot = findValidSlot(s -> s.id >= 36 && s.id <= 44);
        Slot targetSlot = hotbarSlot != null ? hotbarSlot : findValidSlot(s -> s.id >= 9 && s.id <= 35);

        if (targetSlot == null) {
            ChatMessage.brandmessage("Предмет не найден в инвентаре");
            return;
        }

        SwapSettings settings = buildSettings();
        final int slotId = targetSlot.id;

        executor.execute(() -> {
            InventoryUtils.swap(slotId, 45);
        }, settings);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;
        executor.tick();
    }

    @EventHandler
    public void onInput(InputEvent e) {
        if (mc.player == null) return;
        if (executor.isBlocking()) {
            e.setDirectionalLow(false, false, false, false);
            e.setJumping(false);
        }
    }

    private SwapSettings buildSettings() {
        String mode = modeSetting.getSelected();

        return switch (mode) {
            case "Instant" -> SwapSettings.instant();
            case "Legit" -> SwapSettings.legit();
            default -> new SwapSettings()
                    .stopMovement(stopMovement.isValue())
                    .stopSprint(stopSprint.isValue())
                    .closeInventory(closeInventory.isValue())
                    .preStopDelay(preStopDelayMin.getInt(), preStopDelayMax.getInt())
                    .waitStopDelay(waitStopDelayMin.getInt(), waitStopDelayMax.getInt())
                    .preSwapDelay(preSwapDelayMin.getInt(), preSwapDelayMax.getInt())
                    .postSwapDelay(postSwapDelayMin.getInt(), postSwapDelayMax.getInt())
                    .resumeDelay(resumeDelayMin.getInt(), resumeDelayMax.getInt())
                    .velocityThreshold(velocityThreshold.getValue());
        };
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private Slot findValidSlot(Predicate<Slot> slotPredicate) {
        Predicate<Slot> combinedPredicate = s -> s.id != 45 && !s.getStack().isEmpty() && slotPredicate.test(s);

        Item firstType = getItemByType(firstItem.getSelected());
        Item secondType = getItemByType(secondItem.getSelected());
        Item offHandItem = mc.player.getOffHandStack().getItem();
        String offHandItemName = mc.player.getOffHandStack().getName().getString();

        if (offHandItem == firstType) {
            Slot second = InventoryUtils.findSlot(
                    secondType,
                    combinedPredicate.and(s -> !s.getStack().getName().getString().equals(offHandItemName)),
                    Comparator.comparing(s -> s.getStack().hasEnchantments())
            );
            if (second != null) return second;
        }

        if (offHandItem == secondType) {
            Slot first = InventoryUtils.findSlot(
                    firstType,
                    combinedPredicate.and(s -> !s.getStack().getName().getString().equals(offHandItemName)),
                    Comparator.comparing(s -> s.getStack().hasEnchantments())
            );
            if (first != null) return first;
        }

        if (offHandItem != firstType && offHandItem != secondType) {
            Slot first = InventoryUtils.findSlot(
                    firstType,
                    combinedPredicate.and(s -> !s.getStack().getName().getString().equals(offHandItemName)),
                    Comparator.comparing(s -> s.getStack().hasEnchantments())
            );
            if (first != null) return first;

            Slot second = InventoryUtils.findSlot(
                    secondType,
                    combinedPredicate.and(s -> !s.getStack().getName().getString().equals(offHandItemName)),
                    Comparator.comparing(s -> s.getStack().hasEnchantments())
            );
            if (second != null) return second;
        }

        return null;
    }

    private Item getItemByType(String itemType) {
        return switch (itemType) {
            case "Totem of Undying" -> Items.TOTEM_OF_UNDYING;
            case "Player Head" -> Items.PLAYER_HEAD;
            case "Golden Apple" -> Items.GOLDEN_APPLE;
            case "Shield" -> Items.SHIELD;
            default -> Items.AIR;
        };
    }

    @Override
    public void deactivate() {
        executor.cancel();
    }
}