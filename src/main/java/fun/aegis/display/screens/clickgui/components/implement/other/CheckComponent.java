package fun.aegis.display.screens.clickgui.components.implement.other;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import fun.aegis.common.animation.Animation;
import fun.aegis.common.animation.implement.Decelerate;
import fun.aegis.display.screens.clickgui.components.AbstractComponent;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.color.ColorAssist;

import java.awt.*;

import static fun.aegis.common.animation.Direction.BACKWARDS;
import static fun.aegis.common.animation.Direction.FORWARDS;

@Setter
@Accessors(chain = true)
public class CheckComponent extends AbstractComponent {
    private boolean state;
    private Runnable runnable;
    private final Animation alphaAnimation = new Decelerate().setMs(180).setValue(255);

    private static final int RED_COLOR = new Color(180, 80, 80).getRGB();

    @Override
    public CheckComponent position(float x, float y) {
        this.x = x;
        this.y = y;
        return this;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        alphaAnimation.setDirection(state ? FORWARDS : BACKWARDS);

        // j = включено (цвет клиента), l = выключено (красный)
        String icon = state ? "j" : "l";
        int color = state ? ColorAssist.getClientColor() : RED_COLOR;
        
        Fonts.getSize(14, Fonts.Type.ICONSCATEGORY).drawString(matrix, icon, x, y + 2f, color);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (Calculate.isHovered(mouseX, mouseY, x - 2, y - 2, 16, 16) && button == 0) {
            runnable.run();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
