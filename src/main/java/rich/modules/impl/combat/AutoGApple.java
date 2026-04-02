package rich.modules.impl.combat;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SliderSettings;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoGApple extends ModuleStructure {

    final SliderSettings healthThreshold = new SliderSettings("Здоровье", "Порог здоровья для еды")
            .range(3.0f, 20.0f).setValue(16.0f);

    final BooleanSetting smartMode = new BooleanSetting("Умный", "Есть из хотбара, иначе только с левой руки")
            .setValue(true);

    final BooleanSetting goldenHearts = new BooleanSetting("Золотые сердца", "Учитывать absorption")
            .setValue(true);

    final BooleanSetting returnSlot = new BooleanSetting("Возвращать слот", "Возвращать предыдущий слот после еды")
            .setValue(true);

    boolean isEating = false;
    int previousSlot = -1;

    public AutoGApple() {
        super("AutoGApple", "Auto GApple", ModuleCategory.COMBAT);
        settings(healthThreshold, smartMode, goldenHearts, returnSlot);
    }

    @Override
    public void activate() {
        isEating = false;
        previousSlot = -1;
    }

    @Override
    public void deactivate() {
        stopEating();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        handleEating();
    }

    private void handleEating() {
        if (canEat()) {
            if (smartMode.isValue()) {
                if (!hasGappleInHand()) {
                    swapToGappleSlot();
                }
                startEating();
            } else {
                if (hasGappleInOffhand()) {
                    startEating();
                }
            }
        } else if (isEating && !shouldContinueEating()) {
            stopEating();
        }
    }

    private boolean hasGappleInHand() {
        ItemStack mainHand = mc.player.getMainHandStack();
        return mainHand.getItem() == Items.GOLDEN_APPLE;
    }

    private boolean canEat() {
        if (mc.player.isDead()) return false;
        if (mc.player.getItemCooldownManager().isCoolingDown(Items.GOLDEN_APPLE.getDefaultStack())) return false;

        if (smartMode.isValue()) {
            if (!hasGapple()) return false;
        } else {
            if (!hasGappleInOffhand()) return false;
        }

        float health = getEffectiveHealth();
        return health <= healthThreshold.getValue();
    }

    private boolean shouldContinueEating() {
        if (mc.player.isDead()) return false;
        if (!mc.player.isUsingItem()) return false;

        ItemStack usingItem = mc.player.getActiveItem();
        return usingItem.getItem() == Items.GOLDEN_APPLE;
    }

    private float getEffectiveHealth() {
        float health = mc.player.getHealth();
        if (goldenHearts.isValue()) {
            health += mc.player.getAbsorptionAmount();
        }
        return health;
    }

    private boolean hasGappleInOffhand() {
        ItemStack offhandStack = mc.player.getOffHandStack();
        return offhandStack.getItem() == Items.GOLDEN_APPLE;
    }

    private boolean hasGappleInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.GOLDEN_APPLE) {
                return true;
            }
        }
        return false;
    }

    private boolean hasGapple() {
        return hasGappleInOffhand() || hasGappleInHotbar();
    }

    private int findGappleInHotbar() {
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() == Items.GOLDEN_APPLE) {
                return i;
            }
        }
        return -1;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void swapToGappleSlot() {
        int gappleSlot = findGappleInHotbar();
        if (gappleSlot == -1) return;

        int currentSlot = mc.player.getInventory().getSelectedSlot();

        if (currentSlot != gappleSlot) {
            if (previousSlot == -1 && returnSlot.isValue()) {
                previousSlot = currentSlot;
            }
            mc.player.getInventory().setSelectedSlot(gappleSlot);
        }
    }

    private void startEating() {
        if (!isEating && !mc.options.useKey.isPressed()) {
            mc.options.useKey.setPressed(true);
            isEating = true;
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void stopEating() {
        if (isEating) {
            mc.options.useKey.setPressed(false);
            isEating = false;

            if (previousSlot != -1 && returnSlot.isValue()) {
                mc.player.getInventory().setSelectedSlot(previousSlot);
                previousSlot = -1;
            }
        }
    }
}