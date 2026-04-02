package fun.aegis.display.screens.clickgui.newgui.utils;

import com.google.common.base.Suppliers;
import fun.aegis.utils.display.atlasfont.msdf.MsdfFont;
import fun.aegis.utils.display.systemrender.builders.Builder;
import net.minecraft.client.util.math.MatrixStack;

import java.util.function.Supplier;

/**
 * MTSDF fonts for the new ClickGUI.
 * Uses fonts from assets/mre/fonts/
 */
public class MsdfFonts {
    
    // Icons font (0-9, A-W for various icons)
    public static final Supplier<MsdfFont> ICONS = Suppliers.memoize(() -> MsdfFont.builder()
            .name("icons")
            .atlas("icons")
            .data("icons")
            .build());
    
    // Medium text font (default)
    public static final Supplier<MsdfFont> MEDIUM = Suppliers.memoize(() -> MsdfFont.builder()
            .name("medium")
            .atlas("medium")
            .data("medium")
            .build());
    
    // Regular text font
    public static final Supplier<MsdfFont> REGULAR = Suppliers.memoize(() -> MsdfFont.builder()
            .name("regular")
            .atlas("regular")
            .data("regular")
            .build());
    
    // Semibold text font
    public static final Supplier<MsdfFont> SEMIBOLD = Suppliers.memoize(() -> MsdfFont.builder()
            .name("semibold")
            .atlas("semibold")
            .data("semibold")
            .build());

    /**
     * Draw MTSDF icon
     */
    public static void drawIcon(MatrixStack matrix, String icon, float x, float y, float size, int color) {
        Builder.text()
                .font(ICONS.get())
                .text(icon)
                .size(size)
                .color(color)
                .build()
                .render(matrix.peek().getPositionMatrix(), x, y, 0);
    }
    
    /**
     * Draw text with medium font (default)
     */
    public static void drawText(MatrixStack matrix, String text, float x, float y, float size, int color) {
        Builder.text()
                .font(MEDIUM.get())
                .text(text)
                .size(size)
                .color(color)
                .build()
                .render(matrix.peek().getPositionMatrix(), x, y, 0);
    }
    
    /**
     * Draw text with regular font
     */
    public static void drawRegular(MatrixStack matrix, String text, float x, float y, float size, int color) {
        Builder.text()
                .font(REGULAR.get())
                .text(text)
                .size(size)
                .color(color)
                .build()
                .render(matrix.peek().getPositionMatrix(), x, y, 0);
    }
    
    /**
     * Draw text with semibold font
     */
    public static void drawSemibold(MatrixStack matrix, String text, float x, float y, float size, int color) {
        Builder.text()
                .font(SEMIBOLD.get())
                .text(text)
                .size(size)
                .color(color)
                .build()
                .render(matrix.peek().getPositionMatrix(), x, y, 0);
    }
    
    /**
     * Get icon width
     */
    public static float getIconWidth(String icon, float size) {
        return ICONS.get().getWidth(icon, size);
    }
    
    /**
     * Get text width (medium font)
     */
    public static float getTextWidth(String text, float size) {
        return MEDIUM.get().getWidth(text, size);
    }
    
    /**
     * Get text width with regular font
     */
    public static float getRegularWidth(String text, float size) {
        return REGULAR.get().getWidth(text, size);
    }
    
    /**
     * Get text width with semibold font
     */
    public static float getSemiboldWidth(String text, float size) {
        return SEMIBOLD.get().getWidth(text, size);
    }
}
