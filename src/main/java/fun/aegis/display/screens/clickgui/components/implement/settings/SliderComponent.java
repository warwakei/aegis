package fun.aegis.display.screens.clickgui.components.implement.settings;

import fun.aegis.Aegis;
import fun.aegis.display.screens.clickgui.MenuScreen;
import fun.aegis.utils.display.scissor.ScissorAssist;
import fun.aegis.utils.display.color.ColorAssist;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.math.calc.Calculate;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;

import static fun.aegis.common.animation.Easy.Direction.BACKWARDS;
import static fun.aegis.utils.display.font.Fonts.Type.*;

public class SliderComponent extends AbstractSettingComponent {
    public static final int SLIDER_WIDTH = 65;

    private final SliderSettings setting;

    private boolean dragging;
    private double animation;

    public SliderComponent(SliderSettings setting) {
        super(setting);
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        // Сбрасываем dragging если GUI закрывается или ЛКМ отпущена
        if (MenuScreen.INSTANCE.animation.getDirection() == BACKWARDS) {
            dragging = false;
        }
        
        // Проверяем, что ЛКМ всё ещё нажата
        if (dragging && GLFW.glfwGetMouseButton(mc.getWindow().getHandle(), GLFW.GLFW_MOUSE_BUTTON_LEFT) != GLFW.GLFW_PRESS) {
            dragging = false;
        }

        height = 28;

        String value = String.valueOf(setting.getValue());
        float valueW = Fonts.getSize(14, BOLD).getStringWidth(value);

        float nameX = x + 8;
        float nameY = y + 9f;
        float maxNameW = Math.max(0, (x + width - 9f - valueW - 6f) - nameX);

        float nameWidth = Fonts.getSize(14, DEFAULT).getStringWidth(setting.getName());
        if (nameWidth > maxNameW) {
            ScissorAssist scissor = Aegis.getInstance().getScissorManager();
            scissor.push(matrix.peek().getPositionMatrix(), nameX, y + 3f, maxNameW, 14f);
            Fonts.getSize(14, DEFAULT).drawStringWithScroll(matrix, setting.getName(), nameX, nameY, maxNameW, new Color(225, 225, 225, 225).getRGB());
            scissor.pop();
        } else {
            Fonts.getSize(14, DEFAULT).drawString(matrix, setting.getName(), nameX, nameY, 0xFFD4D6E1);
        }

        int clientColor = ColorAssist.getClientColor();
        Fonts.getSize(14, BOLD).drawString(matrix, value, x + width - 9 - valueW, nameY, clientColor);

        float sliderX = x + 8;
        float sliderY = y + 22f;
        float sliderW = Math.max(40f, width - 17f);

        float diff = getDifference(mouseX, matrix, sliderX, sliderY, sliderW);
        changeValue(diff, sliderW);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float sliderX = x + 8;
        float sliderY = y + 22f;
        float sliderW = Math.max(40f, width - 17f);
        dragging = Calculate.isHovered(mouseX, mouseY, sliderX, sliderY - 3.5f, sliderW, 8f) && button == 0;
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private float getDifference(int mouseX, MatrixStack matrix, float sliderX, float sliderY, float sliderW) {
        float percentValue = sliderW * (setting.getValue() - setting.getMin()) / (setting.getMax() - setting.getMin()),
                difference = MathHelper.clamp(mouseX - sliderX, 0, sliderW);

        animation = Calculate.interpolate(animation, percentValue);

        int clientColor = ColorAssist.getClientColor();
        int sliderFillColor = ColorAssist.multAlpha(clientColor, 0.7f);

        rectangle.render(ShapeProperties.create(matrix, sliderX, sliderY, sliderW, 3).round(1)
                .color(0x2D2E414D).build());

        rectangle.render(ShapeProperties.create(matrix, sliderX, sliderY, (float) animation, 3).round(1)
                .color(sliderFillColor).build());

        float v = MathHelper.clamp((float) (sliderX + animation), sliderX, sliderX + sliderW);

        blur.render(ShapeProperties.create(matrix, v - 3F, sliderY - 1F, 5, 5)
                .round(3).softness(0).color(new Color(255, 255, 255, 220).getRGB()).build());

        return difference;
    }

    private void changeValue(float difference, float sliderW) {
        BigDecimal bd = BigDecimal.valueOf((difference / sliderW) * (setting.getMax() - setting.getMin()) + setting.getMin())
                .setScale(2, RoundingMode.HALF_UP);

        if (dragging) {
            float value = difference == 0 ? setting.getMin() : bd.floatValue();
            if (setting.isInteger()) value = (int) value;
            setting.setValue(value);
        }
    }
}
