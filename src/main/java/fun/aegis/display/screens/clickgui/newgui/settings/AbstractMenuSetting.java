package fun.aegis.display.screens.clickgui.newgui.settings;

import fun.aegis.display.screens.clickgui.newgui.theme.Theme;
import net.minecraft.client.gui.DrawContext;

public abstract class AbstractMenuSetting {
    protected float height;

    public abstract void render(DrawContext ctx, float mouseX, float mouseY, float x, float settingY, 
                                float moduleWidth, float alpha, float animEnable, int themeColor, 
                                int textColor, int descriptionColor, Theme theme);

    public abstract void onMouseClicked(double mouseX, double mouseY, int button);

    public abstract float getWidth();

    public abstract float getHeight();

    public abstract boolean isVisible();

    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return false;
    }

    public void onMouseReleased(double mouseX, double mouseY, int button) {
    }
}
