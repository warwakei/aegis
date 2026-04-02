package fun.aegis.display.screens.clickgui.components.implement.category;


import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.common.animation.Animation;
import fun.aegis.common.animation.Direction;
import fun.aegis.common.animation.implement.InOutBack;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.display.screens.clickgui.components.implement.module.ModuleComponent;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.display.scissor.ScissorAssist;
import fun.aegis.Aegis;
import fun.aegis.display.screens.clickgui.MenuScreen;
import fun.aegis.display.screens.clickgui.components.AbstractComponent;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CategoryComponent extends AbstractComponent {
    private final List<ModuleComponent> moduleComponents = new ArrayList<>();
    private static final Set<ModuleComponent> globalModuleComponents = new HashSet<>();
    private final ModuleCategory category;
    private final Animation alphaAnimation = new InOutBack().setMs(300).setValue(1);
    private final Animation scaleAnimation = new InOutBack().setMs(300).setValue(1);
    private boolean initializedAnimations = false;
    private float scroll = 0;
    private float smoothedScroll = 0;

    private void initializeModules() {
        List<Module> modules = Aegis.getInstance()
                .getModuleRepository()
                .modules();
        for (Module module : modules) {
            // Фильтруем только модули текущей категории
            if (module.getCategory().equals(category)) {
                ModuleComponent newComponent = new ModuleComponent(module);
                if (globalModuleComponents.add(newComponent)) {
                    moduleComponents.add(newComponent);
                }
            }
        }
    }

    public void postInitialize() {
        if (!initializedAnimations) {
            if (MenuScreen.INSTANCE.getCategory().equals(category)) {
                alphaAnimation.setDirection(Direction.FORWARDS);
                scaleAnimation.setDirection(Direction.FORWARDS);
                alphaAnimation.reset();
                scaleAnimation.reset();
                alphaAnimation.setMs(0);
                scaleAnimation.setMs(0);
            } else {
                alphaAnimation.setDirection(Direction.BACKWARDS);
                scaleAnimation.setDirection(Direction.BACKWARDS);
                alphaAnimation.reset();
                scaleAnimation.reset();
                alphaAnimation.setMs(0);
                scaleAnimation.setMs(0);
            }
            initializedAnimations = true;
        }
    }

    public CategoryComponent(ModuleCategory category) {
        this.category = category;
        initializeModules();
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        postInitialize();
        MenuScreen menuScreen = MenuScreen.INSTANCE;
        globalModuleComponents.clear();
        Matrix4f positionMatrix = context.getMatrices().peek().getPositionMatrix();
        ScissorAssist scissorManager = Aegis.getInstance().getScissorManager();
        drawCategoryTab(context, context.getMatrices(), mouseX, mouseY);
        
        // Получаем масштаб GUI
        float guiScale = fun.aegis.features.impl.render.Hud.getInstance().guiScale.getValue();
        float baseScale = 0.93f;
        float totalScale = baseScale * guiScale;
        
        // Фильтруем видимые модули
        List<ModuleComponent> visibleComponents = new ArrayList<>();
        for (ModuleComponent component : moduleComponents) {
            if (shouldRenderComponent(component)) {
                visibleComponents.add(component);
            }
        }
        
        int columnWidth = (int)(160 * totalScale);
        int padding = (int)(35 * totalScale);
        float offsetX = 35, offsetY = 14;
        float startX = menuScreen.x + 32;
        float startY = menuScreen.y + 35;
        
        scissorManager.push(positionMatrix, menuScreen.x + offsetX - 75, menuScreen.y + offsetY + 15, menuScreen.width - offsetX + 150, menuScreen.height - offsetY - 15);

        float renderScroll = Math.round(smoothedScroll);
        
        // Рассчитываем высоты для каждого модуля
        int[] columnHeights = new int[2];
        int maxScroll = 0;
        
        // Первый проход - рассчитываем высоты
        for (int i = 0; i < visibleComponents.size(); i++) {
            ModuleComponent component = visibleComponents.get(i);
            int col = i % 2;
            int componentHeight = (int)(component.getComponentHeight() * totalScale) + 11;
            columnHeights[col] += componentHeight;
        }
        
        // Второй проход - рендерим модули в правильном порядке
        int[] currentY = new int[2];
        for (int i = 0; i < visibleComponents.size(); i++) {
            ModuleComponent component = visibleComponents.get(i);
            int col = i % 2;
            int componentHeight = (int)(component.getComponentHeight() * totalScale) + 11;
            
            component.x = startX + (col * (columnWidth + padding));
            component.y = startY + currentY[col] + renderScroll;
            component.width = (int)((columnWidth + 42) * totalScale);
            
            if (component.y > menuScreen.y - componentHeight && menuScreen.y + menuScreen.height + 15 > component.y) {
                component.render(context, mouseX, mouseY, delta);
            }
            
            currentY[col] += componentHeight;
            maxScroll = Math.max(maxScroll, currentY[col]);
        }
        
        scissorManager.pop();
        int clamped = MathHelper.clamp(maxScroll - (menuScreen.height / 2 + 35), 0, maxScroll);
        scroll = MathHelper.clamp(scroll, -clamped, 0);
        smoothedScroll = Calculate.interpolateSmooth(2, smoothedScroll, scroll);

        if (clamped > 0) {
            float scrollbarWidth = 4;
            float scrollbarX = menuScreen.x + menuScreen.width - offsetX - scrollbarWidth + 50;
            float scrollbarY = menuScreen.y + offsetY + 22;
            float scrollbarHeight = menuScreen.height - offsetY * 2 - 14;

            rectangle.render(ShapeProperties.create(context.getMatrices(), scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight)
                    .round(2F)
                    .color(
                            new Color(35, 35, 35, 75).getRGB(),
                            new Color(45, 45, 45, 95).getRGB(),
                            new Color(45, 45, 45, 95).getRGB(),
                            new Color(35, 35, 35, 75).getRGB())
                    .build());

            float contentHeight = clamped;
            float viewHeight = menuScreen.height - offsetY * 2;
            float handleHeight = Math.max(20, viewHeight * (viewHeight / (contentHeight + viewHeight)));
            float scrollRatio = (float) (-renderScroll) / contentHeight;
            float handleY = scrollbarY + (scrollbarHeight - handleHeight) * scrollRatio;

            rectangle.render(ShapeProperties.create(context.getMatrices(), scrollbarX, handleY, scrollbarWidth, handleHeight)
                    .round(2F)
                    .color(
                            new Color(44, 44, 44, 75).getRGB(),
                            new Color(101, 101, 101, 95).getRGB(),
                            new Color(101, 101, 101, 95).getRGB(),
                            new Color(44, 44, 44, 75).getRGB())
                    .build());
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        MenuScreen menuScreen = MenuScreen.INSTANCE;
        float scale = 0.5f + (scaleAnimation.getOutput().floatValue() * 0.5f);
        float baseWidth = 20;
        float baseHeight = 20;
        float scaledWidth = baseWidth * scale;
        float scaledHeight = baseHeight * scale;
        float baseX = ModuleCategory.RENDER.equals(category) ? x + 4.65f : ModuleCategory.MOVEMENT.equals(category) ? x + 4.75f : x + 5.25f;
        float baseY = y;
        float centerX = baseX + baseWidth / 2;
        float centerY = baseY + baseHeight / 2;
        float scaledX = centerX - scaledWidth / 2;
        float scaledY = centerY - scaledHeight / 2;
        float hoverX = ModuleCategory.RENDER.equals(category) ? x + 4.65f : ModuleCategory.MOVEMENT.equals(category) ? x + 4.75f : x + 5.25f;
        float hoverY = y;
        if (Calculate.isHovered(mouseX, mouseY, hoverX, hoverY, baseWidth, baseHeight) && button == 0) {
            MenuScreen.INSTANCE.setCategory(category);
            alphaAnimation.setMs(300);
            scaleAnimation.setMs(300);
            alphaAnimation.setDirection(Direction.FORWARDS);
            scaleAnimation.setDirection(Direction.FORWARDS);
            return true;
        }
        float offsetX = 35, offsetY = 14;
        if (Calculate.isHovered(mouseX, mouseY, menuScreen.x + offsetX - 75, menuScreen.y + offsetY, menuScreen.width - offsetX + 150, menuScreen.height - offsetY + 15)) {
            for (int i = 0; i < moduleComponents.size(); i++) {
                ModuleComponent moduleComponent = moduleComponents.get(i);
                if (shouldRenderComponent(moduleComponent) && moduleComponent.isHover(mouseX, mouseY)) {
                    return moduleComponent.mouseClicked(mouseX, mouseY, button);
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isHover(double mouseX, double mouseY) {
        float scale = 0.5f + (scaleAnimation.getOutput().floatValue() * 0.5f);
        float baseWidth = 20;
        float baseHeight = 20;
        float scaledWidth = baseWidth * scale;
        float scaledHeight = baseHeight * scale;
        float baseX = ModuleCategory.RENDER.equals(category) ? x + 4.65f : ModuleCategory.MOVEMENT.equals(category) ? x + 4.75f : x + 5.25f;
        float baseY = y;
        float centerX = baseX + baseWidth / 2;
        float centerY = baseY + baseHeight / 2;
        float scaledX = centerX - scaledWidth / 2;
        float scaledY = centerY - scaledHeight / 2;

        boolean isHovered = Calculate.isHovered(mouseX, mouseY, scaledX, scaledY, scaledWidth, scaledHeight);

        if (isHovered) {
            return true;
        }

        moduleComponents.forEach(moduleComponent -> moduleComponent.isHover(mouseX, mouseY));
        for (ModuleComponent moduleComponent : moduleComponents) {
            if (moduleComponent.isHover(mouseX, mouseY)) {
                return true;
            }
        }
        return super.isHover(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        moduleComponents.forEach(moduleComponent -> moduleComponent.mouseReleased(mouseX, mouseY, button));
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        MenuScreen menuScreen = MenuScreen.INSTANCE;
        float offsetX = 35, offsetY = 13;
        if (Calculate.isHovered(mouseX, mouseY, menuScreen.x + offsetX, menuScreen.y + offsetY, menuScreen.width - offsetX + 7, menuScreen.height - offsetY + 15)) {
            scroll += amount * 20;
        }
        moduleComponents.forEach(moduleComponent -> {
            if (shouldRenderComponent(moduleComponent)) {
                moduleComponent.mouseScrolled(mouseX, mouseY, amount);
            }
        });
        return super.mouseScrolled(mouseX, mouseY, amount);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        moduleComponents.forEach(moduleComponent -> {
            if (shouldRenderComponent(moduleComponent)) {
                moduleComponent.keyPressed(keyCode, scanCode, modifiers);
            }
        });
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        moduleComponents.forEach(moduleComponent -> {
            if (shouldRenderComponent(moduleComponent)) {
                moduleComponent.charTyped(chr, modifiers);
            }
        });
        return super.charTyped(chr, modifiers);
    }

    private void drawCategoryTab(DrawContext context, MatrixStack matrix, int mouseX, int mouseY) {
        alphaAnimation.setDirection(MenuScreen.INSTANCE.getCategory().equals(category) ? Direction.FORWARDS : Direction.BACKWARDS);
        scaleAnimation.setDirection(MenuScreen.INSTANCE.getCategory().equals(category) ? Direction.FORWARDS : Direction.BACKWARDS);
        float anim = alphaAnimation.getOutput().floatValue();
        float scale = 0.5f + (scaleAnimation.getOutput().floatValue() * 0.5f);
        int alpha = MathHelper.clamp((int) (anim * 135), 0, 135);
        float baseWidth = 20;
        float baseHeight = 20;
        float scaledWidth = baseWidth * scale;
        float scaledHeight = baseHeight * scale;
        float baseX = ModuleCategory.RENDER.equals(category) ? x + 4.65f : ModuleCategory.MOVEMENT.equals(category) ? x + 4.75f : x + 5.25f;
        float baseY = y;
        float centerX = baseX + baseWidth / 2;
        float centerY = baseY + baseHeight / 2;
        float scaledX = centerX - scaledWidth / 2;
        float scaledY = centerY - scaledHeight / 2;
        float hoverX = ModuleCategory.RENDER.equals(category) ? x + 4.65f : ModuleCategory.MOVEMENT.equals(category) ? x + 4.75f : x + 5.25f;
        float hoverY = y;

        if (!MenuScreen.INSTANCE.getCategory().equals(category) && Calculate.isHovered(mouseX, mouseY, hoverX, hoverY, baseWidth, baseHeight)) {
            rectangle.render(ShapeProperties.create(matrix, hoverX, hoverY, baseWidth, baseHeight)
                    .round(4F)
                    .color(new Color(55, 55, 55, 100).getRGB(),
                            new Color(85, 85, 100, 100).getRGB(),
                            new Color(55, 55, 55, 100).getRGB(),
                            new Color(85, 85, 100, 100).getRGB()).build());
        }

        rectangle.render(ShapeProperties.create(matrix, scaledX, scaledY, scaledWidth, scaledHeight)
                .round(5F)
                .color(new Color(21, 21, 21, alpha).getRGB(),
                        new Color(61, 61, 61, alpha).getRGB(),
                        new Color(61, 61, 61, alpha).getRGB(),
                        new Color(21, 21, 21, alpha).getRGB()).build());


        if (ModuleCategory.COMBAT.equals(category)) {
            Fonts.getSize(21, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "A", x + 16f, y + 8.5f, ColorAssist.getText());
        }
        if (ModuleCategory.MOVEMENT.equals(category)) {
            Fonts.getSize(23, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "B", x + 15f, y + 7.5f, ColorAssist.getText());
        }
        if (ModuleCategory.RENDER.equals(category)) {
            Fonts.getSize(21, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "C", x + 15f, y + 7.5f, ColorAssist.getText());
        }
        if (ModuleCategory.PLAYER.equals(category)) {
            Fonts.getSize(23, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "D", x + 15f, y + 7.5f, ColorAssist.getText());
        }
        if (ModuleCategory.MISC.equals(category)) {
            Fonts.getSize(21, Fonts.Type.ICONSCATEGORY).drawCenteredString(context.getMatrices(), "E", x + 15.5f, y + 7.5f, ColorAssist.getText());
        }
    }

    private boolean shouldRenderComponent(ModuleComponent component) {
        MenuScreen menuScreen = MenuScreen.INSTANCE;
        ModuleCategory moduleCategory = component.getModule().getCategory();
        
        // Быстрая проверка категории
        if (!moduleCategory.equals(menuScreen.getCategory())) {
            return false;
        }
        
        // Показываем все модули категории
        return true;
    }
}
