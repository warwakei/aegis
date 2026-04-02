package fun.aegis.display.screens.clickgui.components.implement.settings;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.glfw.GLFW;

import fun.aegis.features.module.setting.implement.BindSetting;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.client.chat.StringHelper;

import java.awt.*;

import static fun.aegis.utils.display.font.Fonts.Type.*;

public class IconBindComponent extends AbstractSettingComponent {
    private final BindSetting setting;
    private boolean binding;

    public IconBindComponent(BindSetting setting) {
        super(setting);
        this.setting = setting;
    }


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();

        java.lang.String bindName = StringHelper.getBindName(setting.getKey());
        java.lang.String name = binding ? "(" + bindName + ") ..." : bindName;
        float stringWidth = Fonts.getSize(12, SEMI).getStringWidth(name) - 2;

        height = 22;

        rectangle.render(ShapeProperties.create(matrix, x + width - stringWidth - 17, y + 6.5f, stringWidth + 10, 13)
                .round(3f)
                .outlineColor(new Color(200, 200, 200, 255).getRGB())
                .color(
                        new Color(61, 67, 71, 80).getRGB(),
                        new Color(71, 77, 81, 80).getRGB(),
                        new Color(81, 87, 91, 80).getRGB(),
                        new Color(91, 97, 101, 80).getRGB())
                .build());
        int bindingColor = ColorHelper.getArgb(255, 135, 136, 148);

        Fonts.getSize(12, SEMI).drawString(matrix, name, x + width - 12 - stringWidth - 1, y + 12.25f, bindingColor);

        Fonts.getSize(14, DEFAULT).drawString(context.getMatrices(), setting.getName(), x + 8, y + 12.25f, 0xFFD4D6E1);
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
