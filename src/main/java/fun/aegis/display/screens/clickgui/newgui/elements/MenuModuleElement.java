package fun.aegis.display.screens.clickgui.newgui.elements;

import fun.aegis.Aegis;
import fun.aegis.display.screens.clickgui.newgui.animation.Animation;
import fun.aegis.display.screens.clickgui.newgui.animation.Easing;
import fun.aegis.display.screens.clickgui.newgui.settings.*;
import fun.aegis.display.screens.clickgui.newgui.theme.Theme;
import fun.aegis.display.screens.clickgui.newgui.theme.ThemeManager;
import fun.aegis.display.screens.clickgui.newgui.utils.MathUtil;
import fun.aegis.display.screens.clickgui.newgui.utils.Rect;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.Setting;
import fun.aegis.features.module.setting.implement.*;
import fun.aegis.display.screens.clickgui.newgui.utils.MsdfFonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

import static fun.aegis.utils.display.interfaces.QuickImports.blur;

@Getter
public class MenuModuleElement extends AbstractMenuElement {
    private final Module module;
    private final List<AbstractMenuSetting> settings = new ArrayList<>();
    private final Animation animation;
    private final Animation animationPosition;
    private final Animation animationY;
    private Rect bounds;
    private Rect boundsBind;
    private boolean binding = false;
    private int lastColumn = -1;
    private boolean animated = false;

    public MenuModuleElement(Module module) {
        this.module = module;
        animation = new Animation(200, module.isState() ? 1 : 0, Easing.LINEAR);
        animationPosition = new Animation(150, 1, Easing.QUAD_IN_OUT);
        animationY = new Animation(150, 1, Easing.QUAD_IN_OUT);

        for (Setting setting : module.settings()) {
            if (setting instanceof SliderSettings slider) {
                settings.add(new MenuSliderSetting(slider));
            } else if (setting instanceof SelectSetting select) {
                settings.add(new MenuModeSetting(select));
            } else if (setting instanceof MultiSelectSetting multi) {
                settings.add(new MenuSelectSetting(multi));
            } else if (setting instanceof BooleanSetting bool) {
                settings.add(new MenuBooleanSetting(bool));
            } else if (setting instanceof ColorSetting color) {
                settings.add(new MenuColorSetting(color));
            } else if (setting instanceof BindSetting bind) {
                settings.add(new MenuKeySetting(bind));
            } else if (setting instanceof ButtonSetting button) {
                settings.add(new MenuButtonSetting(button));
            }
        }
    }

