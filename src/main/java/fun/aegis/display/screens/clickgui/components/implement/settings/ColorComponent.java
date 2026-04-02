package fun.aegis.display.screens.clickgui.components.implement.settings;

import fun.aegis.Aegis;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;

import fun.aegis.features.module.setting.implement.ColorSetting;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.scissor.ScissorAssist;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.display.screens.clickgui.components.AbstractComponent;
import fun.aegis.display.screens.clickgui.components.implement.window.implement.settings.color.component.AlphaComponent;
import fun.aegis.display.screens.clickgui.components.implement.window.implement.settings.color.component.ColorEditorComponent;
import fun.aegis.display.screens.clickgui.components.implement.window.implement.settings.color.component.ColorPresetComponent;
import fun.aegis.display.screens.clickgui.components.implement.window.implement.settings.color.component.HueComponent;
import fun.aegis.display.screens.clickgui.components.implement.window.implement.settings.color.component.SaturationComponent;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fun.aegis.utils.display.font.Fonts.Type.*;

public class ColorComponent extends AbstractSettingComponent {
    private final ColorSetting setting;

    private static final float ROW_HEIGHT = 22f;
    private static final float PICKER_TOP_OVERLAP = 16f;
    private static final float PICKER_BOTTOM_TRIM = 10f;
    private static final float PICKER_SIDE_PADDING = 2.2f;

    private boolean expanded;

    private final List<AbstractComponent> pickerComponents = new ArrayList<>();
    private final HueComponent hueComponent;
    private final SaturationComponent saturationComponent;
    private final AlphaComponent alphaComponent;
    private final ColorEditorComponent colorEditorComponent;
    private final ColorPresetComponent colorPresetComponent;

    private float pickerX;
    private float pickerY;
    private float pickerW;
    private float pickerH;

    public ColorComponent(ColorSetting setting) {
        super(setting);
        this.setting = setting;

        pickerComponents.addAll(
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
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        Matrix4f positionMatrix = matrix.peek().getPositionMatrix();

        height = ROW_HEIGHT;

        float nameX = x + 8;
        float nameY = y + 12.25f;
        float circleLeftX = x + width - 16f;
        float maxNameW = Math.max(0f, circleLeftX - 6f - nameX);
        ScissorAssist scissor = Aegis.getInstance().getScissorManager();
        scissor.push(matrix.peek().getPositionMatrix(), nameX, y, maxNameW, height);
        Fonts.getSize(14, DEFAULT).drawStringWithScroll(context.getMatrices(), setting.getName(), nameX, nameY, maxNameW, 0xFFD4D6E1);
        scissor.pop();
//        Fonts.getSize(12, DEFAULT).drawString(context.getMatrices(), wrapped, x + 9, y + 15, 0xFF878894);

        rectangle.render(ShapeProperties.create(matrix, x + width - 16, y + 10.5f, 7, 7)
                .round(3.5F).color(setting.getColor()).build());

        rectangle.render(ShapeProperties.create(matrix, x + width - 16, y + 10.5f, 7, 7)
                .round(3.5F).thickness(2).softness(1).outlineColor(ColorAssist.getText()).color(0x0FFFFFF).build());

        if (expanded) {
            pickerX = x + PICKER_SIDE_PADDING;
            pickerW = Math.max(0f, width - (PICKER_SIDE_PADDING * 2f));
            pickerY = y + ROW_HEIGHT - PICKER_TOP_OVERLAP;

            alphaComponent.position(pickerX, pickerY).size(pickerW, 0);
            hueComponent.position(pickerX, pickerY).size(pickerW, 0);
            saturationComponent.position(pickerX, pickerY).size(pickerW, 0);
            colorEditorComponent.position(pickerX, pickerY).size(pickerW, 0);

            pickerH = Math.max(0f, ((ColorPresetComponent) colorPresetComponent.position(pickerX, pickerY).size(pickerW, 0)).getWindowHeight() - 40f - PICKER_BOTTOM_TRIM);

            ScissorAssist pickerScissor = Aegis.getInstance().getScissorManager();
            pickerScissor.push(positionMatrix, pickerX, pickerY, pickerW, pickerH);
            pickerComponents.forEach(component -> component.render(context, mouseX, mouseY, delta));
            pickerScissor.pop();

            height = ROW_HEIGHT + pickerH - PICKER_TOP_OVERLAP;
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean overPreview = Calculate.isHovered(mouseX, mouseY, x + width - 13, y + 10, 7, 7);
        if (button == 0 && overPreview) {
            expanded = !expanded;
            return true;
        }

        if (expanded) {
            boolean overPicker = Calculate.isHovered(mouseX, mouseY, pickerX, pickerY, pickerW, pickerH);
            if (!overPicker) {
                expanded = false;
                return false;
            }
            pickerComponents.forEach(component -> component.mouseClicked(mouseX, mouseY, button));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (expanded) {
            pickerComponents.forEach(component -> component.mouseReleased(mouseX, mouseY, button));
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        if (expanded) {
            boolean overPicker = Calculate.isHovered(mouseX, mouseY, pickerX, pickerY, pickerW, pickerH);
            if (overPicker) {
                pickerComponents.forEach(component -> component.mouseScrolled(mouseX, mouseY, amount));
                return true;
            }
        }
        return super.mouseScrolled(mouseX, mouseY, amount);
    }
}
