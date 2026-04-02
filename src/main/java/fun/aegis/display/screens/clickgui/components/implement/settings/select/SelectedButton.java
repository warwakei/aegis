package fun.aegis.display.screens.clickgui.components.implement.settings.select;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Vector4f;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.common.animation.Animation;
import fun.aegis.common.animation.Direction;
import fun.aegis.common.animation.implement.Decelerate;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.display.screens.clickgui.components.AbstractComponent;
import fun.aegis.utils.math.calc.Calculate;
import java.awt.*;
import java.util.List;
import static fun.aegis.utils.display.font.Fonts.Type.BOLD;
import static fun.aegis.utils.math.calc.Calculate.*;

public class SelectedButton extends AbstractComponent {
    private final SelectSetting setting;
    private final String text;
    @Setter
    @Accessors(chain = true)
    private float alpha;
    private final Animation alphaAnimation = new Decelerate().setMs(300).setValue(0.5F);

    public SelectedButton(SelectSetting setting, String text) {
        this.setting = setting;
        this.text = text;
        alphaAnimation.setDirection(Direction.BACKWARDS);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        alphaAnimation.setDirection(setting.isSelected(text) ? Direction.FORWARDS : Direction.BACKWARDS);
        float opacity = alphaAnimation.getOutput().floatValue();
        int adjustedAlpha = (int) Calculate.clamp(opacity * alpha * 255, 0, 255);

        if (!alphaAnimation.isFinished(Direction.BACKWARDS)) {
            rectangle.render(ShapeProperties.create(context.getMatrices(), x + 0.5f, y, width - 1, height - 0.5f).round
                    (SelectedButton.getRound(setting.getList(), text)).color(
                    new Color(58, 58, 60, adjustedAlpha).getRGB(),
                    new Color(58, 58, 60, adjustedAlpha).getRGB(),
                    new Color(58, 58, 60, 0).getRGB(),
                    new Color(58, 58, 60, 0).getRGB()).build());

        }

        Fonts.getSize(13, BOLD).drawString(matrix, text, x + 4, y + 5, ColorAssist.multAlpha(new Color(225, 225, 225, 225).getRGB(), Calculate.clamp(alpha, 0, 1)));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (isHovered(mouseX, mouseY, x, y, width, height) && button == 0) {
            setting.setSelected(text);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public static Vector4f getRound(List<String> list, String text) {
        if (list.size() == 1) return new Vector4f(3);
        if (list.get(list.size() - 1).equals(text)) return new Vector4f(0, 3, 0, 3);
        if (list.get(0).equals(text)) return new Vector4f(3, 0, 3, 0);
        return new Vector4f(0);
    }
}
