package rich.util.render.fakeplayer;

import com.mojang.blaze3d.opengl.GlConst;
import com.mojang.blaze3d.opengl.GlStateManager;
import lombok.experimental.UtilityClass;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import rich.IMinecraft;
import rich.modules.impl.player.FreeCam;
import rich.util.ColorUtil;
import rich.util.render.сliemtpipeline.ClientPipelines;


@UtilityClass
public class FakePlayerRenderer implements IMinecraft {

        private static final float HEAD_SIZE = 0.5f;
        private static final float BODY_WIDTH = 0.5f;
        private static final float BODY_HEIGHT = 0.75f;
        private static final float BODY_DEPTH = 0.25f;
        private static final float ARM_WIDTH = 0.25f;
        private static final float ARM_HEIGHT = 0.75f;
        private static final float LEG_HEIGHT = 0.75f;

        private static final float MODEL_CENTER_Y = LEG_HEIGHT + BODY_HEIGHT / 2;

        private static int currentAlpha = 255;

        public void render(Vec3d position, float alpha) {
                if (mc.player == null || alpha <= 0.001f)
                        return;

                currentAlpha = (int) (Math.min(1f, Math.max(0f, alpha)) * 255);

                Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();

                VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
                MatrixStack stack = new MatrixStack();

                GlStateManager._disableCull();
                GlStateManager._enableBlend();
                GlStateManager._blendFuncSeparate(
                                GlConst.GL_SRC_ALPHA,
                                GlConst.GL_ONE_MINUS_SRC_ALPHA,
                                GlConst.GL_ONE,
                                GlConst.GL_ZERO);

                stack.push();
                stack.translate(position.x - camPos.x, position.y - camPos.y, position.z - camPos.z);

                renderPlayerModel(stack, immediate);

                stack.pop();
                immediate.draw();

                GlStateManager._disableBlend();
                GlStateManager._enableCull();
        }

        private void renderPlayerModel(MatrixStack stack, VertexConsumerProvider.Immediate immediate) {
                Box leftLeg = new Box(-0.25, 0, -0.125, 0, LEG_HEIGHT, 0.125);
                Box rightLeg = new Box(0, 0, -0.125, 0.25, LEG_HEIGHT, 0.125);
                Box body = new Box(-BODY_WIDTH / 2, LEG_HEIGHT, -BODY_DEPTH / 2,
                                BODY_WIDTH / 2, LEG_HEIGHT + BODY_HEIGHT, BODY_DEPTH / 2);
                Box head = new Box(-HEAD_SIZE / 2, LEG_HEIGHT + BODY_HEIGHT, -HEAD_SIZE / 2,
                                HEAD_SIZE / 2, LEG_HEIGHT + BODY_HEIGHT + HEAD_SIZE, HEAD_SIZE / 2);
                Box leftArm = new Box(-BODY_WIDTH / 2 - ARM_WIDTH, LEG_HEIGHT + BODY_HEIGHT - ARM_HEIGHT,
                                -ARM_WIDTH / 2,
                                -BODY_WIDTH / 2, LEG_HEIGHT + BODY_HEIGHT, ARM_WIDTH / 2);
                Box rightArm = new Box(BODY_WIDTH / 2, LEG_HEIGHT + BODY_HEIGHT - ARM_HEIGHT, -ARM_WIDTH / 2,
                                BODY_WIDTH / 2 + ARM_WIDTH, LEG_HEIGHT + BODY_HEIGHT, ARM_WIDTH / 2);

                Box[] bodyParts = { leftLeg, rightLeg, body, head, leftArm, rightArm };

                VertexConsumer consumer = immediate.getBuffer(ClientPipelines.CRYSTAL_FILLED);
                Matrix4f matrix = stack.peek().getPositionMatrix();

                float centerX = 0;
                float centerY = MODEL_CENTER_Y;
                float centerZ = 0;
                float maxDist = 1.0f;

                for (Box part : bodyParts) {
                        drawBoxWithVignette(matrix, consumer, part, centerX, centerY, centerZ, maxDist);
                }
        }

