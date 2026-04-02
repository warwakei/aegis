package fun.aegis.display.screens.clickgui.components.implement.settings;

import fun.aegis.Aegis;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.glfw.GLFW;

import fun.aegis.features.module.setting.implement.BindSetting;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.scissor.ScissorAssist;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.client.chat.StringHelper;

import java.awt.*;

import static fun.aegis.utils.display.font.Fonts.Type.*;

public class BindComponent extends AbstractSettingComponent {
    private final BindSetting setting;
    private boolean binding;

    public BindComponent(BindSetting setting) {
        super(setting);
        this.setting = setting;
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        java.lang.String bindName = StringHelper.getBindName(setting.getKey());
        java.lang.String name = binding ? "(" + bindName + ") ..." : bindName;
        float stringWidth = Fonts.getSize(12, SEMI).getStringWidth(name) - 2;

        height = 18;

        rectangle.render(ShapeProperties.create(matrix, x + width - stringWidth - 19, y + 6.0f, stringWidth + 10, 12.5f)
                .round(3).softness(1).thickness(2).outlineColor(new Color(55,52,55,255).getRGB())
                .color(
                        new Color(25,22,25,0).getRGB(),
                        new Color(31,27,35,0).getRGB(),
                        new Color(31,27,35,0).getRGB(),
                        new Color(25,22,25,0).getRGB())
                .build());
        int bindingColor = ColorHelper.getArgb(255, 135, 136, 148);

        Fonts.getSize(12, SEMI).drawString(matrix, name, x + width - 14 - stringWidth - 1, y + 11.5f, bindingColor);

        float nameX = x + 8;
        float nameY = y + 10.5f;
        float bindBoxX = x + width - stringWidth - 19;
        float maxNameW = Math.max(0, bindBoxX - 6f - nameX);
        ScissorAssist scissor = Aegis.getInstance().getScissorManager();
        scissor.push(matrix.peek().getPositionMatrix(), nameX, y, maxNameW, height);
        Fonts.getSize(14, DEFAULT).drawStringWithScroll(context.getMatrices(), setting.getName(), nameX, nameY, maxNameW, 0xFFD4D6E1);
        scissor.pop();
//        Fonts.getSize(12, DEFAULT).drawString(context.getMatrices(), wrapped, x + 9, y + 15, 0xFF878894);
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (Calculate.isHovered(mouseX, mouseY, x, y, width, height)) {
                binding = !binding;
            } else {
                binding = false;
            }
        }

        if (binding && button > 1) {
            setting.setKey(button);
            binding = false;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int key = keyCode == GLFW.GLFW_KEY_DELETE ? -1 : keyCode;
        if (binding) {
            setting.setKey(key);
            binding = false;
        }

        return super.keyPressed(keyCode, scanCode, modifiers);
    }
}
