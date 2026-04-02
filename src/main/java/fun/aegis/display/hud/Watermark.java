package fun.aegis.display.hud;

import antidaunleak.api.UserProfile;
import net.minecraft.client.gui.DrawContext;
import fun.aegis.utils.client.managers.api.draggable.AbstractDraggable;
import fun.aegis.common.animation.Animation;
import fun.aegis.common.animation.Direction;
import fun.aegis.common.animation.implement.Decelerate;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.shape.implement.LiquidGlass;
import fun.aegis.utils.client.Instance;
import fun.aegis.Aegis;
import fun.aegis.features.impl.render.Hud;
import java.awt.Color;

public class Watermark extends AbstractDraggable {
    private final Animation hoverAnimation = new Decelerate().setMs(300).setValue(0);
    private LiquidGlass liquidGlass = new LiquidGlass();
    private boolean isHovered = false;

    public static Watermark getInstance() {
        return Instance.getDraggable(Watermark.class);
    }

    public Watermark() {
        super("Watermark", 4, 4, 100, 16, false);
    }

    @Override
    public void tick() {
        if (mc.player == null || mc.world == null)
            return;
        hoverAnimation.setDirection(isHovered ? Direction.FORWARDS : Direction.BACKWARDS);
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (mc.player == null || mc.world == null)
            return;
        if (!Hud.getInstance().interfaceSettings.isSelected("Вотермарка"))
            return;

        var matrix = context.getMatrices();

        String username = UserProfile.getInstance().profile("username");
        String text = "Aegis 0.5.0 • " + username;

        float padding = 2f;
        float round = 6f;
        float width = 15 + Fonts.getSize(12, Fonts.Type.BOLD).getStringWidth(text) + padding * 2;
        float height = 13f;
        float x = this.getX();
        float y = this.getY();

        double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
        isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;

        float hoverAlpha = (float) hoverAnimation.getOutput().floatValue();

        blur.render(ShapeProperties.create(matrix, x, y, width, height)
                .round(round).softness(10F).thickness(0).color(ColorAssist.getRect(0.4F)).build());

        if (hoverAlpha > 0.01f) {
            rectangle.render(ShapeProperties.create(matrix, x, y, width, height)
                    .round(round)
                    .color(ColorAssist.getColor(255, 255, 255, (int) (20 * hoverAlpha)))
                    .build());
        }

        float dotSize = (height - padding * 2) / 1.5f;
        float dotX = x + padding + ((height - padding * 2) - dotSize) / 2f;
        float dotY = y + padding + ((height - padding * 2) - dotSize) / 2f;

        rectangle.render(ShapeProperties.create(matrix, dotX, dotY, dotSize, dotSize)
                .round(dotSize / 2)
                .color(ColorAssist.getClientColor())
                .build());

        float textX = x + padding + (height - padding * 2) + padding * 0.5f;
        Fonts.getSize(12, Fonts.Type.BOLD).drawString(matrix, text, textX, y + padding + 3.5f,
                ColorAssist.getText(1f));

        this.setWidth((int) width);
        this.setHeight((int) height);
    }
}
