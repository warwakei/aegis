package fun.aegis.utils.display.widget;

import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.display.shape.ShapeProperties;
import net.minecraft.client.gui.DrawContext;

import java.awt.Color;

public class ContainerBackgroundRender implements QuickImports {

    public static void renderDefault(DrawContext context, int x, int y, int width, int height) {
        renderCustom(context, x, y, width, height,
                new Color(18, 19, 20, 245),
                new Color(0, 2, 5, 245),
                8f
        );
    }

    public static void renderInventory(DrawContext context, int x, int y, int width, int height) {
        renderCustom(context, x, y, width, height,
                new Color(25, 25, 30, 240),
                new Color(15, 15, 20, 240),
                6f
        );
    }


    public static void renderFurnace(DrawContext context, int x, int y, int width, int height) {
        renderCustom(context, x, y, width, height,
                new Color(30, 25, 20, 245),
                new Color(20, 15, 10, 245),
                7f
        );
    }

    public static void renderEnchanting(DrawContext context, int x, int y, int width, int height) {
        renderCustom(context, x, y, width, height,
                new Color(20, 15, 30, 250),
                new Color(10, 5, 20, 250),
                10f
        );
    }

    public static void renderCustom(DrawContext context, int x, int y, int width, int height,
                                    Color mainColor, Color outlineColor, float round) {
        ShapeProperties containerBg = ShapeProperties.create(
                        context.getMatrices(),
                        x,
                        y,
                        width,
                        height
                )
                .round(round)
                .color(mainColor.getRGB())
                .build();

        rectangle.render(containerBg);

        ShapeProperties outline = ShapeProperties.create(
                        context.getMatrices(),
                        x - 1,
                        y - 1,
                        width + 2,
                        height + 2
                )
                .round(round + 1)
                .color(outlineColor.getRGB())
                .build();

        rectangle.render(outline);
    }
}
