package fun.aegis.display.screens.clickgui.components.implement.module;

import lombok.Getter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.ColorHelper;
import org.lwjgl.glfw.GLFW;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.setting.SettingComponentAdder;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.shape.implement.Rectangle;
import fun.aegis.display.screens.clickgui.MenuScreen;
import fun.aegis.display.screens.clickgui.components.AbstractComponent;
import fun.aegis.display.screens.clickgui.components.implement.other.StatusRender;
import fun.aegis.display.screens.clickgui.components.implement.settings.AbstractSettingComponent;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.client.chat.StringHelper;
import fun.aegis.common.animation.Animation;
import fun.aegis.common.animation.implement.Decelerate;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static fun.aegis.utils.display.font.Fonts.Type.*;
import static fun.aegis.common.animation.Direction.*;

@Getter
public class ModuleComponent extends AbstractComponent {
    private final List<AbstractSettingComponent> components = new ArrayList<>();
    private final StatusRender statusRender = new StatusRender();
    private final Module module;
    private boolean binding;
    private final Rectangle rectangle = new Rectangle();
    private final Animation colorAnimation = new Decelerate().setMs(400).setValue(9);
    private final Animation alphaAnimation = new Decelerate().setMs(400).setValue(105);
    
    // Кэш для оптимизации
    private String cachedDescription = "";
    private String[] cachedWords = new String[0];
    private int cachedLineCount = 0;
    private float cachedDescHeight = 0;
    private long lastCacheUpdate = 0;

