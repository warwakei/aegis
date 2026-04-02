package fun.aegis.features.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.option.Perspective;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.common.repository.friend.FriendUtils;
import fun.aegis.common.animation.Animation;
import fun.aegis.common.animation.Direction;
import fun.aegis.common.animation.implement.Decelerate;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.events.player.TickEvent;
import fun.aegis.events.render.DrawEvent;

import java.util.List;

import static net.minecraft.client.render.VertexFormat.DrawMode.QUADS;
import static net.minecraft.client.render.VertexFormats.POSITION_TEXTURE_COLOR;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Arrows extends Module {
    Identifier iconId = Identifier.of("textures/features/arrows/arrow.png");
    Animation radiusAnim = new Decelerate().setMs(150).setValue(6);

    SliderSettings radiusSetting = new SliderSettings("Радиус", "Радиус стрелок")
            .setValue(50).range(30, 100);

    SliderSettings sizeSetting = new SliderSettings("Размер", "Размер стрелок")
            .setValue(10).range(8, 20);

    public Arrows() {
        super("Arrows", "Arrows", ModuleCategory.RENDER);
        setup(radiusSetting, sizeSetting);
    }

    @EventHandler
    public void onTick(TickEvent e) {
        radiusAnim.setDirection(mc.player.isSprinting() ? Direction.FORWARDS : Direction.BACKWARDS);
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        MatrixStack matrix = e.getDrawContext().getMatrices();
        List<AbstractClientPlayerEntity> players = mc.world.getPlayers().stream()
                .filter(p -> p != mc.player && p.isAlive() && p.getHealth() > 0)
                .filter(p -> !isGhostPlayer(p))
                .toList();

        float middleW = mc.getWindow().getScaledWidth() / 2f;
        float middleH = mc.getWindow().getScaledHeight() / 2f;
        float posY = middleH - radiusSetting.getValue() - radiusAnim.getOutput().floatValue();
        float size = sizeSetting.getValue();

        if (!mc.options.hudHidden && mc.options.getPerspective().equals(Perspective.FIRST_PERSON) && !players.isEmpty()) {
            RenderSystem.enableBlend();
            RenderSystem.disableCull();
            RenderSystem.disableDepthTest();
            RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_CONSTANT_ALPHA);
            RenderSystem.setShaderTexture(0, Identifier.of("textures/features/arrows/arrow.png"));
            RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
            BufferBuilder buffer = tessellator.begin(QUADS, POSITION_TEXTURE_COLOR);

            players.forEach(player -> {
                int color = FriendUtils.isFriend(player) ? ColorAssist.getFriendColor() : ColorAssist.getClientColor();
                float yaw = getRotations(player) - mc.player.getYaw();
                matrix.push();
                matrix.translate(middleW, middleH, 0.0F);
                matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(yaw));
                matrix.translate(-middleW, -middleH, 0.0F);
                Matrix4f matrix4f = matrix.peek().getPositionMatrix();
                buffer.vertex(matrix4f, middleW - (size / 2f), posY + size, 0).texture(0f, 1f).color(ColorAssist.multAlpha(ColorAssist.multDark(color, 0.4F), 0.5F));
                buffer.vertex(matrix4f, middleW + size / 2f, posY + size, 0).texture(1f, 1f).color(ColorAssist.multAlpha(ColorAssist.multDark(color, 0.4F), 0.5F));
                buffer.vertex(matrix4f, middleW + size / 2f, posY, 0).texture(1f, 0).color(color);
                buffer.vertex(matrix4f, middleW - (size / 2f), posY, 0).texture(0, 0).color(color);
                matrix.translate(middleW, middleH, 0.0F);
                matrix.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(-yaw));
                matrix.translate(-middleW, -middleH, 0.0F);
                matrix.pop();
            });

            BufferRenderer.drawWithGlobalProgram(buffer.end());
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
            RenderSystem.defaultBlendFunc();
            RenderSystem.disableBlend();
        }
    }

    private boolean isGhostPlayer(AbstractClientPlayerEntity player) {
        if (player.getCustomName() != null) {
            String name = player.getCustomName().getString();
            return name != null && name.startsWith("Ghost_");
        }
        return player.getClass().getSimpleName().equals("OtherClientPlayerEntity")
                && player.getPitch() == -30.0f;
    }

    public static float getRotations(Entity entity) {
        double x = Calculate.interpolate(entity.getX(), entity.getX()) - Calculate.interpolate(mc.player.getX(), mc.player.getX());
        double z = Calculate.interpolate(entity.getZ(), entity.getZ()) - Calculate.interpolate(mc.player.getZ(), mc.player.getZ());
        return (float) -(Math.atan2(x, z) * (180 / Math.PI));
    }
}
