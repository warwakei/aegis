package fun.aegis.display.screens.clickgui.components.implement.window.implement.settings.color;

import net.minecraft.client.gui.DrawContext;

import fun.aegis.features.module.setting.implement.ColorSetting;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.display.screens.clickgui.components.implement.window.AbstractWindow;
import fun.aegis.display.screens.clickgui.components.implement.window.implement.settings.color.component.*;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.display.screens.clickgui.components.AbstractComponent;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ColorWindow extends AbstractWindow {
    private final List<AbstractComponent> components = new ArrayList<>();

    private final HueComponent hueComponent;
    private final SaturationComponent saturationComponent;
    private final AlphaComponent alphaComponent;
    private final ColorEditorComponent colorEditorComponent;
    private final ColorPresetComponent colorPresetComponent;

    public ColorWindow(ColorSetting setting) {

        components.addAll(
                Arrays.asList(
                        hueComponent = new HueComponent(setting),
                        saturationComponent = new SaturationComponent(setting),
                        alphaComponent = new AlphaComponent(setting),
                        colorEditorComponent = new ColorEditorComponent(setting),
                        colorPresetComponent = new ColorPresetComponent(setting)
                )
        );
    }

    @Override
    public void drawWindow(DrawContext context, int mouseX, int mouseY, float delta) {
        blur.render(ShapeProperties.create(context.getMatrices(), x, y + 10, width, height - 10)
                .round(6).thickness(1f).quality(32)
                .color(
                        new Color(18, 19, 20, 200).getRGB(),
                        new Color(0, 2, 5, 200).getRGB(),
                        new Color(0, 2, 5, 200).getRGB(),
                        new Color(18, 19, 20, 200).getRGB())
                .build());

        alphaComponent.position(x, y);
        hueComponent.position(x, y);
        saturationComponent.position(x, y);
        colorEditorComponent.position(x, y);

        height = ((ColorPresetComponent) colorPresetComponent.position(x, y))
                .getWindowHeight() - 40;

        components.forEach(component -> component.render(context, mouseX, mouseY, delta));
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        draggable(Calculate.isHovered(mouseX, mouseY, x, y, width, 17));
        components.forEach(component -> component.mouseClicked(mouseX, mouseY, button));
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        components.forEach(component -> component.mouseScrolled(mouseX, mouseY, amount));
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        components.forEach(component -> component.mouseReleased(mouseX, mouseY, button));
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
