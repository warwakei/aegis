package rich.screens.clickgui.impl.settingsrender;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import rich.modules.module.setting.implement.TextSetting;
import rich.util.interfaces.AbstractSettingComponent;
import rich.util.render.Render2D;
import rich.util.render.shader.Scissor;
import rich.util.render.font.Fonts;

import java.awt.*;

public class TextComponent extends AbstractSettingComponent {
    public static boolean typing = false;
    private final TextSetting textSetting;
    private boolean focused = false;
    private int cursorPosition = 0;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private long lastClickTime = 0;
    private String text = "";

    private float focusAnimation = 0f;
    private float hoverAnimation = 0f;
    private float textScrollOffset = 0f;
    private float targetScrollOffset = 0f;
    private float cursorBlinkAnimation = 0f;
    private float selectionAnimation = 0f;

    private long lastUpdateTime = System.currentTimeMillis();

    private static final float ANIMATION_SPEED = 8f;
    private static final float SCROLL_ANIMATION_SPEED = 10f;
    private static final float INPUT_BOX_WIDTH = 65f;
    private static final float INPUT_BOX_HEIGHT = 10f;
    private static final float TEXT_PADDING = 4f;

    public TextComponent(TextSetting setting) {
        super(setting);
        this.textSetting = setting;
        this.text = textSetting.getText() != null ? textSetting.getText() : "";
        this.cursorPosition = text.length();
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

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        float deltaTime = getDeltaTime();

        boolean hovered = isInputBoxHover(mouseX, mouseY);
        hoverAnimation = lerp(hoverAnimation, hovered ? 1f : 0f, deltaTime * ANIMATION_SPEED);
        focusAnimation = lerp(focusAnimation, focused ? 1f : 0f, deltaTime * ANIMATION_SPEED);
        selectionAnimation = lerp(selectionAnimation, hasSelection() ? 1f : 0f, deltaTime * ANIMATION_SPEED);

        if (focused) {
            cursorBlinkAnimation += deltaTime * 2f;
            if (cursorBlinkAnimation > 1f) cursorBlinkAnimation -= 1f;
        } else {
            cursorBlinkAnimation = 0f;
        }

        int iconAlpha = (int)(200 * alphaMultiplier);
        Fonts.GUI_ICONS.draw("S", x + 0.5f, y + height / 2 - 10.25f, 11, new Color(210, 210, 220, iconAlpha).getRGB());

        Fonts.BOLD.draw(textSetting.getName(), x + 9.5f, y + height / 2 - 7.5f, 6, applyAlpha(new Color(210, 210, 220, 200)).getRGB());

        String description = textSetting.getDescription();
        if (description != null && !description.isEmpty()) {
            Fonts.BOLD.draw(description, x + 0.5f, y + height / 2 + 0.5f, 5, applyAlpha(new Color(128, 128, 128, 128)).getRGB());
        }

        float boxX = x + width - INPUT_BOX_WIDTH - 2;
        float boxY = y + height / 2 - INPUT_BOX_HEIGHT / 2;

        int bgAlpha = (int)(25 + focusAnimation * 15 + hoverAnimation * 10);
        Render2D.rect(boxX, boxY, INPUT_BOX_WIDTH, INPUT_BOX_HEIGHT, applyAlpha(new Color(40, 40, 45, bgAlpha)).getRGB(), 3f);

        float outlineAlpha = 60 + hoverAnimation * 40 + focusAnimation * 60;
        Color outlineColor = focused
                ? new Color(100, 140, 180, (int)(outlineAlpha * alphaMultiplier))
                : new Color(155, 155, 155, (int)(outlineAlpha * alphaMultiplier));
        Render2D.outline(boxX, boxY, INPUT_BOX_WIDTH, INPUT_BOX_HEIGHT, 0.5f, outlineColor.getRGB(), 3f);

        renderTextContent(boxX, boxY, deltaTime);
    }

