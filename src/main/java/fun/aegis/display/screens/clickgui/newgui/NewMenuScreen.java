package fun.aegis.display.screens.clickgui.newgui;

import fun.aegis.Aegis;
import fun.aegis.display.screens.clickgui.newgui.animation.Animation;
import fun.aegis.display.screens.clickgui.newgui.animation.Easing;
import fun.aegis.display.screens.clickgui.newgui.elements.AbstractMenuElement;
import fun.aegis.display.screens.clickgui.newgui.elements.MenuModuleElement;
import fun.aegis.display.screens.clickgui.newgui.panels.HeaderPanel;
import fun.aegis.display.screens.clickgui.newgui.panels.SidebarPanel;
import fun.aegis.display.screens.clickgui.newgui.theme.Theme;
import fun.aegis.display.screens.clickgui.newgui.theme.ThemeManager;
import fun.aegis.display.screens.clickgui.newgui.utils.MathUtil;
import fun.aegis.display.screens.clickgui.newgui.utils.ScrollHandler;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.utils.client.sound.SoundManager;
import fun.aegis.utils.display.shape.ShapeProperties;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import java.util.*;
import static fun.aegis.utils.display.interfaces.QuickImports.blur;

@Getter
public class NewMenuScreen extends Screen {
    public static NewMenuScreen INSTANCE;
    private ModuleCategory selectedCategory = ModuleCategory.COMBAT;
    private ModuleCategory realSelectedCategory = ModuleCategory.COMBAT;
    private float boxX, boxY;
    private int columns = 1;
    private float boxWidth = 522;
    private float boxHeight = 316;
    private boolean draggingWindow;
    private float dragOffsetX, dragOffsetY;
    private final Animation sidebarAnimation = new Animation(300, 0f, Easing.CUBIC_IN_OUT);
    private boolean isSidebarExpanded;
    private final Animation animationClose = new Animation(300, 0f, Easing.BACK_OUT);
    private boolean initialized;
    private final ScrollHandler scrollHandler = new ScrollHandler();
    private boolean closing = false;
    private SidebarPanel sidebarPanel;
    private HeaderPanel headerPanel;
    private int scaledScissorX, scaledScissorY, scaledScissorEndX, scaledScissorEndY;
    private final Animation animationColumns = new Animation(300, 0, Easing.CUBIC_IN_OUT);
    private final Animation animationScrollHeight = new Animation(150, 1, Easing.QUAD_IN_OUT);
    private final Animation animationChangeCategory = new Animation(150, 1, Easing.CUBIC_IN_OUT);
    private boolean isDraggingScrollbar = false;
    private float scrollClickOffset = 0;
    List<AbstractMenuElement> modules = new ArrayList<>();

    public NewMenuScreen() {
        super(Text.of("NewMenuScreen"));
        INSTANCE = this;
    }

    public void initialize() {
        modules.clear();
        modules.addAll(Aegis.getInstance().getModuleRepository().modules().stream()
                .map(MenuModuleElement::new).toList());
    }

    @Override
    protected void init() {
        closing = false;
        animationColumns.setValue(columns == 3 ? 1 : 0);
        boxWidth = MathHelper.lerp(animationColumns.getValue(), 465, 533);
        boxHeight = MathHelper.lerp(animationColumns.getValue(), 282, 320);
        boxX = (this.width - boxWidth) / 2f;
        boxY = (this.height - boxHeight) / 2f;
        animationClose.setValue(0f);
        animationClose.animateTo(1f);

        if (!initialized) {
            this.sidebarPanel = new SidebarPanel(this.sidebarAnimation, category -> {
                this.headerPanel.resetAnim(realSelectedCategory, category);
                realSelectedCategory = category;
                this.scrollHandler.setTargetValue(0);
            }, () -> {
                this.isSidebarExpanded = !this.isSidebarExpanded;
                this.sidebarAnimation.animateTo(this.isSidebarExpanded ? 1f : 0f);
            });
            this.headerPanel = new HeaderPanel(
                    () -> this.columns = (this.columns % 3) + 1,
                    () -> ThemeManager.getInstance().switchTheme()
            );
        }
        initialized = true;
    }

