package fun.aegis.display.screens.clickgui.components.implement.module;

import fun.aegis.display.screens.clickgui.components.AbstractComponent;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.setting.SettingComponentAdder;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.display.screens.clickgui.components.implement.settings.AbstractSettingComponent;
import fun.aegis.display.screens.clickgui.components.implement.window.AbstractWindow;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.display.scissor.ScissorAssist;
import fun.aegis.Aegis;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ModuleSettingsWindow extends AbstractWindow {
    private final List<AbstractSettingComponent> components = new ArrayList<>();
    public final Module module;

    public ModuleSettingsWindow(Module module) {
        this.module = module;
        new SettingComponentAdder().addSettingComponent(module.settings(), components);
        draggable(true);
    }

    @Override
    public void drawWindow(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        ScissorAssist scissorManager = Aegis.getInstance().getScissorManager();
        height = MathHelper.clamp(getComponentHeight() + 5, 0, 200);

        blur.render(ShapeProperties.create(matrix, x, y, width, height).round(8).quality(64)
                .color(new Color(0, 0, 0, 200).getRGB())
                .build());

        rectangle.render(ShapeProperties.create(matrix, x, y, width, height).round(8)

                .thickness(0.1f)
                .outlineColor(new Color(18, 19, 20, 225).getRGB())
                .color(
                        new Color(18, 19, 20, 175).getRGB(),
                        new Color(0, 2, 5, 175).getRGB(),
                        new Color(0, 2, 5, 175).getRGB(),
                        new Color(18, 19, 20, 175).getRGB())
                .build());


        rectangle.render(ShapeProperties.create(matrix, x, y + 22, width, 0.5f)
                .color(new Color(48, 48, 48, 255).getRGB())
                .build());

        Fonts.getSize(15, Fonts.Type.SEMI).drawGradientString(context.getMatrices(), module.getVisibleName() + " Settings",
                x + 10, y + 10, ColorAssist.getText(), new Color(165, 165, 165, 255).getRGB());

        Fonts.getSize(17, Fonts.Type.ICONS).drawString(context.getMatrices(), "K", x + 145, y + 10, ColorAssist.getText(0.5f));

        boolean isLimitedHeight = MathHelper.clamp(height, 0, 200) == 200;
        if (isLimitedHeight) scissorManager.push(matrix.peek().getPositionMatrix(), x, y + 23, width, height - 24);
        float offset = 0;
        int totalHeight = 0;
        for (int i = components.size() - 1; i >= 0; i--) {
            AbstractSettingComponent component = components.get(i);
            Supplier<Boolean> visible = component.getSetting().getVisible();
            if (visible != null && !visible.get()) continue;
            component.x = x;
            component.y = (float) (y + 22 + offset + (getComponentHeight() - 25 - component.height) + smoothedScroll);
            component.width = width;
            component.render(context, mouseX, mouseY, delta);
            offset -= component.height;
            totalHeight += (int) component.height;
        }
        if (isLimitedHeight) scissorManager.pop();
        int maxScroll = (int) Math.max(0, totalHeight - (height - 28));
        scroll = MathHelper.clamp(scroll, -maxScroll, 0);
        smoothedScroll = MathHelper.lerp(0.1F, smoothedScroll, scroll);

        if (isLimitedHeight) {
            float viewableHeight = height - 30;
            float scrollbarHeight = Math.max(20, (viewableHeight / totalHeight) * viewableHeight);
            float scrollPercent = (float) (-smoothedScroll / (float) maxScroll);
            float scrollbarY = y + 30 + (scrollPercent * (viewableHeight - scrollbarHeight));

            float scrollbarX = x + width - 6;
            float scrollbarWidth = 3;

            rectangle.render(ShapeProperties.create(matrix, scrollbarX, y + 30, scrollbarWidth, viewableHeight - 6)
                    .round(1)
                    .color(new Color(30, 30, 30, 100).getRGB())
                    .build());

            rectangle.render(ShapeProperties.create(matrix, scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight - 6)
                    .round(1.5f)
                    .color(new Color(100, 100, 100, 180).getRGB())
                    .build());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (Calculate.isHovered(mouseX, mouseY, x + 145, y + 5, 15, 15)) {
                startCloseAnimation();
                return true;
            }
            if (Calculate.isHovered(mouseX, mouseY, x, y, width, 19)) {
                dragging = true;
                dragX = (int) (x - mouseX);
                dragY = (int) (y - mouseY);
                return true;
            }
        }
        boolean isAnyComponentHovered = components.stream().anyMatch(abstractComponent -> abstractComponent.isHover(mouseX, mouseY));
        if (isAnyComponentHovered) {
            components.forEach(abstractComponent -> {
                if (abstractComponent.isHover(mouseX, mouseY)) {
                    abstractComponent.mouseClicked(mouseX, mouseY, button);
                }
            });
            return true;
        }
        components.forEach(abstractComponent -> abstractComponent.mouseClicked(mouseX, mouseY, button));
        return true;
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        components.forEach(abstractComponent -> abstractComponent.isHover(mouseX, mouseY));
        for (AbstractComponent abstractComponent : components) {
            if (abstractComponent.isHover(mouseX, mouseY)) return true;
        }
        return super.isHovered(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        components.forEach(abstractComponent -> abstractComponent.mouseReleased(mouseX, mouseY, button));
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        boolean scrolled = MathHelper.clamp(height, 0, 200) == 200 && Calculate.isHovered(mouseX, mouseY, x, y, width, height);
        if (scrolled) scroll += amount * 20;
        components.forEach(abstractComponent -> abstractComponent.mouseScrolled(mouseX, mouseY, amount));
        return scrolled;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        components.forEach(abstractComponent -> abstractComponent.keyPressed(keyCode, scanCode, modifiers));
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        components.forEach(abstractComponent -> abstractComponent.charTyped(chr, modifiers));
        return super.charTyped(chr, modifiers);
    }

    public int getComponentHeight() {
        float offsetY = 0;
        for (AbstractSettingComponent component : components) {
            Supplier<Boolean> visible = component.getSetting().getVisible();
            if (visible != null && !visible.get()) continue;
            offsetY += component.height;
        }
        return (int) (offsetY + 25);
    }
}
