package fun.aegis.display.screens.clickgui.components.implement.other;

import fun.aegis.Aegis;
import fun.aegis.display.screens.clickgui.MenuScreen;
import fun.aegis.display.screens.clickgui.components.AbstractComponent;
import fun.aegis.display.screens.clickgui.components.implement.settings.TextComponent;
import fun.aegis.features.impl.render.Hud;
import fun.aegis.utils.client.managers.file.impl.ModuleFile;
import fun.aegis.utils.client.sound.SoundManager;
import fun.aegis.utils.display.font.FontRenderer;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.scissor.ScissorAssist;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.math.calc.Calculate;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;

import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ConfigManagerComponent extends AbstractComponent {
    private static final float PANEL_WIDTH = 100f;
    private static final float PANEL_HEIGHT = 120f;
    private static final float HEADER_HEIGHT = 16f;
    private static final float CONFIG_ITEM_HEIGHT = 13f;
    private static final float INPUT_HEIGHT = 13f;
    private static final float PADDING = 4f;
    private static final float SCROLL_STEP = 10f;

    private final List<String> configs = new ArrayList<>();
    private float scroll = 0f;
    private float smoothedScroll = 0f;

    // Input field
    public static boolean typing = false;
    private String inputText = "";
    private int cursorPosition = 0;
    private int selectionStart = -1;
    private int selectionEnd = -1;
    private boolean dragging = false;
    private long lastClickTime = 0;
    private float xOffset = 0;

    public ConfigManagerComponent() {
        refreshConfigs();
    }

    public void refreshConfigs() {
        configs.clear();
        File customDir = new File(Aegis.getInstance().getClientInfoProvider().configsDir(), "Custom");
        if (!customDir.exists()) {
            if (!customDir.mkdirs()) {
                return;
            }
        }
        File[] configFiles = customDir.listFiles();
        if (configFiles != null) {
            for (File file : configFiles) {
                if (file.isFile() && file.getName().endsWith(".clysm")) {
                    configs.add(file.getName().replace(".clysm", ""));
                }
            }
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        Matrix4f positionMatrix = matrix.peek().getPositionMatrix();

        // Background blur
        blur.render(ShapeProperties.create(matrix, x, y, PANEL_WIDTH, PANEL_HEIGHT).round(7.5f).quality(32)
                .softness(25)
                .color(new Color(11, 12, 18, 80).getRGB())
                .build());

        // Header separator
        rectangle.render(ShapeProperties.create(matrix, x, y + HEADER_HEIGHT, PANEL_WIDTH, 0.5f)
                .color(new Color(55, 55, 70, 180).getRGB(), new Color(55, 55, 70, 15).getRGB(), 
                       new Color(55, 55, 70, 180).getRGB(), new Color(55, 55, 70, 15).getRGB())
                .build());

        // Header - icon "m" and title "Configs"
        int themeColor = Hud.getInstance().colorSetting.getColor();
        FontRenderer iconFont = Fonts.getSize(20, Fonts.Type.ICONSCATEGORY);
        FontRenderer titleFont = Fonts.getSize(15, Fonts.Type.DEFAULT);
        
        String icon = "m";
        String title = "CONFIGS";
        float iconW = iconFont.getStringWidth(icon);
        float titleW = titleFont.getStringWidth(title);
        float totalW = iconW + 4f + titleW;
        float startX = x + (PANEL_WIDTH - totalW) / 2f;
        float centerY = y + HEADER_HEIGHT / 2.5f;
        
        iconFont.drawString(matrix, icon, startX, centerY + 1f, themeColor);
        titleFont.drawString(matrix, title, startX + iconW + 4f, centerY + 1.5f, themeColor);

        // Content area
        float contentY = y + HEADER_HEIGHT + 4f;
        float contentHeight = PANEL_HEIGHT - HEADER_HEIGHT - INPUT_HEIGHT - 12f;

        ScissorAssist scissor = Aegis.getInstance().getScissorManager();
        scissor.push(positionMatrix, x + PADDING, contentY, PANEL_WIDTH - PADDING * 2, contentHeight);

        // Render configs
        float yOff = 0f;
        float totalH = 0f;
        float renderScroll = Math.round(smoothedScroll);
        
        // Делаем копию чтобы избежать ConcurrentModificationException
        List<String> configsCopy = new ArrayList<>(configs);
        for (int i = 0; i < configsCopy.size(); i++) {
            String config = configsCopy.get(i);
            float itemY = contentY + yOff + renderScroll;
            
            renderConfigItem(matrix, config, x + PADDING, itemY, PANEL_WIDTH - PADDING * 2, CONFIG_ITEM_HEIGHT, mouseX, mouseY);
            
            yOff += CONFIG_ITEM_HEIGHT + 2f;
            totalH += CONFIG_ITEM_HEIGHT + 2f;
        }

        scissor.pop();

        // Update scroll
        float maxScroll = Math.max(0f, totalH - contentHeight);
        scroll = MathHelper.clamp(scroll, -maxScroll, 0f);
        smoothedScroll = (float) Calculate.interpolateSmooth(2, smoothedScroll, scroll);

        // Input field at bottom
        float inputY = y + PANEL_HEIGHT - INPUT_HEIGHT - 4f;
        renderInputField(context, x + PADDING, inputY, PANEL_WIDTH - PADDING * 2, INPUT_HEIGHT, mouseX, mouseY);
    }

    private void renderConfigItem(MatrixStack matrix, String config, float itemX, float itemY, float itemW, float itemH, int mouseX, int mouseY) {
        FontRenderer iconFont = Fonts.getSize(18, Fonts.Type.ICONSCATEGORY);
        FontRenderer smallIconFont = Fonts.getSize(14, Fonts.Type.ICONSCATEGORY);
        FontRenderer textFont = Fonts.getSize(11, Fonts.Type.DEFAULT);
        
        boolean hovered = Calculate.isHovered(mouseX, mouseY, itemX, itemY, itemW, itemH);
        
        // Background
        rectangle.render(ShapeProperties.create(matrix, itemX, itemY, itemW, itemH)
                .round(4f)
                .color(hovered ? new Color(40, 40, 50, 120).getRGB() : new Color(25, 25, 35, 80).getRGB())
                .build());

        // Icon "h" (config icon) - bigger
        iconFont.drawString(matrix, "h", itemX + 2f, itemY + (itemH - 13f) / 2f + 5f, 0xFFD4D6E1);

        // Config name
        String displayName = config;
        float maxNameWidth = itemW - 50f;
        if (textFont.getStringWidth(displayName) > maxNameWidth) {
            while (textFont.getStringWidth(displayName + "...") > maxNameWidth && displayName.length() > 0) {
                displayName = displayName.substring(0, displayName.length() - 1);
            }
            displayName += "...";
        }
        textFont.drawString(matrix, displayName, itemX + 18f, itemY + (itemH - 8f) / 2f + 3f, 0xFFD4D6E1);

        // Load button "j"
        float loadX = itemX + itemW - 28f;
        boolean loadHovered = Calculate.isHovered(mouseX, mouseY, loadX, itemY + 1f, 13f, itemH - 2f);
        smallIconFont.drawString(matrix, "j", loadX, itemY + (itemH - 10f) / 2f + 4f, loadHovered ? 0xFF7BFF7B : 0xFF878894);

        // Delete button "l"
        float deleteX = itemX + itemW - 13f;
        boolean deleteHovered = Calculate.isHovered(mouseX, mouseY, deleteX, itemY + 1f, 13f, itemH - 2f);
        smallIconFont.drawString(matrix, "l", deleteX, itemY + (itemH - 10f) / 2f + 4f, deleteHovered ? 0xFFFF7B7B : 0xFF878894);
    }

    private void renderInputField(DrawContext context, float inputX, float inputY, float inputW, float inputH, int mouseX, int mouseY) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(10, Fonts.Type.DEFAULT);
        FontRenderer iconFont = Fonts.getSize(14, Fonts.Type.ICONSCATEGORY);
        
        updateXOffset(font, cursorPosition);

        // Input background
        float textFieldWidth = inputW - 18f;
        rectangle.render(ShapeProperties.create(matrix, inputX, inputY, textFieldWidth, inputH)
                .round(4f)
                .color(new Color(25, 25, 35, 120).getRGB())
                .build());

        // Text content
        ScissorAssist scissor = Aegis.getInstance().getScissorManager();
        scissor.push(matrix.peek().getPositionMatrix(), inputX + 2f, inputY, textFieldWidth - 4f, inputH);

        String displayText = inputText.isEmpty() && !typing ? "Name..." : inputText;
        
        // Selection highlight
        if (typing && selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd) {
            int start = Math.max(0, Math.min(getStartOfSelection(), inputText.length()));
            int end = Math.max(0, Math.min(getEndOfSelection(), inputText.length()));
            if (start < end) {
                float selectionXStart = inputX + 4f - xOffset + font.getStringWidth(inputText.substring(0, start));
                float selectionXEnd = inputX + 4f - xOffset + font.getStringWidth(inputText.substring(0, end));
                float selectionWidth = selectionXEnd - selectionXStart;
                rectangle.render(ShapeProperties.create(matrix, selectionXStart, inputY + (inputH / 2) - 4, selectionWidth, 8).color(0xFF5585E8).build());
            }
        }

        font.drawString(matrix, displayText, inputX + 4f - xOffset, inputY + (inputH / 2) - 2f, typing ? -1 : 0xFF878894);

        // Cursor
        long currentTime = System.currentTimeMillis();
        boolean focused = typing && (currentTime % 1000 < 500);
        if (focused && (selectionStart == -1 || selectionStart == selectionEnd)) {
            float cursorX = font.getStringWidth(inputText.substring(0, cursorPosition));
            rectangle.render(ShapeProperties.create(matrix, inputX + 4f - xOffset + cursorX, inputY + (inputH / 2) - 4f, 0.5f, 8).color(-1).build());
        }

        scissor.pop();

        // Add button "z"
        float addX = inputX + textFieldWidth + 2f;
        boolean addHovered = Calculate.isHovered(mouseX, mouseY, addX, inputY, 14f, inputH);
        rectangle.render(ShapeProperties.create(matrix, addX, inputY, 14f, inputH)
                .round(3f)
                .color(addHovered ? new Color(50, 50, 60, 150).getRGB() : new Color(35, 35, 45, 120).getRGB())
                .build());
        float iconZWidth = iconFont.getStringWidth("z");
        iconFont.drawString(matrix, "z", addX + (14f - iconZWidth) / 2f, inputY + (inputH - 10f) / 2f + 3f, addHovered ? 0xFF7BFF7B : 0xFFD4D6E1);

        if (dragging) {
            double[] transformed = transformMouseCoords(mouseX, mouseY);
            cursorPosition = getCursorIndexAt(transformed[0], inputX);
            if (selectionStart == -1) {
                selectionStart = cursorPosition;
            }
            selectionEnd = cursorPosition;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (!Calculate.isHovered(mouseX, mouseY, x, y, PANEL_WIDTH, PANEL_HEIGHT)) {
            typing = false;
            clearSelection();
            return false;
        }

        float contentY = y + HEADER_HEIGHT + 4f;
        float contentHeight = PANEL_HEIGHT - HEADER_HEIGHT - INPUT_HEIGHT - 12f;
        float inputY = y + PANEL_HEIGHT - INPUT_HEIGHT - 4f;
        float inputW = PANEL_WIDTH - PADDING * 2;
        float textFieldWidth = inputW - 20f;

        // Check input field click
        if (Calculate.isHovered(mouseX, mouseY, x + PADDING, inputY, textFieldWidth, INPUT_HEIGHT) && button == 0) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastClickTime < 250) {
                selectionStart = 0;
                selectionEnd = inputText.length();
            } else {
                typing = true;
                dragging = true;
                lastClickTime = currentTime;
                double[] transformed = transformMouseCoords(mouseX, mouseY);
                cursorPosition = getCursorIndexAt(transformed[0], x + PADDING);
                selectionStart = cursorPosition;
                selectionEnd = cursorPosition;
            }
            return true;
        }

        // Check add button click
        float addX = x + PADDING + textFieldWidth + 4f;
        if (Calculate.isHovered(mouseX, mouseY, addX, inputY, 16f, INPUT_HEIGHT) && button == 0) {
            saveConfig();
            return true;
        }

        // Check config items click
        if (Calculate.isHovered(mouseX, mouseY, x + PADDING, contentY, PANEL_WIDTH - PADDING * 2, contentHeight)) {
            float yOff = 0f;
            float renderScroll = Math.round(smoothedScroll);
            
            // Делаем копию чтобы избежать ConcurrentModificationException
            List<String> configsCopy = new ArrayList<>(configs);
            for (int i = 0; i < configsCopy.size(); i++) {
                String config = configsCopy.get(i);
                float itemY = contentY + yOff + renderScroll;
                float itemW = PANEL_WIDTH - PADDING * 2;
                
                if (Calculate.isHovered(mouseX, mouseY, x + PADDING, itemY, itemW, CONFIG_ITEM_HEIGHT)) {
                    // Load button
                    float loadX = x + PADDING + itemW - 28f;
                    if (Calculate.isHovered(mouseX, mouseY, loadX, itemY + 1f, 13f, CONFIG_ITEM_HEIGHT - 2f)) {
                        loadConfig(config);
                        return true;
                    }
                    
                    // Delete button
                    float deleteX = x + PADDING + itemW - 13f;
                    if (Calculate.isHovered(mouseX, mouseY, deleteX, itemY + 1f, 13f, CONFIG_ITEM_HEIGHT - 2f)) {
                        deleteConfig(config);
                        return true;
                    }
                }
                
                yOff += CONFIG_ITEM_HEIGHT + 2f;
            }
        }

        typing = false;
        clearSelection();
        return true;
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (Calculate.isHovered(mouseX, mouseY, x, y, PANEL_WIDTH, PANEL_HEIGHT)) {
            scroll += amount * SCROLL_STEP;
            return true;
        }
        return false;
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (typing) {
            float maxTextWidth = Math.max(0, PANEL_WIDTH - PADDING * 2 - 30f);
            if (Fonts.getSize(11).getStringWidth(inputText) < maxTextWidth) {
                deleteSelectedText();
                inputText = inputText.substring(0, cursorPosition) + chr + inputText.substring(cursorPosition);
                cursorPosition++;
                clearSelection();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (typing) {
            if (Screen.hasControlDown()) {
                switch (keyCode) {
                    case GLFW.GLFW_KEY_A -> selectAllText();
                    case GLFW.GLFW_KEY_V -> pasteFromClipboard();
                    case GLFW.GLFW_KEY_C -> copyToClipboard();
                }
            } else {
                switch (keyCode) {
                    case GLFW.GLFW_KEY_BACKSPACE -> handleBackspace();
                    case GLFW.GLFW_KEY_ENTER -> {
                        saveConfig();
                        typing = false;
                    }
                    case GLFW.GLFW_KEY_LEFT, GLFW.GLFW_KEY_RIGHT -> moveCursor(keyCode);
                }
            }
            return true;
        }
        return false;
    }

    private void saveConfig() {
        if (inputText.isEmpty()) return;
        
        try {
            File customDir = new File(Aegis.getInstance().getClientInfoProvider().configsDir(), "Custom");
            if (!customDir.exists()) {
                if (!customDir.mkdirs()) {
                    return;
                }
            }
            
            ModuleFile moduleFile = new ModuleFile(
                    Aegis.getInstance().getModuleRepository(),
                    Aegis.getInstance().getDraggableRepository()
            );
            moduleFile.saveToFile(customDir, inputText + ".clysm");
            
            SoundManager.playSound(SoundManager.SAVED);
            inputText = "";
            cursorPosition = 0;
            clearSelection();
            refreshConfigs();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void loadConfig(String name) {
        try {
            File customDir = new File(Aegis.getInstance().getClientInfoProvider().configsDir(), "Custom");
            File configFile = new File(customDir, name + ".clysm");
            
            if (configFile.exists() && configFile.length() > 0) {
                ModuleFile moduleFile = new ModuleFile(
                        Aegis.getInstance().getModuleRepository(),
                        Aegis.getInstance().getDraggableRepository()
                );
                moduleFile.loadFromFile(customDir, name + ".clysm");
                SoundManager.playSound(SoundManager.LOADED);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void deleteConfig(String name) {
        try {
            File customDir = new File(Aegis.getInstance().getClientInfoProvider().configsDir(), "Custom");
            File configFile = new File(customDir, name + ".clysm");
            
            if (configFile.exists()) {
                configFile.delete();
                refreshConfigs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private double[] transformMouseCoords(double mouseX, double mouseY) {
        MenuScreen menu = MenuScreen.INSTANCE;
        float scale = menu.getScaleAnimation();
        float centerX = menu.x + menu.width / 2f;
        float centerY = menu.y + menu.height / 2f;
        double transformedX = (mouseX - centerX) / scale + centerX;
        double transformedY = (mouseY - centerY) / scale + centerY;
        return new double[]{transformedX, transformedY};
    }

    private void pasteFromClipboard() {
        String clipboardText = GLFW.glfwGetClipboardString(window.getHandle());
        if (clipboardText != null) {
            deleteSelectedText();
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
        selectionEnd = inputText.length();
        cursorPosition = inputText.length();
    }

    private void handleBackspace() {
        if (hasSelection()) {
            replaceText(getStartOfSelection(), getEndOfSelection(), "");
        } else if (cursorPosition > 0) {
            replaceText(cursorPosition - 1, cursorPosition, "");
        }
    }

    private void moveCursor(int keyCode) {
        if (Screen.hasShiftDown()) {
            if (selectionStart == -1) {
                selectionStart = cursorPosition;
            }
        } else {
            clearSelection();
        }

        if (keyCode == GLFW.GLFW_KEY_LEFT && cursorPosition > 0) {
            cursorPosition--;
        } else if (keyCode == GLFW.GLFW_KEY_RIGHT && cursorPosition < inputText.length()) {
            cursorPosition++;
        }

        if (Screen.hasShiftDown()) {
            selectionEnd = cursorPosition;
        }
    }

    private void replaceText(int start, int end, String replacement) {
        if (start < 0) start = 0;
        if (end > inputText.length()) end = inputText.length();
        if (start > end) {
            int temp = start;
            start = end;
            end = temp;
        }
        inputText = inputText.substring(0, start) + replacement + inputText.substring(end);
        cursorPosition = start + replacement.length();
        clearSelection();
    }

    private boolean hasSelection() {
        return selectionStart != -1 && selectionEnd != -1 && selectionStart != selectionEnd;
    }

    private String getSelectedText() {
        return inputText.substring(getStartOfSelection(), getEndOfSelection());
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

    private int getCursorIndexAt(double mouseX, float inputX) {
        FontRenderer font = Fonts.getSize(11);
        float relativeX = (float) mouseX - inputX - 4f + xOffset;
        int position = 0;
        while (position < inputText.length()) {
            float charWidth = font.getStringWidth(inputText.substring(position, position + 1));
            float textWidth = font.getStringWidth(inputText.substring(0, position));
            if (textWidth + charWidth / 2 > relativeX) {
                break;
            }
            position++;
        }
        return Math.max(0, Math.min(position, inputText.length()));
    }

    private void updateXOffset(FontRenderer font, int cursorPosition) {
        float cursorX = font.getStringWidth(inputText.substring(0, Math.min(cursorPosition, inputText.length())));
        float visibleWidth = PANEL_WIDTH - PADDING * 2 - 30f;
        if (cursorX < xOffset) {
            xOffset = Math.max(0, cursorX - 10);
        } else if (cursorX - xOffset > visibleWidth) {
            xOffset = cursorX - visibleWidth + 10;
        }
        if (xOffset < 0) xOffset = 0;
    }

    private void deleteSelectedText() {
        if (hasSelection()) {
            replaceText(getStartOfSelection(), getEndOfSelection(), "");
        }
    }
}