    @Override
    public void tick() {
        if (closing && animationClose.getValue() <= 0.01f) {
            this.close();
        }
        super.tick();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        if (!initialized) return;
        MatrixStack matrix = context.getMatrices();
        
        animationColumns.update(columns == 3 ? 1 : 0);
        boxWidth = MathHelper.lerp(animationColumns.getValue(), 465, 533);
        boxHeight = MathHelper.lerp(animationColumns.getValue(), 282, 320);

        float progress = animationClose.update(closing ? 0.0f : 1.0f);
        progress = Math.min(Math.max(progress, 0), 1);
        float scale = 0.85f + 0.15f * progress;
        Theme theme = ThemeManager.getInstance().getCurrentTheme();

        matrix.push();
        float scaleX = boxX + boxWidth / 2f;
        float scaleY = boxY + boxHeight / 2f;
        matrix.translate(scaleX, scaleY, 1);
        matrix.scale(scale, scale, 1);
        matrix.translate(-scaleX, -scaleY, 1);

        int baseBg = Theme.applyAlpha(theme.getBackgroundColorInt(), progress * 4);
        blur.render(ShapeProperties.create(matrix, boxX, boxY, boxWidth, boxHeight).round(9).color(baseBg).build());

        // Sidebar
        sidebarPanel.render(context, boxX, boxY, boxHeight, progress, realSelectedCategory);

        // Header
        float sidebarProgress = sidebarAnimation.update();
        float sidebarWidth = 30f + (88f - 30f) * sidebarProgress;
        float contentStartX = boxX + 8f + sidebarWidth + 8f;
        float sidebarY = boxY + 8f;
        float contentY = boxY + 22f + 8f + 8f;
        headerPanel.render(context, contentStartX, sidebarY, boxX, columns, boxWidth, progress, realSelectedCategory);

        // Scrollbar
        float visibleHeight = boxHeight - (22f + 8f + 8f + 8f);
        float scrollProgress = scrollHandler.getMax() == 0 ? 0f : (float) (scrollHandler.getValue() / scrollHandler.getMax());
        float scrollHeight = Math.max(visibleHeight * (visibleHeight / (float) (visibleHeight + scrollHandler.getMax())), 20);
        scrollHeight = Math.min(visibleHeight, animationScrollHeight.update(scrollHeight));
        float denom = Math.max(1f, (visibleHeight - scrollHeight));
        float scrollY = contentY + denom * scrollProgress;
        scrollY = Math.min(contentY + visibleHeight, scrollY);

        float widthScroll = 1.2f;
        blur.render(ShapeProperties.create(matrix, boxX + boxWidth - 8 - widthScroll, contentY, widthScroll, visibleHeight)
                .round(0.5f).color(Theme.applyAlpha(theme.getForegroundColorInt(), progress)).build());
        
        if (scrollY + scrollHeight > visibleHeight + contentY) {
            blur.render(ShapeProperties.create(matrix, boxX + boxWidth - 8 - widthScroll, contentY, widthScroll, visibleHeight)
                    .round(1f).color(Theme.applyAlpha(theme.getForegroundStrokeInt(), progress)).build());
        } else {
            blur.render(ShapeProperties.create(matrix, boxX + boxWidth - 8 - widthScroll, scrollY, widthScroll, scrollHeight)
                    .round(1f).color(Theme.applyAlpha(theme.getForegroundStrokeInt(), progress)).build());
        }

        // Modules
        float contentWidth = boxX + (columns == 3 ? 530 : 461) - contentStartX - 8f;
        this.scaledScissorX = (int) contentStartX;
        this.scaledScissorY = (int) (boxY + (22f + 8f + 8f));
        this.scaledScissorEndX = (int) (boxX + boxWidth);
        this.scaledScissorEndY = (int) (boxY + boxHeight);

        context.enableScissor(scaledScissorX, scaledScissorY, scaledScissorEndX, scaledScissorEndY);
        animationChangeCategory.update(selectedCategory == realSelectedCategory ? 1 : 0);
        renderModules(context, mouseX, mouseY, progress * animationChangeCategory.getValue(), contentStartX, contentWidth, contentY);
        context.disableScissor();

        if (animationChangeCategory.getValue() == 0) {
            selectedCategory = realSelectedCategory;
        }

        if (isDraggingScrollbar) {
            float scrollbarY = boxY + 22f + 8f + 8f;
            float newY = mouseY - scrollbarY - scrollClickOffset;
            float scrollRatio = newY / denom;
            scrollHandler.setTargetValue(-(scrollRatio * scrollHandler.getMax()));
        }

        matrix.pop();
        super.render(context, mouseX, mouseY, delta);
    }

