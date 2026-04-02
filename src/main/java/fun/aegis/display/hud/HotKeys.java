package fun.aegis.display.hud;

import fun.aegis.common.animation.Direction;
import fun.aegis.features.impl.render.Hud;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import fun.aegis.utils.client.managers.api.draggable.AbstractDraggable;
import fun.aegis.features.module.Module;
import fun.aegis.utils.display.font.FontRenderer;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.glow.GlowEffect;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.client.chat.StringHelper;
import fun.aegis.Aegis;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.awt.*;

public class HotKeys extends AbstractDraggable {
    private List<Module> keysList = new ArrayList<>();
    private long lastKeyChange = 0;
    private java.lang.String currentRandomKey = "NONE";

    public HotKeys() {
        super("Кейбинды", 300, 40, 80, 23, true);
    }

    @Override
    public boolean visible() {
        return Hud.getInstance().interfaceSettings.isSelected("Кейбинды") && (!keysList.isEmpty() || PlayerInteractionHelper.isChat(mc.currentScreen));
    }

    @Override
    public void tick() {
        keysList = Aegis.getInstance().getModuleProvider().getModules().stream()
                .filter(module -> module.getAnimation().getOutput().floatValue() != 0 && module.getKey() != -1)
                .toList();
        if (keysList.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastKeyChange >= 1000) {
                List<java.lang.String> availableKeys = List.of("A", "B", "C", "D", "E");
                currentRandomKey = availableKeys.get(new Random().nextInt(availableKeys.size()));
                lastKeyChange = currentTime;
            }
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(13, Fonts.Type.DEFAULT);
        FontRenderer fontModule = Fonts.getSize(13, Fonts.Type.DEFAULT);
        FontRenderer icon = Fonts.getSize(23, Fonts.Type.ICONCATACLYSMREG);
        FontRenderer items = Fonts.getSize(12, Fonts.Type.ICONCATACLYSMREG);
        FontRenderer categoryIcon = Fonts.getSize(16, Fonts.Type.ICONSCATEGORY);

        // Убрана переменная moduleCountText и связанные расчеты
        
        blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .round(4).softness(10F).thickness(0).color(ColorAssist.getRect(0.4F)).build());

        // Перемещаем иконку вправо
        float iconX = getX() + getWidth() - 18;
        icon.drawString(matrix, "B", iconX, getY() + 5f, new Color(225, 225, 255, 255).getRGB());
        font.drawString(matrix, getName(), getX() + 4, getY() + 6.5f, ColorAssist.getText());

        float centerX = getX() + getWidth() / 2F;
        int offset = 23;
        int maxWidth = 80;

        if (keysList.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            float centerY = getY() + offset;
            java.lang.String name = "Пример модулей";
            java.lang.String bind = "[" + currentRandomKey + "]";
            java.lang.String iconChar = "A";
            int textColor = ColorAssist.getText();
            int textAlpha = 255;
            int colorWithAlpha = ColorAssist.rgba((textColor >> 16) & 255, (textColor >> 8) & 255, textColor & 255, textAlpha);
            int color = new Color(225, 225, 255, 255).getRGB();
            float bindWidth = fontModule.getStringWidth(bind);
            float bindBoxWidth = bindWidth + 6;
            Calculate.scale(matrix, centerX, centerY, 1, 1, () -> {
                categoryIcon.drawString(matrix, iconChar, getX() + 4.5f, centerY + 1.5f, color);
                fontModule.drawString(matrix, name, getX() + 19, centerY + 1, colorWithAlpha);
                fontModule.drawString(matrix, bind, getX() + getWidth() - bindWidth - 8, centerY + 1, color);
            });
            int width = (int) fontModule.getStringWidth(name + bind) + 25;
            maxWidth = Math.max(width, maxWidth);
            offset += 11;
        } else {
            for (Module module : keysList) {
                java.lang.String bind = "[" + StringHelper.getBindName(module.getKey()) + "]";
                float centerY = getY() + offset;
                float animation = module.getAnimation().getOutput().floatValue();
                java.lang.String iconChar;
                switch (module.getCategory()) {
                    case COMBAT -> iconChar = "b";
                    case MOVEMENT -> iconChar = "c";
                    case RENDER -> iconChar = "d";
                    case PLAYER -> iconChar = "e";
                    case MISC -> iconChar = "f";
                    default -> iconChar = module.getCategory().getReadableName().substring(0, 1);
                }
                int textColor = ColorAssist.getText();
                int textAlpha = 255;
                int colorWithAlpha = ColorAssist.rgba((textColor >> 16) & 255, (textColor >> 8) & 255, textColor & 255, textAlpha);
                int color = new Color(225, 225, 255, 255).getRGB();
                float bindWidth = fontModule.getStringWidth(bind);
                float bindBoxWidth = bindWidth + 6;
                Calculate.scale(matrix, centerX, centerY, 1, animation, () -> {
                    categoryIcon.drawString(matrix, iconChar, getX() + 4.5f, centerY + 1.5f, color);
                        fontModule.drawString(matrix, module.getName(), getX() + 19, centerY + 1, colorWithAlpha);
                    fontModule.drawString(matrix, bind, getX() + getWidth() - bindWidth - 8, centerY + 1, color);
                });
                float width = fontModule.getStringWidth(module.getName() + bind) + 25;
                maxWidth = (int) Math.max(width, maxWidth);
                offset += (int) (animation * 11);
            }
        }
        setWidth(maxWidth + 10);
        setHeight(offset);
    }
}
