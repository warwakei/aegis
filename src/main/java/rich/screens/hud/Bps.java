package rich.screens.hud;

import net.minecraft.client.gui.DrawContext;
import rich.client.draggables.AbstractHudElement;
import rich.modules.impl.render.Hud;
import rich.util.render.font.Fonts;

import java.awt.*;

public class Bps extends AbstractHudElement {

    private double lastX = 0;
    private double lastZ = 0;
    private double currentBps = 0;
    private double displayBps = 0;
    private double targetBps = 0;
    private long lastUpdateTime = 0;

    private static final double BPS_SMOOTHING = 0.04;
    private static final double DISPLAY_SMOOTHING = 0.025;

    public Bps() {
        super("BPS", 10, 52, 90, 24, true);
        startAnimation();
    }

    @Override
    public boolean visible() {
        return Hud.getInstance() != null && Hud.getInstance().showBps.isValue();
    }

    private double roundToStep(double value, double step) {
        return Math.round(value / step) * step;
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) return;
        if (mc.player == null) return;

        float alphaFactor = alpha / 255.0f;

        long currentTime = System.currentTimeMillis();
        double deltaTime = (currentTime - lastUpdateTime) / 1000.0;

        if (lastUpdateTime > 0 && deltaTime > 0) {
            double dx = mc.player.getX() - lastX;
            double dz = mc.player.getZ() - lastZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            double instantBps = distance / deltaTime;

            currentBps = currentBps + (instantBps - currentBps) * BPS_SMOOTHING;
            targetBps = roundToStep(currentBps, 0.50);
        }

        displayBps = displayBps + (targetBps - displayBps) * DISPLAY_SMOOTHING;

        lastX = mc.player.getX();
        lastZ = mc.player.getZ();
        lastUpdateTime = currentTime;

        float x = getX();
        float y = getY();

        double roundedDisplayBps = roundToStep(displayBps, 0.50);
        String bpsValue = String.format("%.2f", roundedDisplayBps);
        String bpsText = "b/s";

        float bpsValueWidth = Fonts.BOLD.getWidth(bpsValue, 6);
        float bpsTextWidth = Fonts.BOLD.getWidth(bpsText, 6);

        float bpsWidth = 10 + 12 + 12 + bpsValueWidth + 2 + bpsTextWidth + 5;

        setWidth((int) (bpsWidth + 2));
        setHeight(22);

        HudStyle.panel(x, y + 3, bpsWidth, 20, 5f, alphaFactor, HudStyle.Variant.ACCENT);

        float textY = y + 7;
        int iconAlpha = clampAlpha(alphaFactor);

        Fonts.ICONSTYPETHO.draw("l", x + 5, textY + 0.5f, 11, new Color(255, 255, 255, iconAlpha).getRGB());

        float bpsOffsetX = x + 20;
        Fonts.TEST.draw(">", bpsOffsetX, textY + 1.5f, 8, new Color(155, 155, 155, iconAlpha).getRGB());
        bpsOffsetX += 10;

        Fonts.BOLD.draw(bpsValue, bpsOffsetX, textY + 3, 6, new Color(255, 255, 255, iconAlpha).getRGB());
        bpsOffsetX += bpsValueWidth + 2;

        Fonts.BOLD.draw(bpsText, bpsOffsetX, textY + 3, 6, new Color(155, 155, 155, iconAlpha).getRGB());
    }

    private int clampAlpha(float alpha) {
        return Math.max(0, Math.min(255, (int) (alpha * 255)));
    }
}

