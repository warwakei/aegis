package fun.aegis.display.screens.clickgui;

import fun.aegis.features.impl.misc.SelfDestruct;
import fun.aegis.features.impl.render.Hud;
import lombok.Getter;
import lombok.Setter;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.common.animation.Easy.Direction;
import fun.aegis.common.animation.Easy.EaseBackIn;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.client.sound.SoundManager;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.display.screens.clickgui.components.AbstractComponent;
import fun.aegis.display.screens.clickgui.components.implement.other.BackgroundComponent;
import fun.aegis.display.screens.clickgui.components.implement.other.CategoryContainerComponent;
import fun.aegis.display.screens.clickgui.components.implement.other.UserComponent;
import fun.aegis.display.screens.clickgui.components.implement.settings.TextComponent;
import fun.aegis.utils.math.calc.Calculate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static fun.aegis.common.animation.Easy.Direction.BACKWARDS;
import static fun.aegis.common.animation.Easy.Direction.FORWARDS;

@Setter
@Getter
public class MenuScreen extends Screen implements QuickImports {
    public static MenuScreen INSTANCE = new MenuScreen();
    private final List<AbstractComponent> components = new ArrayList<>();
    private final BackgroundComponent backgroundComponent = new BackgroundComponent();
    private final UserComponent userComponent = new UserComponent();
    private final CategoryContainerComponent categoryContainerComponent = new CategoryContainerComponent();
    public final EaseBackIn animation = new EaseBackIn(325, 1f, 1.5f);
    public ModuleCategory category = ModuleCategory.COMBAT;
    public int x, y, width, height;
    private boolean guiDragging = false;
    private double dragOffsetX, dragOffsetY;
    private float offsetXPercent = 0.5f;
    private float offsetYPercent = 0.5f;
    private int lastScreenWidth = 0;
    private int lastScreenHeight = 0;
    private double lastTransformedMouseX = 0;
    private double lastTransformedMouseY = 0;

    public void initialize() {
        animation.setDirection(FORWARDS);
        categoryContainerComponent.initializeCategoryComponents();
        components.addAll(Arrays.asList(backgroundComponent, userComponent, categoryContainerComponent));
    }

    public MenuScreen() {
        super(Text.of("MenuScreen"));
        initialize();
    }

    @Override
    public void tick() {
        close();
        components.forEach(AbstractComponent::tick);
        super.tick();
    }

    private double[] transformMouseCoords(double mouseX, double mouseY) {
        float scale = getScaleAnimation();
        if (scale <= 0.01f) scale = 1f;
        float centerX = x + width / 2f;
        float centerY = y + height / 2f;
        double transformedX = (mouseX - centerX) / scale + centerX;
        double transformedY = (mouseY - centerY) / scale + centerY;
        return new double[]{transformedX, transformedY};
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Получаем масштаб из HUD настроек
        float guiScale = Hud.getInstance().guiScale.getValue();
        
        // Базовые размеры с уменьшением на 7% (0.93)
        float baseScale = 0.93f;
        width = (int)(400 * baseScale * guiScale);
        height = (int)(250 * baseScale * guiScale);

        int currentWidth = window.getScaledWidth();
        int currentHeight = window.getScaledHeight();

        // Проверяем изменение размера окна
        if (lastScreenWidth != currentWidth || lastScreenHeight != currentHeight) {
            // Если это первый раз, центрируем
            if (lastScreenWidth == 0 && lastScreenHeight == 0) {
                x = currentWidth / 2 - width / 2;
                y = currentHeight / 2 - height / 2;
            } else {
                // При изменении размера пересчитываем позицию на основе процентов
                x = (int) (currentWidth * offsetXPercent - width / 2);
                y = (int) (currentHeight * offsetYPercent - height / 2);
            }
            
            // Убеждаемся что GUI не выходит за границы экрана
            x = Math.max(0, Math.min(x, currentWidth - width));
            y = Math.max(0, Math.min(y, currentHeight - height));
            
            lastScreenWidth = currentWidth;
            lastScreenHeight = currentHeight;
        }

        // Трансформируем координаты мыши один раз
        double[] transformed = transformMouseCoords(mouseX, mouseY);
        lastTransformedMouseX = transformed[0];
        lastTransformedMouseY = transformed[1];

        rectangle.render(ShapeProperties.create(context.getMatrices(), 0, 0, window.getScaledWidth(), window.getScaledHeight()).color(Calculate.applyOpacity(0xFF000000, (int)(120 * getScaleAnimation()))).build());
        backgroundComponent.position(x - 20, y).size(width + 40, height);
        categoryContainerComponent.position(x - 20, y);

        Calculate.scale(context.getMatrices(), x + (float) width / 2, y + (float) height / 2, getScaleAnimation(), () -> {
            components.forEach(component -> component.render(context, (int)lastTransformedMouseX, (int)lastTransformedMouseY, delta));
            windowManager.render(context, (int)lastTransformedMouseX, (int)lastTransformedMouseY, delta);
        });
        super.render(context, mouseX, mouseY, delta);
    }

