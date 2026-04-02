package fun.aegis.display.screens.clickgui.components.implement.settings;

import fun.aegis.display.screens.clickgui.components.implement.other.ButtonComponent;
import fun.aegis.Aegis;
import fun.aegis.utils.display.scissor.ScissorAssist;
import net.minecraft.client.gui.DrawContext;

import fun.aegis.features.module.setting.implement.ButtonSetting;
import fun.aegis.utils.display.font.Fonts;

import static fun.aegis.utils.display.font.Fonts.Type.BOLD;

public class SButtonComponent extends AbstractSettingComponent {
    private final ButtonComponent buttonComponent = new ButtonComponent();
    private final ButtonSetting setting;

    public SButtonComponent(ButtonSetting setting) {
        super(setting);
        this.setting = setting;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {

        height = 22;

        ((ButtonComponent) buttonComponent.setText("Click on me")
                .setRunnable(setting.getRunnable())
                .position(x + width - 9 - buttonComponent.width, y + 6.5f))
                .render(context, mouseX, mouseY, delta);

        float nameX = x + 9;
        float nameY = y + 12.25f;
        float maxNameW = Math.max(0, (x + width - 12f - buttonComponent.width) - nameX);
        ScissorAssist scissor = Aegis.getInstance().getScissorManager();
        scissor.push(context.getMatrices().peek().getPositionMatrix(), nameX, y, maxNameW, height);
        Fonts.getSize(14, BOLD).drawStringWithScroll(context.getMatrices(), setting.getName(), nameX, nameY, maxNameW, 0xFFD4D6E1);
        scissor.pop();
    }


    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        buttonComponent.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
