package fun.aegis.display.screens.clickgui.newgui.panels;

import fun.aegis.Aegis;
import fun.aegis.display.screens.clickgui.newgui.animation.Animation;
import fun.aegis.display.screens.clickgui.newgui.animation.Easing;
import fun.aegis.display.screens.clickgui.newgui.theme.Theme;
import fun.aegis.display.screens.clickgui.newgui.theme.ThemeManager;
import fun.aegis.display.screens.clickgui.newgui.utils.MsdfFonts;
import fun.aegis.display.screens.clickgui.newgui.utils.Rect;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.utils.display.shape.ShapeProperties;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import static fun.aegis.utils.display.interfaces.QuickImports.blur;

public class HeaderPanel {
    public Rect themeButtonBounds;
    public Rect searchBarBounds;
    public Rect layoutToggleButtonBounds;
    private final Runnable onLayoutToggle;
    private final Runnable onThemeSwitch;
    private ModuleCategory lastCategory = ModuleCategory.COMBAT;
    private final Animation animation = new Animation(300, 1, Easing.QUAD_IN_OUT);
    private String searchText = "";

    public HeaderPanel(Runnable onLayoutToggle, Runnable onThemeSwitch) {
        this.onLayoutToggle = onLayoutToggle;
        this.onThemeSwitch = onThemeSwitch;
    }

    public void render(DrawContext ctx, float contentStartX, float sidebarY, float boxX, int columns,
                       float boxWidth, float progress, ModuleCategory selectedCategory) {
        MatrixStack matrix = ctx.getMatrices();
        Theme theme = ThemeManager.getInstance().getCurrentTheme();
        animation.update(1);
        int sideBar = Theme.applyAlpha(theme.getForegroundColorInt(), progress);
        int textColor = Theme.applyAlpha(theme.getWhiteInt(), progress);
        float x = contentStartX;
        float iconSize = 7f;
        float textSize = 7f;

        // Breadcrumbs
        String name = selectedCategory.getReadableName();
        float textW = MathHelper.lerp(animation.getValue(), 
                MsdfFonts.getTextWidth(lastCategory.getReadableName(), textSize), 
                MsdfFonts.getTextWidth(name, textSize));
        float breadcrumbWidth = 16 + 12 + textW + 16;
        float h = 22;

        blur.render(ShapeProperties.create(matrix, x, sidebarY, breadcrumbWidth, h)
                .round(7).color(sideBar).build());

        float cx = x + 8;
        float vy = sidebarY + h / 2f;
        MsdfFonts.drawIcon(matrix, "7", cx, vy - iconSize / 2 - 0.5f, iconSize, 
                Theme.applyAlpha(theme.getColorInt(), progress));
        cx += MsdfFonts.getIconWidth("7", iconSize) + 4;
        MsdfFonts.drawIcon(matrix, "A", cx + 1, vy - 6 / 2 - 0.3f, 6f, 
                Theme.applyAlpha(theme.getForegroundGrayInt(), progress));
        cx += MsdfFonts.getIconWidth("A", 6f) + 4;
        MsdfFonts.drawText(matrix, name, cx, vy - textSize / 2f, textSize, textColor);

        x += breadcrumbWidth + 8;

        // Stats
        int enabled = 0, total = 0;
        for (Module m : Aegis.getInstance().getModuleRepository().modules()) {
            if (m.getCategory() == selectedCategory) {
                total++;
                if (m.isState()) enabled++;
            }
        }
        float statsW = 60;
        blur.render(ShapeProperties.create(matrix, x, sidebarY, statsW, h).round(7).color(sideBar).build());
        MsdfFonts.drawText(matrix, enabled + "/" + total, x + 8, sidebarY + 7, textSize, textColor);
        x += statsW + 8;

        // Theme button
        float size = 22;
        themeButtonBounds = new Rect(x, sidebarY, size, size);
        blur.render(ShapeProperties.create(matrix, x, sidebarY, size, size).round(6).color(sideBar).build());
        String themeIcon = ThemeManager.getInstance().isDark() ? "C" : "D";
        float ix = x + (size - MsdfFonts.getIconWidth(themeIcon, iconSize)) / 2f;
        float iy = sidebarY + (size - iconSize) / 2f;
        MsdfFonts.drawIcon(matrix, themeIcon, ix, iy, iconSize, Theme.applyAlpha(theme.getColorInt(), progress));
        x += size + 8;

        // Layout button
        layoutToggleButtonBounds = new Rect(x, sidebarY, size, size);
        blur.render(ShapeProperties.create(matrix, x, sidebarY, size, size).round(6).color(sideBar).build());
        String layoutIcon = columns == 2 ? ":" : columns == 3 ? ";" : "9";
        ix = x + (size - MsdfFonts.getIconWidth(layoutIcon, iconSize)) / 2f;
        MsdfFonts.drawIcon(matrix, layoutIcon, ix, iy, iconSize, Theme.applyAlpha(theme.getColorInt(), progress));

        // Search bar - positioned within content area
        float searchW = 128, searchH = 22, pad = 8;
        float contentEndX = boxX + boxWidth - 8;
        float searchX = contentEndX - pad - searchW;
        searchBarBounds = new Rect(searchX, sidebarY, searchW, searchH);
        blur.render(ShapeProperties.create(matrix, searchX, sidebarY, searchW, searchH).round(6).color(sideBar).build());
        String searchDisplay = searchText.isEmpty() ? "Search" : searchText;
        int searchColor = searchText.isEmpty() ? Theme.applyAlpha(theme.getWhiteInt(), progress * 0.5f) : textColor;
        MsdfFonts.drawText(matrix, searchDisplay, searchX + 8, sidebarY + 7, textSize, searchColor);
    }

    public void resetAnim(ModuleCategory last, ModuleCategory next) {
        animation.reset(0);
        this.lastCategory = last;
    }

    public boolean handleMouseClicked(double mouseX, double mouseY) {
        if (layoutToggleButtonBounds != null && layoutToggleButtonBounds.contains(mouseX, mouseY)) { onLayoutToggle.run(); return true; }
        if (themeButtonBounds != null && themeButtonBounds.contains(mouseX, mouseY)) { onThemeSwitch.run(); return true; }
        if (searchBarBounds != null && searchBarBounds.contains(mouseX, mouseY)) { 
            searchText = "";
            return true; 
        }
        return false;
    }

    public boolean handleKeyPressed(int keyCode, int scanCode, int modifiers) {
        if (searchBarBounds == null) return false;
        
        if (keyCode == 259) { // Backspace
            if (!searchText.isEmpty()) {
                searchText = searchText.substring(0, searchText.length() - 1);
            }
            return true;
        }
        return false;
    }

    public boolean handleCharTyped(char chr, int modifiers) {
        if (searchBarBounds == null) return false;
        if (Character.isLetterOrDigit(chr) || chr == ' ' || chr == '-' || chr == '_') {
            if (searchText.length() < 32) {
                searchText += chr;
            }
            return true;
        }
        return false;
    }

    public String getSearchText() { return searchText; }
    public void setSearchText(String text) { this.searchText = text; }
}
