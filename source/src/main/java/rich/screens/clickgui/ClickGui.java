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
import rich.screens.clickgui.impl.background.render.AvatarRenderer;
import rich.screens.clickgui.profile.ProfileAppearanceScreen;
import rich.screens.clickgui.impl.configs.ConfigsRenderer;
import rich.screens.clickgui.impl.feedback.GuiToastManager;
import rich.screens.clickgui.impl.feedback.GuiTooltip;
import rich.screens.clickgui.impl.module.ModuleComponent;
import rich.screens.clickgui.impl.module.handler.ModuleBindHandler;
import rich.screens.clickgui.impl.settingsrender.BindComponent;
import rich.screens.clickgui.impl.settingsrender.TextComponent;
import rich.util.animations.Direction;
import rich.util.animations.GuiAnimation;
import rich.util.interfaces.AbstractSettingComponent;
import rich.util.math.FrameRateCounter;
import rich.util.render.Render2D;
import rich.util.render.layout.Bounds;
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
    private final GuiTooltip tooltip = new GuiTooltip();
    private final ModuleBindHandler bindNames = new ModuleBindHandler();
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
        tooltip.reset();
        GuiToastManager.clear();
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

    /** клики считаем в координатах рендера */
    private float clickGuiMouseScale() {
        int guiScale = mc.getWindow().calculateScaleFactor(mc.options.getGuiScale().getValue(), mc.forcesUnicodeFont());
        return (float) FIXED_GUI_SCALE / guiScale;
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

        // затемнение фона убрано оставил меню
        // int dimAlpha = (int) (200 * animValue);
        // if (dimAlpha > 0) {
        //     Render2D.rect(0, 0, 5000, 5000, new Color(2, 3, 6, dimAlpha).getRGB(), 0);
        // }

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
        background.renderHeader(bgX, bgY, selectedCategory, alphaMultiplier, FIXED_GUI_SCALE);
        background.renderCategoryNames(bgX, bgY, selectedCategory, alphaMultiplier);

        Bounds moduleList = ClickGuiLayout.moduleList(bgX, bgY);
        Bounds settingsPanel = ClickGuiLayout.settingsPanel(bgX, bgY);

        float normalAlpha = background.getNormalPanelAlpha();
        float searchAlpha = background.getSearchPanelAlpha();

        if (normalAlpha > 0.01f) {
            configsRenderer.render(context, bgX, bgY, mx, my, delta, FIXED_GUI_SCALE, alphaMultiplier * normalAlpha, selectedCategory);

            boolean isAutoBuySliding = autoBuyRenderer.isSliding();
            boolean shouldRenderModules = isModuleCategory(selectedCategory);
            boolean slidingToModuleCategory = isAutoBuySliding && isModuleCategory(selectedCategory);

            if (shouldRenderModules || slidingToModuleCategory) {
                moduleComponent.updateScroll(delta, scrollSpeed);
                moduleComponent.updateScrollFades(delta, scrollSpeed, moduleList.h(), settingsPanel.h());
                moduleComponent.renderModuleList(context, moduleList.x(), moduleList.y(), moduleList.w(), moduleList.h(), mx, my, FIXED_GUI_SCALE, alphaMultiplier * normalAlpha);
                moduleComponent.renderSettingsPanel(context, settingsPanel.x(), settingsPanel.y(), settingsPanel.w(), settingsPanel.h(), mx, my, delta, FIXED_GUI_SCALE, alphaMultiplier * normalAlpha);
            }

            autoBuyRenderer.render(context, bgX, bgY, mx, my, delta, FIXED_GUI_SCALE, alphaMultiplier * normalAlpha, selectedCategory);
        }

        if (searchAlpha > 0.01f) {
            background.renderSearchResults(context, bgX, bgY, mx, my, FIXED_GUI_SCALE, alphaMultiplier);
        }

        // Тосты и тултип — поверх панелей, под карточкой профиля
        Bounds settingsPanelBounds = ClickGuiLayout.settingsPanel(bgX, bgY);
        GuiToastManager.render(context, bgX + ClickGuiLayout.BG_WIDTH / 2f,
                bgY + ClickGuiLayout.BG_HEIGHT - 8f, alphaMultiplier);

        String tooltipText = null;
        float tooltipRowY = my;
        if (!closing && normalAlpha > 0.9f && isModuleCategory(selectedCategory)
                && !background.isSearchActive() && !dragHandler.isDragging()
                && moduleComponent.getBindingModule() == null
                && settingsPanelBounds.contains(mx, my)) {
            var target = moduleComponent.getTooltipTargetAt(mx, my);
            if (target != null) {
                tooltipText = target.getSetting().getDescription();
                tooltipRowY = target.y;
            }
        }
        tooltip.render(context, tooltipText, settingsPanelBounds.right(), tooltipRowY, mx, my, alphaMultiplier * normalAlpha);

        // Карточка профиля — оверрайд поверх всех элементов ClickGui со своей альфой
        background.renderAvatar(context, bgX, bgY, alphaMultiplier);

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

        float scale = clickGuiMouseScale();
        double mx = click.x() / scale, my = click.y() / scale;

        float[] bg = calculateBackground(scale);
        float bgX = bg[0], bgY = bg[1];

        if (click.button() == 0 && mx >= bgX + AvatarRenderer.X && mx < bgX + AvatarRenderer.X + AvatarRenderer.WIDTH
                && my >= bgY + AvatarRenderer.Y && my < bgY + AvatarRenderer.Y + AvatarRenderer.HEIGHT) {
            mc.setScreen(new ProfileAppearanceScreen(this));
            return true;
        }

        if (background.isSearchBoxHovered(mx, my, bgX, bgY) && click.button() == 0) {
            background.setSearchActive(true);
            return true;
        }

        if (background.isSearchActive()) {
            if (click.button() == 0) {
                ModuleStructure searchModule = background.getSearchModuleAtPosition(mx, my, bgX, bgY);
                if (searchModule != null) {
                    moduleComponent.switchModuleWithFeedback(searchModule);
                    return true;
                }

                if (ClickGuiLayout.contentPanel(bgX, bgY).contains(mx, my)) {
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

        Bounds moduleList = ClickGuiLayout.moduleList(bgX, bgY);

        if (click.button() == 2) {
            if (isAnyBindListening()) {
                for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
                    if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                        bindComponent.handleMiddleMouseBind();
                        showSettingBindToast(bindComponent);
                        return true;
                    }
                }
            }

            if (moduleComponent.getBindingModule() != null) {
                return true;
            }

            ModuleStructure module = moduleComponent.getModuleAtPosition(mx, my, moduleList.x(), moduleList.y(), moduleList.w(), moduleList.h());
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
            ModuleStructure starModule = moduleComponent.getModuleForStarClick(mx, my, moduleList.x(), moduleList.y(), moduleList.w(), moduleList.h());
            if (starModule != null && click.button() == 0) {
                moduleComponent.toggleFavorite(starModule);
                return true;
            }

            ModuleStructure module = moduleComponent.getModuleAtPosition(mx, my, moduleList.x(), moduleList.y(), moduleList.w(), moduleList.h());
            if (module != null) {
                if (click.button() == 0) moduleComponent.switchModuleWithFeedback(module);
                else if (click.button() == 1) moduleComponent.selectModule(module);
                return true;
            }

            Bounds settingsPanel = ClickGuiLayout.settingsPanel(bgX, bgY);
            if (settingsPanel.contains(mx, my)) {
                for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
                    if (!c.getSetting().isVisible()) continue;
                    boolean wasListening = c instanceof BindComponent bind && bind.isListening();
                    if (c.mouseClicked(mx, my, click.button())) {
                        if (wasListening) showSettingBindToast((BindComponent) c);
                        return true;
                    }
                }
            }
        }

        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (closing) return false;

        float scale = clickGuiMouseScale();
        double mx = click.x() / scale, my = click.y() / scale;

        if (selectedCategory == ModuleCategory.AUTOBUY) {
            autoBuyRenderer.mouseReleased(mx, my, click.button());
        }

//        if (selectedCategory == ModuleCategory.CONFIGS) {
//            configsRenderer.mouseReleased(mx, my, click.button());
//        }

        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c.getSetting().isVisible() && c.mouseReleased(mx, my, click.button())) {
                return true;
            }
        }

        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseDragged(Click click, double deltaX, double deltaY) {
        if (closing) return false;

        float scale = clickGuiMouseScale();
        double mx = click.x() / scale;
        double my = click.y() / scale;

        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (c.getSetting().isVisible() && c.mouseDragged(mx, my, click.button(), deltaX / scale, deltaY / scale)) {
                return true;
            }
        }

        return super.mouseDragged(click, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (closing) return false;

        if (isAnyBindListening()) {
            for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
                if (c instanceof BindComponent bindComponent && bindComponent.isListening()) {
                    bindComponent.handleScrollBind(vertical);
                    showSettingBindToast(bindComponent);
                    return true;
                }
            }
        }

        if (moduleComponent.getBindingModule() != null) {
            return true;
        }

        float scale = clickGuiMouseScale();
        double mx = mouseX / scale, my = mouseY / scale;

        float[] bg = calculateBackground(scale);
        float bgX = bg[0], bgY = bg[1];

        if (background.isSearchActive()) {
            Bounds contentPanel = ClickGuiLayout.contentPanel(bgX, bgY);
            if (contentPanel.contains(mx, my)) {
                background.handleSearchScroll(vertical, contentPanel.h());
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

        Bounds moduleList = ClickGuiLayout.moduleList(bgX, bgY);
        if (moduleList.contains(mx, my)) {
            moduleComponent.handleModuleScroll(vertical, moduleList.h());
            return true;
        }

        Bounds settingsPanel = ClickGuiLayout.settingsPanel(bgX, bgY);
        if (settingsPanel.contains(mx, my)) {
            moduleComponent.handleSettingScroll(vertical, settingsPanel.h());
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
            boolean cleared = input.key() == GLFW.GLFW_KEY_DELETE;
            binding.setKey(cleared ? GLFW.GLFW_KEY_UNKNOWN : input.key());
            moduleComponent.setBindingModule(null);
            if (cleared) {
                GuiToastManager.show("Бинд " + binding.getName() + " сброшен", GuiToastManager.Kind.INFO);
            } else {
                String keyName = bindNames.getBindDisplayName(input.key());
                GuiToastManager.show(binding.getName() + " → " + keyName, GuiToastManager.Kind.BIND);
            }
            return true;
        }

        for (AbstractSettingComponent c : moduleComponent.getSettingComponents()) {
            if (!c.getSetting().isVisible()) continue;
            boolean wasListening = c instanceof BindComponent bind && bind.isListening();
            if (c.keyPressed(input.key(), input.scancode(), input.modifiers())) {
                if (wasListening) showSettingBindToast((BindComponent) c);
                return true;
            }
        }

        return super.keyPressed(input);
    }

    private void showSettingBindToast(BindComponent component) {
        if (component.isListening()) return; // отмена через ESC — без тоста
        var setting = (rich.modules.module.setting.implement.BindSetting) component.getSetting();
        int key = setting.getKey();
        if (key == GLFW.GLFW_KEY_UNKNOWN || key == -1) {
            GuiToastManager.show("Бинд «" + setting.getName() + "» сброшен", GuiToastManager.Kind.INFO);
        } else {
            String name = setting.getType() == 0 ? mouseBindName(key) : bindNames.getBindDisplayName(key);
            GuiToastManager.show(setting.getName() + " → " + name, GuiToastManager.Kind.BIND);
        }
    }

    private String mouseBindName(int button) {
        return switch (button) {
            case GLFW.GLFW_MOUSE_BUTTON_LEFT -> "LMB";
            case GLFW.GLFW_MOUSE_BUTTON_RIGHT -> "RMB";
            case GLFW.GLFW_MOUSE_BUTTON_MIDDLE -> "MMB";
            default -> "M" + (button + 1);
        };
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
