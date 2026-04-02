package fun.aegis.display.screens.clickgui.components.implement.module;

import com.google.common.base.Suppliers;
import fun.aegis.Aegis;
import fun.aegis.common.animation.Animation;
import fun.aegis.common.animation.implement.Decelerate;
import fun.aegis.display.screens.clickgui.components.AbstractComponent;
import fun.aegis.display.screens.clickgui.components.implement.settings.AbstractSettingComponent;
import fun.aegis.display.screens.clickgui.components.implement.settings.BindComponent;
import fun.aegis.display.screens.clickgui.components.implement.settings.CheckboxComponent;
import fun.aegis.display.screens.clickgui.components.implement.settings.ColorComponent;
import fun.aegis.display.screens.clickgui.components.implement.settings.GroupComponent;
import fun.aegis.display.screens.clickgui.components.implement.settings.SButtonComponent;
import fun.aegis.display.screens.clickgui.components.implement.settings.SliderComponent;
import fun.aegis.display.screens.clickgui.components.implement.settings.TextComponent;
import fun.aegis.display.screens.clickgui.components.implement.settings.multiselect.MultiSelectComponent;
import fun.aegis.display.screens.clickgui.components.implement.settings.select.SelectComponent;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.setting.SettingComponentAdder;
import fun.aegis.utils.client.chat.StringHelper;
import fun.aegis.utils.display.atlasfont.msdf.MsdfFont;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.scissor.ScissorAssist;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.systemrender.builders.Builder;
import fun.aegis.utils.math.calc.Calculate;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static fun.aegis.utils.display.font.Fonts.Type.DEFAULT;

public class PanelsModuleComponent extends AbstractComponent {
    private final Module module;
    private final List<AbstractSettingComponent> settingComponents = new ArrayList<>();

    private static final float ROW_H = 17f;
    private static final float HEADER_TEXT_X = 7f;
    private static final float HEADER_TEXT_Y = 5.5f;
    private static final float HEADER_ARROW_X_OFFSET = 12f;
    private static final float HEADER_ARROW_Y = 4f;
    private static final float HEADER_RADIUS = 3f;
    private static final float OUTLINE_THICKNESS = 1.5f;
    private static final float DIVIDER_MARGIN_X = 5f;
    private static final float DIVIDER_HEIGHT = 0.5f;
    private static final float DIVIDER_Y_OFFSET = 2f;
    private static final float SETTINGS_TOP_PADDING = 2f;
    private static final float SETTINGS_BOTTOM_PADDING = 8f;
    private static final float SETTINGS_RENDER_THRESHOLD = 0.65f;

    private static final float BIND_PANEL_H = 40f;

    private static final int EXPAND_ANIMATION_MS = 281;

    private static final Supplier<MsdfFont> ICOLIST_FONT = Suppliers.memoize(() -> MsdfFont.builder()
            .atlas("icolist")
            .data("icolist")
            .build());

    private boolean expanded = false;
    private boolean bindExpanded = false;
    private boolean binding = false;

    private long lastBindToggleMs = 0L;

    private final Animation expandAnimation = new Decelerate().setMs(EXPAND_ANIMATION_MS).setValue(1);
    private final Animation hoverAnimation = new Decelerate().setMs(140).setValue(1);

    public PanelsModuleComponent(Module module) {
        this.module = module;
        new SettingComponentAdder().addSettingComponent(module.settings(), settingComponents);

        expandAnimation.setDirection(fun.aegis.common.animation.Direction.BACKWARDS);
        expandAnimation.reset();

        hoverAnimation.setDirection(fun.aegis.common.animation.Direction.BACKWARDS);
        hoverAnimation.reset();
    }

    public Module getModule() {
        return module;
    }

    private float estimateSettingHeight(AbstractSettingComponent c) {
        if (c instanceof CheckboxComponent) return 15f;
        if (c instanceof BindComponent) return 15f;
        if (c instanceof ColorComponent) return 15f;
        if (c instanceof TextComponent) return 15f;
        if (c instanceof SliderComponent) return 20f;
        if (c instanceof GroupComponent) return 15f;
        if (c instanceof SButtonComponent) return 15f;
        if (c instanceof SelectComponent) return 15f;
        if (c instanceof MultiSelectComponent) return 15f;
        return 15f;
    }

