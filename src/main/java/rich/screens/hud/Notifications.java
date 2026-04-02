package rich.screens.hud;

import net.minecraft.client.gui.DrawContext;
import rich.client.draggables.AbstractHudElement;
import rich.util.animations.Animation;
import rich.util.animations.Direction;
import rich.util.animations.OutBack;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Notifications extends AbstractHudElement {

    private static final int FORCED_GUI_SCALE = 2;

    private static Notifications instance;

    public static Notifications getInstance() {
        return instance;
    }

    private final List<Notification> list = new ArrayList<>();
    private static final float NOTIFICATION_HEIGHT = 16f;
    private static final float NOTIFICATION_GAP = 3f;

    public Notifications() {
        super("Notifications", 0, 0, 110, 16, false);
        instance = this;
    }

    private int getCurrentGuiScale() {
        int scale = mc.options.getGuiScale().getValue();
        if (scale == 0) {
            scale = mc.getWindow().calculateScaleFactor(0, mc.forcesUnicodeFont());
        }
        return scale;
    }

    private float getScaleFactor() {
        return (float) getCurrentGuiScale() / (float) FORCED_GUI_SCALE;
    }

    private float getVirtualWidth() {
        return mc.getWindow().getFramebufferWidth() / (float) FORCED_GUI_SCALE;
    }

    private float getVirtualHeight() {
        return mc.getWindow().getFramebufferHeight() / (float) FORCED_GUI_SCALE;
    }

    @Override
    public boolean visible() {
        return !list.isEmpty();
    }

    @Override
    public void tick() {
        list.forEach(notif -> {
            if (System.currentTimeMillis() > notif.removeTime ||
                    (notif.text.contains("Hi I'm a notification") && !isChat(mc.currentScreen))) {
                notif.anim.setDirection(Direction.BACKWARDS);
            }
        });
        list.removeIf(notif -> notif.anim.isFinished(Direction.BACKWARDS));

        if (isChat(mc.currentScreen)) {
            boolean hasHiNotification = list.stream()
                    .anyMatch(n -> n.text.contains("Hi I'm a notification"));
            if (!hasHiNotification) {
                addNotification("Hi I'm a notification", 99999999);
            }
        }

        updatePosition();
    }

    private void updatePosition() {
        if (mc.getWindow() == null) return;

        float virtualWidth = getVirtualWidth();
        float virtualHeight = getVirtualHeight();

        float crosshairX = virtualWidth / 2f;
        float crosshairY = virtualHeight / 2f;

        this.setX((int) (crosshairX - 60));
        this.setY((int) (crosshairY + 100));
    }

    public void addNotification(String text, long duration) {
        Animation anim = new OutBack().setMs(700).setValue(1);
        anim.setDirection(Direction.FORWARDS);

        int targetIndex = list.size();
        float targetY = targetIndex * (NOTIFICATION_HEIGHT + NOTIFICATION_GAP);

        Notification notification = new Notification(text, anim, System.currentTimeMillis(), System.currentTimeMillis() + duration);
        notification.currentY = targetY;
        notification.targetY = targetY;
        notification.velocityY = 0;

        list.add(notification);
        if (list.size() > 12) list.removeFirst();
        list.sort(Comparator.comparingDouble(notif -> -notif.removeTime));

        updateTargetPositions();
    }

    private void updateTargetPositions() {
        float offsetY = 0;
        for (int i = 0; i < list.size(); i++) {
            Notification notif = list.get(i);
            float anim = notif.anim.getOutput().floatValue();
            notif.targetY = offsetY;
            offsetY += (NOTIFICATION_HEIGHT + NOTIFICATION_GAP) * anim;
        }
    }

    private int clampAlpha(int value) {
        return Math.max(0, Math.min(255, value));
    }

    private int clampAlpha(float value) {
        return Math.max(0, Math.min(255, (int) value));
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        alpha = clampAlpha(alpha);
        if (alpha <= 0) return;

        float alphaFactor = alpha / 255.0f;
        updatePosition();
        updateTargetPositions();

        float springStiffness = 180f;
        float damping = 12f;
        float deltaTime = 0.016f;

        for (Notification notification : list) {
            float diff = notification.targetY - notification.currentY;
            float springForce = diff * springStiffness;
            float dampingForce = notification.velocityY * damping;
            float acceleration = springForce - dampingForce;

            notification.velocityY += acceleration * deltaTime;
            notification.currentY += notification.velocityY * deltaTime;

            if (Math.abs(diff) < 0.01f && Math.abs(notification.velocityY) < 0.01f) {
                notification.currentY = notification.targetY;
                notification.velocityY = 0;
            }
        }

        float offsetX = 5;
        float maxWidth = 0;
        float totalHeight = 0;

        for (Notification notification : list) {
            float anim = notification.anim.getOutput().floatValue();
            if (anim <= 0.01f) continue;

            anim = Math.max(0f, Math.min(1f, anim));

            float textWidth = Fonts.BOLD.getWidth(notification.text, 6);
            float width = textWidth + offsetX * 2 + 22;
            maxWidth = Math.max(maxWidth, width);

            float startY = this.getY() + notification.currentY;
            float startX = this.getX() + (120 - width) / 2;

            int bgAlpha = clampAlpha(225 * anim * alphaFactor);
            int icAlpha = clampAlpha(155 * anim * alphaFactor);

            if (bgAlpha > 0) {
                Render2D.gradientRect(startX, startY, width, NOTIFICATION_HEIGHT,
                        new int[]{
                                new Color(52, 52, 52, bgAlpha).getRGB(),
                                new Color(32, 32, 32, bgAlpha).getRGB(),
                                new Color(52, 52, 52, bgAlpha).getRGB(),
                                new Color(32, 32, 32, bgAlpha).getRGB()
                        }, 4);

                Render2D.outline(startX, startY, width, NOTIFICATION_HEIGHT, 0.35f,
                        new Color(90, 90, 90, bgAlpha).getRGB(), 4);

                Render2D.outline(startX + 2.75f, startY + 2, 12, 12, 0.35f,
                        new Color(90, 90, 90, bgAlpha).getRGB(), 4);

                Fonts.BOLD.draw(notification.text, startX + offsetX + 16, startY + 4.5f, 6,
                        new Color(255, 255, 255, bgAlpha).getRGB());

                Fonts.GUI_ICONS.draw("C", startX + 5f, startY + 4f, 8,
                        new Color(255, 255, 255, icAlpha).getRGB());
            }

            totalHeight = Math.max(totalHeight, notification.currentY + NOTIFICATION_HEIGHT);
        }

        if (maxWidth > 0) {
            setWidth((int) Math.ceil(maxWidth));
        }
        setHeight((int) Math.ceil(Math.max(NOTIFICATION_HEIGHT, totalHeight)));
    }

    public static class Notification {
        String text;
        Animation anim;
        long startTime;
        long removeTime;

        float currentY;
        float targetY;
        float velocityY;

        Notification(String text, Animation anim, long startTime, long removeTime) {
            this.text = text;
            this.anim = anim;
            this.startTime = startTime;
            this.removeTime = removeTime;
            this.currentY = 0;
            this.targetY = 0;
            this.velocityY = 0;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > removeTime;
        }
    }
}