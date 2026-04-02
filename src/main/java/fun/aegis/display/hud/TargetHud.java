package fun.aegis.display.hud;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import fun.aegis.utils.math.time.StopWatch;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import fun.aegis.utils.client.managers.api.draggable.AbstractDraggable;
import fun.aegis.features.impl.combat.Aura;
import fun.aegis.features.impl.render.Hud;
import fun.aegis.common.animation.Animation;
import fun.aegis.common.animation.Direction;
import fun.aegis.common.animation.implement.Decelerate;
import fun.aegis.utils.display.font.FontRenderer;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.Aegis;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.glow.GlowEffect;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.display.geometry.Render2D;
import fun.aegis.utils.display.scissor.ScissorAssist;
import fun.aegis.utils.client.packet.network.Network;
import java.awt.*;

public class TargetHud extends AbstractDraggable {
    private final Animation animation = new Decelerate().setMs(650).setValue(1);
    private final Animation faceAlphaAnimation = new Decelerate().setMs(125).setValue(1);
    private final StopWatch stopWatch = new StopWatch();
    private final StopWatch healthUpdateTimer = new StopWatch();
    private final StopWatch distanceUpdateTimer = new StopWatch();
    private LivingEntity lastTarget;
    private Item lastItem = Items.AIR;
    private float displayedHealth;
    private float displayedAbsorption;
    private float displayedDistance;
    private float lastKnownMaxHealth;

    public TargetHud() {
        super("Таргет худ", 10, 80, 100, 36, true);
    }

    @Override
    public boolean visible() {
        return scaleAnimation.isDirection(Direction.FORWARDS);
    }

    @Override
    public void tick() {
        LivingEntity auraTarget = Aura.getInstance().getTarget();
        if (auraTarget != null) {
            if (lastTarget != auraTarget) {
                displayedHealth = PlayerInteractionHelper.getHealth(auraTarget);
                displayedAbsorption = auraTarget.getAbsorptionAmount();
                lastKnownMaxHealth = auraTarget.getMaxHealth();
            }
            lastTarget = auraTarget;
            startAnimation();
            faceAlphaAnimation.setDirection(Direction.FORWARDS);
        } else if (PlayerInteractionHelper.isChat(mc.currentScreen)) {
            if (lastTarget != mc.player) {
                displayedHealth = PlayerInteractionHelper.getHealth(mc.player);
                displayedAbsorption = mc.player.getAbsorptionAmount();
                lastKnownMaxHealth = mc.player.getMaxHealth();
            }
            lastTarget = mc.player;
            startAnimation();
            faceAlphaAnimation.setDirection(Direction.FORWARDS);
        } else if (stopWatch.finished(500)) {
            stopAnimation();
            faceAlphaAnimation.setDirection(Direction.BACKWARDS);
        }
        
        if (lastTarget != null) {
            float currentHealth = PlayerInteractionHelper.getHealth(lastTarget);
            float currentAbsorption = lastTarget.getAbsorptionAmount();
            float maxHealth = lastTarget.getMaxHealth();
            
            if (healthUpdateTimer.finished(16)) {
                if (Math.abs(currentHealth - displayedHealth) > 0.1f || Math.abs(currentAbsorption - displayedAbsorption) > 0.1f) {
                    float healthDiff = currentHealth - displayedHealth;
                    float absorptionDiff = currentAbsorption - displayedAbsorption;
                    
                    float healthStep = Math.signum(healthDiff) * Math.min(Math.abs(healthDiff) * 0.3f, 0.8f);
                    float absorptionStep = Math.signum(absorptionDiff) * Math.min(Math.abs(absorptionDiff) * 0.3f, 0.4f);
                    
                    displayedHealth = MathHelper.clamp(displayedHealth + healthStep, 0, maxHealth);
                    displayedAbsorption = Math.max(0, displayedAbsorption + absorptionStep);
                }
                
                lastKnownMaxHealth = maxHealth;
                healthUpdateTimer.reset();
            }
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (Hud.getInstance().interfaceSettings.isSelected("Таргет худ") && Hud.getInstance().state) {
            if (lastTarget != null) {
                MatrixStack matrix = context.getMatrices();
                drawMain(context, matrix);
                drawFace(context);
            }
        }
    }

    private void drawMain(DrawContext context, MatrixStack matrix) {
        FontRenderer font = Fonts.getSize(17, Fonts.Type.REGULAR);
        FontRenderer distancefont = Fonts.getSize(13, Fonts.Type.SEMI); // Увеличено с 12 до 13
        
        if (distanceUpdateTimer.finished(10)) {
            float actualDistance = mc.player.distanceTo(lastTarget);
            float roundedDistance = Math.round(actualDistance * 2) / 2.0f;
            displayedDistance = MathHelper.clamp(Calculate.interpolateSmooth(0.5f, displayedDistance, roundedDistance), 0, 100);
            distanceUpdateTimer.reset();
        }
        
        String distanceText = String.format("%.1f", displayedDistance);

        float nameWidth = font.getStringWidth(lastTarget.getName().getString());
        float baseWidth = Math.max(34 + 36 + 10, 100);
        setWidth((int) baseWidth + 10);
        setHeight((int) 40);

        blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight() - 10)
                .round(6).softness(10F).thickness(0).color(ColorAssist.getRect(0.4F)).build());
        