    private float getExpandedHeight() {
        float h = ROW_H;
        if (!expanded && bindExpanded) {
            h += BIND_PANEL_H;
        }
        if (expanded) {
            for (AbstractSettingComponent c : settingComponents) {
                Supplier<Boolean> visible = c.getSetting().getVisible();
                if (visible != null && !visible.get()) continue;
                h += c.height > 0 ? c.height : estimateSettingHeight(c);
            }
        }
        return h + SETTINGS_BOTTOM_PADDING;
    }

    private float getAnimatedHeight() {
        boolean anyExpanded = expanded || bindExpanded;
        expandAnimation.setDirection(anyExpanded ? fun.aegis.common.animation.Direction.FORWARDS : fun.aegis.common.animation.Direction.BACKWARDS);
        if (module.settings().isEmpty() && !bindExpanded) {
            return ROW_H;
        }
        float targetH = getExpandedHeight();
        float t = expandAnimation.getOutput().floatValue();
        return ROW_H + (targetH - ROW_H) * t;
    }

    public float getComponentHeight() {
        return getAnimatedHeight();
    }

    private boolean isSettingHover(AbstractSettingComponent c, double mouseX, double mouseY) {
        return Calculate.isHovered(mouseX, mouseY, c.x, c.y, c.width, c.height);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        Matrix4f positionMatrix = matrix.peek().getPositionMatrix();

        float rowH = ROW_H;
        float radius = HEADER_RADIUS;

        float totalH = getAnimatedHeight();
        float t = expandAnimation.getOutput().floatValue();
        boolean anyExpanded = expanded || bindExpanded;
        boolean renderSettings = anyExpanded || t > SETTINGS_RENDER_THRESHOLD;

        boolean hovered = Calculate.isHovered(mouseX, mouseY, x, y, width, rowH);
        hoverAnimation.setDirection(hovered ? fun.aegis.common.animation.Direction.FORWARDS : fun.aegis.common.animation.Direction.BACKWARDS);
        float hoverT = hoverAnimation.getOutput().floatValue();
        hoverT = Math.max(0f, Math.min(1f, hoverT));

        int baseAlpha = 100;
        int hoverAlpha = 120;
        float stateT = module.getAnimation().getOutput().floatValue();
        stateT = Math.max(0f, Math.min(1f, stateT));

        int textColor = ColorAssist.lerp(stateT, 0xFF818390, 0xFFFFFFFF);
        int arrowColor = ColorAssist.lerp(stateT, 0xFF818390, 0xFF9FA1AE);

        int a = (int) (baseAlpha + (hoverAlpha - baseAlpha) * hoverT);
        int disabledBg = new Color(16, 17, 27, a).getRGB();
        int enabledBg = new Color(26, 28, 40, a).getRGB();
        int moduleBgColor = ColorAssist.lerp(stateT, disabledBg, enabledBg);
        int outline = new Color(63, 65, 75, 100).getRGB();

        if (totalH > rowH + 0.5f) {
            blur.render(ShapeProperties.create(matrix, x, y, width, totalH)
                    .thickness(OUTLINE_THICKNESS)
                    .quality(64)
                    .round(radius)
                    .outlineColor(outline)
                    .color(moduleBgColor)
                    .build());

            blur.render(ShapeProperties.create(matrix, x + DIVIDER_MARGIN_X, y + rowH + DIVIDER_Y_OFFSET, width - (DIVIDER_MARGIN_X * 2f), DIVIDER_HEIGHT)
                    .outlineColor(outline)
                    .color(moduleBgColor)
                    .build());
        } else {
            blur.render(ShapeProperties.create(matrix, x, y, width, rowH)
                    .thickness(OUTLINE_THICKNESS)
                    .quality(64)
                    .round(radius)
                    .outlineColor(outline)
                    .color(moduleBgColor)
                    .build());
        }

        Fonts.getSize(14, DEFAULT).drawString(matrix, module.getVisibleName(), x + HEADER_TEXT_X, y + HEADER_TEXT_Y + 0.5f, textColor);

        if (!module.settings().isEmpty()) {
            String arrowIcon = expanded ? "B" : "H";
            Builder.text()
                    .font(ICOLIST_FONT.get())
                    .text(arrowIcon)
                    .size(6)
                    .color(arrowColor)
                    .build()
                    .render(positionMatrix, x + width - HEADER_ARROW_X_OFFSET, y + HEADER_ARROW_Y);
        }

        if (totalH <= rowH + 0.5f) {
            height = rowH;
            return;
        }

        if (!renderSettings) {
            height = totalH;
            return;
        }

        float visibleSettingsH = Math.max(0f, totalH - (rowH + SETTINGS_TOP_PADDING));
        ScissorAssist scissor = Aegis.getInstance().getScissorManager();
        scissor.push(positionMatrix, x, y + rowH, width, rowH + SETTINGS_TOP_PADDING + visibleSettingsH);

        float yOff = rowH + SETTINGS_TOP_PADDING;

        if (!expanded && bindExpanded) {
            renderBindPanel(context, mouseX, mouseY, yOff);
            yOff += BIND_PANEL_H;
        }

        if (expanded) {
            for (int i = 0; i < settingComponents.size(); i++) {
                AbstractSettingComponent c = settingComponents.get(i);
                Supplier<Boolean> visible = c.getSetting().getVisible();
                if (visible != null && !visible.get()) continue;

                c.x = x - 2f;
                c.y = y + yOff;
                c.width = width + 2f;
                c.render(context, mouseX, mouseY, delta);
                yOff += c.height > 0 ? c.height : estimateSettingHeight(c);
            }
        }

        scissor.pop();
        height = totalH;
    }

