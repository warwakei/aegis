package rich.screens.clickgui;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import rich.IMinecraft;
import rich.Initialization;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.ModuleStructure;
import rich.screens.clickgui.impl.DragHandler;
import rich.screens.clickgui.impl.autobuy.autobuyui.AutoBuyRenderer;
import rich.screens.clickgui.impl.background.BackgroundComponent;
import rich.screens.clickgui.impl.configs.ConfigsRenderer;
import rich.screens.clickgui.impl.module.ModuleComponent;
import rich.screens.clickgui.impl.settingsrender.BindComponent;
import rich.screens.clickgui.impl.settingsrender.TextComponent;
import rich.util.animations.Direction;
import rich.util.animations.GuiAnimation;
import rich.util.interfaces.AbstractSettingComponent;
import rich.util.math.FrameRateCounter;
import rich.util.render.Render2D;
import rich.util.render.shader.Scissor;
import rich.util.render.gif.GifRender;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class ClickGui extends Screen implements IMinecraft {
    public static ClickGui INSTANCE = new ClickGui();
    private static final int FIXED_GUI_SCALE = 2;

    private final BackgroundComponent background = new BackgroundComponent();
    private final ModuleComponent moduleComponent = new ModuleComponent();
    private final AutoBuyRenderer autoBuyRenderer = new AutoBuyRenderer();
    private final ConfigsRenderer configsRenderer = new ConfigsRenderer();
    private final DragHandler dragHandler = new DragHandler();
    private ModuleCategory selectedCategory = ModuleCategory.COMBAT;

    private final GuiAnimation openAnimation = new GuiAnimation();
    private boolean closing = false;
    private boolean waitingForSlide = false;
    private boolean slideTriggered = false;

    private float hintAlphaAnimation = 0f;
    private long lastHintUpdateTime = System.currentTimeMillis();
    private static final float HINT_ANIM_SPEED = 6f;
    private static final float OFFSET_THRESHOLD = 5f;

    private int lastMouseX;
    private int lastMouseY;
    private float lastDelta;

    public ClickGui() {
        super(Text.of("MenuScreen"));
    }

    public boolean isClosing() {
        return closing;
    }

    @Override
    protected void init() {
        super.init();
        closing = false;
        waitingForSlide = false;
        slideTriggered = false;
        openAnimation.setMs(250).setValue(1.0).setDirection(Direction.FORWARDS).reset();
        hintAlphaAnimation = 0f;
        lastHintUpdateTime = System.currentTimeMillis();

        long handle = mc.getWindow().getHandle();
        double centerX = mc.getWindow().getWidth() / 2.0;
        double centerY = mc.getWindow().getHeight() / 2.0;
        GLFW.glfwSetCursorPos(handle, centerX, centerY);

        background.setSearchActive(false);
        autoBuyRenderer.resetForClose();
        updateModules();
    }

    private void updateModules() {
        List<ModuleStructure> modules = new ArrayList<>();
        try {
            var repo = Initialization.getInstance().getManager().getModuleRepository();
            if (repo != null) {
                for (ModuleStructure m : repo.modules()) {
                    if (m.getCategory() == selectedCategory) modules.add(m);
                }
            }
        } catch (Exception ignored) {}
        moduleComponent.updateModules(modules, selectedCategory);
    }

    public void openGui() {
        if (mc.currentScreen == null) {
            closing = false;
            waitingForSlide = false;
            slideTriggered = false;
            openAnimation.setMs(250).setValue(1.0).setDirection(Direction.FORWARDS).reset();
            mc.setScreen(this);
        }
    }

    @Override
    public void tick() {
        GifRender.tick();
        moduleComponent.tick();
        super.tick();
    }

    private float[] calculateBackground(float scale) {
        int vw = mc.getWindow().getWidth() / FIXED_GUI_SCALE;
        int vh = mc.getWindow().getHeight() / FIXED_GUI_SCALE;
        float bgX = (vw - BackgroundComponent.BG_WIDTH) / 2f + dragHandler.getOffsetX();
        float bgY = (vh - BackgroundComponent.BG_HEIGHT) / 2f + dragHandler.getOffsetY();
        return new float[]{bgX, bgY, vw, vh};
    }

    private boolean isAnyBindListening() {
        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                return true;
            }
        }
        return false;
    }

    private void updateHintAnimation() {
        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastHintUpdateTime) / 1000f, 0.1f);
        lastHintUpdateTime = currentTime;

        float offsetX = Math.abs(dragHandler.getOffsetX());
        float offsetY = Math.abs(dragHandler.getOffsetY());
        boolean shouldShow = (offsetX > OFFSET_THRESHOLD || offsetY > OFFSET_THRESHOLD);

        float target = shouldShow ? 1f : 0f;
        float diff = target - hintAlphaAnimation;

        if (Math.abs(diff) < 0.001f) {
            hintAlphaAnimation = target;
        } else {
            hintAlphaAnimation += diff * HINT_ANIM_SPEED * deltaTime;
            hintAlphaAnimation = Math.max(0f, Math.min(1f, hintAlphaAnimation));
        }
    }

    private boolean isModuleCategory(ModuleCategory category) {
        return category != ModuleCategory.AUTOBUY ;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        lastDelta = delta;

        FrameRateCounter.INSTANCE.recordFrame();

        if (waitingForSlide && selectedCategory == ModuleCategory.AUTOBUY) {
            if (!slideTriggered) {
                autoBuyRenderer.triggerSlideOut();
                slideTriggered = true;
            }

            if (autoBuyRenderer.isSlideOutComplete()) {
                waitingForSlide = false;
                slideTriggered = false;
                startActualClose();
            }
        }

        if (closing && !waitingForSlide && openAnimation.isFinished(Direction.BACKWARDS)) {
            closing = false;
            TextComponent.typing = false;
            moduleComponent.setBindingModule(null);
            dragHandler.stopDrag();
            autoBuyRenderer.resetForClose();
            mc.currentScreen = null;
        }
    }

    public void renderOverlay(DrawContext context, RenderTickCounter tickCounter) {
        if (mc.getWindow() == null) return;

        float delta = lastDelta;
        int mouseX = lastMouseX;
        int mouseY = lastMouseY;

        float scrollSpeed = Math.min(1f, 60f / Math.max(FrameRateCounter.INSTANCE.getFps(), 1));
        float animValue = openAnimation.getOutput().floatValue();

        int screenWidth = mc.getWindow().getScaledWidth();
        int screenHeight = mc.getWindow().getScaledHeight();

        context.createNewRootLayer();

        int dimAlpha = (int) (125 * animValue);
        if (dimAlpha > 0) {
            Render2D.rect(0, 0, 5000, 5000, new Color(0, 0, 0, dimAlpha).getRGB(), 0);
        }

        int guiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        float scale = (float) FIXED_GUI_SCALE / guiScale;

        float mx = mouseX / scale, my = mouseY / scale;

        if (!closing || waitingForSlide) {
            dragHandler.update(mx, my);
        }

        updateHintAnimation();

        context.getMatrices().pushMatrix();
        context.getMatrices().scale(scale, scale);

        float[] bg = calculateBackground(scale);
        float bgX = bg[0];
        float bgY = bg[1];
        int vw = (int) bg[2];
        int vh = (int) bg[3];

        float yOffset;
        if (closing && !waitingForSlide) {
            yOffset = (1f - animValue) * 30f;
        } else {
            yOffset = (1f - animValue) * -15f;
        }
        bgY += yOffset;

        float alphaMultiplier = animValue;

        context.getMatrices().pushMatrix();

        background.render(context, bgX, bgY, selectedCategory, delta, alphaMultiplier);
        background.renderCategoryPanel(bgX, bgY, alphaMultiplier);
        background.renderHeader(bgX, bgY, selectedCategory, alphaMultiplier);
        background.renderCategoryNames(bgX, bgY, selectedCategory, alphaMultiplier);

        float mlX = bgX + 92f, mlY = bgY + 38f, mlW = 120f, mlH = BackgroundComponent.BG_HEIGHT - 46f;
        float spX = bgX + 218f, spY = bgY + 38f, spW = 172f, spH = BackgroundComponent.BG_HEIGHT - 46f;

        float normalAlpha = background.getNormalPanelAlpha();
        float searchAlpha = background.getSearchPanelAlpha();

        if (normalAlpha > 0.01f) {
            configsRenderer.render(context, bgX, bgY, mx, my, delta, FIXED_GUI_SCALE, alphaMultiplier * normalAlpha, selectedCategory);

            boolean isAutoBuySliding = autoBuyRenderer.isSliding();
            boolean shouldRenderModules = isModuleCategory(selectedCategory);
            boolean slidingToModuleCategory = isAutoBuySliding && isModuleCategory(selectedCategory);

            if (shouldRenderModules || slidingToModuleCategory) {
                moduleComponent.updateScroll(delta, scrollSpeed);
                moduleComponent.updateScrollFades(delta, scrollSpeed, mlH, spH);
                moduleComponent.renderModuleList(context, mlX, mlY, mlW, mlH, mx, my, FIXED_GUI_SCALE, alphaMultiplier * normalAlpha);
                moduleComponent.renderSettingsPanel(context, spX, spY, spW, spH, mx, my, delta, FIXED_GUI_SCALE, alphaMultiplier * normalAlpha);
            }

            autoBuyRenderer.render(context, bgX, bgY, mx, my, delta, FIXED_GUI_SCALE, alphaMultiplier * normalAlpha, selectedCategory);
        }

        if (searchAlpha > 0.01f) {
            background.renderSearchResults(context, bgX, bgY, mx, my, FIXED_GUI_SCALE, alphaMultiplier);
        }

        Scissor.reset();

        context.getMatrices().popMatrix();

        float finalHintAlpha = hintAlphaAnimation * alphaMultiplier;
        if (finalHintAlpha > 0.01f) {
            int hintAlpha = (int) (255 * finalHintAlpha);
            float centerX = vw / 2f;
            float centerY = vh / 2f;
            float textY = centerY + BackgroundComponent.BG_HEIGHT / 2f + 10f;
//            Fonts.TEST.drawCentered("Press CTRL + ALT to reset position", centerX, textY + 65, 6, new Color(150, 150, 150, hintAlpha).getRGB());
        }

        context.getMatrices().popMatrix();
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (closing) return false;

        int guiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        float scale = (float) FIXED_GUI_SCALE / guiScale;
        double mx = click.x() / scale, my = click.y() / scale;

        float[] bg = calculateBackground(scale);
        float bgX = bg[0], bgY = bg[1];

        if (background.isSearchBoxHovered(mx, my, bgX, bgY) && click.button() == 0) {
            background.setSearchActive(true);
            return true;
        }

        if (background.isSearchActive()) {
            if (click.button() == 0) {
                ModuleStructure searchModule = background.getSearchModuleAtPosition(mx, my, bgX, bgY);
                if (searchModule != null) {
                    searchModule.switchState();
                    return true;
                }

                float panelX = bgX + 92f;
                float panelY = bgY + 38f;
                float panelW = BackgroundComponent.BG_WIDTH - 100f;
                float panelH = BackgroundComponent.BG_HEIGHT - 46f;

                if (mx >= panelX && mx <= panelX + panelW && my >= panelY && my <= panelY + panelH) {
                    return true;
                }

                if (!background.isSearchBoxHovered(mx, my, bgX, bgY)) {
                    background.setSearchActive(false);
                }
            } else if (click.button() == 1) {
                ModuleStructure searchModule = background.getSearchModuleAtPosition(mx, my, bgX, bgY);
                if (searchModule != null) {
                    background.setSearchActive(false);
                    selectedCategory = searchModule.getCategory();
                    moduleComponent.selectModuleFromSearch(searchModule);
                    updateModules();
                    return true;
                }
            }
            return true;
        }

        if (selectedCategory == ModuleCategory.AUTOBUY) {
            if (autoBuyRenderer.mouseClicked(mx, my, click.button(), bgX, bgY, selectedCategory)) {
                return true;
            }
        }

//        if (selectedCategory == ModuleCategory.CONFIGS) {
//            if (configsRenderer.mouseClicked(mx, my, click.button(), bgX, bgY, selectedCategory)) {
//                return true;
//            }
//        }

        float mlX = bgX + 92f, mlY = bgY + 38f, mlW = 120f, mlH = BackgroundComponent.BG_HEIGHT - 48f;

        if (click.button() == 2) {
            if (isAnyBindListening()) {
                for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
                    if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                        bindComponent.handleMiddleMouseBind();
                        return true;
                    }
                }
            }

            if (moduleComponent.getBindingModule() != null) {
                return true;
            }

            ModuleStructure module = moduleComponent.getModuleAtPosition(mx, my, mlX, mlY, mlW, mlH);
            if (module != null) {
                moduleComponent.setBindingModule(module);
                return true;
            }

            if (dragHandler.startDrag(mx, my, bgX, bgY, BackgroundComponent.BG_WIDTH, BackgroundComponent.BG_HEIGHT)) {
                return true;
            }
        }

        ModuleCategory cat = background.getCategoryAtPosition(mx, my, bgX, bgY);
        if (cat != null) {
            selectedCategory = cat;
            updateModules();
            return true;
        }

        if (isModuleCategory(selectedCategory)) {
            ModuleStructure starModule = moduleComponent.getModuleForStarClick(mx, my, mlX, mlY, mlW, mlH);
            if (starModule != null && click.button() == 0) {
                moduleComponent.toggleFavorite(starModule);
                return true;
            }

            ModuleStructure module = moduleComponent.getModuleAtPosition(mx, my, mlX, mlY, mlW, mlH);
            if (module != null) {
                if (click.button() == 0) module.switchState();
                else if (click.button() == 1) moduleComponent.selectModule(module);
                return true;
            }

            float spX = bgX + 218f, spY = bgY + 38f, spW = 172f, spH = BackgroundComponent.BG_HEIGHT - 48f;
            if (mx >= spX && mx <= spX + spW && my >= spY && my <= spY + spH) {
                for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
                    if (c.getSetting().isVisible() && c.mouseClicked(mx, my, click.button())) return true;
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (closing) return false;

        if (selectedCategory == ModuleCategory.AUTOBUY) {
            autoBuyRenderer.mouseReleased(click.x(), click.y(), click.button());
        }

//        if (selectedCategory == ModuleCategory.CONFIGS) {
//            configsRenderer.mouseReleased(click.x(), click.y(), click.button());
//        }

        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c.getSetting().isVisible() && c.mouseReleased(click.x(), click.y(), click.button())) {
                return true;
            }
        }

        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (closing) return false;

        if (isAnyBindListening()) {
            for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
                if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                    bindComponent.handleScrollBind(vertical);
                    return true;
                }
            }
        }

        if (moduleComponent.getBindingModule() != null) {
            return true;
        }

        int guiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        float scale = (float) FIXED_GUI_SCALE / guiScale;
        double mx = mouseX / scale, my = mouseY / scale;

        float[] bg = calculateBackground(scale);
        float bgX = bg[0], bgY = bg[1];

        if (background.isSearchActive()) {
            float panelX = bgX + 92f;
            float panelY = bgY + 38f;
            float panelW = BackgroundComponent.BG_WIDTH - 100f;
            float panelH = BackgroundComponent.BG_HEIGHT - 46f;

            if (mx >= panelX && mx <= panelX + panelW && my >= panelY && my <= panelY + panelH) {
                background.handleSearchScroll(vertical, panelH);
                return true;
            }
        }

        if (selectedCategory == ModuleCategory.AUTOBUY) {
            if (autoBuyRenderer.mouseScrolled(mx, my, vertical, bgX, bgY, selectedCategory)) {
                return true;
            }
        }

