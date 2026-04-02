package rich.screens.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import rich.client.draggables.AbstractHudElement;
import rich.util.animations.Direction;
import rich.util.render.Render2D;
import rich.util.render.item.ItemRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class Inventory extends AbstractHudElement {

    private static final int SLOT_SIZE = 12;
    private static final int SLOTS_PER_ROW = 9;
    private static final int INVENTORY_ROWS = 3;
    private static final float ITEM_SCALE = 0.5f;

    private int filledSlots = 0;

    public Inventory() {
        super("Inventory", 20, 60, 200, 80, true);
        stopAnimation();
    }

    @Override
    public boolean visible() {
        return !scaleAnimation.isFinished(Direction.BACKWARDS);
    }

    @Override
    public void tick() {
        if (mc.player == null) {
            filledSlots = 0;
            stopAnimation();
            return;
        }

        filledSlots = 0;
        for (int i = 9; i < 36; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty()) {
                filledSlots++;
            }
        }

        boolean hasItems = filledSlots > 0;
        boolean inChat = isChat(mc.currentScreen);

        if (hasItems || inChat) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) return;
        if (mc.player == null) return;

        float alphaFactor = alpha / 255.0f;

        float x = getX();
        float y = getY();

        float padding = 6;
        float slotGap = 1;

        float slotsWidth = SLOTS_PER_ROW * SLOT_SIZE + (SLOTS_PER_ROW - 1) * slotGap;
        float slotsHeight = INVENTORY_ROWS * SLOT_SIZE + (INVENTORY_ROWS - 1) * slotGap;

        float contentWidth = slotsWidth + padding * 2;
        float contentHeight = slotsHeight + padding * 2;

        setWidth((int) contentWidth);
        setHeight((int) (contentHeight + 4));

        float contentY = y;

        int bgAlpha = (int) (255 * alphaFactor);

        Render2D.gradientRect(x + 2, contentY + 2, contentWidth - 4, contentHeight - 4,
                new int[]{
                        new Color(52, 52, 52, bgAlpha).getRGB(),
                        new Color(32, 32, 32, bgAlpha).getRGB(),
                        new Color(52, 52, 52, bgAlpha).getRGB(),
                        new Color(32, 32, 32, bgAlpha).getRGB()
                },
                5);

        Render2D.outline(x + 2, contentY + 2, contentWidth - 4, contentHeight - 4, 0.35f, new Color(90, 90, 90, bgAlpha).getRGB(), 5);

        float slotsStartX = x + padding;
        float slotsStartY = contentY + padding;

        List<CountLabel> countLabels = new ArrayList<>();

        for (int row = 0; row < INVENTORY_ROWS; row++) {
            for (int col = 0; col < SLOTS_PER_ROW; col++) {
                int slotIndex = 9 + row * SLOTS_PER_ROW + col;

                float slotX = slotsStartX + col * (SLOT_SIZE + slotGap);
                float slotY = slotsStartY + row * (SLOT_SIZE + slotGap);

                ItemStack stack = mc.player.getInventory().getStack(slotIndex);

                Render2D.rect(slotX, slotY, SLOT_SIZE, SLOT_SIZE, new Color(28, 28, 28, bgAlpha).getRGB(), 2);

                if (!stack.isEmpty()) {
                    float itemSize = 16 * ITEM_SCALE;
                    float itemX = slotX + (SLOT_SIZE - itemSize) / 2;
                    float itemY = slotY + (SLOT_SIZE - itemSize) / 2;

                    if (ItemRender.needsContextRender(stack)) {
                        ItemRender.drawItemWithContext(context, stack, itemX, itemY, ITEM_SCALE, alphaFactor);
                    } else {
                        ItemRender.drawItem(stack, itemX, itemY, ITEM_SCALE, alphaFactor);
                    }

                    int count = stack.getCount();
                    if (count > 1) {
                        countLabels.add(new CountLabel(slotX, slotY, count));
                    }
                }
            }
        }

        int textAlpha = (int) (255 * alphaFactor);
        int textColor = (textAlpha << 24) | 0xFFFFFF;

        for (CountLabel label : countLabels) {
            String countText = String.valueOf(label.count);
            int textWidth = mc.textRenderer.getWidth(countText);
            int textX = (int) (label.slotX + SLOT_SIZE - textWidth);
            int textY = (int) (label.slotY + SLOT_SIZE - mc.textRenderer.fontHeight + 1);

            context.drawText(mc.textRenderer, countText, textX, textY, textColor, true);
        }
    }

    private record CountLabel(float slotX, float slotY, int count) {}
}