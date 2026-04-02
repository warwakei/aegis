package fun.aegis.display.screens.clickgui.components.implement.window.implement.settings.group;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import fun.aegis.features.module.setting.SettingComponentAdder;
import fun.aegis.features.module.setting.implement.GroupSetting;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.display.screens.clickgui.components.implement.settings.AbstractSettingComponent;
import fun.aegis.display.screens.clickgui.components.implement.window.AbstractWindow;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.display.scissor.ScissorAssist;
import fun.aegis.Aegis;
import fun.aegis.display.screens.clickgui.components.AbstractComponent;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

@Getter
public class GroupWindow extends AbstractWindow {
    private final List<AbstractSettingComponent> components = new ArrayList<>();
    private final GroupSetting setting;

    public GroupWindow(GroupSetting setting) {
        this.setting = setting;
        new SettingComponentAdder().addSettingComponent(setting.getSubSettings(), components);
    }

    @Override
    public void drawWindow(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        ScissorAssist scissorManager = Aegis.getInstance().getScissorManager();
        height = MathHelper.clamp(getComponentHeight(), 0, 200);

        rectangle.render(ShapeProperties.create(matrix, x, y, width  + 30, height).round(4).softness(2).thickness(1).outlineColor(new Color(75, 75, 75, 255).getRGB()).color(
                new Color(14,14,16,255).getRGB(),
                new Color(31,27,35,255).getRGB(),
                new Color(31,27,35,255).getRGB(),
                new Color(14,14,16,255).getRGB()).build());

        Fonts.getSize(15, Fonts.Type.SEMI).drawGradientString(context.getMatrices(), setting.getName() + " Settings",
                x + 10, y + 10, ColorAssist.getText(), new Color(165, 165, 165, 255).getRGB());
        boolean isLimitedHeight = MathHelper.clamp(height, 0, 200) == 200;
        if (isLimitedHeight) scissorManager.push(matrix.peek().getPositionMatrix(), x, y + 23, width, height - 28);
        float offset = 0;
        int totalHeight = 0;
        for (int i = components.size() - 1; i >= 0; i--) {
            AbstractSettingComponent component = components.get(i);
            Supplier<Boolean> visible = component.getSetting().getVisible();
            if (visible != null && !visible.get()) continue;
            component.x = x;
            component.y = (float) (y + 19 + offset + (getComponentHeight() - 25 - component.height) + smoothedScroll);
            component.width = width + 30;
            component.render(context, mouseX, mouseY, delta);
            offset -= component.height;
            totalHeight += (int) component.height;
        }
        if (isLimitedHeight) scissorManager.pop();
        int maxScroll = (int) Math.max(0, totalHeight - (height - 23));
        scroll = MathHelper.clamp(scroll, -maxScroll, 0);
        smoothedScroll = MathHelper.lerp(0.1F, smoothedScroll, scroll);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        draggable(Calculate.isHovered(mouseX, mouseY, x, y, width, 19) && button == 0);
        boolean isAnyComponentHovered = components.stream().anyMatch(abstractComponent -> abstractComponent.isHover(mouseX, mouseY));
        if (isAnyComponentHovered) {
            components.forEach(abstractComponent -> {
                if (abstractComponent.isHover(mouseX, mouseY)) {
                    abstractComponent.mouseClicked(mouseX, mouseY, button);
                }
            });
            return super.mouseClicked(mouseX, mouseY, button);
        }
        components.forEach(abstractComponent -> abstractComponent.mouseClicked(mouseX, mouseY, button));
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        components.forEach(abstractComponent -> abstractComponent.isHover(mouseX, mouseY));
        for (AbstractComponent abstractComponent : components) {
            if (abstractComponent.isHover(mouseX, mouseY)) return true;
        }
        return super.isHover(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
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