//        if (selectedCategory == ModuleCategory.CONFIGS) {
//            if (configsRenderer.mouseScrolled(mx, my, vertical, bgX, bgY, selectedCategory)) {
//                return true;
//            }
//        }

        float mlX = bgX + 92f, mlY = bgY + 38f, mlW = 120f, mlH = BackgroundComponent.BG_HEIGHT - 48f;
        if (mx >= mlX && mx <= mlX + mlW && my >= mlY && my <= mlY + mlH) {
            moduleComponent.handleModuleScroll(vertical, mlH);
            return true;
        }

        float spX = bgX + 218f, spY = bgY + 38f, spW = 172f, spH = BackgroundComponent.BG_HEIGHT - 48f;
        if (mx >= spX && mx <= spX + spW && my >= spY && my <= spY + spH) {
            moduleComponent.handleSettingScroll(vertical, spH);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (input.key() == GLFW.GLFW_KEY_ESCAPE) {
            if (autoBuyRenderer.isEditing()) {
                return true;
            }
            if (configsRenderer.isEditing()) {
                return true;
            }
            if (background.isSearchActive()) {
                background.setSearchActive(false);
                return true;
            }
            close();
            return true;
        }

        if (closing) return false;

        if (selectedCategory == ModuleCategory.AUTOBUY) {
            if (autoBuyRenderer.keyPressed(input.key(), input.scancode(), input.modifiers())) {
                return true;
            }
        }

//        if (selectedCategory == ModuleCategory.CONFIGS) {
//            if (configsRenderer.keyPressed(input.key(), input.scancode(), input.modifiers())) {
//                return true;
//            }
//        }

        if (background.isSearchActive()) {
            if (background.handleSearchKey(input.key())) {
                return true;
            }
        }

        if (dragHandler.isResetNeeded(input.key(), input.modifiers())) {
            dragHandler.reset();
            return true;
        }

        ModuleStructure binding = moduleComponent.getBindingModule();
        if (binding != null) {
            binding.setKey(input.key() == GLFW.GLFW_KEY_DELETE ? GLFW.GLFW_KEY_UNKNOWN : input.key());
            moduleComponent.setBindingModule(null);
            return true;
        }

        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c.getSetting().isVisible() && c.keyPressed(input.key(), input.scancode(), input.modifiers())) return true;
        }

        return super.keyPressed(input);
    }

    @Override
    public boolean charTyped(CharInput input) {
        if (closing) return false;

        if (selectedCategory == ModuleCategory.AUTOBUY) {
            if (autoBuyRenderer.charTyped((char) input.codepoint(), input.modifiers())) {
                return true;
            }
        }

//        if (selectedCategory == ModuleCategory.CONFIGS) {
//            if (configsRenderer.charTyped((char) input.codepoint(), input.modifiers())) {
//                return true;
//            }
//        }

        if (background.isSearchActive()) {
            if (background.handleSearchChar((char) input.codepoint())) {
                return true;
            }
        }

        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c.getSetting().isVisible() && c.charTyped((char) input.codepoint(), input.modifiers())) return true;
        }
        return super.charTyped(input);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private void startActualClose() {
        openAnimation.setDirection(Direction.BACKWARDS);
        openAnimation.reset();

        long handle = mc.getWindow().getHandle();
        double centerX = mc.getWindow().getWidth() / 2.0;
        double centerY = mc.getWindow().getHeight() / 2.0;

        GLFW.glfwSetInputMode(handle, GLFW.GLFW_CURSOR, GLFW.GLFW_CURSOR_DISABLED);
        GLFW.glfwSetCursorPos(handle, centerX, centerY);

        TextComponent.typing = false;
        moduleComponent.setBindingModule(null);
        background.setSearchActive(false);
        dragHandler.stopDrag();
    }

    @Override
    public void close() {
        if (!closing) {
            closing = true;

            if (selectedCategory == ModuleCategory.AUTOBUY) {
                waitingForSlide = true;
                slideTriggered = false;
            } else {
                waitingForSlide = false;
                startActualClose();
            }
        }
    }
}