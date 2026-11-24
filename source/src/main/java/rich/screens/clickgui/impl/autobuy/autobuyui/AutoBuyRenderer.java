package rich.screens.clickgui.impl.autobuy.autobuyui;

import net.minecraft.client.gui.DrawContext;
import rich.IMinecraft;
import rich.modules.module.category.ModuleCategory;
import rich.screens.clickgui.ClickGuiLayout;
import rich.util.render.layout.Bounds;

public class AutoBuyRenderer implements IMinecraft {

    private final AutoBuyGuiComponent autoBuyComponent = new AutoBuyGuiComponent();

    private ModuleCategory lastCategory = null;
    private float categoryAlpha = 0f;
    private long lastUpdateTime = System.currentTimeMillis();

    private boolean wasActive = false;
    private boolean pendingSlideOut = false;
    private boolean slideOutComplete = false;

    private static final float FADE_SPEED = 14f;

    public void render(DrawContext context, float bgX, float bgY, float mouseX, float mouseY,
                       float delta, int guiScale, float alphaMultiplier, ModuleCategory currentCategory) {

        long currentTime = System.currentTimeMillis();
        float deltaTime = Math.min((currentTime - lastUpdateTime) / 1000f, 0.1f);
        lastUpdateTime = currentTime;

        boolean isActive = currentCategory == ModuleCategory.AUTOBUY;

        if (wasActive && !isActive && !pendingSlideOut) {
            pendingSlideOut = true;
            slideOutComplete = false;
            autoBuyComponent.startSlideOut();
        }

        if (isActive && !wasActive) {
            pendingSlideOut = false;
            slideOutComplete = false;
            autoBuyComponent.resetSlide();
            autoBuyComponent.resetPositions();
        }

        if (pendingSlideOut && autoBuyComponent.isSlidOut()) {
            slideOutComplete = true;
            pendingSlideOut = false;
        }

        wasActive = isActive;

        float targetAlpha;
        if (isActive) {
            targetAlpha = 1f;
        } else if (pendingSlideOut) {
            targetAlpha = 1f;
        } else {
            targetAlpha = 0f;
        }

        float diff = targetAlpha - categoryAlpha;

        if (Math.abs(diff) < 0.01f) {
            categoryAlpha = targetAlpha;
        } else {
            categoryAlpha += diff * FADE_SPEED * deltaTime;
        }

        categoryAlpha = Math.max(0f, Math.min(1f, categoryAlpha));

        if (categoryAlpha <= 0.01f && !pendingSlideOut) return;

        Bounds panel = ClickGuiLayout.contentPanel(bgX, bgY);

        autoBuyComponent.position(panel.x(), panel.y());
        autoBuyComponent.size(panel.w(), panel.h());
        autoBuyComponent.resetHover();
        autoBuyComponent.render(context, mouseX, mouseY, delta, guiScale, alphaMultiplier * categoryAlpha);
    }

    public boolean isSliding() {
        return pendingSlideOut && !slideOutComplete;
    }

    public void triggerSlideOut() {
        if (!autoBuyComponent.isSlidingOut()) {
            pendingSlideOut = true;
            slideOutComplete = false;
            autoBuyComponent.startSlideOut();
        }
    }

    public boolean isSlideOutComplete() {
        return slideOutComplete || autoBuyComponent.isSlidOut();
    }

    public void resetForClose() {
        pendingSlideOut = false;
        slideOutComplete = false;
        autoBuyComponent.resetSlide();
        categoryAlpha = 0f;
        wasActive = false;
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button, float bgX, float bgY, ModuleCategory currentCategory) {
        if (currentCategory != ModuleCategory.AUTOBUY) return false;
        if (categoryAlpha < 0.5f) return false;
        if (autoBuyComponent.isSlidingOut()) return false;

        Bounds panel = ClickGuiLayout.contentPanel(bgX, bgY);

        return autoBuyComponent.mouseClicked(mouseX, mouseY, button, panel.x(), panel.y(), panel.w(), panel.h());
    }

    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return false;
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double amount, float bgX, float bgY, ModuleCategory currentCategory) {
        if (currentCategory != ModuleCategory.AUTOBUY) return false;
        if (categoryAlpha < 0.5f) return false;
        if (autoBuyComponent.isSlidingOut()) return false;

        Bounds panel = ClickGuiLayout.contentPanel(bgX, bgY);

        return autoBuyComponent.mouseScrolled(mouseX, mouseY, amount, panel.x(), panel.y(), panel.w(), panel.h());
    }

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (categoryAlpha < 0.5f) return false;
        return autoBuyComponent.keyPressed(keyCode, scanCode, modifiers);
    }

    public boolean charTyped(char chr, int modifiers) {
        if (categoryAlpha < 0.5f) return false;
        return autoBuyComponent.charTyped(chr, modifiers);
    }

    public boolean isEditing() {
        return autoBuyComponent.isEditing();
    }

    public float getCategoryAlpha() {
        return categoryAlpha;
    }

    public boolean isOnAutoBuy() {
        return wasActive;
    }
}