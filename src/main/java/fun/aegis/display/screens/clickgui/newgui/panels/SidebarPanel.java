package fun.aegis.display.screens.clickgui.newgui.panels;

import fun.aegis.display.screens.clickgui.newgui.animation.Animation;
import fun.aegis.display.screens.clickgui.newgui.animation.Easing;
import fun.aegis.display.screens.clickgui.newgui.theme.Theme;
import fun.aegis.display.screens.clickgui.newgui.theme.ThemeManager;
import fun.aegis.display.screens.clickgui.newgui.utils.MathUtil;
import fun.aegis.display.screens.clickgui.newgui.utils.MsdfFonts;
import fun.aegis.display.screens.clickgui.newgui.utils.Rect;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.utils.display.shape.ShapeProperties;
import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import java.util.*;
import java.util.function.Consumer;
import static fun.aegis.utils.display.interfaces.QuickImports.blur;

public class SidebarPanel {
    @Getter
    private final Map<ModuleCategory, Rect> categoryBounds = new HashMap<>();
    @Getter
    private Rect sidebarToggleButtonBounds;
    private final Animation sidebarAnimation;
    private final Consumer<ModuleCategory> onCategorySelect;
    private final Runnable onSidebarToggle;
    private final List<SideBarCategory> categories = new ArrayList<>();
    private final Animation animationChange = new Animation(200, 1, Easing.LINEAR);
    private Rect animRect = new Rect(0, 0, 0, 0);

    public SidebarPanel(Animation sidebarAnimation, Consumer<ModuleCategory> onCategorySelect, Runnable onSidebarToggle) {
        this.sidebarAnimation = sidebarAnimation;
        this.onCategorySelect = onCategorySelect;
        this.onSidebarToggle = onSidebarToggle;
        for (ModuleCategory cat : ModuleCategory.values()) {
            categories.add(new SideBarCategory(cat));
        }
    }

    public void render(DrawContext ctx, float boxX, float boxY, float height, float progress, 
                       ModuleCategory selectedCategory) {
        MatrixStack matrix = ctx.getMatrices();
        Theme theme = ThemeManager.getInstance().getCurrentTheme();
        
        float sidebarProgress = sidebarAnimation.update();
        float collapsedSidebarWidth = 30f;
        float expandedSidebarWidth = 88;
        float sidebarWidth = collapsedSidebarWidth + (expandedSidebarWidth - collapsedSidebarWidth) * sidebarProgress;
        float sidebarPadding = 8;
        float sidebarX = boxX + sidebarPadding;
        float sidebarY = boxY + sidebarPadding;
        float sidebarHeight = height - sidebarPadding * 2;

        int sideBar = Theme.applyAlpha(theme.getForegroundColorInt(), progress);
        categoryBounds.clear();

        blur.render(ShapeProperties.create(matrix, sidebarX, sidebarY, sidebarWidth, sidebarHeight)
                .round(7).color(sideBar).build());

        // Logo
        float logoSize = 14;
        float logoX = sidebarX + (collapsedSidebarWidth - logoSize) / 2f;
        float logoY = sidebarY + 8;
        MsdfFonts.drawIcon(matrix, "5", logoX + 2, logoY + 3, 11f, 
                Theme.applyAlpha(theme.getColorInt(), progress));

        int textColor = Theme.applyAlpha(theme.getWhiteInt(), progress * Math.min(1f, sidebarProgress * 2f));
        int textColorDisable = Theme.applyAlpha(theme.getGrayLightInt(), progress);
        int iconColorDisable = Theme.applyAlpha(theme.getGrayInt(), progress);
        int primary = Theme.applyAlpha(theme.getColorInt(), progress);

        // Client name
        MsdfFonts.drawText(matrix, "Cyphrone.fun", 
                logoX + logoSize + 8, logoY + (logoSize - 7) / 2f + 1, 7f, textColor);

        float iconSize = 7f;
        float padding = 10.5f;
        float startY = sidebarY + 35;

        // Render selected category background
        int index = 0;
        for (SideBarCategory sideBarCategory : categories) {
            if (selectedCategory == sideBarCategory.getCategory()) {
                float categoryY = startY + index * (iconSize + padding);
                float newX = (float) MathUtil.interpolate(animRect.x(), sidebarX + 4, animationChange.getValue());
                float newY = (float) MathUtil.interpolate(animRect.y(), categoryY, animationChange.getValue());
                animRect = new Rect(newX, newY, sidebarWidth - 8, iconSize + 11);
                
                blur.render(ShapeProperties.create(matrix, animRect.x(), animRect.y(), sidebarWidth - 8, iconSize + 11)
                        .round(4).color(Theme.applyAlpha(theme.getForegroundLightInt(), progress)).build());
                break;
            }
            index++;
        }

        animationChange.animateTo(1f);
        animationChange.update();

        // Render categories
        index = 0;
        for (SideBarCategory sideBarCategory : categories) {
            float categoryY = startY + index * (iconSize + padding);
            sideBarCategory.render(ctx, sidebarX + 4, categoryY, sidebarWidth - 8, iconSize + 11, 
                    sidebarProgress, selectedCategory == sideBarCategory.getCategory(), 
                    textColor, textColorDisable, iconColorDisable, primary);
            categoryBounds.put(sideBarCategory.getCategory(), new Rect(sidebarX + 4, categoryY, sidebarWidth - 8, iconSize + 11));
            index++;
        }

        // Toggle button
        float toggleX = logoX + 5;
        float toggleY = sidebarY + sidebarHeight - 27;
        MsdfFonts.drawIcon(matrix, "6", toggleX, toggleY, 7f, 
                Theme.applyAlpha(theme.getGrayInt(), progress));
        sidebarToggleButtonBounds = new Rect(toggleX, toggleY, 8, 8);
    }

    public boolean handleMouseClicked(double mouseX, double mouseY) {
        if (sidebarToggleButtonBounds != null && sidebarToggleButtonBounds.contains(mouseX, mouseY)) {
            onSidebarToggle.run();
            return true;
        }
        for (Map.Entry<ModuleCategory, Rect> entry : categoryBounds.entrySet()) {
            if (entry.getValue().contains(mouseX, mouseY)) {
                animationChange.animateTo(0);
                animationChange.setValue(0);
                onCategorySelect.accept(entry.getKey());
                return true;
            }
        }
        return false;
    }
}
