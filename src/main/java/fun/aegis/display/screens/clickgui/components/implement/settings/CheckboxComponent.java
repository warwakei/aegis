package fun.aegis.display.screens.clickgui.components.implement.settings;

import fun.aegis.Aegis;
import net.minecraft.client.gui.DrawContext;

import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.display.screens.clickgui.components.implement.other.CheckComponent;
import fun.aegis.utils.display.scissor.ScissorAssist;

import java.awt.*;

import static fun.aegis.utils.display.font.Fonts.Type.*;
import static fun.aegis.utils.display.font.Fonts.Type.DEFAULT;

public class CheckboxComponent extends AbstractSettingComponent {
    private final CheckComponent checkComponent = new CheckComponent();
    private final BooleanSetting setting;

    public CheckboxComponent(BooleanSetting setting) {
        super(setting);
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        height = 18;

        float nameX = x + 8;
        float nameY = y + 7.25f;
        float toggleLeftX = x + width - 26f;
        float maxNameW = Math.max(0f, toggleLeftX - 6f - nameX);
        ScissorAssist scissor = Aegis.getInstance().getScissorManager();
        scissor.push(context.getMatrices().peek().getPositionMatrix(), nameX, y, maxNameW, height);
        Fonts.getSize(14, DEFAULT).drawStringWithScroll(context.getMatrices(), setting.getName(), nameX, nameY, maxNameW, 0xFFD4D6E1);
        scissor.pop();

        ((CheckComponent) checkComponent.position(x + width - 20, y + 3f))
                .setRunnable(() -> setting.setValue(!setting.isValue()))
                .setState(setting.isValue())
                .render(context, mouseX, mouseY, delta);
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        checkComponent.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
