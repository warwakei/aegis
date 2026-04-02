package fun.aegis.display.screens.clickgui.components.implement.settings.multiselect;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import fun.aegis.common.animation.Animation;
import fun.aegis.common.animation.Direction;
import fun.aegis.common.animation.implement.Decelerate;
import fun.aegis.utils.display.font.FontRenderer;
import fun.aegis.display.screens.clickgui.components.implement.settings.AbstractSettingComponent;
import fun.aegis.utils.display.scissor.ScissorAssist;
import fun.aegis.features.module.setting.implement.MultiSelectSetting;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.font.Fonts;

import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.Aegis;

import java.awt.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fun.aegis.utils.display.font.Fonts.Type.*;

public class MultiSelectComponent extends AbstractSettingComponent {
    private final MultiSelectSetting setting;

    private final Map<String, Animation> chipAnimations = new HashMap<>();
    private final Map<String, Animation> chipHoverAnimations = new HashMap<>();

    private float chipsStartX;
    private float chipsStartY;
    private float chipsMaxW;
    private float chipsEndY;

    public MultiSelectComponent(MultiSelectSetting setting) {
        super(setting);
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrices = context.getMatrices();

        float headerH = 12f;

        float nameX = x + 8;
        float nameY = y + 7f;
        float maxNameW = Math.max(0, (x + width) - 6f - nameX);
        ScissorAssist scissor = Aegis.getInstance().getScissorManager();
        scissor.push(matrices.peek().getPositionMatrix(), nameX, y + 2f, maxNameW, headerH);
        Fonts.getSize(13, DEFAULT).drawStringWithScroll(matrices, setting.getName(), nameX, nameY, maxNameW, 0xFFD4D6E1);
        scissor.pop();

        chipsStartX = x + 8f;
        chipsStartY = y + headerH + 2f;
        chipsMaxW = Math.max(40f, width - 17f);

        renderChips(matrices, mouseX, mouseY);
        height = (int) Math.max(headerH + 6f, (chipsEndY - y) + 4f);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            handleChipClick(mouseX, mouseY);
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return Calculate.isHovered(mouseX, mouseY, x, y, width, height);
    }

    private void renderChips(MatrixStack matrix, int mouseX, int mouseY) {
        FontRenderer font = Fonts.getSize(12, DEFAULT);
        List<String> list = setting.getList();

        float chipPadX = 3f;
        float chipH = 10f;
        float rowGap = 2f;
        float colGap = 3f;

        float cx = chipsStartX;
        float cy = chipsStartY;
        float maxX = chipsStartX + chipsMaxW;

        int clientColor = ColorAssist.getClientColor();

        for (String s : list) {
            float textW = font.getStringWidth(s);
            float chipW = Math.min(chipsMaxW, textW + chipPadX * 2f);

            if (cx + chipW > maxX) {
                cx = chipsStartX;
                cy += chipH + rowGap;
            }

            boolean hovered = Calculate.isHovered(mouseX, mouseY, cx, cy, chipW, chipH);
            Animation hoverAnim = chipHoverAnimations.computeIfAbsent(s, k -> new Decelerate().setMs(120).setValue(1));
            hoverAnim.setDirection(hovered ? Direction.FORWARDS : Direction.BACKWARDS);
            float hoverT = hoverAnim.getOutput().floatValue();
            hoverT = Math.max(0f, Math.min(1f, hoverT));

            boolean selected = setting.isSelected(s);
            Animation anim = chipAnimations.computeIfAbsent(s, k -> new Decelerate().setMs(140).setValue(1));
            anim.setDirection(selected ? Direction.FORWARDS : Direction.BACKWARDS);
            float t = anim.getOutput().floatValue();
            t = Math.max(0f, Math.min(1f, t));

            int bgOff = new Color(40, 40, 45, 40).getRGB();
            int bgOn = ColorAssist.multAlpha(clientColor, 0.7f);
            int outlineOff = new Color(55, 52, 55, 180).getRGB();
            int outlineOn = ColorAssist.multAlpha(clientColor, 0.9f);
            int textOff = 0xFFD4D6E1;
            int textOn = 0xFFFFFFFF;

            int bg = ColorAssist.lerp(t, bgOff, bgOn);
            int outline = ColorAssist.lerp(t, outlineOff, outlineOn);
            int text = ColorAssist.lerp(t, textOff, textOn);

            int bgHoverTint = (ColorAssist.alpha(bg) << 24) | 0xFFFFFF;
            int outlineHoverTint = (ColorAssist.alpha(outline) << 24) | 0xFFFFFF;
            bg = ColorAssist.lerp(hoverT * 0.18f, bg, bgHoverTint);
            outline = ColorAssist.lerp(hoverT * 0.35f, outline, outlineHoverTint);
            text = ColorAssist.lerp(hoverT * 0.25f, text, 0xFFFFFFFF);

            rectangle.render(ShapeProperties.create(matrix, cx, cy, chipW, chipH)
                    .round(2)
                    .thickness(1f)
                    .outlineColor(outline)
                    .color(bg)
                    .build());

            float tx = cx + (chipW - textW) / 2f;
            float textH = font.getStringHeight(s);
            float ty = cy + (chipH - textH) / 2f + textH - 8.5f;
            font.drawString(matrix, s, tx, ty, text);

            cx += chipW + colGap;
        }

        chipsEndY = cy + chipH;
    }

    private void handleChipClick(double mouseX, double mouseY) {
        FontRenderer font = Fonts.getSize(12, DEFAULT);
        List<String> list = setting.getList();

        float chipPadX = 3f;
        float chipH = 10f;
        float rowGap = 2f;
        float colGap = 3f;

        float cx = chipsStartX;
        float cy = chipsStartY;
        float maxX = chipsStartX + chipsMaxW;

        for (String s : list) {
            float textW = font.getStringWidth(s);
            float chipW = Math.min(chipsMaxW, textW + chipPadX * 2f);

            if (cx + chipW > maxX) {
                cx = chipsStartX;
                cy += chipH + rowGap;
            }

            if (Calculate.isHovered(mouseX, mouseY, cx, cy, chipW, chipH)) {
                if (setting.isSelected(s)) {
                    setting.getSelected().remove(s);
                } else {
                    setting.getSelected().add(s);
                }
                return;
            }

            cx += chipW + colGap;
        }
    }
}
