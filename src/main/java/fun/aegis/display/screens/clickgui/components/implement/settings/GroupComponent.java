package fun.aegis.display.screens.clickgui.components.implement.settings;

import fun.aegis.display.screens.clickgui.components.implement.other.SettingComponent;
import net.minecraft.client.gui.DrawContext;

import fun.aegis.features.module.setting.implement.GroupSetting;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.display.screens.clickgui.components.implement.window.AbstractWindow;
import fun.aegis.display.screens.clickgui.components.implement.window.implement.settings.group.GroupWindow;
import fun.aegis.display.screens.clickgui.components.implement.other.CheckComponent;

import java.awt.*;

import static fun.aegis.utils.display.font.Fonts.Type.*;
import static fun.aegis.utils.display.font.Fonts.Type.DEFAULT;

public class GroupComponent extends AbstractSettingComponent {
    private final CheckComponent checkComponent = new CheckComponent();
    private final SettingComponent settingComponent = new SettingComponent();

    private final GroupSetting setting;

    public GroupComponent(GroupSetting setting) {
        super(setting);
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        height = 18;

        Fonts.getSize(14, DEFAULT).drawString(context.getMatrices(), setting.getName(), x + 8, y + 7.25f, 0xFFD4D6E1);

        ((CheckComponent) checkComponent.position(x + width - 20, y + 3f))
                .setRunnable(() -> setting.setValue(!setting.isValue()))
                .setState(setting.isValue())
                .render(context, mouseX, mouseY, delta);

        ((SettingComponent) settingComponent.position(x + width - 31, y + 2.5f))
                .setRunnable(() -> spawnWindow(mouseX, mouseY))
                .render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        checkComponent.mouseClicked(mouseX, mouseY, button);
        settingComponent.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    private void spawnWindow(int mouseX, int mouseY) {
        AbstractWindow existingWindow = null;

        for (AbstractWindow window : windowManager.getWindows()) {
            if (window instanceof GroupWindow && ((GroupWindow) window).getSetting() == setting) {
                existingWindow = window;
                break;
            }
        }

        if (existingWindow != null) {
            windowManager.delete(existingWindow);
        } else {
            AbstractWindow groupWindow = new GroupWindow(setting)
                    .position(mouseX + 10, mouseY)
                    .size(137, 23)
                    .draggable(false);

            windowManager.add(groupWindow);
        }
    }
}
