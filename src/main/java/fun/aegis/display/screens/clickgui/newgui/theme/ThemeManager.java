package fun.aegis.display.screens.clickgui.newgui.theme;

import fun.aegis.features.impl.render.Hud;
import fun.aegis.features.impl.render.TargetESP;
import lombok.Getter;

@Getter
public class ThemeManager {
    private static ThemeManager instance;
    private Theme currentTheme = Theme.DARK;

    public static ThemeManager getInstance() {
        if (instance == null) {
            instance = new ThemeManager();
        }
        return instance;
    }

    public void switchTheme() {
        currentTheme = currentTheme == Theme.DARK ? Theme.LIGHT : Theme.DARK;
        applyThemeColors();
    }

    public void setTheme(Theme theme) {
        this.currentTheme = theme;
        applyThemeColors();
    }

    public boolean isDark() {
        return currentTheme == Theme.DARK;
    }

    private void applyThemeColors() {
        try {
            Hud hud = Hud.getInstance();
            if (hud != null) {
                hud.colorSetting.setColor(currentTheme.getColorInt());
            }

            TargetESP targetESP = TargetESP.getInstance();
            if (targetESP != null) {
                targetESP.colorSetting.setColor(currentTheme.getColorInt());
            }
        } catch (Exception e) {
        }
    }
}

