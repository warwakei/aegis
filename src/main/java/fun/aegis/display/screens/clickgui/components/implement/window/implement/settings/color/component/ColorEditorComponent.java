package fun.aegis.display.screens.clickgui.components.implement.window.implement.settings.color.component;

import lombok.RequiredArgsConstructor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;

import fun.aegis.features.module.setting.implement.ColorSetting;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.display.screens.clickgui.components.AbstractComponent;

@RequiredArgsConstructor
public class ColorEditorComponent extends AbstractComponent {
    private final ColorSetting setting;


    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();


        int displayValue = (int) (setting.getAlpha() * 100);
     }

    
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        float boxW = 22f;
        float boxH = 14f;
        float boxX = x + width - boxW - 6f;
        float boxY = y + 90.5F;
        if (Calculate.isHovered(mouseX, mouseY, boxX, boxY, boxW, boxH)) {
            setting.setAlpha(MathHelper.clamp((float) (setting.getAlpha() - (amount * 2) / 100), 0, 1));
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