    private void renderTextContent(float boxX, float boxY, float deltaTime) {
        float textAreaX = boxX + TEXT_PADDING;
        float textAreaWidth = INPUT_BOX_WIDTH - TEXT_PADDING * 2;
        float textY = boxY + INPUT_BOX_HEIGHT / 2 - 2.5f;

        String displayText = text;
        float fullTextWidth = Fonts.BOLD.getWidth(displayText, 5);

        if (focused) {
            String beforeCursor = text.substring(0, cursorPosition);
            float cursorX = Fonts.BOLD.getWidth(beforeCursor, 5);

            if (cursorX - targetScrollOffset > textAreaWidth - 2) {
                targetScrollOffset = cursorX - textAreaWidth + 2;
            } else if (cursorX - targetScrollOffset < 0) {
                targetScrollOffset = cursorX;
            }

            if (fullTextWidth <= textAreaWidth) {
                targetScrollOffset = 0;
            }

            targetScrollOffset = Math.max(0, Math.min(targetScrollOffset, Math.max(0, fullTextWidth - textAreaWidth)));
        } else {
            targetScrollOffset = 0;
        }

        textScrollOffset = lerp(textScrollOffset, targetScrollOffset, deltaTime * SCROLL_ANIMATION_SPEED);

        Scissor.enable(boxX + 2, boxY, INPUT_BOX_WIDTH - 4, INPUT_BOX_HEIGHT,2);

        if (text.isEmpty() && !focused) {
            Fonts.BOLD.draw("Enter text...", textAreaX, textY, 5, applyAlpha(new Color(100, 100, 105, 100)).getRGB());
        } else {
            if (focused && hasSelection() && selectionAnimation > 0.01f) {
                int start = getStartOfSelection();
                int end = getEndOfSelection();
                String beforeSelection = text.substring(0, start);
                String selection = text.substring(start, end);

                float selectionX = textAreaX + Fonts.BOLD.getWidth(beforeSelection, 5) - textScrollOffset;
                float selectionWidth = Fonts.BOLD.getWidth(selection, 5);

                int selAlpha = (int)(100 * selectionAnimation * alphaMultiplier);
                Render2D.rect(selectionX, boxY + 2, selectionWidth, INPUT_BOX_HEIGHT - 4,
                        new Color(100, 140, 180, selAlpha).getRGB(), 2f);
            }

            int textAlpha = (int)((160 + focusAnimation * 60) * alphaMultiplier);
            Fonts.BOLD.draw(displayText, textAreaX - textScrollOffset, textY, 5,
                    new Color(210, 210, 220, textAlpha).getRGB());

            if (focused && !hasSelection()) {
                float cursorAlpha = (float)(Math.sin(cursorBlinkAnimation * Math.PI * 2) * 0.5 + 0.5);
                if (cursorAlpha > 0.3f) {
                    String beforeCursor = text.substring(0, cursorPosition);
                    float cursorXPos = textAreaX + Fonts.BOLD.getWidth(beforeCursor, 5) - textScrollOffset;
                    int cursorAlphaInt = (int)(255 * cursorAlpha * focusAnimation * alphaMultiplier);
                    Render2D.rect(cursorXPos, boxY + 2, 0.5f, INPUT_BOX_HEIGHT - 4,
                            new Color(180, 180, 185, cursorAlphaInt).getRGB(), 0f);
                }
            }
        }

        Scissor.disable();
    }

