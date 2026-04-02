package fun.aegis.features.impl.combat;

import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import fun.aegis.utils.interactions.inv.InventoryFlowManager;
import fun.aegis.utils.interactions.inv.InventoryTask;
import fun.aegis.utils.math.time.StopWatch;
import fun.aegis.utils.math.script.Script;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.item.SplashPotionItem;
import net.minecraft.screen.slot.Slot;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.features.module.setting.implement.MultiSelectSetting;
import fun.aegis.events.player.PostMotionEvent;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoBuff extends Module {
    StopWatch throwCooldown = new StopWatch();
    Script script = new Script();
    int originalSlot = -1;
    int lastPotionCount = 0;

    MultiSelectSetting potionTypes = new MultiSelectSetting("Potions", "Which potions to throw")
            .value("Speed", "Strength", "Resistance", "Jump Boost", "Haste")
            .selected("Speed", "Strength");

    SliderSettings throwDelay = new SliderSettings("Throw Delay", "Delay between potion throws (ms)")
            .setValue(100).range(50, 500);

    public AutoBuff() {
        super("AutoBuff", "Auto Buff", ModuleCategory.COMBAT);
        setup(potionTypes, throwDelay);
    }

    @Override
    public void activate() {
        lastPotionCount = 0;
    }

    @EventHandler
    public void onPostMotion(PostMotionEvent e) {
        if (PlayerInteractionHelper.nullCheck()) return;

        // Ищем зелье в хотбаре
        Slot potionSlot = findBuffPotion();
        if (potionSlot == null) return;

        ItemStack potionStack = potionSlot.getStack();
        int currentCount = potionStack.getCount();

        // Проверяем что зелье было выброшено (count уменьшился на 1 или больше)
        if (lastPotionCount > 0 && currentCount < lastPotionCount - 1) {
            // Зелье было выброшено, ждём перед следующим броском
            if (!throwCooldown.finished((long) throwDelay.getValue())) {
                lastPotionCount = currentCount;
                return;
            }
        }

        // Если count стал 0 или меньше - зелье закончилось, ищем новое
        if (currentCount <= 0) {
            lastPotionCount = 0;
            return;
        }

        // Кидаем зелье
        if (throwCooldown.finished((long) throwDelay.getValue())) {
            throwPotion(potionSlot);
            lastPotionCount = currentCount;
            throwCooldown.reset();
        }
    }

    private Slot findBuffPotion() {
        // Ищем в хотбаре (слоты 0-8)
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (isBuffPotion(stack)) {
                return new Slot(mc.player.getInventory(), i, 0, 0);
            }
        }
        return null;
    }

    private boolean isBuffPotion(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }
        
        // Проверяем что это кидающееся зелье (splash potion)
        return stack.isOf(Items.SPLASH_POTION) || stack.isOf(Items.LINGERING_POTION);
    }

    private void throwPotion(Slot potionSlot) {
        if (mc.player == null) return;

        originalSlot = mc.player.getInventory().selectedSlot;
        final int potionSlotIndex;
        
        int tempIndex = -1;
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i) == potionSlot.getStack()) {
                tempIndex = i;
                break;
            }
        }
        potionSlotIndex = tempIndex;
        
        if (potionSlotIndex == -1) return;
        
        InventoryFlowManager.unPressMoveKeys();

        script.cleanup().addTickStep(0, () -> {
            InventoryTask.switchTo(potionSlotIndex);
            
            script.cleanup().addTickStep(1, () -> {
                if (mc.player != null) {
                    mc.options.useKey.setPressed(true);
                }
                
                script.cleanup().addTickStep(2, () -> {
                    if (mc.player != null) {
                        mc.options.useKey.setPressed(false);
                    }
                    
                    script.cleanup().addTickStep(3, () -> {
                        InventoryTask.switchTo(originalSlot);
                        InventoryFlowManager.enableMoveKeys();
                    });
                });
            });
        });
    }

    @EventHandler
    public void onMotion(PostMotionEvent e) {
        script.update();
    }
}
