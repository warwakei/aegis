package rich.client.draggables;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import rich.Initialization;
import rich.modules.impl.render.Hud;
import rich.util.ColorUtil;
import rich.util.animations.SweepAnim;
import rich.util.config.impl.drag.DragConfig;
import rich.util.render.Render2D;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Drag {

    private static final float OUTLINE_OFFSET = 3.0f;
    private static final float OUTLINE_THICKNESS = 1.0f;
    private static final int OUTLINE_COLOR = ColorUtil.rgba(255, 255, 255, 255);

    private static final Set<String> EXCLUDED_ELEMENTS = Set.of("Notifications", "Watermark", "Info");

    private static HudElement draggingElement;
    private static int startX, startY;
    private static final Map<HudElement, SweepAnim> sweepAnimations = new HashMap<>();
    private static final Map<HudElement, Boolean> wasHovered = new HashMap<>();

    public static void onDraw(DrawContext context, int mouseX, int mouseY, float delta, boolean isChatScreen) {
        HudManager hudManager = getHudManager();
        if (hudManager == null) return;

        Hud hud = Hud.getInstance();
        if (hud == null || !hud.isState()) return;

        if (!isChatScreen) {
            if (draggingElement != null) {
                DragConfig.getInstance().save();
                draggingElement = null;
            }
            sweepAnimations.clear();
            wasHovered.clear();
        }

        if (isChatScreen && draggingElement != null) {
            draggingElement.setX(mouseX - startX);
            draggingElement.setY(mouseY - startY);
        }

        hudManager.render(context, delta, mouseX, mouseY);

        if (isChatScreen) {
            for (HudElement element : hudManager.getEnabledElements()) {
                if (!element.visible()) {
                    sweepAnimations.remove(element);
                    wasHovered.remove(element);
                    continue;
                }

                if (EXCLUDED_ELEMENTS.contains(element.getName())) {
                    continue;
                }

                boolean isHovered = isHovered(element, mouseX, mouseY);
                boolean previouslyHovered = wasHovered.getOrDefault(element, false);

                float rounding = element.getRoundingRadius();
                float offset = OUTLINE_OFFSET;
                float outlineX = element.getX() - offset;
                float outlineY = element.getY() - offset;
                float outlineWidth = element.getWidth() + offset * 2;
                float outlineHeight = element.getHeight() + offset * 2;
                float outlineRounding = Math.max(0, rounding + offset);

                SweepAnim anim = sweepAnimations.computeIfAbsent(element, e -> new SweepAnim(0.05f));

                if (isHovered && !previouslyHovered) {
                    anim.start();
                } else if (!isHovered && previouslyHovered) {
                    anim.reset();
                }

                wasHovered.put(element, isHovered);

                anim.update();
                float progress = anim.getProgress();

                if (isHovered || anim.isActive()) {
                    float baseAlpha = 0.3f;
                    Render2D.glowOutline(outlineX, outlineY, outlineWidth, outlineHeight,
                            OUTLINE_THICKNESS, OUTLINE_COLOR, outlineRounding, progress, baseAlpha);
                }

                if (!isHovered && anim.isCompleted()) {
                    sweepAnimations.remove(element);
                    wasHovered.remove(element);
                }
            }
        }
    }

    public static void onMouseClick(Click click) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!(mc.currentScreen instanceof ChatScreen)) return;

        if (click.button() == 0) {
            HudManager hudManager = getHudManager();
            if (hudManager == null) return;

            double mouseX = click.x();
            double mouseY = click.y();

            HudElement element = hudManager.getElementAt(mouseX, mouseY);
            if (element != null) {
                if (element instanceof AbstractHudElement abstractElement && abstractElement.isDraggable()) {
                    draggingElement = element;
                    startX = (int) mouseX - element.getX();
                    startY = (int) mouseY - element.getY();
                }
            }
        }
    }

    public static void onMouseRelease(Click click) {
        if (click.button() == 0 && draggingElement != null) {
            DragConfig.getInstance().save();
            draggingElement = null;
        }
    }

    public static void resetDragging() {
        if (draggingElement != null) {
            DragConfig.getInstance().save();
            draggingElement = null;
        }
        sweepAnimations.clear();
        wasHovered.clear();
    }

    public static boolean isDragging() {
        return draggingElement != null;
    }

    private static boolean isHovered(HudElement element, double mouseX, double mouseY) {
        int x = element.getX();
        int y = element.getY();
        int width = element.getWidth();
        int height = element.getHeight();
        return mouseX >= x && mouseX <= x + width &&
                mouseY >= y && mouseY <= y + height;
    }

    private static HudManager getHudManager() {
        if (Initialization.getInstance() == null) return null;
        if (Initialization.getInstance().getManager() == null) return null;
        return Initialization.getInstance().getManager().getHudManager();
    }

    public static void tick() {
        HudManager hudManager = getHudManager();
        if (hudManager != null) {
            hudManager.tick();
        }
    }
}