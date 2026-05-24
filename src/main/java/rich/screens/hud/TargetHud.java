package rich.screens.hud;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import rich.client.draggables.AbstractHudElement;
import rich.modules.impl.combat.Aura;
import rich.util.ColorUtil;
import rich.util.network.Network;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;
import rich.util.string.PlayerInteractionHelper;
import rich.util.timer.StopWatch;

import java.awt.*;

public class TargetHud extends AbstractHudElement {

    private static final float HEALTH_LERP_SPEED = 9.0f;
    private static final float MAX_HEALTH_LERP_SPEED = 7.5f;
    private static final float BAR_LERP_SPEED = 4.0f;
    private static final float TRAIL_LERP_SPEED = 4.5f;
    private static final float HEALTH_WAVE_SPEED = 1000f;
    private static final float ABSORPTION_WAVE_SPEED = 900f;
    private static final float DEFAULT_MAX_HEALTH = 20f;
    private static final int BAR_WIDTH = 76;
    private static final int BAR_HEIGHT = 5;
    private static final float BAR_RADIUS = 2.5f;
    private static final float FACE_SIZE = 28f;
    private static final float FACE_HAT_SCALE = 1.1f;
    private static final float CONTENT_PADDING = 8f;
    private static final float HEALTH_SNAP_STEP = 0.25f;

    private final StopWatch stopWatch = new StopWatch();
    private LivingEntity lastTarget;
    private LivingEntity previousTarget;

    private float healthAnimation = 0;
    private float trailAnimation = 0;
    private float absorptionAnimation = 0;
    private float displayedHealth = 0;
    private float displayedMaxHealth = DEFAULT_MAX_HEALTH;
    private long lastUpdateTime = System.currentTimeMillis();
    private long startTime = System.currentTimeMillis();

    public TargetHud() {
        super("TargetHud", 10, 80, 130, 44, true);
    }

    @Override
    public boolean visible() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity auraTarget = Aura.target;
        LivingEntity newTarget = null;

        if (auraTarget != null && isEntityValid(auraTarget)) {
            newTarget = auraTarget;
        } else if (isChat(mc.currentScreen) && isEntityValid(mc.player)) {
            newTarget = mc.player;
        }

        if (newTarget != lastTarget) {
            previousTarget = lastTarget;
            lastTarget = newTarget;
            if (newTarget != null) {
                startAnimation();
                stopWatch.reset();
            }
        }

