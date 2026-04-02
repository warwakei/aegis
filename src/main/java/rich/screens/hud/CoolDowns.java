package rich.screens.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import rich.client.draggables.AbstractHudElement;
import rich.util.ColorUtil;
import rich.util.animations.Direction;
import rich.util.render.Render2D;
import rich.util.render.shader.Scissor;
import rich.util.render.font.Fonts;
import rich.util.render.item.ItemRender;

import java.awt.*;
import java.util.*;
import java.util.List;

public class CoolDowns extends AbstractHudElement {

    private static final int FORCED_GUI_SCALE = 2;

    private static class CoolDownInfo {
        Item item;
        long startTime;
        float startProgress;
        long estimatedTotalMs;
        int displaySeconds = -1;
        long nextTickTime = 0;
        boolean estimateReady = false;

        CoolDownInfo(Item item, float progress) {
            this.item = item;
            this.startTime = System.currentTimeMillis();
            this.startProgress = progress;
            this.estimatedTotalMs = 0;
            this.nextTickTime = 0;
            this.estimateReady = false;
        }

        void updateEstimate(float currentProgress) {
            if (estimateReady) return;

            long now = System.currentTimeMillis();
            long elapsed = now - startTime;

            if (elapsed < 200) return;

            if (startProgress > currentProgress && startProgress > 0.01f) {
                float progressConsumed = startProgress - currentProgress;
                if (progressConsumed > 0.01f) {
                    estimatedTotalMs = (long) (elapsed / progressConsumed);

                    long remainingMs = (long) (currentProgress * estimatedTotalMs);
                    displaySeconds = (int) Math.ceil(remainingMs / 1000.0);
                    nextTickTime = now + 1000;
                    estimateReady = true;
                }
            }
        }

        int getDisplaySeconds(float currentProgress) {
            if (currentProgress <= 0) {
                displaySeconds = 0;
                return 0;
            }

            if (!estimateReady) {
                return -1;
            }

            long now = System.currentTimeMillis();

            if (now >= nextTickTime && nextTickTime > 0) {
                displaySeconds = Math.max(0, displaySeconds - 1);
                nextTickTime = now + 1000;

                int calculatedSeconds;
                if (estimatedTotalMs > 0) {
                    long remainingMs = (long) (currentProgress * estimatedTotalMs);
                    calculatedSeconds = (int) Math.ceil(remainingMs / 1000.0);
                } else {
                    calculatedSeconds = displaySeconds;
                }

                if (Math.abs(displaySeconds - calculatedSeconds) > 2) {
                    displaySeconds = calculatedSeconds;
                }
            }

            return Math.max(0, displaySeconds);
        }
    }

    private final Map<Item, CoolDownInfo> cooldownMap = new LinkedHashMap<>();
    private final Map<Item, Float> cooldownAnimations = new LinkedHashMap<>();
    private final Set<Item> activeCooldowns = new HashSet<>();

    private float animatedWidth = 80;
    private float animatedHeight = 23;
    private long lastRenderTime = System.currentTimeMillis();

    private long lastItemChange = 0;
    private int currentItemIndex = 0;

    private static final float ANIMATION_SPEED = 8.0f;
    private static final float ITEM_SCALE = 0.5f;
    private static final String TIMER_TEMPLATE = "00:00";
    private static final Item[] EXAMPLE_ITEMS = {
            Items.ENDER_EYE, Items.ENDER_PEARL, Items.SUGAR, Items.MACE, Items.ENCHANTED_GOLDEN_APPLE,
            Items.TRIDENT, Items.CROSSBOW, Items.DRIED_KELP, Items.NETHERITE_SCRAP
    };

    public CoolDowns() {
        super("CoolDowns", 10, 40, 80, 23, true);
        stopAnimation();
    }

    @Override
    public boolean visible() {
        return !scaleAnimation.isFinished(Direction.BACKWARDS);
    }

