package fun.aegis.utils.display.widget;

import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.systemrender.builders.Builder;

import net.minecraft.client.gui.DrawContext;

import java.awt.Color;

public class SliderRender implements QuickImports {

    public static void renderSlider(DrawContext context, int x, int y, int width, int height, double value, boolean active, String text) {
        ShapeProperties animRect = ShapeProperties.create(
                        context.getMatrices(),
                        x + (width - width ) / 2f,
                        y + (height - height ) / 2f,
                        width ,
                        height
                )
                .round(4f)
                .thickness(3)
                .outlineColor(new Color(150,150,150,150).getRGB())
                .color( new Color(18, 19, 20, 125).getRGB(),
                        new Color(0, 2, 5, 125).getRGB(),
                        new Color(0, 2, 5, 125).getRGB(),
                        new Color(18, 19, 20, 125).getRGB())
                .build();

        rectangle.render(animRect);

        ShapeProperties slider = ShapeProperties.create(context.getMatrices(), (float) (x + (value * (width - 7))), y + 1, 7f, height - 2)
                .round(2.5f)
                .color(new Color(124, 124, 124, active ? 155 : 0).getRGB())
                .build();
        rectangle.render(slider);

        if (text != null && !text.isEmpty()) {
            Fonts.getSize(18, Fonts.Type.DEFAULT)
                    .drawString(context.getMatrices(),
                            text,
                            x - Fonts.getSize(18, Fonts.Type.DEFAULT).getStringWidth(text) / 2f + width / 2f,
                            y + 7f,
                            Color.WHITE.getRGB());
        }
    }
}
