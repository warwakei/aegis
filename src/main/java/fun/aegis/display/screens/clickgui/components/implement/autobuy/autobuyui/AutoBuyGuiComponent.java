package fun.aegis.display.screens.clickgui.components.implement.autobuy.autobuyui;

import fun.aegis.display.screens.clickgui.components.implement.autobuy.items.AutoBuyableItem;
import fun.aegis.display.screens.clickgui.components.implement.autobuy.manager.AutoBuyManager;
import fun.aegis.display.screens.clickgui.components.implement.autobuy.originalitems.ItemRegistry;
import fun.aegis.display.screens.clickgui.components.implement.autobuy.window.AutoBuyItemSettingsWindow;
import fun.aegis.display.screens.clickgui.components.implement.other.StatusRender;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.utils.display.font.FontRenderer;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.display.screens.clickgui.MenuScreen;
import fun.aegis.display.screens.clickgui.components.AbstractComponent;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.display.geometry.Render2D;
import fun.aegis.Aegis;
import fun.aegis.utils.display.scissor.ScissorAssist;
import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

@Setter
@Accessors(chain = true)
public class AutoBuyGuiComponent extends AbstractComponent {
    private float scroll = 0f;
    private float smoothedScroll = 0f;
    private final List<StatusRender> itemStatusRenders = new ArrayList<>();
    private final AutoBuyManager autoBuyManager = AutoBuyManager.getInstance();

    public AutoBuyGuiComponent() {
        updateItemStatusRenders();
    }

