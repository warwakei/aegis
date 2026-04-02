package fun.aegis.display.hud;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import fun.aegis.features.impl.render.Hud;
import fun.aegis.utils.client.managers.api.draggable.AbstractDraggable;
import fun.aegis.utils.display.geometry.Render2D;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;

import java.util.List;

public class Armor extends AbstractDraggable {

    private static final int SLOT_SIZE = 18;
    private static final int SLOT_COUNT = 4;

    public Armor() {
        super("Броня", 0, 0, SLOT_COUNT * SLOT_SIZE, SLOT_SIZE, true);
    }

    @Override
    public boolean visible() {
        if (mc.player == null) return false;
        if (!Hud.getInstance().interfaceSettings.isSelected("Броня")) return false;
        return mc.player.getInventory().armor.stream().anyMatch(stack -> !stack.isEmpty()) 
                || PlayerInteractionHelper.isChat(mc.currentScreen);
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (mc.player == null) return;

        List<ItemStack> armorList = Lists.reverse(mc.player.getInventory().armor);

        // Draw armor items without background
        for (int i = 0; i < SLOT_COUNT; i++) {
            float slotX = getX() + i * SLOT_SIZE;
            float slotY = getY();

            if (i < armorList.size()) {
                ItemStack stack = armorList.get(i);
                if (!stack.isEmpty()) {
                    Render2D.defaultDrawStack(context, stack, slotX, slotY, false, true, 1F);
                }
            }
        }
    }
}