        // Removed rectangle border
        
        float healthBarX = getX() + 29;
        float healthBarY = getY() + 18; // Уменьшено с 24 до 18 (поднято на 6 пикселей вверх)
        float healthBarWidth = getWidth() - 33;
        float healthBarHeight = 4;
        
        blur.render(ShapeProperties.create(matrix, healthBarX, healthBarY, healthBarWidth, healthBarHeight)
                .round(2).quality(12)
                .color(new Color(0, 0, 0, 150).getRGB())
                .build());
        
        float healthPercentage = displayedHealth / Math.max(0.1f, lastKnownMaxHealth);
        float healthFillWidth = MathHelper.clamp(healthBarWidth * healthPercentage, 0, healthBarWidth);
        
        rectangle.render(ShapeProperties.create(matrix, healthBarX, healthBarY, healthFillWidth, healthBarHeight)
                .round(2)
                .color(ColorAssist.getClientColor(0.8F))
                .build());
        
        if (displayedAbsorption > 0 && !Network.isFunTime()) {
            float absorptionPercentage = Math.min(displayedAbsorption / 20.0F, 1.0f);
            float absorptionFillWidth = healthBarWidth * absorptionPercentage;
            float absorptionFillX = healthBarX + healthBarWidth - absorptionFillWidth;
            
            rectangle.render(ShapeProperties.create(matrix, absorptionFillX, healthBarY, absorptionFillWidth, healthBarHeight)
                    .round(2)
                    .color(new Color(255, 215, 0, 180).getRGB(), new Color(255, 128, 0, 180).getRGB(), new Color(255, 215, 0, 180).getRGB(), new Color(255, 128, 0, 180).getRGB())
                    .build());
        }

        if (nameWidth > 50) {
            ScissorAssist scissorManager = Aegis.getInstance().getScissorManager();
            scissorManager.push(matrix.peek().getPositionMatrix(), getX(), getY(), getWidth() - 29, getHeight());
            font.drawGradientString(matrix, lastTarget.getName().getString(), getX() + 29, getY() + 6f, ColorAssist.getText(), ColorAssist.getText(0.15F));
            scissorManager.pop();
        } else {
            font.drawString(matrix, lastTarget.getName().getString(), getX() + 29, getY() + 6f, ColorAssist.getText());
        }

        float actualTotalHealth = displayedHealth + displayedAbsorption;
        String healthText = (lastTarget.isInvisible() && !Network.isSpookyTime() && !Network.isCopyTime()) ? " ??" : String.format("%.0f", actualTotalHealth);
        Color healthColor = (displayedAbsorption > 0 && !Network.isFunTime()) ? new Color(255, 215, 0, 225) : new Color(255,255,255,225);
        
        // Перемещаем HP текст в правый верхний угол с сдвигом
        float healthTextX = getX() + getWidth() - distancefont.getStringWidth(healthText) - 4; // Сдвиг влево на 2 пикселя (было -2)
        float healthTextY = getY() + 11; // Сдвиг вниз на 5 пикселей (было 9)
        distancefont.drawString(matrix, healthText, healthTextX, healthTextY, healthColor.getRGB());
    }

    private void drawFace(DrawContext context) {
        EntityRenderer<? super LivingEntity, ?> baseRenderer = mc.getEntityRenderDispatcher().getRenderer(lastTarget);
        if (!(baseRenderer instanceof LivingEntityRenderer<?, ?, ?>)) {
            return;
        }
        @SuppressWarnings("unchecked")
        LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?> renderer = (LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>) baseRenderer;
        LivingEntityRenderState state = renderer.getAndUpdateRenderState(lastTarget, tickCounter.getTickDelta(false));
        Identifier textureLocation = renderer.getTexture(state);
        float alpha = faceAlphaAnimation.getOutput().floatValue();
        Calculate.setAlpha(alpha, () -> {
            Render2D.drawTexture(context, textureLocation, getX() + 5, getY() + 5.5F, 20, 4, 8, 8, 64, ColorAssist.getRect(1), ColorAssist.multRed(-1, 1 + lastTarget.hurtTime / 4F));
        });
    }
}
