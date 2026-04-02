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

    private final StopWatch stopWatch = new StopWatch();
    private LivingEntity lastTarget;

    private float healthAnimation = 0;
    private float trailAnimation = 0;
    private float absorptionAnimation = 0;
    private float displayedHealth = 0;
    private long lastUpdateTime = System.currentTimeMillis();
    private long startTime = System.currentTimeMillis();

    public TargetHud() {
        super("TargetHud", 10, 80, 112, 40, true);
    }

    @Override
    public boolean visible() {
        return true;
    }

    @Override
    public void tick() {
        LivingEntity auraTarget = Aura.target;
        if (auraTarget != null) {
            lastTarget = auraTarget;
            startAnimation();
            stopWatch.reset();
        } else if (isChat(mc.currentScreen)) {
            lastTarget = mc.player;
            startAnimation();
            stopWatch.reset();
        } else if (stopWatch.finished(10)) {
            stopAnimation();
        }
    }

    private float lerp(float current, float target, float deltaTime, float speed) {
        float factor = (float) (1.0 - Math.pow(0.001, deltaTime * speed));
        return current + (target - current) * factor;
    }

    private float snapToStep(float value, float step) {
        return Math.round(value / step) * step;
    }

    private float getHealth(LivingEntity entity) {
        if (entity.isInvisible() && !Network.isSpookyTime() && !Network.isCopyTime()) {
            return entity.getMaxHealth();
        }
        return entity.getHealth();
    }

    private String getHealthString(float health) {
        if (lastTarget != null && lastTarget.isInvisible() && !Network.isSpookyTime() && !Network.isCopyTime()) {
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

        setWidth(112);
        setHeight(40);

        float scaleAlpha = scaleAnimation.getOutput().floatValue();

        drawBackground(x, y, scaleAlpha);
        drawFace(x, y, scaleAlpha);
        drawContent(x, y, scaleAlpha, deltaTime);
    }

    private void drawBackground(float x, float y, float alpha) {
        int alphaInt = (int) (255 * alpha);

        Render2D.gradientRect(x + 2, y + 2, getWidth() - 4, getHeight() - 4,
                new int[]{
                        new Color(52, 52, 52, alphaInt).getRGB(),
                        new Color(22, 22, 22, alphaInt).getRGB(),
                        new Color(52, 52, 52, alphaInt).getRGB(),
                        new Color(22, 22, 22, alphaInt).getRGB()
                },
                6);

        Render2D.outline(x + 2, y + 2, getWidth() - 4, getHeight() - 4, 0.35f, new Color(90, 90, 90, alphaInt).getRGB(), 5);

        int blurTint = ColorUtil.rgba(0, 0, 0, 0);
        Render2D.blur(x + 2, y + 2, 1, 1, 0f, 7, blurTint);
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

        float faceSize = 24;
        float faceX = x + 9;
        float faceY = y + 8;

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

        float hatScale = 1.1f;
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
        float faceSize = 24;
        float faceX = x + 9;
        float contentX = faceX + faceSize + 6;
        float nameY = y + 13;

        float hp = getHealth(lastTarget);
        float maxHp = lastTarget.getMaxHealth();
        float absorp = lastTarget.getAbsorptionAmount();

        boolean isInvisible = lastTarget.isInvisible() && !Network.isSpookyTime() && !Network.isCopyTime();

        float targetDisplayHealth;
        if (isInvisible) {
            targetDisplayHealth = maxHp;
        } else {
            targetDisplayHealth = hp + absorp;
        }
        displayedHealth = lerp(displayedHealth, targetDisplayHealth, deltaTime, 5f);
        float snappedHealth = snapToStep(displayedHealth, 0.25f);

        String hpStr = getHealthString(snappedHealth);

        String name = lastTarget.getName().getString();
        float hpWidth = Fonts.BOLD.getWidth(hpStr, 5.5f);

        Fonts.BOLD.draw(name, contentX, nameY, 5.5f,
                new Color(255, 255, 255, (int) (255 * alpha)).getRGB());

        int hpColor = new Color(215, 215, 215, (int) (255 * alpha)).getRGB();
        Fonts.BOLD.draw(hpStr, x + getWidth() - 10 - hpWidth, nameY, 5.5f, hpColor);

        float targetHealth;
        if (isInvisible) {
            targetHealth = 1.0f;
        } else {
            targetHealth = hp / maxHp;
        }
        healthAnimation = lerp(healthAnimation, targetHealth, deltaTime, 3f);

        if (targetHealth > trailAnimation) {
            trailAnimation = targetHealth;
        }
        trailAnimation = lerp(trailAnimation, targetHealth, deltaTime, 3.5f);

        float targetAbsorption;
        if (isInvisible) {
            targetAbsorption = 0;
        } else {
            targetAbsorption = absorp / maxHp;
        }
        absorptionAnimation = lerp(absorptionAnimation, targetAbsorption, deltaTime, 3f);

        float barX = contentX;
        float barY = nameY + 12f;
        float barWidth = 64;
        float barHeight = 4;
        float barRadius = 2;

        Render2D.rect(barX, barY, barWidth, barHeight,
                new Color(30, 30, 30, (int) (200 * alpha)).getRGB(), barRadius);

        float healthPercent = Math.max(0, Math.min(1, healthAnimation));
        float trailPercent = Math.max(0, Math.min(1, trailAnimation));

        if (trailPercent > healthPercent) {
            int trailColor = new Color(55, 55, 55, (int) (160 * alpha)).getRGB();
            Render2D.rect(barX, barY, barWidth * trailPercent, barHeight, trailColor, barRadius);
        }

        if (healthPercent > 0.01f) {
            long elapsed = System.currentTimeMillis() - startTime;
            float waveSpeed = 1500f;
            float wavePhase = (elapsed % (long) waveSpeed) / waveSpeed * (float) Math.PI * 2f;

            int[] colors = new int[4];
            for (int i = 0; i < 2; i++) {
                float charWave = (float) Math.sin(wavePhase - i * 1.5f);
                float waveFactor = (charWave + 1f) / 2f;

                int baseGray = (int) (155 + 100 * waveFactor);

                colors[i * 2] = new Color(baseGray, baseGray, baseGray, (int) (255 * alpha)).getRGB();
                colors[i * 2 + 1] = new Color(baseGray, baseGray, baseGray, (int) (255 * alpha)).getRGB();
            }

            Render2D.gradientRect(barX, barY, barWidth * healthPercent, barHeight, colors, barRadius);
        }

        float absorptionPercent = Math.max(0, Math.min(1, absorptionAnimation));
        if (absorptionPercent > 0.01f && !Network.isFunTime()) {
            long elapsed = System.currentTimeMillis() - startTime;
            float waveSpeed = 1200f;
            float wavePhase = (elapsed % (long) waveSpeed) / waveSpeed * (float) Math.PI * 2f;

            int[] goldColors = new int[4];
            for (int i = 0; i < 2; i++) {
                float charWave = (float) Math.sin(wavePhase - i * 1.5f);
                float waveFactor = (charWave + 1f) / 2f;

                int cr = 255;
                int cg = (int) (165 + 50 * waveFactor);
                int cb = 0;

                goldColors[i * 2] = new Color(cr, cg, cb, (int) (200 * alpha)).getRGB();
                goldColors[i * 2 + 1] = new Color(cr, cg, cb, (int) (200 * alpha)).getRGB();
            }

            Render2D.gradientRect(barX, barY, barWidth * absorptionPercent, barHeight, goldColors, barRadius);
        }
    }
}