    private void renderModules(DrawContext ctx, float mouseX, float mouseY, float alpha, float contentStartX, float contentWidth, float startY) {
        scrollHandler.update();
        
        String searchText = headerPanel.getSearchText().toLowerCase();
        List<AbstractMenuElement> filteredModules = this.modules.stream()
                .filter(m -> searchText.isEmpty() ? m.getCategory() == selectedCategory : m.getName().toLowerCase().contains(searchText))
                .sorted(Comparator.comparing(AbstractMenuElement::getName, String.CASE_INSENSITIVE_ORDER))
                .toList();

        float padding = 6f;
        float scrollbarWidth = 6f;
        float maxContentWidth = contentWidth - scrollbarWidth;
        float moduleWidth = (maxContentWidth - padding * Math.max(0, columns - 1)) / columns;

        double[] columnHeights = new double[columns];
        for (int i = 0; i < columns; i++) {
            columnHeights[i] = padding;
        }

        for (AbstractMenuElement module : filteredModules) {
            int col = 0;
            for (int j = 1; j < columns; j++) {
                if (columnHeights[j] < columnHeights[col]) col = j;
            }
            float x = contentStartX + col * (moduleWidth + padding);
            float y = (float) (startY + columnHeights[col] - scrollHandler.getValue());
            module.render(ctx, mouseX, mouseY, x, y, moduleWidth, alpha, col);
            columnHeights[col] += module.getHeight() + padding;
        }

        double maxY = Arrays.stream(columnHeights).max().orElse(0);
        float visibleHeight = boxHeight - (22f + 8f + 8f);
        scrollHandler.setMax(Math.max(0, maxY - visibleHeight) + (maxY > visibleHeight ? 4 : 0));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (closing) return false;
        if (headerPanel.handleMouseClicked(mouseX, mouseY)) return true;
        if (sidebarPanel.handleMouseClicked(mouseX, mouseY)) return true;

        if (button == 0) {
            if (MathUtil.isHovered(mouseX, mouseY, boxX, boxY, boxWidth, 20)) {
                draggingWindow = true;
                dragOffsetX = (float) mouseX - boxX;
                dragOffsetY = (float) mouseY - boxY;
                return true;
            }
        }

        if (!animationClose.isDone()) return false;

        float scrollbarX = boxX + boxWidth - 8 - 2;
        float scrollbarY = boxY + 22f + 8f + 8f;
        float visibleHeight = boxHeight - (22f + 8f + 8f);
        if (button == 0 && MathUtil.isHovered(mouseX, mouseY, scrollbarX, scrollbarY, 2, visibleHeight)) {
            isDraggingScrollbar = true;
            return true;
        }

        if (!MathUtil.isHoveredByCords(mouseX, mouseY, scaledScissorX, scaledScissorY, scaledScissorEndX, scaledScissorEndY)) {
            return false;
        }

        String searchText = headerPanel.getSearchText().toLowerCase();
        this.modules.stream()
                .filter(m -> searchText.isEmpty() ? m.getCategory() == selectedCategory : m.getName().toLowerCase().contains(searchText))
                .forEach(menuModule -> menuModule.onMouseClicked(mouseX, mouseY, button));

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            draggingWindow = false;
            isDraggingScrollbar = false;
        }
        for (AbstractMenuElement module : modules) {
            module.onMouseReleased(mouseX, mouseY, button);
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (headerPanel != null && headerPanel.handleKeyPressed(keyCode, scanCode, modifiers)) {
            return true;
        }
        
        boolean result = false;
        for (AbstractMenuElement module : modules) {
            if (module.keyPressed(keyCode, scanCode, modifiers)) result = true;
        }
        if (result) return true;

        if (keyCode == 256 && !closing) {
            SoundManager.playSound(SoundManager.CLOSE_GUI);
            closing = true;
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (headerPanel != null && headerPanel.handleCharTyped(chr, modifiers)) {
            return true;
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        float visibleHeight = boxHeight - (22f + 8f + 8f);
        float baseStep = (float) Math.max(20f, Math.min(60f, (scrollHandler.getMax() / visibleHeight) * 10));
        scrollHandler.scroll(verticalAmount * baseStep / 8);
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (button == 0 && draggingWindow) {
            boxX = (float) mouseX - dragOffsetX;
            boxY = (float) mouseY - dragOffsetY;
            return true;
        }
        for (AbstractMenuElement module : modules) {
            module.onMouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (animationClose.getValue() <= 0.01f || !closing) {
            super.close();
        }
    }

    public void openGui() {
        closing = false;
        animationClose.setValue(0f);
        animationClose.animateTo(1f);
        MinecraftClient.getInstance().setScreen(this);
        SoundManager.playSound(SoundManager.OPEN_GUI);
    }
}