    @Override
    public void tick() {
        if (mc.player == null) {
            cooldownMap.clear();
            activeCooldowns.clear();
            cooldownAnimations.clear();
            stopAnimation();
            return;
        }

        activeCooldowns.clear();
        Set<Item> checkedItems = new HashSet<>();

        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!stack.isEmpty() && !checkedItems.contains(stack.getItem())) {
                checkedItems.add(stack.getItem());
                checkAndUpdateCooldown(stack.getItem());
            }
        }

        ItemStack mainHand = mc.player.getMainHandStack();
        if (!mainHand.isEmpty() && !checkedItems.contains(mainHand.getItem())) {
            checkAndUpdateCooldown(mainHand.getItem());
        }
        ItemStack offHand = mc.player.getOffHandStack();
        if (!offHand.isEmpty() && !checkedItems.contains(offHand.getItem())) {
            checkAndUpdateCooldown(offHand.getItem());
        }

        boolean shouldShow = !cooldownAnimations.isEmpty() || isChat(mc.currentScreen);
        if (shouldShow) {
            startAnimation();
        } else {
            stopAnimation();
        }

        if (cooldownAnimations.isEmpty() && isChat(mc.currentScreen)) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastItemChange >= 1000) {
                currentItemIndex = (currentItemIndex + 1) % EXAMPLE_ITEMS.length;
                lastItemChange = currentTime;
            }
        }
    }

    private void checkAndUpdateCooldown(Item item) {
        if (mc.player == null) return;

        var cooldownManager = mc.player.getItemCooldownManager();
        ItemStack stack = item.getDefaultStack();

        if (cooldownManager.isCoolingDown(stack)) {
            float progress = cooldownManager.getCooldownProgress(stack, 0.0f);
            activeCooldowns.add(item);

            CoolDownInfo info = cooldownMap.get(item);
            if (info == null) {
                info = new CoolDownInfo(item, progress);
                cooldownMap.put(item, info);
            } else {
                info.updateEstimate(progress);
            }

            if (!cooldownAnimations.containsKey(item)) {
                cooldownAnimations.put(item, 0f);
            }
        }
    }

    private String formatDuration(int seconds) {
        if (seconds < 0) return "...";
        if (seconds == 0) return "0:00";
        int minutes = seconds / 60;
        int secs = seconds % 60;
        return String.format("%d:%02d", minutes, secs);
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) return;

        float alphaFactor = alpha / 255.0f;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastRenderTime) / 1000.0f;
        lastRenderTime = currentTime;
        deltaTime = Math.min(deltaTime, 0.1f);

        List<Item> toRemove = new ArrayList<>();
        for (Map.Entry<Item, Float> entry : cooldownAnimations.entrySet()) {
            Item item = entry.getKey();
            float currentAnim = entry.getValue();
            float targetAnim = activeCooldowns.contains(item) ? 1f : 0f;
            float newAnim = currentAnim + (targetAnim - currentAnim) * Math.min(1f, deltaTime * ANIMATION_SPEED);

            if (Math.abs(newAnim - targetAnim) < 0.01f) {
                newAnim = targetAnim;
            }

            if (newAnim <= 0.01f && targetAnim == 0f) {
                toRemove.add(item);
            } else {
                cooldownAnimations.put(item, newAnim);
            }
        }
        for (Item item : toRemove) {
            cooldownAnimations.remove(item);
            cooldownMap.remove(item);
        }

        float x = getX();
        float y = getY();

        int offset = 23;
        float targetWidth = 80;

        boolean hasAnimatingCooldowns = !cooldownAnimations.isEmpty();
        int blurTint = ColorUtil.rgba(0, 0, 0, 0);
        Render2D.blur(x, y, 1, 1, 0f, 0, blurTint);

        float fixedTimerWidth = Fonts.BOLD.getWidth(TIMER_TEMPLATE, 6);

        if (!hasAnimatingCooldowns) {
            offset += 11;
            String name = "Example CoolDown";
            float nameWidth = Fonts.BOLD.getWidth(name, 6);
            targetWidth = Math.max(nameWidth + fixedTimerWidth + 55, targetWidth);
        } else {
            for (Map.Entry<Item, Float> entry : cooldownAnimations.entrySet()) {
                Item item = entry.getKey();
                float animation = entry.getValue();
                if (animation <= 0) continue;

                CoolDownInfo info = cooldownMap.get(item);
                if (info == null) continue;

                offset += (int) (animation * 11);

                String name = item.getDefaultStack().getName().getString();
                float nameWidth = Fonts.BOLD.getWidth(name, 6);
                targetWidth = Math.max(nameWidth + fixedTimerWidth + 55, targetWidth);
            }
        }

        float targetHeight = offset + 2;

        animatedWidth = animatedWidth + (targetWidth - animatedWidth) * Math.min(1f, deltaTime * ANIMATION_SPEED);
        animatedHeight = animatedHeight + (targetHeight - animatedHeight) * Math.min(1f, deltaTime * ANIMATION_SPEED);

        if (Math.abs(animatedWidth - targetWidth) < 0.3f) animatedWidth = targetWidth;
        if (Math.abs(animatedHeight - targetHeight) < 0.3f) animatedHeight = targetHeight;

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
                    }, 5);
            Render2D.outline(x, y, getWidth(), contentHeight, 0.35f, new Color(90, 90, 90, bgAlpha).getRGB(), 5);
        }

        Scissor.enable(x, y, getWidth(), contentHeight, FORCED_GUI_SCALE);

        Render2D.gradientRect(x + getWidth() - 22.5f, y + 5, 14, 12,
                new int[]{
                        new Color(52, 52, 52, bgAlpha).getRGB(),
                        new Color(52, 52, 52, bgAlpha).getRGB(),
                        new Color(52, 52, 52, bgAlpha).getRGB(),
                        new Color(52, 52, 52, bgAlpha).getRGB()
                }, 3);

        Fonts.ICONS.draw("D", x + getWidth() - 20f, y + 6.5f, 9, new Color(165, 165, 165, bgAlpha).getRGB());
        Fonts.BOLD.draw("CoolDowns", x + 8, y + 6.5f, 6, new Color(255, 255, 255, bgAlpha).getRGB());

        int moduleOffset = 23;
        float timerBoxWidth = fixedTimerWidth + 4;
        float fixedTimerBoxX = x + getWidth() - timerBoxWidth - 9.5f;

        if (!hasAnimatingCooldowns) {
            Item item = EXAMPLE_ITEMS[currentItemIndex];
            String name = "Example CoolDown";
            String duration = "0:00";

            Render2D.gradientRect(fixedTimerBoxX + 1, y + moduleOffset - 1f, timerBoxWidth, 9,
                    new int[]{
                            new Color(52, 52, 52, bgAlpha).getRGB(),
                            new Color(52, 52, 52, bgAlpha).getRGB(),
                            new Color(52, 52, 52, bgAlpha).getRGB(),
                            new Color(52, 52, 52, bgAlpha).getRGB()
                    }, 3);

            Render2D.blur(x, y, 1, 1, 0f, 0, blurTint);

            Render2D.outline(fixedTimerBoxX + 1, y + moduleOffset - 1f, timerBoxWidth, 9, 0.05f,
                    new Color(132, 132, 132, bgAlpha).getRGB(), 2);

            float itemX = x + 8;
            float itemY = y + moduleOffset - 1f;

            if (ItemRender.needsContextRender(item.getDefaultStack())) {
                ItemRender.drawItemWithContext(context, item.getDefaultStack(), itemX, itemY, ITEM_SCALE, alphaFactor);
            } else {
                ItemRender.drawItem(item.getDefaultStack(), itemX, itemY, ITEM_SCALE, alphaFactor);
            }

            float nameX = x + 20;
            Fonts.BOLD.draw(name, nameX, y + moduleOffset - 1f, 6, new Color(255, 255, 255, bgAlpha).getRGB());

            float durationWidth = Fonts.BOLD.getWidth(duration, 6);
            float durationX = fixedTimerBoxX + (timerBoxWidth - durationWidth) / 2;
            Fonts.BOLD.draw(duration, durationX + 1, y + moduleOffset, 6, new Color(165, 165, 165, bgAlpha).getRGB());
        } else {
            for (Map.Entry<Item, Float> entry : cooldownAnimations.entrySet()) {
                Item item = entry.getKey();
                float animation = entry.getValue();
                if (animation <= 0) continue;

                CoolDownInfo info = cooldownMap.get(item);
                if (info == null) continue;

                var cooldownManager = mc.player.getItemCooldownManager();
                float currentProgress = cooldownManager.getCooldownProgress(item.getDefaultStack(), 0.0f);

                String name = item.getDefaultStack().getName().getString();
                int remainingSeconds = info.getDisplaySeconds(currentProgress);
                String duration = formatDuration(remainingSeconds);

                int textAlpha = (int) (255 * animation * alphaFactor);

                Render2D.gradientRect(fixedTimerBoxX + 1, y + moduleOffset - 1f, timerBoxWidth, 9,
                        new int[]{
                                new Color(52, 52, 52, textAlpha).getRGB(),
                                new Color(52, 52, 52, textAlpha).getRGB(),
                                new Color(52, 52, 52, textAlpha).getRGB(),
                                new Color(52, 52, 52, textAlpha).getRGB()
                        }, 3);
                Render2D.blur(x, y, 1, 1, 0f, 0, blurTint);

                Render2D.outline(fixedTimerBoxX + 1, y + moduleOffset - 1f, timerBoxWidth, 9, 0.05f,
                        new Color(132, 132, 132, textAlpha).getRGB(), 2);

                float itemX = x + 8;
                float itemY = y + moduleOffset - 1f;

                if (ItemRender.needsContextRender(item.getDefaultStack())) {
                    ItemRender.drawItemWithContext(context, item.getDefaultStack(), itemX, itemY, ITEM_SCALE, animation * alphaFactor);
                } else {
                    ItemRender.drawItem(item.getDefaultStack(), itemX, itemY, ITEM_SCALE, animation * alphaFactor);
                }

                float nameX = x + 20;
                Fonts.BOLD.draw(name, nameX, y + moduleOffset - 0.5f, 6, new Color(255, 255, 255, textAlpha).getRGB());

                float durationWidth = Fonts.BOLD.getWidth(duration, 6);
                float durationX = fixedTimerBoxX + (timerBoxWidth - durationWidth) / 2;
                Fonts.BOLD.draw(duration, durationX + 1, y + moduleOffset, 6, new Color(165, 165, 165, textAlpha).getRGB());

                moduleOffset += (int) (animation * 11);
            }
        }

        Scissor.disable();
    }
}