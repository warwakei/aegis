package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import rich.IMinecraft;
import rich.events.api.EventHandler;
import rich.events.impl.JumpEvent;
import rich.events.impl.WorldRenderEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.ColorSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.ColorUtil;
import rich.util.render.сliemtpipeline.ClientPipelines;
import rich.util.timer.StopWatch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class JumpCircle extends ModuleStructure implements IMinecraft {

    private final List<Circle> circles = new ArrayList<>();

    final Identifier circleTexture = Identifier.of("rich", "images/circle/circle.png");
    final Identifier glowTexture = Identifier.of("rich", "images/particle/glow.png");

    final SliderSettings maxSize = new SliderSettings("Max Size", "Максимальный размер круга")
            .setValue(2f)
            .range(1.0f, 3.0f);

    final SliderSettings speed = new SliderSettings("Speed", "Скорость анимации")
            .setValue(2000f)
            .range(1000f, 3000f);

    final BooleanSetting doubleRing = new BooleanSetting("Двойное кольцо", "Второе кольцо с задержкой")
            .setValue(true);

    final SliderSettings ringOffset = new SliderSettings("Смещение кольца", "Задержка второго кольца")
            .range(0.05f, 0.3f).setValue(0.15f)
            .visible(() -> doubleRing.isValue());

    final BooleanSetting rotatingSegments = new BooleanSetting("Вращающиеся сегменты", "Добавить вращающиеся сегменты")
            .setValue(true);

    final BooleanSetting glow = new BooleanSetting("Glow", "Эффект свечения")
            .setValue(true);

    final SliderSettings glowIntensity = new SliderSettings("Интенсивность свечения", "Сила glow эффекта")
            .range(0.5f, 2.0f).setValue(1.2f)
            .visible(() -> glow.isValue());

    final ColorSetting color1 = new ColorSetting("Цвет 1", "Первый цвет")
            .value(ColorUtil.getColor(137, 97, 72, 255));

    final ColorSetting color2 = new ColorSetting("Цвет 2", "Второй цвет")
            .value(ColorUtil.getColor(255, 255, 255, 255));

    final ColorSetting glowColor = new ColorSetting("Цвет свечения", "Цвет для glow эффекта")
            .value(ColorUtil.getColor(180, 200, 255, 100));

    private static final int SEGMENTS = 96;

    public JumpCircle() {
        super("JumpCircle", "Jump Circle", ModuleCategory.RENDER);
        settings(maxSize, speed, doubleRing, ringOffset, rotatingSegments, glow, glowIntensity, color1, color2, glowColor);
    }

    @EventHandler
    public void onJump(JumpEvent event) {
        if (mc.player == null || event.getPlayer() != mc.player) return;

        Vec3d pos = new Vec3d(
                mc.player.getX(),
                Math.floor(mc.player.getY()) + 0.001,
                mc.player.getZ()
        );
        circles.add(new Circle(pos, new StopWatch()));
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        long maxTime = (long) speed.getValue();

        Iterator<Circle> iterator = circles.iterator();
        while (iterator.hasNext()) {
            Circle circle = iterator.next();
            if (circle.timer.elapsedTime() > maxTime) {
                iterator.remove();
            }
        }

        if (circles.isEmpty()) return;

        MatrixStack matrices = e.getStack();
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();

        for (Circle circle : circles) {
            renderSingleCircle(matrices, immediate, circle, cameraPos);
        }

        immediate.draw();
    }

    private void renderSingleCircle(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Circle circle, Vec3d cameraPos) {
        float lifeTime = circle.timer.elapsedTime();
        float maxTime = speed.getValue();
        float progress = Math.min(lifeTime / maxTime, 1f);

        if (progress >= 1f) return;

        float easedProgress = bounceOut(progress);
        float scale = easedProgress * maxSize.getValue();

        float fadeInDuration = 0.375f;
        float glowStart = 0.65f;
        float fadeOutStart = 0.85f;
        float alpha;

        if (progress < fadeInDuration) {
            alpha = progress / fadeInDuration;
        } else if (progress >= fadeOutStart) {
            float fadeOutProgress = (progress - fadeOutStart) / (1f - fadeOutStart);
            alpha = 1f - fadeOutProgress;

            if (progress > glowStart) {
                float glowProgress = (progress - glowStart) / (fadeOutStart - glowStart);
                float glowPulse = (float) (Math.sin(glowProgress * Math.PI * 3) * 0.75 + 0.75);
                alpha += glowPulse * (1f - fadeOutProgress);
            }
        } else if (progress > glowStart) {
            float glowProgress = (progress - glowStart) / (fadeOutStart - glowStart);
            float glowPulse = (float) (Math.sin(glowProgress * Math.PI * 3) * 0.75 + 0.75);
            alpha = 1f + glowPulse;
        } else {
            alpha = 1f;
        }

        // Пульация для красоты
        float pulse = (float) Math.sin(progress * Math.PI * 6) * 0.125f + 1.0f;
        alpha *= pulse;

        alpha = Math.max(0f, Math.min(1f, alpha));

        float rotationOffset = (lifeTime / 1000f) * 0.5f * 360f;

        Vec3d circlePos = circle.pos();

        // Рендер второго кольца
        if (doubleRing.isValue() && progress > ringOffset.getValue()) {
            float secondProgress = (progress - ringOffset.getValue()) / (1f - ringOffset.getValue());
            float secondScale = bounceOut(secondProgress) * maxSize.getValue() * 0.85f;
            float secondAlpha = alpha * 0.6f;
            float secondRotation = rotationOffset + 180f;

            if (glow.isValue()) {
                renderGradientGlow(matrices, immediate, circlePos, secondScale, secondAlpha * 0.08f * glowIntensity.getValue(), secondRotation, cameraPos);
            }
            renderGradientCircle(matrices, immediate, circlePos, secondScale, secondAlpha, secondRotation, cameraPos);
        }

        // Вращающиеся сегменты
        if (rotatingSegments.isValue()) {
            renderRotatingSegments(matrices, immediate, circlePos, scale, alpha * 0.4f, rotationOffset, cameraPos);
        }

        if (glow.isValue()) {
            renderGradientGlow(matrices, immediate, circlePos, scale, alpha * 0.1f * glowIntensity.getValue(), rotationOffset, cameraPos);
        }

        renderGradientCircle(matrices, immediate, circlePos, scale, alpha, rotationOffset, cameraPos);
    }

    private void renderGradientCircle(MatrixStack matrices, VertexConsumerProvider.Immediate immediate,
                                      Vec3d pos, float size, float alpha, float rotationOffset, Vec3d cameraPos) {
        VertexConsumer buffer = immediate.getBuffer(ClientPipelines.BLOOM_ESP.apply(circleTexture));

        matrices.push();

        float x = (float) (pos.x - cameraPos.x);
        float y = (float) (pos.y - cameraPos.y);
        float z = (float) (pos.z - cameraPos.z);

        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float radius = size / 2f;

        int c1 = color1.getColor();
        int c2 = color2.getColor();

        for (int i = 0; i < SEGMENTS; i++) {
            float angle1 = (float) (2 * Math.PI * i / SEGMENTS);
            float angle2 = (float) (2 * Math.PI * (i + 1) / SEGMENTS);

            float t = (float) i / SEGMENTS;
            float tNext = (float) (i + 1) / SEGMENTS;

            float adjustedT = (t + rotationOffset / 360f) % 1f;
            float adjustedTNext = (tNext + rotationOffset / 360f) % 1f;

            int currentColor = getGradientColor(c1, c2, adjustedT, alpha);
            int nextColor = getGradientColor(c1, c2, adjustedTNext, alpha);

            float x1 = (float) (Math.cos(angle1) * radius);
            float z1 = (float) (Math.sin(angle1) * radius);
            float x2 = (float) (Math.cos(angle2) * radius);
            float z2 = (float) (Math.sin(angle2) * radius);

            float u1 = (float) (0.5 + 0.5 * Math.cos(angle1));
            float v1 = (float) (0.5 + 0.5 * Math.sin(angle1));
            float u2 = (float) (0.5 + 0.5 * Math.cos(angle2));
            float v2 = (float) (0.5 + 0.5 * Math.sin(angle2));

            int centerColor = ColorUtil.lerpColor(currentColor, nextColor, 0.5f);

            buffer.vertex(matrix, 0, 0, 0).texture(0.5f, 0.5f).color(centerColor);
            buffer.vertex(matrix, x1, z1, 0).texture(u1, v1).color(currentColor);
            buffer.vertex(matrix, x2, z2, 0).texture(u2, v2).color(nextColor);
            buffer.vertex(matrix, x2, z2, 0).texture(u2, v2).color(nextColor);
        }

        matrices.pop();
    }

    private void renderGradientGlow(MatrixStack matrices, VertexConsumerProvider.Immediate immediate,
                                    Vec3d pos, float scale, float alpha, float rotationOffset, Vec3d cameraPos) {
        int c1 = color1.getColor();
        int c2 = color2.getColor();

        for (int layer = 0; layer < 3; layer++) {
            float layerScale = scale * (1.3f + layer * 0.4f);
            float layerAlpha = alpha * (0.35f - layer * 0.1f);

            renderGlowLayer(matrices, immediate, pos, layerScale, layerAlpha, rotationOffset, c1, c2, cameraPos);
        }

        float coreAlpha = alpha * 0.2f;
        int coreColor1 = ColorUtil.multAlpha(c1, coreAlpha);
        int coreColor2 = ColorUtil.multAlpha(c2, coreAlpha);
        int mixedCore = ColorUtil.lerpColor(coreColor1, coreColor2, 0.5f);
        renderTexturedQuad(matrices, immediate, pos, scale * 2.5f, mixedCore, glowTexture, cameraPos);
    }

    private void renderGlowLayer(MatrixStack matrices, VertexConsumerProvider.Immediate immediate,
                                 Vec3d pos, float size, float alpha, float rotationOffset,
                                 int c1, int c2, Vec3d cameraPos) {
        VertexConsumer buffer = immediate.getBuffer(ClientPipelines.BLOOM_ESP.apply(glowTexture));

        int glowSegments = 16;
        float radius = size / 2f;

        for (int i = 0; i < glowSegments; i++) {
            float angle = (float) (2 * Math.PI * i / glowSegments);
            float t = (float) i / glowSegments;

            float adjustedT = (t + rotationOffset / 360f) % 1f;
            int glowColor = getGradientColor(c1, c2, adjustedT, alpha);

            float glowX = (float) (pos.x + Math.cos(angle) * radius * 0.8f);
            float glowZ = (float) (pos.z + Math.sin(angle) * radius * 0.8f);
            Vec3d glowPos = new Vec3d(glowX, pos.y, glowZ);

            float glowSize = size * 0.4f;
            renderTexturedQuadAtPos(matrices, buffer, glowPos, glowSize, glowColor, cameraPos);
        }
    }

    private void renderRotatingSegments(MatrixStack matrices, VertexConsumerProvider.Immediate immediate,
                                        Vec3d pos, float size, float alpha, float rotationOffset, Vec3d cameraPos) {
        int segmentCount = 8;
        float segmentArc = 0.3f; // размер сегмента в радианах

        VertexConsumer buffer = immediate.getBuffer(ClientPipelines.BLOOM_ESP.apply(glowTexture));

        for (int i = 0; i < segmentCount; i++) {
            float baseAngle = (float) (2 * Math.PI * i / segmentCount) + (rotationOffset / 180f) * (float) Math.PI;

            int segments = 8;
            float angleStep = segmentArc / segments;

            for (int j = 0; j < segments; j++) {
                float angle1 = baseAngle + angleStep * j;
                float angle2 = baseAngle + angleStep * (j + 1);

                float t = (float) j / segments;
                int segColor = getGradientColor(color1.getColor(), color2.getColor(), t, alpha * 0.5f);

                float radius = size / 2f;
                float x1 = (float) (Math.cos(angle1) * radius);
                float z1 = (float) (Math.sin(angle1) * radius);
                float x2 = (float) (Math.cos(angle2) * radius);
                float z2 = (float) (Math.sin(angle2) * radius);

                matrices.push();
                matrices.translate((float) (pos.x - cameraPos.x), (float) (pos.y - cameraPos.y), (float) (pos.z - cameraPos.z));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));

                org.joml.Matrix4f matrix = matrices.peek().getPositionMatrix();

                buffer.vertex(matrix, x1 * 0.95f, z1 * 0.95f, 0).texture(0, 0).color(segColor);
                buffer.vertex(matrix, x1, z1, 0).texture(0, 1).color(segColor);
                buffer.vertex(matrix, x2, z2, 0).texture(1, 1).color(segColor);
                buffer.vertex(matrix, x2 * 0.95f, z2 * 0.95f, 0).texture(1, 0).color(segColor);

                matrices.pop();
            }
        }
    }

    private void renderTexturedQuadAtPos(MatrixStack matrices, VertexConsumer buffer, Vec3d pos, float size, int color, Vec3d cameraPos) {
        matrices.push();

        float x = (float) (pos.x - cameraPos.x);
        float y = (float) (pos.y - cameraPos.y);
        float z = (float) (pos.z - cameraPos.z);

        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float half = size / 2f;

        buffer.vertex(matrix, -half, -half, 0).texture(0, 0).color(color);
        buffer.vertex(matrix, half, -half, 0).texture(1, 0).color(color);
        buffer.vertex(matrix, half, half, 0).texture(1, 1).color(color);
        buffer.vertex(matrix, -half, half, 0).texture(0, 1).color(color);

        matrices.pop();
    }

    private int getGradientColor(int c1, int c2, float t, float alpha) {
        float gradientT;
        if (t <= 0.5f) {
            gradientT = t * 2f;
        } else {
            gradientT = (1f - t) * 2f;
        }

        int color = ColorUtil.lerpColor(c1, c2, gradientT);
        return ColorUtil.multAlpha(color, alpha);
    }

    private void renderTexturedQuad(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Vec3d pos, float size, int color, Identifier texture, Vec3d cameraPos) {
        VertexConsumer buffer = immediate.getBuffer(ClientPipelines.BLOOM_ESP.apply(texture));

        matrices.push();

        float x = (float) (pos.x - cameraPos.x);
        float y = (float) (pos.y - cameraPos.y);
        float z = (float) (pos.z - cameraPos.z);

        matrices.translate(x, y, z);
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90f));

        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float half = size / 2f;

        buffer.vertex(matrix, -half, -half, 0).texture(0, 0).color(color);
        buffer.vertex(matrix, half, -half, 0).texture(1, 0).color(color);
        buffer.vertex(matrix, half, half, 0).texture(1, 1).color(color);
        buffer.vertex(matrix, -half, half, 0).texture(0, 1).color(color);

        matrices.pop();
    }

    private float bounceOut(float value) {
        float n1 = 7.5625f;
        float d1 = 2.75f;
        if (value < 1.0f / d1) {
            return n1 * value * value;
        } else if (value < 2.0f / d1) {
            return n1 * (value -= 1.5f / d1) * value + 0.75f;
        } else if (value < 2.5f / d1) {
            return n1 * (value -= 2.25f / d1) * value + 0.9375f;
        } else {
            return n1 * (value -= 2.625f / d1) * value + 0.984375f;
        }
    }

    public record Circle(Vec3d pos, StopWatch timer) {}
}