    private boolean isInputBoxHover(double mouseX, double mouseY) {
        float boxX = x + width - INPUT_BOX_WIDTH - 2;
        float boxY = y + height / 2 - INPUT_BOX_HEIGHT / 2;
        return mouseX >= boxX && mouseX <= boxX + INPUT_BOX_WIDTH &&
                mouseY >= boxY && mouseY <= boxY + INPUT_BOX_HEIGHT;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean wasInside = isInputBoxHover(mouseX, mouseY);

        if (wasInside && button == 0) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < 250 && focused) {
                selectAllText();
            } else {
                focused = true;
                typing = true;
                cursorPosition = getCursorIndexAt(mouseX);
                selectionStart = cursorPosition;
                selectionEnd = cursorPosition;
            }
            lastClickTime = currentTime;
            return true;
        } else if (!wasInside && focused) {
            applyText();
            focused = false;
            typing = false;
            clearSelection();
        }

        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (focused && button == 0) {
            cursorPosition = getCursorIndexAt(mouseX);
            selectionEnd = cursorPosition;
            return true;
        }
        return false;
    }

    private boolean isControlDown() {
        long window = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS;
    }

    private boolean isShiftDown() {
        long window = mc.getWindow().getHandle();
        return GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_SHIFT) == GLFW.GLFW_PRESS ||
                GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_SHIFT) == GLFW.GLFW_PRESS;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (!focused) return false;

        if (isControlDown()) {
            switch (keyCode) {
                case GLFW.GLFW_KEY_A -> {
                    selectAllText();
                    return true;
                }
                case GLFW.GLFW_KEY_V -> {
                    pasteFromClipboard();
                    return true;
                }
                case GLFW.GLFW_KEY_C -> {
                    copyToClipboard();
                    return true;
                }
                case GLFW.GLFW_KEY_X -> {
                    if (hasSelection()) {
                        copyToClipboard();
                        deleteSelectedText();
                    }
                    return true;
                }
            }
        } else {
            switch (keyCode) {
                case GLFW.GLFW_KEY_BACKSPACE -> {
                    handleBackspace();
                    return true;
                }
                case GLFW.GLFW_KEY_DELETE -> {
                    handleDelete();
                    return true;
                }
                case GLFW.GLFW_KEY_LEFT -> {
                    moveCursor(-1);
                    return true;
                }
                case GLFW.GLFW_KEY_RIGHT -> {
                    moveCursor(1);
                    return true;
                }
                case GLFW.GLFW_KEY_HOME -> {
                    cursorPosition = 0;
                    updateSelectionAfterCursorMove();
                    return true;
                }
                case GLFW.GLFW_KEY_END -> {
                    cursorPosition = text.length();
                    updateSelectionAfterCursorMove();
                    return true;
                }
                case GLFW.GLFW_KEY_ENTER -> {
                    applyText();
                    focused = false;
                    typing = false;
                    return true;
                }
                case GLFW.GLFW_KEY_ESCAPE -> {
                    text = textSetting.getText() != null ? textSetting.getText() : "";
                    cursorPosition = text.length();
                    focused = false;
                    typing = false;
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!focused) return false;

        if (Character.isISOControl(chr)) {
            return false;
        }

        int maxLength = textSetting.getMax() > 0 ? textSetting.getMax() : Integer.MAX_VALUE;

        if (text.length() < maxLength || hasSelection()) {
            deleteSelectedText();
            text = text.substring(0, cursorPosition) + chr + text.substring(cursorPosition);
            cursorPosition++;
            clearSelection();
            return true;
        }

        return false;
    }

    @Override
    public void tick() {
    }

    private void applyText() {
        int minLength = textSetting.getMin() > 0 ? textSetting.getMin() : 0;
        int maxLength = textSetting.getMax() > 0 ? textSetting.getMax() : Integer.MAX_VALUE;

        if (text.length() >= minLength && text.length() <= maxLength) {
            textSetting.setText(text);
        } else {
            text = textSetting.getText() != null ? textSetting.getText() : "";
            cursorPosition = text.length();
        }
    }

    private void handleBackspace() {
        if (hasSelection()) {
            replaceText(getStartOfSelection(), getEndOfSelection(), "");
        } else if (cursorPosition > 0) {
            replaceText(cursorPosition - 1, cursorPosition, "");
        }
    }

    private void handleDelete() {
        if (hasSelection()) {
            replaceText(getStartOfSelection(), getEndOfSelection(), "");
        } else if (cursorPosition < text.length()) {
            text = text.substring(0, cursorPosition) + text.substring(cursorPosition + 1);
        }
    }

    private void moveCursor(int direction) {
        if (hasSelection() && !isShiftDown()) {
            if (direction < 0) {
                cursorPosition = getStartOfSelection();
            } else {
                cursorPosition = getEndOfSelection();
            }
            clearSelection();
        } else {
            if (direction < 0 && cursorPosition > 0) {
                cursorPosition--;
            } else if (direction > 0 && cursorPosition < text.length()) {
                cursorPosition++;
            }
            updateSelectionAfterCursorMove();
        }
    }

    private void updateSelectionAfterCursorMove() {
        if (isShiftDown()) {
            if (selectionStart == -1) {
                selectionStart = selectionEnd != -1 ? selectionEnd : cursorPosition;
            }
            selectionEnd = cursorPosition;
        } else {
            clearSelection();
        }
    }

    private void pasteFromClipboard() {
        String clipboardText = GLFW.glfwGetClipboardString(mc.getWindow().getHandle());
        if (clipboardText != null && !clipboardText.isEmpty()) {
            clipboardText = clipboardText.replaceAll("[\n\r\t]", "");

            if (hasSelection()) {
                deleteSelectedText();
            }

            int maxLength = textSetting.getMax() > 0 ? textSetting.getMax() : Integer.MAX_VALUE;
            int remainingSpace = maxLength - text.length();
            if (clipboardText.length() > remainingSpace) {
                clipboardText = clipboardText.substring(0, remainingSpace);
            }

            if (!clipboardText.isEmpty()) {
                text = text.substring(0, cursorPosition) + clipboardText + text.substring(cursorPosition);
                cursorPosition += clipboardText.length();
            }
        }
    }

    private void copyToClipboard() {
        if (hasSelection()) {
            GLFW.glfwSetClipboardString(mc.getWindow().getHandle(), getSelectedText());
        }
    }

    private void selectAllText() {
        selectionStart = 0;
        selectionEnd = text.length();
        cursorPosition = text.length();
    }

    private void replaceText(int start, int end, String replacement) {
        if (start < 0) start = 0;
        if (end > text.length()) end = text.length();
        if (start > end) {
            int temp = start;
            start = end;
            end = temp;
        }

        text = text.substring(0, start) + replacement + text.substring(end);
        cursorPosition = start + replacement.length();
        clearSelection();
    }

    private void deleteSelectedText() {
        if (hasSelection()) {
            replaceText(getStartOfSelection(), getEndOfSelection(), "");
        }
    }

    private boolean hasSelection() {
        return selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd;
    }

    private String getSelectedText() {
        if (!hasSelection()) return "";
        return text.substring(getStartOfSelection(), getEndOfSelection());
    }

    private int getStartOfSelection() {
        return Math.min(selectionStart, selectionEnd);
    }

    private int getEndOfSelection() {
        return Math.max(selectionStart, selectionEnd);
    }

    private void clearSelection() {
        selectionStart = -1;
        selectionEnd = -1;
    }

    private int getCursorIndexAt(double mouseX) {
        float boxX = x + width - INPUT_BOX_WIDTH - 2;
        float textAreaX = boxX + TEXT_PADDING;
        float relativeX = (float)(mouseX - textAreaX + textScrollOffset);

        if (relativeX <= 0) return 0;

        int position = 0;
        float lastWidth = 0;

        while (position < text.length()) {
            float currentWidth = Fonts.BOLD.getWidth(text.substring(0, position + 1), 5);
            float midPoint = (lastWidth + currentWidth) / 2;

            if (relativeX < midPoint) {
                return position;
            }

            lastWidth = currentWidth;
            position++;
        }

        return text.length();
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public boolean isFocused() {
        return focused;
    }
}