package fun.aegis.display.screens.clickgui.components.implement.settings;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import org.lwjgl.glfw.GLFW;

import fun.aegis.features.module.setting.implement.TextSetting;
import fun.aegis.utils.display.font.FontRenderer;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.client.chat.StringHelper;
import fun.aegis.utils.display.scissor.ScissorAssist;
import fun.aegis.Aegis;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TextComponent extends AbstractSettingComponent {
    public static boolean typing;
    private final TextSetting setting;
    private float rectX, rectY, rectWidth, rectHeight;
    private boolean dragging;
    private int cursorPosition = 0;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private long lastClickTime = 0;
    private float xOffset = 0;
    private String text = "";

    public TextComponent(TextSetting setting) {
        super(setting);
        this.setting = setting;
        String initialText = setting.getText();
        this.text = initialText != null && !initialText.isEmpty() ? initialText : "";
        this.cursorPosition = this.text.length();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        String wrapped = StringHelper.wrap(setting.getDescription(), 70, 13);
        FontRenderer font = Fonts.getSize(13);
        height = (int) (20 + font.getStringHeight(wrapped) / 3);

        this.rectX = x + width - 61.5F;
        this.rectY = y + 7.0F;
        this.rectWidth = 53.0F;
        this.rectHeight = 12.0F;

        rectangle.render(ShapeProperties.create(matrix, rectX, rectY, rectWidth, rectHeight)
                .round(2).thickness(2).outlineColor(ColorAssist.getOutline()).color(ColorAssist.getGuiRectColor(1)).build());

        Fonts.getSize(14, Fonts.Type.BOLD).drawString(context.getMatrices(), setting.getName(), x + 9, y + 6, 0xFFD4D6E1);
        font.drawString(context.getMatrices(), wrapped, x + 9, y + 16, 0xFF878894);

        updateXOffset(font, cursorPosition);

        ScissorAssist scissor = Aegis.getInstance().getScissorManager();
        scissor.push(matrix.peek().getPositionMatrix(), rectX + 1, (float) window.getScaledHeight() / 2 - 96, rectWidth - 3, 220);

        if (typing && selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd) {
            int start = Math.max(0, Math.min(getStartOfSelection(), text.length()));
            int end = Math.max(0, Math.min(getEndOfSelection(), text.length()));
            if (start < end) {
                float selectionXStart = rectX + 3 - xOffset + font.getStringWidth(text.substring(0, start));
                float selectionXEnd = rectX + 3 - xOffset + font.getStringWidth(text.substring(0, end));
                float selectionWidth = selectionXEnd - selectionXStart;

                rectangle.render(ShapeProperties.create(matrix, selectionXStart, rectY + (rectHeight / 2) - 4, selectionWidth, 8).color(0xFF5585E8).build());
            }
        }

        font.drawString(context.getMatrices(), text, rectX + 3 - xOffset, rectY + (rectHeight / 2) - 1.0F, typing ? -1 : 0xFF878894);

        if (!typing && text.isEmpty()) {
            String placeholder = setting.getText() != null ? setting.getText() : "";
            font.drawString(context.getMatrices(), placeholder, rectX + 3, rectY + (rectHeight / 2) - 1.0F, 0xFF878894);
        }

        scissor.pop();
        long currentTime = System.currentTimeMillis();
        boolean focused = typing && (currentTime % 1000 < 500);

        if (focused && (selectionStart == -1 || selectionStart == selectionEnd)) {
            float cursorX = font.getStringWidth(text.substring(0, cursorPosition));
            rectangle.render(ShapeProperties.create(matrix, rectX + 3 - xOffset + cursorX, rectY + (rectHeight / 2) - 3.5F, 0.5F, 7).color(-1).build());
        }

        if (dragging) {
            cursorPosition = getCursorIndexAt(mouseX);

            if (selectionStart == -1) {
                selectionStart = cursorPosition + 1;
            }
            selectionEnd = cursorPosition;
        }
    }


    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        dragging = true;
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (Calculate.isHovered(mouseX, mouseY, rectX, rectY, rectWidth, rectHeight) && button == 0) {
            typing = true;
            dragging = false;
            lastClickTime = System.currentTimeMillis();
            cursorPosition = getCursorIndexAt(mouseX);
            selectionStart = -1;
            selectionEnd = -1;
            return true;
        } else {
            if (typing && text.length() >= setting.getMin() && text.length() <= setting.getMax()) {
                setting.setText(text);
            }
            typing = false;
            clearSelection();
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }


    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        dragging = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }


    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (typing && (text.length() < setting.getMax())) {
            if (chr >= 32 && chr <= 126) {
                deleteSelectedText();
                text = text.substring(0, cursorPosition) + chr + text.substring(cursorPosition);
                cursorPosition++;
                clearSelection();
                if (text.length() >= setting.getMin() && text.length() <= setting.getMax()) {
                    setting.setText(text);
                }
                return true;
            }
        }
        return super.charTyped(chr, modifiers);
    }

    
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (typing) {
            if (Screen.hasControlDown()) switch (keyCode) {
                case GLFW.GLFW_KEY_A -> selectAllText();
                case GLFW.GLFW_KEY_V -> pasteFromClipboard();
                case GLFW.GLFW_KEY_C -> copyToClipboard();
            }
            else switch (keyCode) {
                case GLFW.GLFW_KEY_BACKSPACE, GLFW.GLFW_KEY_ENTER -> handleTextModification(keyCode);
                case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT -> moveCursor(keyCode);
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }


    private void pasteFromClipboard() {
        String clipboardText = GLFW.glfwGetClipboardString(window.getHandle());
        if (clipboardText != null) {
            replaceText(cursorPosition, cursorPosition, clipboardText);
        }
    }


    private void copyToClipboard() {
        if (hasSelection()) {
            GLFW.glfwSetClipboardString(window.getHandle(), getSelectedText());
        }
    }


    private void selectAllText() {
        selectionStart = 0;
        selectionEnd = text.length();
    }

    
    private void handleTextModification(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_BACKSPACE) {
            if (hasSelection()) {
                replaceText(getStartOfSelection(), getEndOfSelection(), "");
            } else if (cursorPosition > 0) {
                replaceText(cursorPosition - 1, cursorPosition, "");
            }
        } else if (keyCode == GLFW.GLFW_KEY_ENTER) {
            if (text.length() >= setting.getMin() && text.length() <= setting.getMax()) {
                setting.setText(text);
                typing = false;
            }
        }
    }

    
    private void moveCursor(int keyCode) {
        if (keyCode == GLFW.GLFW_KEY_LEFT && cursorPosition > 0) {
            cursorPosition--;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT && cursorPosition < text.length()) {
            cursorPosition++;
        }
        updateSelectionAfterCursorMove();
    }

    
    private void updateSelectionAfterCursorMove() {
        if (Screen.hasShiftDown()) {
            if (selectionStart == -1) selectionStart = cursorPosition;
            selectionEnd = cursorPosition;
        } else {
            clearSelection();
        }
    }

    
    private void replaceText(int start, int end, String replacement) {
        if (start < 0) start = 0;
        if (end > text.length()) end = text.length();
        if (start > end) start = end;

        text = text.substring(0, start) + replacement + text.substring(end);
        cursorPosition = start + replacement.length();
        clearSelection();
    }

    
    private boolean hasSelection() {
        return selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd;
    }


    private String getSelectedText() {
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
        FontRenderer font = Fonts.getSize(13, Fonts.Type.BOLD);
        float relativeX = (float) mouseX - rectX - 3 + xOffset;
        int position = 0;
        while (position < text.length()) {
            float textWidth = font.getStringWidth(text.substring(0, position + 1));
            if (textWidth > relativeX) {
                break;
            }
            position++;
        }
        return position;
    }

    
    private void updateXOffset(FontRenderer font, int cursorPosition) {
        float cursorX = font.getStringWidth(text.substring(0, cursorPosition));
        if (cursorX < xOffset) {
            xOffset = cursorX;
        } else if (cursorX - xOffset > rectWidth - 7) {
            xOffset = cursorX - (rectWidth - 7);
        }
    }

    
    private void deleteSelectedText() {
        if (hasSelection()) {
            replaceText(getStartOfSelection(), getEndOfSelection(), "");
        }
    }
}