    public ModuleComponent(Module module) {
        this.module = module;
        new SettingComponentAdder().addSettingComponent(module.settings(), components);
        colorAnimation.setDirection(module.isState() ? FORWARDS : BACKWARDS);
        alphaAnimation.setDirection(module.isState() ? FORWARDS : BACKWARDS);
        colorAnimation.reset();
        alphaAnimation.reset();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boolean noSettings = module.settings().isEmpty();
        java.lang.String description = ModuleDescriptions.getDescription(module);
        float maxWidth = width - 25;
        
        // Обновляем кэш если описание изменилось
        if (!description.equals(cachedDescription) || System.currentTimeMillis() - lastCacheUpdate > 5000) {
            cachedDescription = description;
            cachedWords = description.split(" ");
            
            // Вычисляем количество строк один раз
            float currentX = x + 10;
            int lineCount = 1;
            for (String word : cachedWords) {
                float wordWidth = Fonts.getSize(12, DEFAULT).getStringWidth(word + " ");
                if (currentX + wordWidth > x + maxWidth) {
                    lineCount++;
                    currentX = x + 10;
                }
                currentX += wordWidth;
            }
            cachedLineCount = lineCount;
            cachedDescHeight = lineCount == 1 ? lineCount * Fonts.getSize(12, DEFAULT).getStringHeight(" ") - 13 : lineCount * Fonts.getSize(12, DEFAULT).getStringHeight(" ") - 20;
            lastCacheUpdate = System.currentTimeMillis();
        }
        
        float nameY = noSettings ? 9.5F : 8;
        String point = "• ";
        
        colorAnimation.setDirection(module.isState() ? FORWARDS : BACKWARDS);
        alphaAnimation.setDirection(module.isState() ? FORWARDS : BACKWARDS);
        int brightnessOffset = colorAnimation.getOutput().intValue();
        int alphaOffset = Math.min(150 + alphaAnimation.getOutput().intValue(), 205);

        blur.render(ShapeProperties.create(context.getMatrices(), x, y, width, height = getComponentHeight())
                .round(5)
                .quality(128)
                .color(new Color(20, 20, 30, 180).getRGB())
                .build());

        rectangle.render(ShapeProperties.create(context.getMatrices(), x, y + cachedDescHeight + 25, width, 1)
                .color(new Color(40, 40, 60, 200).getRGB(), new Color(70, 70, 80, 200).getRGB(), new Color(70, 70, 80, 200).getRGB(), new Color(40, 40, 60, 200).getRGB())
                .build());

        if (!module.settings().isEmpty()) {
            Fonts.getSize(18, GUIICONS).drawString(context.getMatrices(), "A", x + 7, y + cachedDescHeight + 6F + 27f, new Color(150, 150, 150, 255).getRGB());
            Fonts.getSize(16, GUIICONS).drawString(context.getMatrices(), "B", x + 20, y + cachedDescHeight + 6F + 27.5f, new Color(150, 150, 150, 255).getRGB());
        } else {
            Fonts.getSize(18, GUIICONS).drawString(context.getMatrices(), "A", x + 7, y + cachedDescHeight + 6F + 27f, new Color(150, 150, 150, 255).getRGB());
        }

        statusRender.position(x + width - 16, y + cachedDescHeight + 5.5F + 25.5f)
                .setRunnable(module::switchState)
                .setState(module.isState())
                .render(context, mouseX, mouseY, delta);

        Fonts.getSize(15, DEFAULT).drawString(context.getMatrices(), point + module.getVisibleName(), x + 11, y + nameY, new Color(255, 255, 255, Math.min(alphaOffset + 50, 255)).getRGB());

        // Рендерим описание с улучшенным контрастом
        float currentX = x + 10;
        float currentY = y + 19;
        StringBuilder line = new StringBuilder();
        int currentLine = 1;

        for (String word : cachedWords) {
            float wordWidth = Fonts.getSize(12, DEFAULT).getStringWidth(word + " ");
            if (currentX + wordWidth > x + maxWidth) {
                if (currentLine == 1) {
                    Fonts.getSize(14, GUIICONS).drawString(context.getMatrices(), "C", x + 6.5f, currentY + 0.5f, new Color(150, 150, 150, 255).getRGB());
                    Fonts.getSize(12, DEFAULT).drawString(context.getMatrices(), line.toString(), x + 15, currentY, new Color(180, 180, 180, 240).getRGB());
                } else {
                    Fonts.getSize(12, DEFAULT).drawString(context.getMatrices(), line.toString(), x + 5, currentY, new Color(180, 180, 180, 240).getRGB());
                }
                line = new StringBuilder();
                currentY += Fonts.getSize(12, DEFAULT).getStringHeight(word) - 7;
                currentX = x + 10;
                currentLine++;
            }
            line.append(word).append(" ");
            currentX += wordWidth;
        }

        if (!line.isEmpty()) {
            if (currentLine == 1) {
                Fonts.getSize(14, GUIICONS).drawString(context.getMatrices(), "C", x + 6.5f, currentY + 0.5f, new Color(150, 150, 150, 255).getRGB());
                Fonts.getSize(12, DEFAULT).drawString(context.getMatrices(), line.toString(), x + 15, currentY, new Color(180, 180, 180, 240).getRGB());
            } else {
                Fonts.getSize(12, DEFAULT).drawString(context.getMatrices(), line.toString(), x + 7, currentY, new Color(180, 180, 180, 240).getRGB());
            }
        }

        drawBind(context, cachedDescHeight);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (Calculate.isHovered(mouseX, mouseY, x, y, width, getComponentHeight()) && button == 1 && !module.settings().isEmpty()) {
            if (MenuScreen.windowManager.getWindows().stream().noneMatch(w -> w instanceof ModuleSettingsWindow && ((ModuleSettingsWindow) w).module.equals(module))) {
                ModuleSettingsWindow settingsWindow = new ModuleSettingsWindow(module);
                settingsWindow.position(MenuScreen.INSTANCE.x + MenuScreen.INSTANCE.width + 24, MenuScreen.INSTANCE.y).size(160, settingsWindow.getComponentHeight());
                MenuScreen.windowManager.add(settingsWindow);
            }
            return true;
        }


        java.lang.String bindName = StringHelper.getBindName(module.getKey());
        java.lang.String description = ModuleDescriptions.getDescription(module);
        float maxWidth = width - 25;
        float currentX = x + 10;
        int lineCount = 1;
        java.lang.String[] words = description.split(" ");
        for (java.lang.String word : words) {
            float wordWidth = Fonts.getSize(12, DEFAULT).getStringWidth(word + " ");
            if (currentX + wordWidth > x + maxWidth) {
                lineCount++;
                currentX = x + 10;
            }
            currentX += wordWidth;
        }
        float descHeight = lineCount == 1 ? lineCount * Fonts.getSize(12, DEFAULT).getStringHeight(" ") - 13 : lineCount * Fonts.getSize(12, DEFAULT).getStringHeight(" ") - 20;
        float stringWidth = module.getKey() < 0 ? 10 : Fonts.getSize(12, DEFAULT).getStringWidth(bindName);
        float bindX = module.settings().isEmpty() ? x + width - 37.5f - stringWidth : x + width - 37.5f - stringWidth;
        float bindY = module.settings().isEmpty() ? y + descHeight + 5.5F + 27 : y + descHeight + 5.5F + 27;

        if (Calculate.isHovered(mouseX, mouseY, bindX, bindY, stringWidth + 6, 9) && button == 0) {
            binding = !binding;
        } else if (binding) {
            module.setKey(button);
            binding = false;
        }

        statusRender.mouseClicked(mouseX, mouseY, button);
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        return Calculate.isHovered(mouseX, mouseY, x, y, width, height);
    }