        private void drawBoxWithVignette(Matrix4f matrix, VertexConsumer consumer, Box box,
                        float centerX, float centerY, float centerZ, float maxDist) {
                float x1 = (float) box.minX;
                float y1 = (float) box.minY;
                float z1 = (float) box.minZ;
                float x2 = (float) box.maxX;
                float y2 = (float) box.maxY;
                float z2 = (float) box.maxZ;

                drawQuadVignette(matrix, consumer, x1, y1, z1, x2, y1, z1, x2, y1, z2, x1, y1, z2, centerX, centerY,
                                centerZ, maxDist);
                drawQuadVignette(matrix, consumer, x1, y2, z2, x2, y2, z2, x2, y2, z1, x1, y2, z1, centerX, centerY,
                                centerZ, maxDist);
                drawQuadVignette(matrix, consumer, x1, y1, z1, x1, y2, z1, x2, y2, z1, x2, y1, z1, centerX, centerY,
                                centerZ, maxDist);
                drawQuadVignette(matrix, consumer, x2, y1, z2, x2, y2, z2, x1, y2, z2, x1, y1, z2, centerX, centerY,
                                centerZ, maxDist);
                drawQuadVignette(matrix, consumer, x1, y1, z2, x1, y2, z2, x1, y2, z1, x1, y1, z1, centerX, centerY,
                                centerZ, maxDist);
                drawQuadVignette(matrix, consumer, x2, y1, z1, x2, y2, z1, x2, y2, z2, x2, y1, z2, centerX, centerY,
                                centerZ, maxDist);
        }

        private void drawQuadVignette(Matrix4f matrix, VertexConsumer consumer,
                        float x1, float y1, float z1,
                        float x2, float y2, float z2,
                        float x3, float y3, float z3,
                        float x4, float y4, float z4,
                        float centerX, float centerY, float centerZ, float maxDist) {

                consumer.vertex(matrix, x1, y1, z1)
                                .color(getVignetteColor(x1, y1, z1, centerX, centerY, centerZ, maxDist));
                consumer.vertex(matrix, x2, y2, z2)
                                .color(getVignetteColor(x2, y2, z2, centerX, centerY, centerZ, maxDist));
                consumer.vertex(matrix, x3, y3, z3)
                                .color(getVignetteColor(x3, y3, z3, centerX, centerY, centerZ, maxDist));
                consumer.vertex(matrix, x4, y4, z4)
                                .color(getVignetteColor(x4, y4, z4, centerX, centerY, centerZ, maxDist));
        }

        private int getVignetteColor(float x, float y, float z,
                        float centerX, float centerY, float centerZ, float maxDist) {

                float modelHalfWidth = 0.7f;
                float modelHalfHeight = 1f;
                float modelHalfDepth = 1f;

                float dx = Math.abs(x - centerX) / modelHalfWidth;
                float dy = Math.abs(y - centerY) / modelHalfHeight;
                float dz = Math.abs(z - centerZ) / modelHalfDepth;

                float t = Math.max(Math.max(dx, dy), dz);
                t = Math.min(1f, t);

                float colorIntensity = 0.6f;
                t = t * colorIntensity;

                FreeCam freeCam = new FreeCam();
                
                int clientColor = freeCam.fakeplayer.getColor();
                int white = ColorUtil.rgba(255, 255, 255, currentAlpha);

                return ColorUtil.interpolateColor(white, clientColor, t);
        }

        public void renderFromBox(Box playerBox, float alpha) {
                double centerX = (playerBox.minX + playerBox.maxX) / 2;
                double bottomY = playerBox.minY;
                double centerZ = (playerBox.minZ + playerBox.maxZ) / 2;

                render(new Vec3d(centerX, bottomY, centerZ), alpha);
        }
}
