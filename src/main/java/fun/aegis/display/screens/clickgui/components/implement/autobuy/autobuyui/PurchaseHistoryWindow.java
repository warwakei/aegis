package fun.aegis.display.screens.clickgui.components.implement.autobuy.autobuyui;

import fun.aegis.display.screens.clickgui.components.implement.autobuy.items.AutoBuyableItem;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.geometry.Render2D;
import fun.aegis.utils.display.shape.ShapeProperties;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import java.awt.Color;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import static fun.aegis.utils.display.interfaces.QuickImports.rectangle;

public class PurchaseHistoryWindow {
    private static final List<PurchaseRecord> purchases = new CopyOnWriteArrayList<>();
    private static final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss");
    private float scroll = 0f;
    private float smoothedScroll = 0f;
    private final MinecraftClient mc = MinecraftClient.getInstance();

    public static void addPurchase(AutoBuyableItem item, int price) {
        String cleanName = item.getDisplayName();
        long currentTime = System.currentTimeMillis();
        for (PurchaseRecord record : purchases) {
            if (record.itemName.equals(cleanName) && record.price == price && (currentTime - record.timestamp) < 1000) {
                return;
            }
        }
        ItemStack stackCopy = item.createItemStack();
        stackCopy.setCount(1);
        purchases.add(0, new PurchaseRecord(stackCopy, cleanName, price, item.getSettings().getMinQuantity(), currentTime));
        if (purchases.size() > 50) {
            purchases.remove(purchases.size() - 1);
        }
    }

    public static void addPurchase(String itemName, int price) {
        String cleanName = itemName;
        long currentTime = System.currentTimeMillis();
        for (PurchaseRecord record : purchases) {
            if (record.itemName.equals(cleanName) && record.price == price && (currentTime - record.timestamp) < 1000) {
                return;
            }
        }
        purchases.add(0, new PurchaseRecord(null, cleanName, price, 1, currentTime));
        if (purchases.size() > 50) {
            purchases.remove(purchases.size() - 1);
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta, int width, int height, int backgroundWidth, int backgroundHeight) {
        float panelX = (width - backgroundWidth) / 2f - 185f;
        float panelY = (height - backgroundHeight) / 2f;
        float panelWidth = 180f;
        float panelHeight = backgroundHeight;
        context.getMatrices().push();
        rectangle.render(ShapeProperties.create(context.getMatrices(), panelX, panelY, panelWidth, panelHeight)
                .round(6)
                .thickness(2)
                .outlineColor(new Color(54, 54, 56, 255).getRGB())
                .color(new Color(12, 12, 12, 200).getRGB())
                .build());
        Fonts.getSize(14, Fonts.Type.SEMI).drawString(context.getMatrices(), "История покупок", panelX + 23, panelY + 10, new Color(255, 255, 255, 255).getRGB());
        if (!purchases.isEmpty()) {
            float contentHeight = purchases.size() * 50f;
            float maxScroll = Math.max(0, contentHeight - (panelHeight - 30));
            scroll = Math.min(0, Math.max(scroll, -maxScroll));
            smoothedScroll = smoothedScroll + (scroll - smoothedScroll) * 0.15f;
            float itemY = panelY + 30 + smoothedScroll;
            List<PurchaseRecord> purchasesCopy = new ArrayList<>(purchases);
            for (PurchaseRecord record : purchasesCopy) {
                if (itemY + 50 >= panelY + 30 && itemY <= panelY + panelHeight - 10) {
                    if (record.itemStack != null && !record.itemStack.isEmpty()) {
                        Render2D.defaultDrawStack(context, record.itemStack, panelX + 6, itemY + 13.5f, false, false, 1.0f);
                    }
                    String displayName = record.itemName;
                    if (displayName.length() > 18) {
                        displayName = displayName.substring(0, 15) + "...";
                    }
                    Fonts.getSize(12, Fonts.Type.SEMI).drawString(context.getMatrices(), displayName, panelX + 30, itemY + 5, new Color(255, 255, 255, 255).getRGB());
                    String priceText = "$" + formatPrice(record.price);
                    if (record.quantity > 1) {
                        priceText += " x" + record.quantity;
                    }
                    Fonts.getSize(11, Fonts.Type.REGULAR).drawString(context.getMatrices(), priceText, panelX + 30, itemY + 16, new Color(100, 255, 100, 255).getRGB());
                    String timeText = timeFormat.format(new Date(record.timestamp));
                    Fonts.getSize(10, Fonts.Type.REGULAR).drawString(context.getMatrices(), timeText, panelX + 30, itemY + 26, new Color(150, 150, 150, 255).getRGB());
                }
                itemY += 50;
            }
            if (maxScroll > 0) {
                float scrollbarHeight = Math.max(20, (panelHeight - 30) * (panelHeight - 30) / contentHeight);
                float scrollPercent = -smoothedScroll / maxScroll;
                float scrollbarY = panelY + 30 + scrollPercent * (panelHeight - 30 - scrollbarHeight);
                rectangle.render(ShapeProperties.create(context.getMatrices(), panelX + panelWidth - 6, panelY + 30, 3, panelHeight - 30)
                        .round(1)
                        .color(new Color(30, 30, 30, 100).getRGB())
                        .build());
                rectangle.render(ShapeProperties.create(context.getMatrices(), panelX + panelWidth - 6, scrollbarY, 3, scrollbarHeight)
                        .round(1.5f)
                        .color(new Color(100, 100, 100, 180).getRGB())
                        .build());
            }
        } else {
            Fonts.getSize(13, Fonts.Type.REGULAR).drawString(context.getMatrices(), "Покупки отсутствуют", panelX + panelWidth / 2 - 35, panelY + 105, new Color(150, 150, 150, 255).getRGB());
        }
        context.getMatrices().pop();
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount, int width, int height, int backgroundWidth, int backgroundHeight) {
        if (purchases.isEmpty()) return false;
        float panelX = (width - backgroundWidth) / 2f - 185f;
        float panelY = (height - backgroundHeight) / 2f;
        float panelWidth = 180f;
        float panelHeight = backgroundHeight;
        if (mouseX >= panelX && mouseX <= panelX + panelWidth && mouseY >= panelY && mouseY <= panelY + panelHeight) {
            scroll += amount * 20;
            return true;
        }
        return false;
    }

    private String formatPrice(int price) {
        if (price >= 1_000_000) {
            return String.format("%.1fM", price / 1_000_000.0);
        } else if (price >= 1_000) {
            return String.format("%.1fK", price / 1_000.0);
        }
        return String.valueOf(price);
    }

    public static void clear() {
        purchases.clear();
    }

    public static class PurchaseRecord {
        private final ItemStack itemStack;
        private final String itemName;
        private final int price;
        private final int quantity;
        private final long timestamp;

        public PurchaseRecord(ItemStack itemStack, String itemName, int price, int quantity, long timestamp) {
            this.itemStack = itemStack;
            this.itemName = itemName;
            this.price = price;
            this.quantity = quantity;
            this.timestamp = timestamp;
        }
    }
}
