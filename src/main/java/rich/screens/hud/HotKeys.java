package rich.screens.hud;

import net.minecraft.client.gui.DrawContext;
import org.lwjgl.glfw.GLFW;
import rich.Initialization;
import rich.client.draggables.AbstractHudElement;
import rich.modules.module.ModuleStructure;
import rich.util.animations.Direction;
import rich.util.render.Render2D;
import rich.util.render.shader.Scissor;
import rich.util.render.font.Fonts;
import rich.util.string.KeyHelper;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HotKeys extends AbstractHudElement {

    private List<ModuleStructure> keysList = new ArrayList<>();
    private long lastKeyChange = 0;
    private String currentRandomKey = "NONE";

    private float animatedWidth = 80;
    private float animatedHeight = 23;
    private long lastUpdateTime = System.currentTimeMillis();

    private static final float ANIMATION_SPEED = 8.0f;

    public HotKeys() {
        super("HotKeys", 300, 40, 80, 23, true);
        stopAnimation();
    }

    @Override
    public boolean visible() {
        return !scaleAnimation.isFinished(Direction.BACKWARDS);
    }

    @Override
    public void tick() {
        if (Initialization.getInstance() == null ||
                Initialization.getInstance().getManager() == null ||
                Initialization.getInstance().getManager().getModuleProvider() == null) {
            return;
        }

        keysList = Initialization.getInstance().getManager().getModuleProvider().getModuleStructures().stream()
                .filter(module -> module.isState()
                        && module.getKey() != GLFW.GLFW_KEY_UNKNOWN)
                .toList();

        boolean hasActiveKeys = !keysList.isEmpty();
        boolean inChat = isChat(mc.currentScreen);

        if (hasActiveKeys || inChat) {
            startAnimation();
        } else {
            stopAnimation();
        }

        if (!hasActiveKeys && inChat) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastKeyChange >= 1000) {
                List<String> availableKeys = List.of("A", "B", "C", "D", "E");
                currentRandomKey = availableKeys.get(new Random().nextInt(availableKeys.size()));
                lastKeyChange = currentTime;
            }
        }
    }

    private float lerp(float current, float target, float deltaTime) {
        float factor = (float) (1.0 - Math.pow(0.001, deltaTime * ANIMATION_SPEED));
        return current + (target - current) * factor;
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) return;

        float alphaFactor = alpha / 255.0f;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;

        deltaTime = Math.min(deltaTime, 0.1f);

        float x = getX();
        float y = getY();

        boolean hasActiveKeys = !keysList.isEmpty();
        boolean showExample = !hasActiveKeys && isChat(mc.currentScreen);

        int offset = 23;
        float targetWidth = 80;

        if (showExample) {
            offset += 11;
            String name = "Example Module";
            String bind = "[" + currentRandomKey + "]";
            float bindWidth = Fonts.BOLD.getWidth(bind, 6);
            float nameWidth = Fonts.BOLD.getWidth(name, 6);
            targetWidth = Math.max(nameWidth + bindWidth + 50, targetWidth);
        } else {
            for (ModuleStructure module : keysList) {
                offset += 11;

                String bind = "[" + KeyHelper.getKeyName(module.getKey()) + "]";
                float bindWidth = Fonts.BOLD.getWidth(bind, 6);
                float nameWidth = Fonts.BOLD.getWidth(module.getName(), 6);
                targetWidth = Math.max(nameWidth + bindWidth + 50, targetWidth);
            }
        }

        float targetHeight = offset + 2;

        animatedWidth = lerp(animatedWidth, targetWidth, deltaTime);
        animatedHeight = lerp(animatedHeight, targetHeight, deltaTime);

        if (Math.abs(animatedWidth - targetWidth) < 0.3f) {
            animatedWidth = targetWidth;
        }
        if (Math.abs(animatedHeight - targetHeight) < 0.3f) {
            animatedHeight = targetHeight;
        }

        setWidth((int) Math.ceil(animatedWidth));
        setHeight((int) Math.ceil(animatedHeight));

        float contentHeight = animatedHeight;
        int bgAlpha = (int) (255 * alphaFactor);

        if (contentHeight > 0) {
            Render2D.gradientRect(x, y, getWidth(), contentHeight,
                    new int[]{
                            new Color(52, 52, 52, bgAlpha).getRGB(),
                            new Color(32, 32, 32, bgAlpha).getRGB(),
                            new Color(52, 52, 52, bgAlpha).getRGB(),
                            new Color(32, 32, 32, bgAlpha).getRGB()
                    },
                    5);
            Render2D.outline(x, y, getWidth(), contentHeight, 0.35f, new Color(90, 90, 90, bgAlpha).getRGB(), 5);
        }

        Scissor.enable(x, y, getWidth(), contentHeight,2);

        long activeModules = keysList.size();
        String moduleCountText = String.valueOf(activeModules);
        float countTextWidth = Fonts.BOLD.getWidth(moduleCountText, 6);
        float activeTextWidth = Fonts.BOLD.getWidth("Active:", 6);

        Render2D.gradientRect(x + getWidth() - countTextWidth - activeTextWidth + 2, y + 5, 14, 12,
                new int[]{
                        new Color(52, 52, 52, bgAlpha).getRGB(),
                        new Color(52, 52, 52, bgAlpha).getRGB(),
                        new Color(52, 52, 52, bgAlpha).getRGB(),
                        new Color(52, 52, 52, bgAlpha).getRGB()
                },
                3);

        Fonts.HUD_ICONS.draw("g", x + getWidth() - countTextWidth - activeTextWidth + 4, y + 6, 10, new Color(165, 165, 165, bgAlpha).getRGB());

        Fonts.BOLD.draw("Binds", x + 8, y + 6.5f, 6, new Color(255, 255, 255, bgAlpha).getRGB());

        int moduleOffset = 23;

        if (showExample) {
            String name = "Example Module";
            String bind = "[" + currentRandomKey + "]";

            float bindWidth = Fonts.BOLD.getWidth(bind, 6);

            float bindBoxX = x + getWidth() - bindWidth - 11.5f;

            Render2D.gradientRect(bindBoxX, y + moduleOffset - 2f, bindWidth + 4, 9,
                    new int[]{
                            new Color(52, 52, 52, bgAlpha).getRGB(),
                            new Color(52, 52, 52, bgAlpha).getRGB(),
                            new Color(52, 52, 52, bgAlpha).getRGB(),
                            new Color(52, 52, 52, bgAlpha).getRGB()
                    },
                    3);

            Render2D.outline(bindBoxX, y + moduleOffset - 2f, bindWidth + 4, 9, 0.05f,
                    new Color(132, 132, 132, bgAlpha).getRGB(), 2);

            Render2D.rect(x + 8, y + moduleOffset - 1, 1f, 7,
                    new Color(155, 155, 155, (int) (128 * alphaFactor)).getRGB(), 1);
            Fonts.BOLD.draw(name, x + 13, y + moduleOffset - 1.5f, 6,
                    new Color(255, 255, 255, bgAlpha).getRGB());
            Fonts.BOLD.draw(bind, bindBoxX + 2, y + moduleOffset - 1, 6,
                    new Color(165, 165, 165, bgAlpha).getRGB());
        } else {
            for (ModuleStructure module : keysList) {
                String bind = "[" + KeyHelper.getKeyName(module.getKey()) + "]";

                float bindWidth = Fonts.BOLD.getWidth(bind, 6);

                int textColor = new Color(255, 255, 255, bgAlpha).getRGB();
                int accentColor = new Color(165, 165, 165, bgAlpha).getRGB();
                int separatorColor = new Color(155, 155, 155, (int) (128 * alphaFactor)).getRGB();

                float bindBoxX = x + getWidth() - bindWidth - 11.5f;

                Render2D.gradientRect(bindBoxX, y + moduleOffset - 2f, bindWidth + 4, 9,
                        new int[]{
                                new Color(52, 52, 52, bgAlpha).getRGB(),
                                new Color(52, 52, 52, bgAlpha).getRGB(),
                                new Color(52, 52, 52, bgAlpha).getRGB(),
                                new Color(52, 52, 52, bgAlpha).getRGB()
                        },
                        3);

                Render2D.outline(bindBoxX, y + moduleOffset - 2f, bindWidth + 4, 9, 0.05f,
                        new Color(132, 132, 132, bgAlpha).getRGB(), 2);

                Render2D.rect(x + 8, y + moduleOffset - 1, 1f, 7, separatorColor, 1);
                Fonts.BOLD.draw(module.getName(), x + 13, y + moduleOffset - 1.5f, 6, textColor);
                Fonts.BOLD.draw(bind, bindBoxX + 2, y + moduleOffset - 1, 6, accentColor);

                moduleOffset += 11;
            }
        }

        Scissor.disable();
    }
}