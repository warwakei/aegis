package fun.aegis.display.screens.clickgui.components.implement.other;

import lombok.Setter;
import lombok.experimental.Accessors;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.display.screens.clickgui.MenuScreen;
import fun.aegis.display.screens.clickgui.components.AbstractComponent;
import fun.aegis.Aegis;
import fun.aegis.utils.display.color.ColorAssist;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Setter @Accessors(chain = true)
public class BackgroundComponent extends AbstractComponent {

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        MatrixStack matrix = context.getMatrices();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        String currentTime = LocalTime.now().format(formatter);
        String point = " • ";
        Aegis.getInstance().getScissorManager().push(matrix.peek().getPositionMatrix(), 0, 0, window.getScaledWidth(), window.getScaledHeight());

        blur.render(ShapeProperties.create(matrix, x, y, width, height).round(8).quality(64)
                .color(new Color(0, 0, 0, 200).getRGB())
                .build());

        rectangle.render(ShapeProperties.create(matrix, x, y, width, height).round(8)
                .softness(22)
                .thickness(0.1f)
                .outlineColor(new Color(18, 19, 20, 225).getRGB())
                .color(
                        new Color(18, 19, 20, 175).getRGB(),
                        new Color(0, 2, 5, 175).getRGB(),
                        new Color(0, 2, 5, 175).getRGB(),
                        new Color(18, 19, 20, 175).getRGB())
                .build());

        blur.render(ShapeProperties.create(matrix, x + 10.5f, y + 10f, 20, 20).round(5).quality(64)
                .color(new Color(0, 0, 0, 200).getRGB())
                .build());

        rectangle.render(ShapeProperties.create(matrix, x + 10.5f, y + 10f, 20, 20).round(5)
                .softness(22)
                .thickness(0.1f)
                .outlineColor(new Color(18, 19, 20, 225).getRGB())
                .color(
                        new Color(18, 19, 20, 175).getRGB(),
                        new Color(0, 2, 5, 175).getRGB(),
                        new Color(0, 2, 5, 175).getRGB(),
                        new Color(18, 19, 20, 175).getRGB())
                .build());

        Fonts.getSize(26, Fonts.Type.ICONS).drawString(matrix, "A ", x + 14f, y + 15F, new Color(225, 225, 255, 255).getRGB());

        String icon;
        switch (MenuScreen.INSTANCE.getCategory()) {
            case COMBAT -> {
                icon = "b";
                Fonts.getSize(17, Fonts.Type.ICONSCATEGORY).drawString(matrix, icon, x + 55f, y + 14.5f, new Color(225, 225, 255, 255).getRGB());
            }
            case MOVEMENT -> {
                icon = "c";
                Fonts.getSize(18, Fonts.Type.ICONSCATEGORY).drawString(matrix, icon, x + 54f, y + 14f, new Color(225, 225, 255, 255).getRGB());
            }
            case RENDER -> {
                icon = "d";
                Fonts.getSize(17, Fonts.Type.ICONSCATEGORY).drawString(matrix, icon, x + 54f, y + 14f, new Color(225, 225, 255, 255).getRGB());
            }
            case PLAYER -> {
                icon = "e";
                Fonts.getSize(17, Fonts.Type.ICONSCATEGORY).drawString(matrix, icon, x + 54f, y + 14f, new Color(225, 225, 255, 255).getRGB());
            }
            case MISC -> {
                icon = "f";
                Fonts.getSize(18, Fonts.Type.ICONSCATEGORY).drawString(matrix, icon, x + 54f, y + 14f, new Color(225, 225, 255, 255).getRGB());
            }
            default -> {
                icon = MenuScreen.INSTANCE.getCategory().getReadableName().substring(0, 1);
                Fonts.getSize(21, Fonts.Type.ICONSCATEGORY).drawString(matrix, icon, x + 50f, y + 13.5f, new Color(225, 225, 255, 255).getRGB());
            }
        }

        Fonts.getSize(15, Fonts.Type.DEFAULT).drawString(matrix, point + MenuScreen.INSTANCE.getCategory().getReadableName(), x + 63, y + 13.5f, new Color(245, 245, 255, 255).getRGB());

        Aegis.getInstance().getScissorManager().pop();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return super.charTyped(chr, modifiers);
    }
}
