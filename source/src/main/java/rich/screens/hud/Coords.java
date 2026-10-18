package rich.screens.hud;

import net.minecraft.client.gui.DrawContext;
import rich.client.draggables.AbstractHudElement;
import rich.util.render.font.Fonts;

import java.awt.*;

public class Coords extends AbstractHudElement {

    public Coords() {
        super("Coords", 10, 28, 140, 24, true);
        startAnimation();
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) return;
        if (mc.player == null) return;

        float alphaFactor = alpha / 255.0f;

        float x = getX();
        float y = getY();

        int playerX = (int) mc.player.getX();
        int playerY = (int) mc.player.getY();
        int playerZ = (int) mc.player.getZ();

        String xText = "x";
        String yText = "y";
        String zText = "z";

        String xValue = String.valueOf(playerX);
        String yValue = String.valueOf(playerY);
        String zValue = String.valueOf(playerZ);

        float xTextWidth = Fonts.BOLD.getWidth(xText, 6);
        float yTextWidth = Fonts.BOLD.getWidth(yText, 6);
        float zTextWidth = Fonts.BOLD.getWidth(zText, 6);

        float xValueWidth = Fonts.BOLD.getWidth(xValue, 6);
        float yValueWidth = Fonts.BOLD.getWidth(yValue, 6);
        float zValueWidth = Fonts.BOLD.getWidth(zValue, 6);

        float coordsWidth = 10 + 12 + xTextWidth + 2 + xValueWidth + 8 + 8 +
                yTextWidth + 2 + yValueWidth + 8 + 8 +
                zTextWidth + 2 + zValueWidth;

        setWidth((int) (coordsWidth + 24));
        setHeight(22);

        HudStyle.panel(x, y + 3, coordsWidth, 20, 5f, alphaFactor, HudStyle.Variant.SOFT);

        float textY = y + 7;
        float textX = x;

        int iconAlpha = clampAlpha(alphaFactor);
        Fonts.ICONSTYPETHO.draw("n", textX + 5, textY + 0.5f, 11, new Color(255, 255, 255, iconAlpha).getRGB());

        float offsetX = textX + 22;

        Fonts.BOLD.draw(xText, offsetX, textY + 3, 6, new Color(155, 155, 155, iconAlpha).getRGB());
        offsetX += xTextWidth + 2;

        Fonts.BOLD.draw(xValue, offsetX, textY + 3, 6, new Color(255, 255, 255, iconAlpha).getRGB());
        offsetX += xValueWidth;

        Fonts.TEST.draw(">", offsetX + 4, textY + 1.5f, 8, new Color(155, 155, 155, iconAlpha).getRGB());
        offsetX += 12;

        Fonts.BOLD.draw(yText, offsetX, textY + 3, 6, new Color(155, 155, 155, iconAlpha).getRGB());
        offsetX += yTextWidth + 2;

        Fonts.BOLD.draw(yValue, offsetX, textY + 3, 6, new Color(255, 255, 255, iconAlpha).getRGB());
        offsetX += yValueWidth;

        Fonts.TEST.draw(">", offsetX + 4, textY + 1.5f, 8, new Color(155, 155, 155, iconAlpha).getRGB());
        offsetX += 12;

        Fonts.BOLD.draw(zText, offsetX, textY + 3, 6, new Color(155, 155, 155, iconAlpha).getRGB());
        offsetX += zTextWidth + 2;

        Fonts.BOLD.draw(zValue, offsetX, textY + 3, 6, new Color(255, 255, 255, iconAlpha).getRGB());
    }

    private int clampAlpha(float alpha) {
        return Math.max(0, Math.min(255, (int) (alpha * 255)));
    }
}