    public boolean isHover(double mouseX, double mouseY) {
        return Calculate.isHovered(mouseX, mouseY, x, y, width, getComponentHeight());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float rowH = ROW_H;
        boolean overHeader = Calculate.isHovered(mouseX, mouseY, x, y, width, rowH);

        float totalH = getAnimatedHeight();
        float t = expandAnimation.getOutput().floatValue();
        boolean renderSettings = expanded || bindExpanded || t > 0.65f;

        if (overHeader) {
            if (button == 2) {
                long now = System.currentTimeMillis();
                if (now - lastBindToggleMs < 200L) {
                    return true;
                }
                lastBindToggleMs = now;

                expanded = false;
                bindExpanded = !bindExpanded;
                binding = false;
                return true;
            }

            if (button == 0) {
                module.switchState();
                return true;
            }
            if (button == 1) {
                if (!module.settings().isEmpty()) {
                    expanded = !expanded;
                    if (expanded) {
                        bindExpanded = false;
                        binding = false;
                    }
                    return true;
                }
                return false;
            }
        }

        if (renderSettings && totalH > rowH + 0.5f && mouseY <= (y + totalH)) {
            if (!expanded && bindExpanded) {
                float bindY = y + rowH + SETTINGS_TOP_PADDING;
                if (Calculate.isHovered(mouseX, mouseY, x, bindY, width, BIND_PANEL_H)) {
                    if (handleBindPanelClick(mouseX, mouseY, button, bindY)) {
                        return true;
                    }
                }
            }

            if (expanded) {
                boolean any = false;
                for (AbstractSettingComponent c : settingComponents) {
                    Supplier<Boolean> visible = c.getSetting().getVisible();
                    if (visible != null && !visible.get()) continue;
                    if (isSettingHover(c, mouseX, mouseY)) {
                        any = true;
                        c.mouseClicked(mouseX, mouseY, button);
                    }
                }
                if (any) return true;
            }
        }

        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (bindExpanded) {
            if (keyCode == GLFW.GLFW_KEY_DELETE) {
                module.setKey(-1);
                bindExpanded = true;
                binding = false;
                return true;
            }

            if (binding) {
                int key = keyCode == GLFW.GLFW_KEY_ESCAPE ? -1 : keyCode;
                if (key != GLFW.GLFW_KEY_RIGHT_SHIFT) {
                    module.setKey(key);
                    binding = false;
                }
                return true;
            }
        }

        if (expanded && getAnimatedHeight() > (ROW_H + 0.5f)) {
            for (AbstractSettingComponent c : settingComponents) {
                c.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return false;
    }

    private void renderBindPanel(DrawContext context, int mouseX, int mouseY, float yOff) {
        MatrixStack matrix = context.getMatrices();

        float panelX = x;
        float panelY = y + yOff;
        float panelW = width;
        float panelH = 50;

        rectangle.render(ShapeProperties.create(matrix, panelX, panelY, panelW, panelH)
                .round(0).softness(0).thickness(0).outlineColor(ColorAssist.getOutline(0F, 1))
                .color(ColorAssist.getOutline(0F, 1))
                .build());

        Fonts.getSize(15, DEFAULT).drawString(matrix, "Бинд", panelX + 5, panelY + 6.3f, 0xFFD4D6E1);
        Fonts.getSize(15, DEFAULT).drawString(matrix, "Режим", panelX + 5, panelY + 24.3f, 0xFFD4D6E1);

        String keyName = StringHelper.getBindName(module.getKey());
        String shownKey = binding ? "(" + keyName + ")" : keyName;
        float keyBoxW = Math.max(14f, Fonts.getSize(14, DEFAULT).getStringWidth(shownKey) + 10f);
        float keyBoxX = panelX + panelW - keyBoxW - 5f;
        float keyBoxY = panelY + 1.8f;

        rectangle.render(ShapeProperties.create(matrix, keyBoxX, keyBoxY, keyBoxW, 13)
                .round(2).thickness(2).softness(1).outlineColor(ColorAssist.getOutline(0.8F, 1)).color(ColorAssist.getOutline(0.1F, 1)).build());

        int keyColor = binding ? 0xFF8187FF : 0xFFD4D6E1;
        Fonts.getSize(14, DEFAULT).drawString(matrix, shownKey, keyBoxX + 5f, keyBoxY + 4.8f, keyColor);

        float toggleBoxX = panelX + panelW - 57f;
        float toggleBoxY = panelY + 20f;
        float toggleBoxW = 52f;
        float toggleBoxH = 13f;

        rectangle.render(ShapeProperties.create(matrix, toggleBoxX, toggleBoxY, toggleBoxW, toggleBoxH)
                .round(2).thickness(2).softness(1).outlineColor(ColorAssist.getOutline(0.8F, 1)).color(ColorAssist.getOutline(0.1F, 1)).build());

        if (module.getType() == 1) {
            rectangle.render(ShapeProperties.create(matrix, toggleBoxX + 23f, toggleBoxY, 29f, toggleBoxH)
                    .round(2, 2, 0, 0).color(0xFF8187FF).build());
        } else {
            rectangle.render(ShapeProperties.create(matrix, toggleBoxX, toggleBoxY, 23f, toggleBoxH)
                    .round(0, 0, 2, 2).color(0xFF8187FF).build());
        }

        float halfW = toggleBoxW / 2f;
        String holdText = "HOLD";
        String toggleText = "TOGGLE";
        float holdTextW = Fonts.getSize(12).getStringWidth(holdText);
        float toggleTextW = Fonts.getSize(12).getStringWidth(toggleText);
        float holdTextX = toggleBoxX + (halfW - holdTextW) / 2.5f;
        float toggleTextX = toggleBoxX + halfW + (halfW - toggleTextW);
        float textY = toggleBoxY + 5.3f;
        Fonts.getSize(12).drawString(matrix, holdText, holdTextX, textY, 0xFFD4D6E1);
        Fonts.getSize(12).drawString(matrix, toggleText, toggleTextX, textY, 0xFFD4D6E1);
    }

    private boolean handleBindPanelClick(double mouseX, double mouseY, int button, float bindY) {
        if (button == 0) {
            float panelX = x;
            float panelW = width;

            String keyName = StringHelper.getBindName(module.getKey());
            String shownKey = binding ? "(" + keyName + ") ..." : keyName;
            float keyBoxW = Math.max(28f, Fonts.getSize(14, DEFAULT).getStringWidth(shownKey) + 10f);
            float keyBoxX = panelX + panelW - keyBoxW - 5f;
            float keyBoxY = bindY + 1.8f;
            if (Calculate.isHovered(mouseX, mouseY, keyBoxX, keyBoxY, keyBoxW, 13)) {
                binding = !binding;
                return true;
            }

            float toggleBoxX = panelX + panelW - 57f;
            float toggleBoxY = bindY + 20f;
            if (Calculate.isHovered(mouseX, mouseY, toggleBoxX, toggleBoxY, 52, 13)) {
                module.setType(module.getType() != 1 ? 1 : 0);
                return true;
            }
        }

        if (binding && button > 1) {
            module.setKey(button);
            binding = false;
            return true;
        }

        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (expanded && getAnimatedHeight() > (ROW_H + 0.5f)) {
            for (AbstractSettingComponent c : settingComponents) {
                if (c.charTyped(chr, modifiers)) {
                    return true;
                }
            }
        }
        return false;
    }
}
