package rich.screens.clickgui.impl.settingsrender;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import rich.util.interfaces.AbstractSettingComponent;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;

public class SliderComponent extends AbstractSettingComponent {
    private final SliderSettings sliderSettings;
    private boolean dragging = false;
    private float animatedPercentage = 0f;
    private float knobAnimation = 0f;

    private boolean inputMode = false;
    private String inputText = "";
    private int cursorPosition = 0;

    private float inputAnimation = 0f;
    private float hoverAnimation = 0f;
    private float unitsAlpha = 1f;
    private float valueOffsetX = 0f;
    private float backgroundAlpha = 0f;

    private long lastUpdateTime = System.currentTimeMillis();

    private static final float ANIMATION_SPEED = 8f;
    private static final float FAST_ANIMATION_SPEED = 12f;

    public SliderComponent(SliderSettings setting) {
        super(setting);
        this.sliderSettings = setting;
        float range = sliderSettings.getMax() - sliderSettings.getMin();
        if (range > 0) {
            this.animatedPercentage = (sliderSettings.getValue() - sliderSettings.getMin()) / range;
        }
    }

    private int clampAlpha(float alpha) {
        return Math.max(0, Math.min(255, (int) alpha));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (dragging) {
            updateValue(mouseX);
        }

        float deltaTime = getDeltaTime();
        updateAnimations(mouseX, mouseY, deltaTime);

        float range = sliderSettings.getMax() - sliderSettings.getMin();
        float targetPercentage = range > 0 ? (sliderSettings.getValue() - sliderSettings.getMin()) / range : 0f;
        animatedPercentage += (targetPercentage - animatedPercentage) * 0.25f;

        float knobTarget = dragging ? 1f : 0f;
        knobAnimation += (knobTarget - knobAnimation) * 0.25f;
        knobAnimation = Math.max(0f, Math.min(1f, knobAnimation));

        int iconAlpha = (int)(200 * alphaMultiplier);
        Fonts.GUI_ICONS.draw("H", x - 0.5f, y + 0.5f, 9, new Color(210, 210, 210, iconAlpha).getRGB());

        Fonts.BOLD.draw(sliderSettings.getName(), x + 9.5f, y + 1f, 6, applyAlpha(new Color(210, 210, 220, 200)).getRGB());

        renderValueInput(mouseX, mouseY);
        renderSlider();
    }

