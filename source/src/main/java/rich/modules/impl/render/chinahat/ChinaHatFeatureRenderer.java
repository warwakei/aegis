package rich.modules.impl.render.chinahat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;
import rich.modules.impl.render.ChinaHat;
import rich.util.ColorUtil;
import rich.util.render.сliemtpipeline.ClientPipelines;

public class ChinaHatFeatureRenderer extends FeatureRenderer<PlayerEntityRenderState, PlayerEntityModel> {

    private static final float PI2 = (float) (Math.PI * 2);
    private static final int CIRCLE_SEGMENTS = 720;
    private static final int OUTLINE_SEGMENTS = 360;

    public ChinaHatFeatureRenderer(FeatureRendererContext<PlayerEntityRenderState, PlayerEntityModel> context) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrixStack, OrderedRenderCommandQueue queue, int light, PlayerEntityRenderState state, float limbAngle, float limbDistance) {
        MinecraftClient mc = MinecraftClient.getInstance();

        ChinaHat chinaHat = ChinaHat.getInstance();
        if (chinaHat == null || !chinaHat.isState()) return;

        if (mc.player == null) return;

        if (mc.options.getPerspective().isFirstPerson()) {
            return;
        }

        if (!isLocalPlayer(state, mc)) {
            return;
        }

        matrixStack.push();

        this.getContextModel().head.applyTransform(matrixStack);

        matrixStack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(180f));
        matrixStack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(90f));
        matrixStack.translate(0, 0.42f, 0);
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        renderFlatHat(matrixStack, immediate, chinaHat);
        renderOutline(matrixStack, immediate, chinaHat);
        immediate.draw();

        matrixStack.pop();
    }

    private boolean isLocalPlayer(PlayerEntityRenderState state, MinecraftClient mc) {
        try {
            if (state.id == mc.player.getId()) {
                return true;
            }
        } catch (Exception ignored) {}

        try {
            if (state.playerName != null && mc.player.getName() != null) {
                return state.playerName.getString().equals(mc.player.getName().getString());
            }
        } catch (Exception ignored) {}

        return false;
    }

    private void renderFlatHat(MatrixStack stack, VertexConsumerProvider provider, ChinaHat chinaHat) {
        VertexConsumer consumer = provider.getBuffer(ClientPipelines.CHINA_HAT);

        Matrix4f matrix = stack.peek().getPositionMatrix();

        float width = 0.55f;
        float coneHeight = 0.31f;
        int alpha = 185;
        float animSpeed = 5;

        int centerColor = getGradientColor(0, CIRCLE_SEGMENTS, chinaHat, animSpeed);
        centerColor = ColorUtil.replAlpha(centerColor, alpha);

        consumer.vertex(matrix, 0, coneHeight, 0).color(centerColor);

        for (int i = 0; i <= CIRCLE_SEGMENTS; i++) {
            int color = getGradientColor(i, CIRCLE_SEGMENTS, chinaHat, animSpeed);
            color = ColorUtil.replAlpha(color, alpha);

            float angle = i * PI2 / CIRCLE_SEGMENTS;
            float x = -MathHelper.sin(angle) * width;
            float z = MathHelper.cos(angle) * width;

            consumer.vertex(matrix, x, 0, z).color(color);
        }

        for (int i = CIRCLE_SEGMENTS; i >= 0; i--) {
            int color = getGradientColor(i, CIRCLE_SEGMENTS, chinaHat, animSpeed);
            color = ColorUtil.replAlpha(color, alpha);

            float angle = i * PI2 / CIRCLE_SEGMENTS;
            float x = -MathHelper.sin(angle) * width;
            float z = MathHelper.cos(angle) * width;

            consumer.vertex(matrix, x, 0, z).color(color);
        }

        consumer.vertex(matrix, 0, coneHeight, 0).color(centerColor);
    }

    private void renderOutline(MatrixStack stack, VertexConsumerProvider provider, ChinaHat chinaHat) {
        VertexConsumer consumer = provider.getBuffer(ClientPipelines.CHINA_HAT_OUTLINE);

        Matrix4f matrix = stack.peek().getPositionMatrix();

        float width = 0.55f;
        float animSpeed = 5;
        int outlineAlpha = 255;

        for (int i = 0; i <= OUTLINE_SEGMENTS; i++) {
            int color = getGradientColor(i * 2, OUTLINE_SEGMENTS * 2, chinaHat, animSpeed);
            color = ColorUtil.replAlpha(color, outlineAlpha);

            float angle = i * PI2 / OUTLINE_SEGMENTS;
            float x = -MathHelper.sin(angle) * width;
            float z = MathHelper.cos(angle) * width;

            consumer.vertex(matrix, x, 0, z).color(color);
        }
    }

    private int getGradientColor(int index, int size, ChinaHat chinaHat, float animSpeed) {
        long time = System.currentTimeMillis();
        float timeOffset = (time / (1000f / animSpeed)) % size;

        int adjustedIndex = (int) ((index + timeOffset) % size);

        int color1 = chinaHat.color1.getColor();
        int color2 = chinaHat.color2.getColor();

        float progress = (float) adjustedIndex / size;

        if (progress < 0.5f) {
            return ColorUtil.interpolateColor(color1, color2, progress * 2f);
        } else {
            return ColorUtil.interpolateColor(color2, color1, (progress - 0.5f) * 2f);
        }
    }
}