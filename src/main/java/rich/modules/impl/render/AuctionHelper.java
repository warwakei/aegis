package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.s2c.play.ScreenHandlerSlotUpdateS2CPacket;
import net.minecraft.screen.slot.Slot;
import rich.events.api.EventHandler;
import rich.events.impl.HandledScreenEvent;
import rich.events.impl.PacketEvent;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.screens.clickgui.impl.autobuy.AuctionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuctionHelper extends ModuleStructure {

    final BooleanSetting filterThorns = new BooleanSetting("Фильтр шипов", "Не показывать броню с шипами")
            .setValue(true);

    final BooleanSetting showPricePerItem = new BooleanSetting("Цена за штуку", "Учитывать цену за 1 предмет")
            .setValue(false);

    Slot firstSlot = null;
    Slot secondSlot = null;
    Slot thirdSlot = null;

    boolean needUpdate = false;
    int updateDelay = 0;

    static final int GREEN_COLOR = 0xFF00FF00;
    static final int ORANGE_COLOR = 0xFFFF8C00;
    static final int RED_COLOR = 0xFFFF3333;

    public AuctionHelper() {
        super("AuctionHelper", "Auction Helper", ModuleCategory.RENDER);
        settings(filterThorns, showPricePerItem);
    }

    @Override
    public void activate() {
        firstSlot = null;
        secondSlot = null;
        thirdSlot = null;
        needUpdate = false;
        updateDelay = 0;
    }

    @Override
    public void deactivate() {
        firstSlot = null;
        secondSlot = null;
        thirdSlot = null;
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (e.getPacket() instanceof ScreenHandlerSlotUpdateS2CPacket) {
            needUpdate = true;
            updateDelay = 2;
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (updateDelay > 0) {
            updateDelay--;
            return;
        }

        if (needUpdate && mc.currentScreen instanceof GenericContainerScreen screen) {
            updateSlots(screen);
            needUpdate = false;
        }
    }

    private void updateSlots(GenericContainerScreen screen) {
        List<SlotData> validSlots = new ArrayList<>();

        for (Slot slot : screen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty()) continue;

            int price = AuctionUtils.getPrice(stack);
            if (price <= 0) continue;

            if (filterThorns.isValue() && AuctionUtils.isArmorItem(stack) && AuctionUtils.hasThornsEnchantment(stack)) {
                continue;
            }

            int effectivePrice = showPricePerItem.isValue() && stack.getCount() > 1
                    ? price / stack.getCount()
                    : price;

            validSlots.add(new SlotData(slot, effectivePrice));
        }

        validSlots.sort(Comparator.comparingInt(data -> data.price));

        firstSlot = validSlots.size() > 0 ? validSlots.get(0).slot : null;
        secondSlot = validSlots.size() > 1 ? validSlots.get(1).slot : null;
        thirdSlot = validSlots.size() > 2 ? validSlots.get(2).slot : null;
    }

    @EventHandler
    public void onHandledScreen(HandledScreenEvent e) {
        if (mc.player == null) return;
        if (!(mc.currentScreen instanceof GenericContainerScreen screen)) return;

        DrawContext context = e.getDrawContext();

        int offsetX = (screen.width - e.getBackgroundWidth()) / 2;
        int offsetY = (screen.height - e.getBackgroundHeight()) / 2;

        highlightSlot(context, firstSlot, offsetX, offsetY, getBlinkingColor(GREEN_COLOR));
        highlightSlot(context, secondSlot, offsetX, offsetY, getBlinkingColor(ORANGE_COLOR));
        highlightSlot(context, thirdSlot, offsetX, offsetY, getBlinkingColor(RED_COLOR));
    }

    private int getBlinkingColor(int color) {
        float alpha = (float) Math.abs(Math.sin(System.currentTimeMillis() / 800.0 * Math.PI));
        alpha = 0.1f + alpha * 0.4f;

        int a = (int) (((color >> 24) & 0xFF) * alpha);
        if (a < 50) a = 50;

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private void highlightSlot(DrawContext context, Slot slot, int offsetX, int offsetY, int color) {
        if (slot == null) return;

        int x1 = offsetX + slot.x;
        int y1 = offsetY + slot.y;
        int x2 = x1 + 16;
        int y2 = y1 + 16;

        context.fill(x1, y1, x2, y2, color);
    }

    private record SlotData(Slot slot, int price) {}
}