    private float getDeltaTime() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastUpdateTime) / 1000f, 0.1f);
        lastUpdateTime = currentTime;
        return deltaTime;
    }

    private float lerp(float current, float target, float speed) {
        float diff = target - current;
        if (Math.abs(diff) < 0.001f) {
            return target;
        }
        return current + diff * Math.min(speed, 1f);
    }

    private void updateAnimations(int mouseX, int mouseY, float deltaTime) {
        float inputTarget = inputMode ? 1f : 0f;
        inputAnimation = lerp(inputAnimation, inputTarget, deltaTime * FAST_ANIMATION_SPEED);

        boolean isHovered = isValueHover(mouseX, mouseY) && !inputMode;
        float hoverTarget = isHovered ? 1f : 0f;
        hoverAnimation = lerp(hoverAnimation, hoverTarget, deltaTime * ANIMATION_SPEED);

        float unitsTarget = inputMode ? 0f : 1f;
        unitsAlpha = lerp(unitsAlpha, unitsTarget, deltaTime * ANIMATION_SPEED);

        float offsetTarget = inputMode ? 1f : 0f;
        valueOffsetX = lerp(valueOffsetX, offsetTarget, deltaTime * ANIMATION_SPEED);

        float bgTarget = inputMode ? 1f : 0f;
        backgroundAlpha = lerp(backgroundAlpha, bgTarget, deltaTime * ANIMATION_SPEED);
    }

    private void renderValueInput(int mouseX, int mouseY) {
        String valueText = sliderSettings.isInteger()
                ? String.valueOf((int) sliderSettings.getValue())
                : String.format("%.1f", sliderSettings.getValue());
        String unitsText = " units";
        String fullText = valueText + unitsText;

        float fullTextWidth = Fonts.BOLD.getWidth(fullText, 5);
        float valueTextWidth = Fonts.BOLD.getWidth(valueText, 5);
        float unitsTextWidth = Fonts.BOLD.getWidth(unitsText, 5);

        float baseX = x + width - fullTextWidth - 4;
        float textY = y + 2f;

        float centerOffset = (unitsTextWidth / 2f) * valueOffsetX;
        float currentValueX = baseX + centerOffset;

        float inputBoxX = baseX - 3;
        float inputBoxY = textY - 1;
        float inputBoxWidth = fullTextWidth + 6;
        float inputBoxHeight = 8;

        if (backgroundAlpha > 0.01f) {
            int bgAlpha = clampAlpha(200 * backgroundAlpha * alphaMultiplier);
            Render2D.rect(inputBoxX, inputBoxY, inputBoxWidth, inputBoxHeight,
                    new Color(40, 40, 45, bgAlpha).getRGB(), 2f);
        }

        float combinedOutlineAlpha = Math.max(hoverAnimation * 0.4f, inputAnimation);
        if (combinedOutlineAlpha > 0.01f) {
            int outlineAlpha = clampAlpha(180 * combinedOutlineAlpha * alphaMultiplier);
            Render2D.outline(inputBoxX, inputBoxY, inputBoxWidth, inputBoxHeight,
                    0.1f, new Color(180, 180, 180, outlineAlpha).getRGB(), 2f);
        }

        if (inputMode && inputAnimation > 0.5f) {
            String displayText = inputText;
            float displayTextWidth = Fonts.BOLD.getWidth(displayText, 5);
            float centeredX = inputBoxX + (inputBoxWidth - displayTextWidth) / 2f;

            int textAlpha = clampAlpha(220 * Math.min(1f, (inputAnimation - 0.5f) * 2f) * alphaMultiplier);
            Fonts.BOLD.draw(displayText, centeredX, textY, 5,
                    new Color(230, 230, 235, textAlpha).getRGB());

            long currentTime = System.currentTimeMillis();
            if ((currentTime % 1000) < 500) {
                String beforeCursor = inputText.substring(0, cursorPosition);
                float cursorX = centeredX + Fonts.BOLD.getWidth(beforeCursor, 5);
                int cursorAlpha = clampAlpha(255 * inputAnimation * alphaMultiplier);
                Render2D.rect(cursorX, inputBoxY + 2, 0.5f, inputBoxHeight - 4,
                        new Color(180, 180, 180, cursorAlpha).getRGB(), 0f);
            }
        } else {
            float valueAlpha = 1f - (inputAnimation * 0.5f);
            int valueAlphaInt = clampAlpha(160 * valueAlpha * alphaMultiplier);

            if (valueAlphaInt > 0) {
                Fonts.BOLD.draw(valueText, currentValueX, textY, 5,
                        new Color(100, 100, 105, valueAlphaInt).getRGB());
            }

            if (unitsAlpha > 0.01f) {
                int unitsAlphaInt = clampAlpha(160 * unitsAlpha * alphaMultiplier);
                if (unitsAlphaInt > 0) {
                    Fonts.BOLD.draw(unitsText, currentValueX + valueTextWidth, textY, 5,
                            new Color(100, 100, 105, unitsAlphaInt).getRGB());
                }
            }
        }
    }

    private void renderSlider() {
        float sliderY = y + 11;
        float sliderHeight = 2.5f;
        float sliderPadding = 1f;
        float sliderTrackWidth = width - 2;

        Render2D.rect(x + sliderPadding, sliderY, sliderTrackWidth, sliderHeight,
                applyAlpha(new Color(60, 60, 65, 220)).getRGB(), 2f);

        float filledWidth = sliderTrackWidth * animatedPercentage;

        if (filledWidth > 0) {
            Render2D.rect(x + sliderPadding, sliderY, filledWidth, sliderHeight,
                    applyAlpha(new Color(130, 130, 135, 230)).getRGB(), 2f);
        }

        float knobBaseSize = 5f;
        float knobSize = knobBaseSize + (knobAnimation * 1f);
        float knobX = x + sliderPadding + (sliderTrackWidth * animatedPercentage) - (knobSize / 2f);
        float knobY = sliderY + (sliderHeight / 2f) - (knobSize / 2f);

        knobX = Math.max(x + sliderPadding - (knobSize / 2f),
                Math.min(knobX, x + sliderPadding + sliderTrackWidth - (knobSize / 2f)));

        Render2D.rect(knobX, knobY, knobSize, knobSize,
                applyAlpha(new Color(180, 180, 185, 255)).getRGB(), knobSize / 2f);
    }

    private boolean isValueHover(double mouseX, double mouseY) {
        String valueText = sliderSettings.isInteger()
                ? String.valueOf((int) sliderSettings.getValue())
                : String.format("%.1f", sliderSettings.getValue());
        String fullText = valueText + " units";

        float fullTextWidth = Fonts.BOLD.getWidth(fullText, 5);
        float boxX = x + width - fullTextWidth - 7;
        float boxY = y;

        return mouseX >= boxX && mouseX <= boxX + fullTextWidth + 10 &&
                mouseY >= boxY && mouseY <= boxY + 10;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            if (isValueHover(mouseX, mouseY) && !inputMode) {
                inputMode = true;
                String currentValue = sliderSettings.isInteger()
                        ? String.valueOf((int) sliderSettings.getValue())
                        : String.format("%.1f", sliderSettings.getValue());
                inputText = currentValue;
                cursorPosition = inputText.length();
                return true;
            }

            if (inputMode && !isValueHover(mouseX, mouseY)) {
                applyInputValue();
                inputMode = false;
                inputText = "";
                return true;
            }

            if (isSliderHover(mouseX, mouseY) && !inputMode) {
                dragging = true;
                updateValue(mouseX);
                return true;
            }
        }
        return false;
    }

    private boolean isSliderHover(double mouseX, double mouseY) {
        float sliderY = y + 6;
        float sliderHeight = 12f;
        return mouseX >= x && mouseX <= x + width && mouseY >= sliderY && mouseY <= sliderY + sliderHeight;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0 && dragging) {
            dragging = false;
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (dragging && button == 0) {
            updateValue(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!inputMode) return false;

        switch (keyCode) {
            case GLFW.GLFW_KEY_ENTER, GLFW.GLFW_KEY_KP_ENTER -> {
                applyInputValue();
                inputMode = false;
                inputText = "";
                return true;
            }
            case GLFW.GLFW_KEY_ESCAPE -> {
                inputMode = false;
                inputText = "";
                return true;
            }
            case GLFW.GLFW_KEY_BACKSPACE -> {
                if (cursorPosition > 0) {
                    inputText = inputText.substring(0, cursorPosition - 1) + inputText.substring(cursorPosition);
                    cursorPosition--;
                }
                return true;
            }
            case GLFW.GLFW_KEY_DELETE -> {
                if (cursorPosition < inputText.length()) {
                    inputText = inputText.substring(0, cursorPosition) + inputText.substring(cursorPosition + 1);
                }
                return true;
            }
            case GLFW.GLFW_KEY_LEFT -> {
                if (cursorPosition > 0) cursorPosition--;
                return true;
            }
            case GLFW.GLFW_KEY_RIGHT -> {
                if (cursorPosition < inputText.length()) cursorPosition++;
                return true;
            }
            case GLFW.GLFW_KEY_HOME -> {
                cursorPosition = 0;
                return true;
            }
            case GLFW.GLFW_KEY_END -> {
                cursorPosition = inputText.length();
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!inputMode) return false;

        if (isValidInputChar(chr)) {
            String newText = inputText.substring(0, cursorPosition) + chr + inputText.substring(cursorPosition);

            if (isValidInputFormat(newText)) {
                inputText = newText;
                cursorPosition++;
            }
            return true;
        }

        return false;
    }

    private boolean isValidInputChar(char chr) {
        return Character.isDigit(chr) || chr == '.' || chr == '-';
    }

    private boolean isValidInputFormat(String text) {
        if (text.isEmpty() || text.equals("-") || text.equals(".") || text.equals("-.")) {
            return true;
        }

        int dotCount = 0;
        int minusCount = 0;
        int digitsAfterDot = 0;
        boolean foundDot = false;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == '-') {
                if (i != 0) return false;
                minusCount++;
                if (minusCount > 1) return false;
            } else if (c == '.') {
                if (sliderSettings.isInteger()) return false;
                dotCount++;
                if (dotCount > 1) return false;
                foundDot = true;
            } else if (Character.isDigit(c)) {
                if (foundDot) {
                    digitsAfterDot++;
                    if (digitsAfterDot > 1) return false;
                }
            } else {
                return false;
            }
        }

        return true;
    }

    private void applyInputValue() {
        if (inputText.isEmpty() || inputText.equals("-") || inputText.equals(".") || inputText.equals("-.")) {
            return;
        }

        try {
            float value;
            if (sliderSettings.isInteger()) {
                value = Integer.parseInt(inputText);
            } else {
                value = Float.parseFloat(inputText);
            }

            value = Math.max(sliderSettings.getMin(), Math.min(sliderSettings.getMax(), value));

            if (sliderSettings.isInteger()) {
                value = Math.round(value);
            }

            sliderSettings.setValue(value);
        } catch (NumberFormatException ignored) {
        }
    }

    private void updateValue(double mouseX) {
        float sliderPadding = 1f;
        float sliderTrackWidth = width - 2;

        float percentage = (float) ((mouseX - x - sliderPadding) / sliderTrackWidth);
        percentage = Math.max(0, Math.min(1, percentage));

        float range = sliderSettings.getMax() - sliderSettings.getMin();
        float newValue = sliderSettings.getMin() + (range * percentage);

        if (sliderSettings.isInteger()) {
            newValue = Math.round(newValue);
        }

        sliderSettings.setValue(newValue);
    }

    @Override
    public void tick() {
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isInputMode() {
        return inputMode;
    }
}