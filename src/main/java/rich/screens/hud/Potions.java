package rich.screens.hud;

import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;
import rich.client.draggables.AbstractHudElement;
import rich.util.animations.Direction;
import rich.util.render.Render2D;
import rich.util.render.shader.Scissor;
import rich.util.render.font.Fonts;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Potions extends AbstractHudElement {

    private List<StatusEffectInstance> effectsList = new ArrayList<>();
    private Map<String, Float> effectAnimations = new LinkedHashMap<>();
    private Map<String, StatusEffectInstance> cachedEffects = new LinkedHashMap<>();
    private Set<String> activeEffectIds = new HashSet<>();

    private float animatedWidth = 80;
    private float animatedHeight = 23;
    private long lastUpdateTime = System.currentTimeMillis();

    private long lastEffectChange = 0;
    private String currentRandomEffect = "speed";

    private static final List<String> RANDOM_EFFECTS = List.of(
            "speed", "slowness", "haste", "mining_fatigue", "strength",
            "jump_boost", "regeneration", "resistance", "fire_resistance",
            "water_breathing", "invisibility", "night_vision", "hunger",
            "weakness", "poison", "wither", "health_boost", "absorption"
    );

    private static final float ANIMATION_SPEED = 8.0f;
    private static final float ICON_SIZE = 9f;
    private static final int BLINK_THRESHOLD_TICKS = 100;

    public Potions() {
        super("Potions", 300, 100, 80, 23, true);
        stopAnimation();
    }

    @Override
    public boolean visible() {
        return !scaleAnimation.isFinished(Direction.BACKWARDS);
    }

    @Override
    public void tick() {
        if (mc.player == null) {
            effectsList = new ArrayList<>();
            activeEffectIds.clear();
            stopAnimation();
            return;
        }

        Collection<StatusEffectInstance> effects = mc.player.getStatusEffects();
        effectsList = new ArrayList<>(effects.stream()
                .filter(StatusEffectInstance::shouldShowIcon)
                .toList());

        activeEffectIds.clear();
        for (StatusEffectInstance effect : effectsList) {
            String id = getEffectId(effect);
            activeEffectIds.add(id);
            cachedEffects.put(id, effect);
            if (!effectAnimations.containsKey(id)) {
                effectAnimations.put(id, 0f);
            }
        }

        boolean hasActiveEffects = !activeEffectIds.isEmpty() || !effectAnimations.isEmpty();
        boolean inChat = isChat(mc.currentScreen);

        if (hasActiveEffects || inChat) {
            startAnimation();
        } else {
            stopAnimation();
        }

        if (effectsList.isEmpty() && inChat) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastEffectChange >= 1000) {
                currentRandomEffect = RANDOM_EFFECTS.get(new Random().nextInt(RANDOM_EFFECTS.size()));
                lastEffectChange = currentTime;
            }
        }
    }

    private String getEffectId(StatusEffectInstance effect) {
        return effect.getEffectType().getKey()
                .map(key -> key.getValue().toString())
                .orElse("unknown_" + effect.hashCode());
    }

    private float lerp(float current, float target, float deltaTime) {
        float factor = (float) (1.0 - Math.pow(0.001, deltaTime * ANIMATION_SPEED));
        return current + (target - current) * factor;
    }

    private String formatDuration(int ticks) {
        if (ticks == -1) {
            return "∞∞:∞∞";
        }
        int totalSeconds = ticks / 20;
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    private String getEffectName(StatusEffectInstance effect) {
        return effect.getEffectType().value().getName().getString();
    }

    private String getLevelText(int amplifier) {
        if (amplifier <= 0) {
            return "";
        }
        return "LVL " + (amplifier + 1);
    }

    private float getFullNameWidth(StatusEffectInstance effect) {
        String name = getEffectName(effect);
        int amplifier = effect.getAmplifier();
        float nameWidth = Fonts.BOLD.getWidth(name, 6);
        if (amplifier > 0) {
            String levelText = getLevelText(amplifier);
            float levelWidth = Fonts.REGULAR.getWidth(levelText, 6);
            return nameWidth + 3 + levelWidth;
        }
        return nameWidth;
    }

    private Identifier getEffectTexture(RegistryEntry<StatusEffect> effect) {
        return effect.getKey()
                .map(RegistryKey::getValue)
                .map(id -> id.withPrefixedPath("mob_effect/"))
                .orElse(Identifier.ofVanilla("mob_effect/speed"));
    }

    private Identifier getRandomEffectTexture() {
        return Identifier.ofVanilla("mob_effect/" + currentRandomEffect);
    }

    private int getBlinkAlpha(int duration, int baseAlpha) {
        if (duration == -1 || duration > BLINK_THRESHOLD_TICKS) {
            return baseAlpha;
        }

        long currentTime = System.currentTimeMillis();
        double blinkSpeed = 0.008;
        double blinkWave = Math.sin(currentTime * blinkSpeed);
        float blinkFactor = (float) ((blinkWave + 1.0) / 2.0);

        int minAlpha = Math.max(50, baseAlpha - 150);
        return (int) (minAlpha + (baseAlpha - minAlpha) * (1.0f - blinkFactor));
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) return;

        float alphaFactor = alpha / 255.0f;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;
        deltaTime = Math.min(deltaTime, 0.1f);

        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Float> entry : effectAnimations.entrySet()) {
            String id = entry.getKey();
            float currentAnim = entry.getValue();
            float targetAnim = activeEffectIds.contains(id) ? 1f : 0f;
            float newAnim = lerp(currentAnim, targetAnim, deltaTime);

            if (Math.abs(newAnim - targetAnim) < 0.01f) {
                newAnim = targetAnim;
            }

            if (newAnim <= 0.01f && targetAnim == 0f) {
                toRemove.add(id);
            } else {
                effectAnimations.put(id, newAnim);
            }
        }
        for (String id : toRemove) {
            effectAnimations.remove(id);
            cachedEffects.remove(id);
        }

        float x = getX();
        float y = getY();

        boolean hasAnimatingEffects = !effectAnimations.isEmpty();
        boolean showExample = !hasAnimatingEffects && isChat(mc.currentScreen);

        int offset = 23;
        float targetWidth = 80;

        String exampleTimer = "00:00";

        if (showExample) {
            offset += 11;
            String name = "Example Effect";
            String levelText = "LVL";
            float timerWidth = Fonts.BOLD.getWidth(exampleTimer, 6);
            float nameWidth = Fonts.BOLD.getWidth(name, 6);
            float levelWidth = Fonts.REGULAR.getWidth(levelText, 6);
            targetWidth = Math.max(nameWidth + 3 + levelWidth + timerWidth + 60, targetWidth);
        } else if (hasAnimatingEffects) {
            for (Map.Entry<String, Float> entry : effectAnimations.entrySet()) {
                String id = entry.getKey();
                float animation = entry.getValue();
                if (animation <= 0) continue;

                StatusEffectInstance effect = cachedEffects.get(id);
                if (effect == null) continue;

                offset += (int) (animation * 11);

                String timer = "" + formatDuration(effect.getDuration()) + "";
                float timerWidth = Fonts.BOLD.getWidth(timer, 6);
                float fullNameWidth = getFullNameWidth(effect);
                targetWidth = Math.max(fullNameWidth + timerWidth + 60, targetWidth);
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

        Scissor.enable(x, y, getWidth(), contentHeight, 2);

        int effectsCount = activeEffectIds.isEmpty() ? 1 : activeEffectIds.size();
        String countText = String.valueOf(effectsCount);
        float countTextWidth = Fonts.BOLD.getWidth(countText, 6);
        float potionsTextWidth = Fonts.BOLD.getWidth("Potions", 6);

        Render2D.gradientRect(x + getWidth() - countTextWidth - potionsTextWidth + 3, y + 5, 14, 12,
                new int[]{
                        new Color(52, 52, 52, bgAlpha).getRGB(),
                        new Color(52, 52, 52, bgAlpha).getRGB(),
                        new Color(52, 52, 52, bgAlpha).getRGB(),
                        new Color(52, 52, 52, bgAlpha).getRGB()
                },
                3);

        Fonts.HUD_ICONS.draw("f", x + getWidth() - countTextWidth - potionsTextWidth + 5, y + 6, 10, new Color(165, 165, 165, bgAlpha).getRGB());

        Fonts.BOLD.draw("Potions", x + 8, y + 6.5f, 6, new Color(255, 255, 255, bgAlpha).getRGB());

        int moduleOffset = 23;

        if (showExample) {
            String name = "Example Effect";
            String levelText = "LVL";
            String timer = "00:00";

            float timerWidth = Fonts.BOLD.getWidth(timer, 6);
            float timerBoxX = x + getWidth() - timerWidth - 11.5f;

            Render2D.gradientRect(timerBoxX, y + moduleOffset - 2f, timerWidth + 4, 9,
                    new int[]{
                            new Color(52, 52, 52, bgAlpha).getRGB(),
                            new Color(52, 52, 52, bgAlpha).getRGB(),
                            new Color(52, 52, 52, bgAlpha).getRGB(),
                            new Color(52, 52, 52, bgAlpha).getRGB()
                    },
                    3);

            Render2D.outline(timerBoxX, y + moduleOffset - 2f, timerWidth + 4, 9, 0.05f,
                    new Color(132, 132, 132, bgAlpha).getRGB(), 2);

            Identifier randomTexture = getRandomEffectTexture();
            float scale = ICON_SIZE / 18f;
            float iconX = x + 8;
            float iconY = y + moduleOffset - 2.5f;

            context.getMatrices().pushMatrix();
            context.getMatrices().translate(iconX, iconY);
            context.getMatrices().scale(scale, scale);
            context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, randomTexture, 0, 0, 18, 18);
            context.getMatrices().popMatrix();

            float nameX = x + 20;
            Fonts.BOLD.draw(name, nameX, y + moduleOffset - 1.5f, 6,
                    new Color(255, 255, 255, bgAlpha).getRGB());

            float nameWidth = Fonts.BOLD.getWidth(name, 6);

            Fonts.TEST.draw(levelText, nameX + nameWidth + 2, y + moduleOffset - 0.5f, 5,
                    new Color(155, 155, 155, bgAlpha).getRGB());

            Fonts.BOLD.draw(timer, timerBoxX + 2, y + moduleOffset - 1, 6,
                    new Color(165, 165, 165, bgAlpha).getRGB());
        } else if (hasAnimatingEffects) {
            for (Map.Entry<String, Float> entry : effectAnimations.entrySet()) {
                String id = entry.getKey();
                float animation = entry.getValue();
                if (animation <= 0) continue;

                StatusEffectInstance effect = cachedEffects.get(id);
                if (effect == null) continue;

                String name = getEffectName(effect);
                int amplifier = effect.getAmplifier();
                String levelText = getLevelText(amplifier);
                String timer = "" + formatDuration(effect.getDuration()) + "";

                int duration = effect.getDuration();

                float timerWidth = Fonts.BOLD.getWidth(timer, 6);

                int baseAlpha = (int) (255 * animation * alphaFactor);
                int blinkAlpha = getBlinkAlpha(duration, baseAlpha);

                int textColor = new Color(255, 255, 255, blinkAlpha).getRGB();
                int levelColor = new Color(155, 155, 155, blinkAlpha).getRGB();
                int timerColor = new Color(165, 165, 165, blinkAlpha).getRGB();

                float timerBoxX = x + getWidth() - timerWidth - 11.5f;

                Render2D.gradientRect(timerBoxX, y + moduleOffset - 2f, timerWidth + 4, 9,
                        new int[]{
                                new Color(52, 52, 52, blinkAlpha).getRGB(),
                                new Color(52, 52, 52, blinkAlpha).getRGB(),
                                new Color(52, 52, 52, blinkAlpha).getRGB(),
                                new Color(52, 52, 52, blinkAlpha).getRGB()
                        },
                        3);

                Render2D.outline(timerBoxX, y + moduleOffset - 2f, timerWidth + 4, 9, 0.05f,
                        new Color(132, 132, 132, blinkAlpha).getRGB(), 2);

                Identifier effectTexture = getEffectTexture(effect.getEffectType());
                float scale = ICON_SIZE / 18f;
                float iconX = x + 8;
                float iconY = y + moduleOffset - 2.5f;

                context.getMatrices().pushMatrix();
                context.getMatrices().translate(iconX, iconY);
                context.getMatrices().scale(scale, scale);
                int iconColor = new Color(255, 255, 255, blinkAlpha).getRGB();
                context.drawGuiTexture(RenderPipelines.GUI_TEXTURED, effectTexture, 0, 0, 18, 18, iconColor);
                context.getMatrices().popMatrix();

                float nameX = x + 20;
                Fonts.BOLD.draw(name, nameX, y + moduleOffset - 1.5f, 6, textColor);

                if (amplifier > 0) {
                    float nameWidth = Fonts.BOLD.getWidth(name, 6);
                    Fonts.TEST.draw(levelText, nameX + nameWidth + 2, y + moduleOffset - 0.5f, 5, levelColor);
                }

                Fonts.BOLD.draw(timer, timerBoxX + 2, y + moduleOffset - 1, 6, timerColor);

                moduleOffset += (int) (animation * 11);
            }
        }

        Scissor.disable();
    }
}