    public void openGui() {
        if (SelfDestruct.unhooked) return;

        animation.setDirection(Direction.FORWARDS);
        animation.reset();
        mc.setScreen(this);
        SoundManager.playSound(SoundManager.OPEN_GUI);
    }

    public float getScaleAnimation() {
        return (float) animation.getOutput();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        double[] transformed = transformMouseCoords(mouseX, mouseY);

        if (button == 2 && isHoveringBackground(transformed[0], transformed[1])) {
            guiDragging = true;
            dragOffsetX = transformed[0] - x;
            dragOffsetY = transformed[1] - y;
            return true;
        }

        if (!guiDragging) {
            boolean windowHandled = windowManager.mouseClicked(transformed[0], transformed[1], button);
            if (!windowHandled) {
                for (AbstractComponent component : components) {
                    component.mouseClicked(transformed[0], transformed[1], button);
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        double[] transformed = transformMouseCoords(mouseX, mouseY);

        if (button == 2) {
            guiDragging = false;
            offsetXPercent = (x + width / 2f) / window.getScaledWidth();
            offsetYPercent = (y + height / 2f) / window.getScaledHeight();
        }

        for (AbstractComponent component : components) {
            component.mouseReleased(transformed[0], transformed[1], button);
        }
        windowManager.mouseReleased(transformed[0], transformed[1], button);
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        double[] transformed = transformMouseCoords(mouseX, mouseY);

        if (guiDragging && button == 2) {
            x = (int) (transformed[0] - dragOffsetX);
            y = (int) (transformed[1] - dragOffsetY);
            return true;
        }

        boolean windowHandled = windowManager.mouseDragged(transformed[0], transformed[1], button, deltaX, deltaY);
        if (!windowHandled) {
            for (AbstractComponent component : components) {
                component.mouseDragged(transformed[0], transformed[1], button, deltaX, deltaY);
            }
        }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        double[] transformed = transformMouseCoords(mouseX, mouseY);

        boolean windowHandled = windowManager.mouseScrolled(transformed[0], transformed[1], vertical);
        if (!windowHandled) {
            for (AbstractComponent component : components) {
                component.mouseScrolled(transformed[0], transformed[1], vertical);
            }
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 256 && shouldCloseOnEsc()) {
            SoundManager.playSound(SoundManager.CLOSE_GUI);
            animation.setDirection(BACKWARDS);
            return true;
        }
        if (!windowManager.keyPressed(keyCode, scanCode, modifiers)) {
            for (AbstractComponent component : components) {
                component.keyPressed(keyCode, scanCode, modifiers);
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        if (!windowManager.charTyped(chr, modifiers)) {
            for (AbstractComponent component : components) {
                component.charTyped(chr, modifiers);
            }
        }
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        if (animation.finished(BACKWARDS)) {
            TextComponent.typing = false;
            super.close();
        }
    }

    private boolean isHoveringBackground(double mouseX, double mouseY) {
        return mouseX >= x - 20 && mouseX <= x + width + 20 &&
                mouseY >= y && mouseY <= y + height;
    }
}