        if (lastTarget == null && stopWatch.finished(10)) {
            stopAnimation();
        }
    }

    private boolean isEntityValid(LivingEntity entity) {
        return entity != null && !entity.isRemoved() && entity.isAlive();
    }

    private float lerp(float current, float target, float deltaTime, float speed) {
        float factor = (float) (1.0 - Math.pow(0.001, deltaTime * speed));
        return current + (target - current) * factor;
    }

    private float perlinNoise(float x, float y) {
        float n = (float) Math.sin(x * 12.9898f + y * 78.233f) * 43758.5453f;
        return n - (float) Math.floor(n);
    }

    private float snapToStep(float value, float step) {
        return Math.round(value / step) * step;
    }

    private String truncateText(String text, float maxWidth, float fontSize) {
        if (text.isEmpty()) return text;
        
        for (int i = text.length(); i > 0; i--) {
            String truncated = text.substring(0, i) + "...";
            if (Fonts.BOLD.getWidth(truncated, fontSize) <= maxWidth) {
                return truncated;
            }
        }
        return "...";
    }

    private float getHealth(LivingEntity entity) {
        if (isInvisible(entity)) {
            return entity.getMaxHealth();
        }
        float h = entity.getHealth();
        if (Float.isNaN(h) || Float.isInfinite(h) || h < 0.0f) return 0.0f;
        return h;
    }

    private float getMaxHealthSafe(LivingEntity entity) {
        float max = entity.getMaxHealth();
        if (Float.isNaN(max) || Float.isInfinite(max) || max <= 0.0f) return DEFAULT_MAX_HEALTH;
        return max;
    }

    private float getAbsorptionSafe(LivingEntity entity) {
        float a = entity.getAbsorptionAmount();
        if (Float.isNaN(a) || Float.isInfinite(a) || a < 0.0f) return 0.0f;
        return a;
    }

    private float getEffectiveHealthSafe(LivingEntity entity) {
        float h = getHealth(entity);
        float a = getAbsorptionSafe(entity);
        float v = h + a;
        if (Float.isNaN(v) || Float.isInfinite(v) || v < 0.0f) return 0.0f;
        return v;
    }

    private boolean isInvisible(LivingEntity entity) {
        return entity.isInvisible() && !Network.isSpookyTime() && !Network.isCopyTime();
    }

    private String getHealthString(float health) {
        if (lastTarget != null && isInvisible(lastTarget)) {
            return "??";
        }
        if (health >= 100) {
            return String.valueOf((int) health);
        } else if (health >= 10) {
            return String.format("%.1f", health);
        } else {
            return String.format("%.2f", health);
        }
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) return;
        if (lastTarget == null) return;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;
        deltaTime = Math.min(deltaTime, 0.1f);

        float x = getX();
        float y = getY();

        setWidth(130);
        setHeight(44);

        float scaleAlpha = scaleAnimation.getOutput().floatValue();
        float alphaMul = HudStyle.alphaMulFrom255(alpha);
        float finalAlpha = Math.min(scaleAlpha, alphaMul);

        drawBackground(x, y, finalAlpha);
        drawFace(x, y, finalAlpha);
        drawContent(x, y, finalAlpha, deltaTime);
    }

    private void drawBackground(float x, float y, float alpha) {
        float panelW = getWidth() - 2;
        float panelH = getHeight() - 2;
        HudStyle.panel(x + 1, y + 1, panelW, panelH, 7f, alpha, HudStyle.Variant.ACCENT);
        Render2D.outline(x + 1, y + 1, panelW, panelH, 0.8f,
                new Color(82, 128, 184, (int) (80 * alpha)).getRGB(), 7f);
    }

    private void drawFace(float x, float y, float alpha) {
        EntityRenderer<? super LivingEntity, ?> baseRenderer = mc.getEntityRenderDispatcher().getRenderer(lastTarget);
        if (!(baseRenderer instanceof LivingEntityRenderer<?, ?, ?>)) {
            return;
        }

        @SuppressWarnings("unchecked")
        LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?> renderer =
                (LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>) baseRenderer;

        LivingEntityRenderState state = renderer.getAndUpdateRenderState(lastTarget, lastTickDelta);
        Identifier textureLocation = renderer.getTexture(state);

        float faceSize = FACE_SIZE;
        float faceX = x + 9;
        float faceY = y + 8;

        Render2D.rect(faceX - 1, faceY - 1, faceSize + 2, faceSize + 2,
                new Color(82, 128, 184, (int) (60 * alpha)).getRGB(), 5f);

        float hurtPercent = lastTarget.hurtTime > 0 ? lastTarget.hurtTime / 10.0f : 0.0f;
        int r = 255;
        int g = (int) (255 * (1.0f - hurtPercent));
        int b = (int) (255 * (1.0f - hurtPercent));
        int color = new Color(r, g, b, (int) (255 * alpha)).getRGB();

        float u0 = 8f / 64f;
        float v0 = 8f / 64f;
        float u1 = 16f / 64f;
        float v1 = 16f / 64f;

        Render2D.texture(textureLocation, faceX, faceY, faceSize, faceSize,
                u0, v0, u1, v1, color, 0, 4f);

        float hatScale = FACE_HAT_SCALE;
        float hatSize = faceSize * hatScale;
        float hatOffset = (hatSize - faceSize) / 2f;

        float hatU0 = 40f / 64f;
        float hatV0 = 8f / 64f;
        float hatU1 = 48f / 64f;
        float hatV1 = 16f / 64f;

        Render2D.texture(textureLocation, faceX - hatOffset, faceY - hatOffset, hatSize, hatSize,
                hatU0, hatV0, hatU1, hatV1, color, 0f, 4f);
    }

    private void drawContent(float x, float y, float alpha, float deltaTime) {
        float faceX = x + 9;
        float contentX = faceX + FACE_SIZE + CONTENT_PADDING;
        float nameY = y + 12;
        float maxContentWidth = x + getWidth() - contentX - 10;

        float hp = Math.max(0.0f, getHealth(lastTarget));
        float maxHp = getMaxHealthSafe(lastTarget);
        float absorp = getAbsorptionSafe(lastTarget);
        boolean invisible = isInvisible(lastTarget);

        float targetDisplayHealth = invisible ? maxHp : getEffectiveHealthSafe(lastTarget);
        displayedHealth = lerp(displayedHealth, targetDisplayHealth, deltaTime, HEALTH_LERP_SPEED);
        displayedMaxHealth = lerp(displayedMaxHealth, maxHp, deltaTime, MAX_HEALTH_LERP_SPEED);

        float shownMax = Math.max(1.0f, displayedMaxHealth);
        float snappedHealth = snapToStep(displayedHealth, HEALTH_SNAP_STEP);
        float snappedHpOnly = snapToStep(hp, HEALTH_SNAP_STEP);
        float snappedAbsorp = snapToStep(absorp, HEALTH_SNAP_STEP);

        drawNameAndHealth(contentX, nameY, maxContentWidth, snappedHealth, alpha);
        drawHealthBar(contentX, nameY, shownMax, hp, absorp, invisible, alpha, deltaTime);
        drawHealthNumeric(contentX, nameY, snappedHpOnly, shownMax, snappedAbsorp, invisible, alpha);
    }

    private void drawNameAndHealth(float contentX, float nameY, float maxContentWidth, float snappedHealth, float alpha) {
        String name = getEntityName();
        if (Fonts.BOLD.getWidth(name, 5.5f) > maxContentWidth) {
            name = truncateText(name, maxContentWidth, 5.5f);
        }

        String hpStr = getHealthString(snappedHealth);
        float hpWidth = Fonts.BOLD.getWidth(hpStr, 5.5f);

        Fonts.BOLD.draw(name, contentX, nameY, 5.5f,
                new Color(255, 255, 255, (int) (255 * alpha)).getRGB());
        Fonts.BOLD.draw(hpStr, contentX + BAR_WIDTH + 6 - hpWidth, nameY, 5.5f,
                new Color(200, 215, 230, (int) (255 * alpha)).getRGB());
    }

    private String getEntityName() {
        String name = null;
        if (lastTarget.getName() != null) {
            name = lastTarget.getName().getString();
        }
        if (name == null || name.isBlank()) {
            if (lastTarget.getDisplayName() != null) {
                name = lastTarget.getDisplayName().getString();
            }
        }
        if (name == null || name.isBlank()) {
            name = lastTarget.getType().getName().getString();
        }
        if (name == null || name.isBlank()) {
            name = "Unknown";
        }
        int colonIdx = name.indexOf(']');
        if (colonIdx != -1 && name.length() > colonIdx + 1) {
            String after = name.substring(colonIdx + 1).trim();
            if (!after.isEmpty() && after.length() < name.length() - 2) {
                name = after;
            }
        }
        return name;
    }

    private void drawHealthBar(float barX, float barY, float shownMax, float hp, float absorp, boolean invisible, float alpha, float deltaTime) {
        barY += 13f;

        float targetHealth = invisible ? 1.0f : hp / shownMax;
        healthAnimation = lerp(healthAnimation, targetHealth, deltaTime, BAR_LERP_SPEED);

        trailAnimation = Math.max(trailAnimation, targetHealth);
        trailAnimation = lerp(trailAnimation, targetHealth, deltaTime, TRAIL_LERP_SPEED);

        float targetAbsorption = invisible ? 0 : absorp / shownMax;
        absorptionAnimation = lerp(absorptionAnimation, targetAbsorption, deltaTime, BAR_LERP_SPEED);

        Render2D.rect(barX, barY, BAR_WIDTH, BAR_HEIGHT,
                new Color(20, 20, 20, (int) (200 * alpha)).getRGB(), BAR_RADIUS);

        float healthPercent = Math.max(0, Math.min(1, healthAnimation));
        float trailPercent = Math.max(0, Math.min(1, trailAnimation));

        if (trailPercent > healthPercent) {
            Render2D.rect(barX, barY, BAR_WIDTH * trailPercent, BAR_HEIGHT,
                    new Color(45, 45, 45, (int) (180 * alpha)).getRGB(), BAR_RADIUS);
        }

        if (healthPercent > 0.01f) {
            drawHealthGradient(barX, barY, healthPercent, alpha);
        }

        float absorptionPercent = Math.max(0, Math.min(1, absorptionAnimation));
        if (absorptionPercent > 0.01f && !Network.isFunTime()) {
            drawAbsorptionGradient(barX, barY, absorptionPercent, alpha);
        }
    }

    private void drawHealthGradient(float barX, float barY, float healthPercent, float alpha) {
        long elapsed = System.currentTimeMillis() - startTime;
        float wavePhase = (elapsed % (long) HEALTH_WAVE_SPEED) / HEALTH_WAVE_SPEED * (float) Math.PI * 2f;
        int healthBarWidth = (int) (BAR_WIDTH * healthPercent);

        int[] colors = new int[4];
        for (int i = 0; i < 4; i++) {
            float t = i / 3f;
            float perlinWave = perlinNoise(wavePhase + t * 2f, t * 3f);
            float smoothWave = (float) Math.sin(wavePhase - i * 1.2f) * 0.6f + perlinWave * 0.4f;
            float waveFactor = (smoothWave + 1f) / 2f;

            float hue = 0.25f + waveFactor * 0.15f;
            int baseColor = ColorUtil.hsvToRgb(hue, 0.35f, 0.75f + waveFactor * 0.25f);
            colors[i] = new Color(ColorUtil.getRed(baseColor), ColorUtil.getGreen(baseColor),
                    ColorUtil.getBlue(baseColor), (int) (255 * alpha)).getRGB();
        }

        Render2D.gradientRect(barX, barY, healthBarWidth, BAR_HEIGHT, colors, BAR_RADIUS);

        Render2D.rect(barX, barY, healthBarWidth, BAR_HEIGHT * 0.4f,
                new Color(255, 255, 255, (int) (30 * alpha)).getRGB(), BAR_RADIUS);

        if (healthPercent > 0.3f) {
            float highlightWidth = healthBarWidth * 0.3f;
            float highlightAlpha = alpha * 0.18f;
            Render2D.gradientRect(barX, barY, highlightWidth, BAR_HEIGHT / 2f,
                    new int[]{
                            new Color(255, 255, 255, (int) (highlightAlpha * 255)).getRGB(),
                            new Color(255, 255, 255, (int) (highlightAlpha * 100)).getRGB(),
                            new Color(255, 255, 255, 0).getRGB(),
                            new Color(255, 255, 255, 0).getRGB()
                    }, 2);
        }
    }

    private void drawAbsorptionGradient(float barX, float barY, float absorptionPercent, float alpha) {
        long elapsed = System.currentTimeMillis() - startTime;
        float wavePhase = (elapsed % (long) ABSORPTION_WAVE_SPEED) / ABSORPTION_WAVE_SPEED * (float) Math.PI * 2f;
        int absorpBarWidth = (int) (BAR_WIDTH * absorptionPercent);

        int[] goldColors = new int[4];
        for (int i = 0; i < 4; i++) {
            float t = i / 3f;
            float perlinWave = perlinNoise(wavePhase + t * 2.5f, t * 2.8f);
            float smoothWave = (float) Math.sin(wavePhase - i * 1.5f) * 0.6f + perlinWave * 0.4f;
            float waveFactor = (smoothWave + 1f) / 2f;

            int cr = (int) (245 + 10 * waveFactor);
            int cg = (int) (190 + 30 * waveFactor);
            int cb = (int) (25 + 25 * waveFactor);

            goldColors[i] = new Color(cr, cg, cb, (int) (230 * alpha)).getRGB();
        }

        Render2D.gradientRect(barX, barY, absorpBarWidth, BAR_HEIGHT, goldColors, BAR_RADIUS);

        float highlightWidth = absorpBarWidth * 0.4f;
        float highlightAlpha = alpha * 0.22f;
        Render2D.gradientRect(barX, barY, highlightWidth, BAR_HEIGHT / 2f,
                new int[]{
                        new Color(255, 255, 200, (int) (highlightAlpha * 255)).getRGB(),
                        new Color(255, 255, 180, (int) (highlightAlpha * 120)).getRGB(),
                        new Color(255, 255, 150, 0).getRGB(),
                        new Color(255, 255, 150, 0).getRGB()
                }, 2);
    }

    private void drawHealthNumeric(float barX, float barY, float snappedHpOnly, float shownMax, float snappedAbsorp, boolean invisible, float alpha) {
        barY += 13f;

        String hpNumeric;
        if (invisible) {
            hpNumeric = "??/" + getHealthString(shownMax);
        } else {
            hpNumeric = getHealthString(snappedHpOnly) + "/" + getHealthString(shownMax);
            if (snappedAbsorp > 0.01f && !Network.isFunTime()) {
                hpNumeric += " +" + getHealthString(snappedAbsorp);
            }
        }

        float hpTextSize = 4.6f;
        float hpTextW = Fonts.REGULAR.getWidth(hpNumeric, hpTextSize);
        float hpTextX = barX + (BAR_WIDTH - hpTextW) / 2f;
        int hpTextColor = new Color(180, 190, 205, (int) (220 * alpha)).getRGB();
        Fonts.REGULAR.draw(hpNumeric, hpTextX, barY, hpTextSize, hpTextColor);
    }
}