    private void updateItemStatusRenders() {
        itemStatusRenders.clear();
        List<AutoBuyableItem> allItems = ItemRegistry.getAllItems();
        for (AutoBuyableItem item : allItems) {
            StatusRender itemStatus = new StatusRender();
            itemStatus.setState(item.isEnabled())
                    .setRunnable(() -> {
                        autoBuyManager.toggleItem(item);
                        updateItemStatusRenders();
                    });
            itemStatusRenders.add(itemStatus);
        }
    }

    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        renderAllItems(context, matrix, mouseX, mouseY, delta);
    }

    private void renderAllItems(DrawContext context, MatrixStack matrix, int mouseX, int mouseY, float delta) {
        Matrix4f positionMatrix = matrix.peek().getPositionMatrix();
        ScissorAssist scissorManager = Aegis.getInstance().getScissorManager();
        float listX = x + 55f;
        float listY = y + 25f;
        float listWidth = width - 43f - 15f;
        float listHeight = height - 48f;
        float contentHeight = calculateContentHeight();
        float maxScrollAmount = Math.max(0f, contentHeight - listHeight);
        scroll = MathHelper.clamp(scroll, -maxScrollAmount, 0f);
        smoothedScroll = Calculate.interpolate(smoothedScroll, scroll, 0.15f);
        scissorManager.push(positionMatrix, listX, listY + 4, listWidth, listHeight - 11);
        float itemY = listY + 10f + smoothedScroll;
        int globalIndex = 0;
        List<AutoBuyableItem> krushItems = ItemRegistry.getKrush();
        if (!krushItems.isEmpty()) {
            renderCategoryHeader(matrix, listX, itemY, listWidth, "Крушитель");
            itemY += 20f;
            for (int i = 0; i < krushItems.size(); i++) {
                AutoBuyableItem item = krushItems.get(i);
                float itemX = listX + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 50;
                renderItem(context, matrix, item, itemX, itemY, mouseX, mouseY, delta, globalIndex);
                globalIndex++;
            }
            itemY += 50;
            itemY += 25f;
        }
        List<AutoBuyableItem> talismanItems = ItemRegistry.getTalismans();
        if (!talismanItems.isEmpty()) {
            renderCategoryHeader(matrix, listX, itemY, listWidth, "Талисманы");
            itemY += 20f;
            for (int i = 0; i < talismanItems.size(); i++) {
                AutoBuyableItem item = talismanItems.get(i);
                float itemX = listX + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 50;
                renderItem(context, matrix, item, itemX, itemY, mouseX, mouseY, delta, globalIndex);
                globalIndex++;
            }
            itemY += 50;
            itemY += 25f;
        }
        List<AutoBuyableItem> sphereItems = ItemRegistry.getSpheres();
        if (!sphereItems.isEmpty()) {
            renderCategoryHeader(matrix, listX, itemY, listWidth, "Сферы");
            itemY += 20f;
            for (int i = 0; i < sphereItems.size(); i++) {
                AutoBuyableItem item = sphereItems.get(i);
                float itemX = listX + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 50;
                renderItem(context, matrix, item, itemX, itemY, mouseX, mouseY, delta, globalIndex);
                globalIndex++;
            }
            itemY += 50;
            itemY += 25f;
        }
        List<AutoBuyableItem> miscItems = ItemRegistry.getMisc();
        if (!miscItems.isEmpty()) {
            renderCategoryHeader(matrix, listX, itemY, listWidth, "Разное");
            itemY += 20f;
            for (int i = 0; i < miscItems.size(); i++) {
                AutoBuyableItem item = miscItems.get(i);
                float itemX = listX + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 50;
                renderItem(context, matrix, item, itemX, itemY, mouseX, mouseY, delta, globalIndex);
                globalIndex++;
            }
            itemY += 50;
            itemY += 25f;
        }
        List<AutoBuyableItem> donatorItems = ItemRegistry.getDonator();
        if (!donatorItems.isEmpty()) {
            renderCategoryHeader(matrix, listX, itemY, listWidth, "Донаторские");
            itemY += 20f;
            for (int i = 0; i < donatorItems.size(); i++) {
                AutoBuyableItem item = donatorItems.get(i);
                float itemX = listX + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 50;
                renderItem(context, matrix, item, itemX, itemY, mouseX, mouseY, delta, globalIndex);
                globalIndex++;
            }
            itemY += 50;
            itemY += 25f;
        }
        List<AutoBuyableItem> potionItems = ItemRegistry.getPotions();
        if (!potionItems.isEmpty()) {
            renderCategoryHeader(matrix, listX, itemY, listWidth, "Зелья");
            itemY += 20f;
            for (int i = 0; i < potionItems.size(); i++) {
                AutoBuyableItem item = potionItems.get(i);
                float itemX = listX + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 50;
                renderItem(context, matrix, item, itemX, itemY, mouseX, mouseY, delta, globalIndex);
                globalIndex++;
            }
            itemY += 50;
            itemY += 10f;
        }
        scissorManager.pop();
    }

    private float calculateContentHeight() {
        List<AutoBuyableItem> krushItems = ItemRegistry.getKrush();
        List<AutoBuyableItem> talismanItems = ItemRegistry.getTalismans();
        List<AutoBuyableItem> sphereItems = ItemRegistry.getSpheres();
        List<AutoBuyableItem> miscItems = ItemRegistry.getMisc();
        List<AutoBuyableItem> donatorItems = ItemRegistry.getDonator();
        List<AutoBuyableItem> potionItems = ItemRegistry.getPotions();
        float totalHeight = 10f;
        if (!krushItems.isEmpty()) {
            totalHeight += 20f + ((krushItems.size() + 1) / 2) * 50f + 25f;
        }
        if (!talismanItems.isEmpty()) {
            totalHeight += 20f + ((talismanItems.size() + 1) / 2) * 50f + 25f;
        }
        if (!sphereItems.isEmpty()) {
            totalHeight += 20f + ((sphereItems.size() + 1) / 2) * 50f + 25f;
        }
        if (!miscItems.isEmpty()) {
            totalHeight += 20f + ((miscItems.size() + 1) / 2) * 50f + 25f;
        }
        if (!donatorItems.isEmpty()) {
            totalHeight += 20f + ((donatorItems.size() + 1) / 2) * 50f + 25f;
        }
        if (!potionItems.isEmpty()) {
            totalHeight += 20f + ((potionItems.size() + 1) / 2) * 50f + 10f;
        }
        return totalHeight;
    }

    private void renderCategoryHeader(MatrixStack matrix, float x, float y, float width, String categoryName) {
        float textWidth = Fonts.getSize(14, Fonts.Type.SEMI).getStringWidth(categoryName);
        float lineWidth = (width - textWidth - 20f) / 2f;

        rectangle.render(ShapeProperties.create(matrix, x, y + 6, lineWidth - 10, 1)
                .color(new Color(54, 54, 56, 255).getRGB())
                .build());
        Fonts.getSize(14, Fonts.Type.SEMI).drawString(matrix, categoryName, x + lineWidth, y + 4, ColorAssist.getText(1f));
        rectangle.render(ShapeProperties.create(matrix, x + lineWidth + textWidth + 10f, y + 6, lineWidth - 6, 1)
                .color(new Color(54, 54, 56, 255).getRGB())
                .build());
    }

    private void renderItem(DrawContext context, MatrixStack matrix, AutoBuyableItem item, float itemX, float itemY, int mouseX, int mouseY, float delta, int index) {
        FontRenderer font = Fonts.getSize(16, Fonts.Type.SEMI);
        ItemStack itemStack = item.createItemStack();

        blur.render(ShapeProperties.create(matrix, itemX, itemY, 175, 45)
                .round(6).quality(64)
                .color(new Color(0, 0, 0, 200).getRGB())
                .build());

        rectangle.render(ShapeProperties.create(matrix, itemX, itemY, 175, 45)
                .round(6)
                .softness(2)
                .thickness(0.1f)
                .outlineColor(new Color(18, 19, 20, 225).getRGB())
                .color(
                        new Color(18, 19, 20, 175).getRGB(),
                        new Color(0, 2, 5, 175).getRGB(),
                        new Color(0, 2, 5, 175).getRGB(),
                        new Color(18, 19, 20, 175).getRGB())
                .build());

        Render2D.defaultDrawStack(context, itemStack, itemX + 6, itemY + 13.5f, false, false, 1.0f);
        Fonts.getSize(14, Fonts.Type.SEMI).drawGradientString(matrix, item.getDisplayName(), itemX + 30, itemY + 14, ColorAssist.getText(), ColorAssist.getText(0.65F));
        Fonts.getSize(12, Fonts.Type.REGULAR).drawString(matrix, "Цена покупки: $" + item.getSettings().getBuyBelow(), itemX + 30, itemY + 23, ColorAssist.getText(0.65F));
//        Fonts.getSize(12, Fonts.Type.REGULAR).drawString(matrix, "Цена продажи: $" + item.getSettings().getSellAbove(), itemX + 30, itemY + 26, ColorAssist.getText(0.65F));
        Fonts.getSize(12, Fonts.Type.REGULAR).drawString(matrix, "Каличество покупки от: " + item.getSettings().getMinQuantity(), itemX + 30, itemY + 30, ColorAssist.getText(0.65F));
        if (index < itemStatusRenders.size()) {
            itemStatusRenders.get(index).position(itemX + 160, itemY + 18.5f).render(context, mouseX, mouseY, delta);
        }
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float listX = x + 55f;
        float listY = y + 25f;
        float itemY = listY + 10f + smoothedScroll;
        int globalIndex = 0;

        List<AutoBuyableItem> krushItems = ItemRegistry.getKrush();
        if (!krushItems.isEmpty()) {
            itemY += 20f;
            for (int i = 0; i < krushItems.size(); i++) {
                AutoBuyableItem item = krushItems.get(i);
                float itemX = listX + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 50;

                if (button == 1 && Calculate.isHovered(mouseX, mouseY, itemX, itemY, 175, 45)) {
                    openSettingsWindow(item);
                    return true;
                }

                if (button == 0 && globalIndex < itemStatusRenders.size()) {
                    StatusRender status = itemStatusRenders.get(globalIndex);
                    status.position(itemX + 160, itemY + 18.5f);
                    if (status.mouseClicked(mouseX, mouseY, button)) {
                        autoBuyManager.toggleItem(item);
                        updateItemStatusRenders();
                        return true;
                    }
                }
                globalIndex++;
            }
            itemY += 50;
            itemY += 25f;
        }

        List<AutoBuyableItem> talismanItems = ItemRegistry.getTalismans();
        if (!talismanItems.isEmpty()) {
            itemY += 20f;
            for (int i = 0; i < talismanItems.size(); i++) {
                AutoBuyableItem item = talismanItems.get(i);
                float itemX = listX + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 50;

                if (button == 1 && Calculate.isHovered(mouseX, mouseY, itemX, itemY, 175, 45)) {
                    openSettingsWindow(item);
                    return true;
                }

                if (button == 0 && globalIndex < itemStatusRenders.size()) {
                    StatusRender status = itemStatusRenders.get(globalIndex);
                    status.position(itemX + 160, itemY + 18.5f);
                    if (status.mouseClicked(mouseX, mouseY, button)) {
                        autoBuyManager.toggleItem(item);
                        updateItemStatusRenders();
                        return true;
                    }
                }
                globalIndex++;
            }
            itemY += 50;
            itemY += 25f;
        }

        List<AutoBuyableItem> sphereItems = ItemRegistry.getSpheres();
        if (!sphereItems.isEmpty()) {
            itemY += 20f;
            for (int i = 0; i < sphereItems.size(); i++) {
                AutoBuyableItem item = sphereItems.get(i);
                float itemX = listX + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 50;

                if (button == 1 && Calculate.isHovered(mouseX, mouseY, itemX, itemY, 175, 45)) {
                    openSettingsWindow(item);
                    return true;
                }

                if (button == 0 && globalIndex < itemStatusRenders.size()) {
                    StatusRender status = itemStatusRenders.get(globalIndex);
                    status.position(itemX + 160, itemY + 18.5f);
                    if (status.mouseClicked(mouseX, mouseY, button)) {
                        autoBuyManager.toggleItem(item);
                        updateItemStatusRenders();
                        return true;
                    }
                }
                globalIndex++;
            }
            itemY += 50;
            itemY += 25f;
        }

        List<AutoBuyableItem> miscItems = ItemRegistry.getMisc();
        if (!miscItems.isEmpty()) {
            itemY += 20f;
            for (int i = 0; i < miscItems.size(); i++) {
                AutoBuyableItem item = miscItems.get(i);
                float itemX = listX + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 50;

                if (button == 1 && Calculate.isHovered(mouseX, mouseY, itemX, itemY, 175, 45)) {
                    openSettingsWindow(item);
                    return true;
                }

                if (button == 0 && globalIndex < itemStatusRenders.size()) {
                    StatusRender status = itemStatusRenders.get(globalIndex);
                    status.position(itemX + 160, itemY + 18.5f);
                    if (status.mouseClicked(mouseX, mouseY, button)) {
                        autoBuyManager.toggleItem(item);
                        updateItemStatusRenders();
                        return true;
                    }
                }
                globalIndex++;
            }
            itemY += 50;
            itemY += 25f;
        }

        List<AutoBuyableItem> donatorItems = ItemRegistry.getDonator();
        if (!donatorItems.isEmpty()) {
            itemY += 20f;
            for (int i = 0; i < donatorItems.size(); i++) {
                AutoBuyableItem item = donatorItems.get(i);
                float itemX = listX + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 50;

                if (button == 1 && Calculate.isHovered(mouseX, mouseY, itemX, itemY, 175, 45)) {
                    openSettingsWindow(item);
                    return true;
                }

                if (button == 0 && globalIndex < itemStatusRenders.size()) {
                    StatusRender status = itemStatusRenders.get(globalIndex);
                    status.position(itemX + 160, itemY + 18.5f);
                    if (status.mouseClicked(mouseX, mouseY, button)) {
                        autoBuyManager.toggleItem(item);
                        updateItemStatusRenders();
                        return true;
                    }
                }
                globalIndex++;
            }
            itemY += 50;
            itemY += 25f;
        }

        List<AutoBuyableItem> potionItems = ItemRegistry.getPotions();
        if (!potionItems.isEmpty()) {
            itemY += 20f;
            for (int i = 0; i < potionItems.size(); i++) {
                AutoBuyableItem item = potionItems.get(i);
                float itemX = listX + (i % 2) * 190;
                if (i % 2 == 0 && i > 0) itemY += 50;

                if (button == 1 && Calculate.isHovered(mouseX, mouseY, itemX, itemY, 175, 45)) {
                    openSettingsWindow(item);
                    return true;
                }

                if (button == 0 && globalIndex < itemStatusRenders.size()) {
                    StatusRender status = itemStatusRenders.get(globalIndex);
                    status.position(itemX + 160, itemY + 18.5f);
                    if (status.mouseClicked(mouseX, mouseY, button)) {
                        autoBuyManager.toggleItem(item);
                        updateItemStatusRenders();
                        return true;
                    }
                }
                globalIndex++;
            }
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void openSettingsWindow(AutoBuyableItem item) {
        if (MenuScreen.windowManager.getWindows().stream().noneMatch(w -> w instanceof AutoBuyItemSettingsWindow && ((AutoBuyItemSettingsWindow) w).item.equals(item))) {
            AutoBuyItemSettingsWindow settingsWindow = new AutoBuyItemSettingsWindow(item, item.getSettings());
            settingsWindow.position(MenuScreen.INSTANCE.x + MenuScreen.INSTANCE.width + 24, MenuScreen.INSTANCE.y).size(180, settingsWindow.getComponentHeight());
            MenuScreen.windowManager.add(settingsWindow);
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (Calculate.isHovered(mouseX, mouseY, x + 55, y + 38, width - 43 - 15, height - 48)) {
            scroll += amount * 20;
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }
}