    @Override
    public void render(DrawContext ctx, float mouseX, float mouseY, float x, float y, float moduleWidth, float alpha, int column) {
        MatrixStack matrix = ctx.getMatrices();
        
        if (lastColumn == -1) lastColumn = column;
        if (lastColumn != column) {
            animated = true;
            animationPosition.animateTo(x);
            animationY.animateTo(y);
            lastColumn = column;
        }

        if (animated) {
            x = animationPosition.update(x);
            y = animationY.update(y);
            if (animationPosition.isDone() && animationY.isDone()) animated = false;
        } else {
            animationPosition.reset(x);
            animationY.reset(y);
        }

        animation.animateTo(module.isState() ? 1 : 0);
        animation.update();

        float moduleHeight = 22;
        Theme theme = ThemeManager.getInstance().getCurrentTheme();
        
        int moduleBg = Theme.applyAlpha(theme.getForegroundColorInt(), alpha);
        boolean hasSettings = hasSettings();
        float settingAreaHeight = getHeight();
        int settingBg = Theme.applyAlpha(theme.getForegroundDarkInt(), alpha);

        bounds = new Rect(x, y, moduleWidth, moduleHeight);

        if (hasSettings) {
            blur.render(ShapeProperties.create(matrix, x, y, moduleWidth, settingAreaHeight)
                    .round(8).color(settingBg).build());
            blur.render(ShapeProperties.create(matrix, x, y, moduleWidth, moduleHeight)
                    .round(8, 8, 0, 0).color(moduleBg).build());
        } else {
            blur.render(ShapeProperties.create(matrix, x, y, moduleWidth, moduleHeight)
                    .round(8).color(moduleBg).build());
        }

        int enabledColor = Theme.mixColors(theme.getGrayInt(), theme.getColorInt(), animation.getValue());
        enabledColor = Theme.applyAlpha(enabledColor, alpha);
        
        int textColor = Theme.mixColors(theme.getGrayLightInt(), theme.getWhiteInt(), animation.getValue());
        textColor = Theme.applyAlpha(textColor, alpha);

        MsdfFonts.drawIcon(matrix, "B", x + 8, y + 9, 11, enabledColor);
        
        MsdfFonts.drawText(matrix, module.getVisibleName(), x + 18, y + 7, 14, textColor);

        float keyBoxWidth = 22.5f;
        float keyBoxX = x + moduleWidth - keyBoxWidth;
        
        int badgeColor;
        if (binding) {
            badgeColor = theme.getSecondColorInt();
        } else if (module.getKey() != -1) {
            badgeColor = Theme.mixColors(theme.getWhiteGrayInt(), theme.getColorInt(), animation.getValue());
        } else {
            badgeColor = theme.getForegroundLightInt();
        }
        badgeColor = Theme.applyAlpha(badgeColor, alpha);

        blur.render(ShapeProperties.create(matrix, keyBoxX, y, keyBoxWidth, moduleHeight)
                .round(hasSettings ? new float[]{0, 8, 0, 0} : new float[]{0, 8, 8, 0})
                .color(badgeColor).build());

        String keyText = "n/a";
        int keyCode = module.getKey();
        if (keyCode != -1 && keyCode != 0) {
            String name = GLFW.glfwGetKeyName(keyCode, 0);
            if (name != null && !name.isBlank()) {
                keyText = name.toUpperCase();
            } else {
                keyText = "KEY" + keyCode;
            }
        }

        int keyColor = Theme.applyAlpha(
                keyCode != -1 ? Theme.mixColors(theme.getGrayLightInt(), theme.getWhiteInt(), animation.getValue()) : theme.getGrayInt(),
                alpha
        );

        float keyTextWidth = MsdfFonts.getTextWidth(keyText, 14);
        float keyTextX = keyBoxX + (keyBoxWidth - keyTextWidth) / 2f;
        MsdfFonts.drawText(matrix, keyText, keyTextX, y + 7, 14, keyColor);

        boundsBind = new Rect(keyBoxX, y, keyBoxWidth, moduleHeight);

        float padding = 8;
        float startY = y + moduleHeight + padding;
        
        int descriptionColor = Theme.mixColors(theme.getWhiteGrayInt(), theme.getGrayLightInt(), animation.getValue());
        descriptionColor = Theme.applyAlpha(descriptionColor, alpha);

        for (AbstractMenuSetting setting : settings) {
            if (!setting.isVisible()) continue;
            setting.render(ctx, mouseX, mouseY, x, startY, moduleWidth, alpha, animation.getValue(), enabledColor, textColor, descriptionColor, theme);
            startY += setting.getHeight() + 8;
        }
    }

    @Override
    public float getHeight() {
        return 22 + (hasSettings() ? (float) settings.stream()
                .filter(AbstractMenuSetting::isVisible)
                .mapToDouble(m -> m.getHeight() + 8)
                .sum() + 8 : 0);
    }

    public boolean hasSettings() {
        return !settings.isEmpty() && settings.stream().anyMatch(AbstractMenuSetting::isVisible);
    }

    @Override
    public void onMouseClicked(double mouseX, double mouseY, int button) {
        if (bounds != null && bounds.contains(mouseX, mouseY)) {
            if (button > 2 && binding) {
                binding = false;
                module.setKey(button);
                return;
            }
            if (button == 0) {
                if (boundsBind != null && boundsBind.contains(mouseX, mouseY)) {
                    binding = !binding;
                } else {
                    module.switchState();
                }
            } else if (button == 2) {
                binding = !binding;
            }
        }

        for (AbstractMenuSetting setting : settings) {
            setting.onMouseClicked(mouseX, mouseY, button);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (binding) {
            if (keyCode == GLFW.GLFW_KEY_ESCAPE || keyCode == GLFW.GLFW_KEY_DELETE || keyCode == GLFW.GLFW_KEY_BACKSPACE) {
                module.setKey(-1);
            } else {
                module.setKey(keyCode);
            }
            binding = false;
            return true;
        }

        boolean result = false;
        for (AbstractMenuSetting setting : settings) {
            if (setting.keyPressed(keyCode, scanCode, modifiers)) {
                result = true;
            }
        }
        return result;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        return true;
    }

    @Override
    public ModuleCategory getCategory() {
        return module.getCategory();
    }

    @Override
    public String getName() {
        return module.getName();
    }

    @Override
    public void onMouseReleased(double mouseX, double mouseY, int button) {
        for (AbstractMenuSetting setting : settings) {
            setting.onMouseReleased(mouseX, mouseY, button);
        }
    }

    @Override
    public void onMouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
    }
}