    @Override
    public void tick() {
        components.forEach(AbstractComponent::tick);
        super.tick();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        int key = keyCode == GLFW.GLFW_KEY_DELETE ? -1 : keyCode;
        if (binding) {
            module.setKey(key);
            binding = false;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return super.charTyped(chr, modifiers);
    }

    public int getComponentHeight() {
        java.lang.String description = ModuleDescriptions.getDescription(module);
        float maxWidth = width - 25;
        float currentX = x + 10;
        int lineCount = 1;
        java.lang.String[] words = description.split(" ");
        for (java.lang.String word : words) {
            float wordWidth = Fonts.getSize(12, DEFAULT).getStringWidth(word + " ");
            if (currentX + wordWidth > x + maxWidth) {
                lineCount++;
                currentX = x + 10;
            }
            currentX += wordWidth;
        }
        float descHeight = lineCount == 1 ? lineCount * Fonts.getSize(12, DEFAULT).getStringHeight(" ") - 13 : lineCount * Fonts.getSize(12, DEFAULT).getStringHeight(" ") - 20;
        return (int) (module.settings().isEmpty() ? 45 + descHeight : 45 + descHeight);
    }

    private void drawBind(DrawContext context, float descHeight) {
        java.lang.String bindName = StringHelper.getBindName(module.getKey());
        java.lang.String name = binding ? "..." : bindName;
        float stringWidth = module.getKey() < 0 && !binding ? 10 : Fonts.getSize(12, DEFAULT).getStringWidth(name);
        float bindX = module.settings().isEmpty() ? x + width - 37.5f - stringWidth : x + width - 37.5f - stringWidth;
        float back = module.settings().isEmpty() ? y + descHeight + 6F + 23.75f : y + descHeight + 6F + 23.75f;

        rectangle.render(ShapeProperties.create(context.getMatrices(), bindX + 0.25f, back, stringWidth + 6, 10)
                .round(3f)
                .outlineColor(new Color(170, 170, 180, 255).getRGB())
                .color(
                        new Color(80, 90, 100, 120).getRGB(),
                        new Color(90, 100, 110, 120).getRGB(),
                        new Color(100, 110, 120, 120).getRGB(),
                        new Color(110, 120, 130, 120).getRGB())
                .build());

        int bindingColor = ColorHelper.getArgb(255, 160, 160, 170);
        float textX = module.settings().isEmpty() ? x + width - 34.5f - stringWidth : x + width - 34.5f - stringWidth;
        float textY = module.settings().isEmpty() ? y + descHeight + 6F + 28f : y + descHeight + 6F + 28f;

        if (module.getKey() < 0 && !binding) {
            Fonts.getSize(22, GUIICONS).drawString(context.getMatrices(), "G", x + width - 34.5f - 10, y + descHeight + 6F + 26f, new Color(128, 128, 128, 255).getRGB());
        } else {
            Fonts.getSize(12, DEFAULT).drawString(context.getMatrices(), name, textX, textY, bindingColor);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ModuleComponent that = (ModuleComponent) o;
        return module.equals(that.module);
    }

    @Override
    public int hashCode() {
        return Objects.hash(module);
    }
}
