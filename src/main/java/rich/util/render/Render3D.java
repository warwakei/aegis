package rich.util.render;

import lombok.Setter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Pair;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import rich.IMinecraft;
import rich.events.impl.WorldRenderEvent;
import rich.util.ColorUtil;
import rich.util.math.MathUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@UtilityClass
public class Render3D implements IMinecraft {
    private final Map<VoxelShape, Pair<List<Box>, List<Line>>> SHAPE_OUTLINES = new HashMap<>();
    private final Map<VoxelShape, List<Box>> SHAPE_BOXES = new HashMap<>();

    public final List<Line> LINE_DEPTH = new ArrayList<>();
    public final List<Line> LINE = new ArrayList<>();
    public final List<Quad> QUAD_DEPTH = new ArrayList<>();
    public final List<Quad> QUAD = new ArrayList<>();
    public final List<GradientQuad> GRADIENT_QUAD = new ArrayList<>();
    public final List<GradientQuad> GRADIENT_QUAD_DEPTH = new ArrayList<>();

    public final Matrix4f lastProjMat = new Matrix4f();
    public final Matrix4f lastModMat = new Matrix4f();
    public final Matrix4f lastWorldSpaceMatrix = new Matrix4f();

    @Setter
    public MatrixStack.Entry lastWorldSpaceEntry = new MatrixStack().peek();
    @Setter
    public float lastTickDelta = 1.0f;
    @Setter
    public Vec3d lastCameraPos = Vec3d.ZERO;
    @Setter
    public Quaternionf lastCameraRotation = new Quaternionf();

    private float espValue = 1f;
    private float espSpeed = 1f;
    private float prevEspValue;
    private float circleStep;
    private boolean flipSpeed;

    private double smoothY = 0;
    private double smoothY2 = 0;

    public void updateTargetEsp(float deltaTime) {
        prevEspValue = espValue;
        espValue += espSpeed * deltaTime;
        if (espSpeed > 25) flipSpeed = true;
        if (espSpeed < -25) flipSpeed = false;
        espSpeed = flipSpeed ? espSpeed - 0.5f * deltaTime : espSpeed + 0.5f * deltaTime;
        circleStep += 0.06f * deltaTime;
    }

    public void updateTargetEsp() {
        updateTargetEsp(1.0f);
    }

    public float getEspValue() {
        return espValue;
    }

    public float getPrevEspValue() {
        return prevEspValue;
    }

    public float getCircleStep() {
        return circleStep;
    }

    private double easeInOutSine(double t) {
        return -(Math.cos(Math.PI * t) - 1) / 2;
    }

    private double smoothSinAnimation(double input) {
        double sin = (Math.sin(input) + 1) / 2;
        return easeInOutSine(sin);
    }

    public void onWorldRender(WorldRenderEvent e) {
        if (mc.world == null || mc.player == null) return;

        MatrixStack matrices = e.getStack();
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();

        Vec3d cameraPos = lastCameraPos;

        renderGradientQuads(matrices, immediate, cameraPos);
        renderQuads(matrices, immediate, cameraPos);
        renderLines(matrices, immediate, cameraPos);

        immediate.draw();
    }

    private void renderLines(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Vec3d cameraPos) {
        if (LINE.isEmpty() && LINE_DEPTH.isEmpty()) return;

        VertexConsumer buffer = immediate.getBuffer(RenderLayers.lines());

        for (Line line : LINE) {
            drawLineVertex(matrices, buffer, line, cameraPos);
        }
        for (Line line : LINE_DEPTH) {
            drawLineVertex(matrices, buffer, line, cameraPos);
        }

        LINE.clear();
        LINE_DEPTH.clear();
    }

    private void drawLineVertex(MatrixStack matrices, VertexConsumer buffer, Line line, Vec3d cameraPos) {
        MatrixStack.Entry entry = matrices.peek();
        Vector3f normal = getNormal(line.start.toVector3f(), line.end.toVector3f());

        float x1 = (float) (line.start.x - cameraPos.x);
        float y1 = (float) (line.start.y - cameraPos.y);
        float z1 = (float) (line.start.z - cameraPos.z);

        float x2 = (float) (line.end.x - cameraPos.x);
        float y2 = (float) (line.end.y - cameraPos.y);
        float z2 = (float) (line.end.z - cameraPos.z);

        buffer.vertex(entry, x1, y1, z1)
                .color(line.colorStart)
                .normal(entry, normal)
                .lineWidth(line.width);
        buffer.vertex(entry, x2, y2, z2)
                .color(line.colorEnd)
                .normal(entry, normal)
                .lineWidth(line.width);
    }

    private void renderQuads(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Vec3d cameraPos) {
        if (QUAD.isEmpty() && QUAD_DEPTH.isEmpty()) return;

        VertexConsumer buffer = immediate.getBuffer(RenderLayers.debugFilledBox());

        for (Quad quad : QUAD) {
            drawQuadVertex(matrices, buffer, quad, cameraPos);
        }
        for (Quad quad : QUAD_DEPTH) {
            drawQuadVertex(matrices, buffer, quad, cameraPos);
        }

        QUAD.clear();
        QUAD_DEPTH.clear();
    }

    private void drawQuadVertex(MatrixStack matrices, VertexConsumer buffer, Quad quad, Vec3d cameraPos) {
        MatrixStack.Entry entry = matrices.peek();

        float x1 = (float) (quad.x.x - cameraPos.x);
        float y1 = (float) (quad.x.y - cameraPos.y);
        float z1 = (float) (quad.x.z - cameraPos.z);

        float x2 = (float) (quad.y.x - cameraPos.x);
        float y2 = (float) (quad.y.y - cameraPos.y);
        float z2 = (float) (quad.y.z - cameraPos.z);

        float x3 = (float) (quad.w.x - cameraPos.x);
        float y3 = (float) (quad.w.y - cameraPos.y);
        float z3 = (float) (quad.w.z - cameraPos.z);

        float x4 = (float) (quad.z.x - cameraPos.x);
        float y4 = (float) (quad.z.y - cameraPos.y);
        float z4 = (float) (quad.z.z - cameraPos.z);

        buffer.vertex(entry, x1, y1, z1).color(quad.color);
        buffer.vertex(entry, x2, y2, z2).color(quad.color);
        buffer.vertex(entry, x3, y3, z3).color(quad.color);
        buffer.vertex(entry, x4, y4, z4).color(quad.color);
    }

    private void renderGradientQuads(MatrixStack matrices, VertexConsumerProvider.Immediate immediate, Vec3d cameraPos) {
        if (GRADIENT_QUAD.isEmpty() && GRADIENT_QUAD_DEPTH.isEmpty()) return;

        VertexConsumer buffer = immediate.getBuffer(RenderLayers.debugFilledBox());

        for (GradientQuad quad : GRADIENT_QUAD) {
            drawGradientQuadVertex(matrices, buffer, quad, cameraPos);
        }
        for (GradientQuad quad : GRADIENT_QUAD_DEPTH) {
            drawGradientQuadVertex(matrices, buffer, quad, cameraPos);
        }

        GRADIENT_QUAD.clear();
        GRADIENT_QUAD_DEPTH.clear();
    }

    private void drawGradientQuadVertex(MatrixStack matrices, VertexConsumer buffer, GradientQuad quad, Vec3d cameraPos) {
        MatrixStack.Entry entry = matrices.peek();

        float x1 = (float) (quad.p1.x - cameraPos.x);
        float y1 = (float) (quad.p1.y - cameraPos.y);
        float z1 = (float) (quad.p1.z - cameraPos.z);

        float x2 = (float) (quad.p2.x - cameraPos.x);
        float y2 = (float) (quad.p2.y - cameraPos.y);
        float z2 = (float) (quad.p2.z - cameraPos.z);

        float x3 = (float) (quad.p3.x - cameraPos.x);
        float y3 = (float) (quad.p3.y - cameraPos.y);
        float z3 = (float) (quad.p3.z - cameraPos.z);

        float x4 = (float) (quad.p4.x - cameraPos.x);
        float y4 = (float) (quad.p4.y - cameraPos.y);
        float z4 = (float) (quad.p4.z - cameraPos.z);

        buffer.vertex(entry, x1, y1, z1).color(quad.c1);
        buffer.vertex(entry, x2, y2, z2).color(quad.c2);
        buffer.vertex(entry, x3, y3, z3).color(quad.c3);
        buffer.vertex(entry, x4, y4, z4).color(quad.c4);
    }

    public void drawCircle(MatrixStack matrix, LivingEntity lastTarget, float anim, float red, int baseColor1, int baseColor2) {
        double cs = MathUtils.interpolate(circleStep - 0.17, circleStep);
        Vec3d target = MathUtils.interpolate(lastTarget);
        boolean canSee = mc.player != null && mc.player.canSee(lastTarget);

        float hitEffect = Math.min(red * 2f, 1f);
        float distanceMultiplier = 1.0f + (float) Math.sin(hitEffect * Math.PI) * 0.18f;

        int size = 64;

        float entityWidth = lastTarget.getWidth() * distanceMultiplier;
        float entityHeight = lastTarget.getHeight();

        double targetY = smoothSinAnimation(cs) * entityHeight;
        double targetY2 = smoothSinAnimation(cs - 0.35) * entityHeight;

        smoothY = lerp(smoothY, targetY, 0.12);
        smoothY2 = lerp(smoothY2, targetY2, 0.10);

        int color1 = ColorUtil.multRed(baseColor1, 1 + red * 125);
        int color2 = ColorUtil.multRed(baseColor2, 1 + red * 125);

        for (int i = 0; i < size; i++) {
            float t = (float) i / size;
            float tNext = (float) ((i + 1) % size) / size;

            float gradientT = (float) (0.5 - 0.5 * Math.cos(t * Math.PI * 2));
            float gradientTNext = (float) (0.5 - 0.5 * Math.cos(tNext * Math.PI * 2));

            int currentColor = ColorUtil.lerpColor(color1, color2, gradientT);
            int nextColor = ColorUtil.lerpColor(color1, color2, gradientTNext);

            int brightColor = ColorUtil.multAlpha(currentColor, 0.8f * anim);
            int brightColorNext = ColorUtil.multAlpha(nextColor, 0.8f * anim);
            int fadeColor = ColorUtil.multAlpha(currentColor, 0f);
            int fadeColorNext = ColorUtil.multAlpha(nextColor, 0f);

            Vec3d cosSin = MathUtils.cosSin(i, size, entityWidth);
            Vec3d nextCosSin = MathUtils.cosSin((i + 1) % size, size, entityWidth);

            Vec3d circlePoint = target.add(cosSin.x, smoothY, cosSin.z);
            Vec3d trailPoint = target.add(cosSin.x, smoothY2, cosSin.z);
            Vec3d nextCirclePoint = target.add(nextCosSin.x, smoothY, nextCosSin.z);
            Vec3d nextTrailPoint = target.add(nextCosSin.x, smoothY2, nextCosSin.z);

            drawGradientQuad(
                    circlePoint,
                    nextCirclePoint,
                    nextTrailPoint,
                    trailPoint,
                    brightColor,
                    brightColorNext,
                    fadeColorNext,
                    fadeColor,
                    canSee
            );

            drawGradientQuad(
                    trailPoint,
                    nextTrailPoint,
                    nextCirclePoint,
                    circlePoint,
                    fadeColor,
                    fadeColorNext,
                    brightColorNext,
                    brightColor,
                    canSee
            );

            int trailColorTop = ColorUtil.multAlpha(currentColor, 0.15f * anim);
            int trailColorBottom = ColorUtil.multAlpha(currentColor, 0f);
            drawLineGradient(circlePoint, trailPoint, trailColorTop, trailColorBottom, 6f, canSee);

            int circleColor = ColorUtil.multAlpha(currentColor, 1f * anim);
            int circleColorNext = ColorUtil.multAlpha(nextColor, 1f * anim);
            drawLineGradient(circlePoint, nextCirclePoint, circleColor, circleColorNext, 2f, canSee);
        }
    }

    public void drawRadiusCircle(Vec3d center, float radius, int color) {
        if (mc.player == null) return;

        double baseY = center.y;
        int fillColor = ColorUtil.multAlpha(color, 0.25f);

        int radiusInt = (int) Math.ceil(radius) + 1;

        for (int dx = -radiusInt; dx <= radiusInt; dx++) {
            for (int dz = -radiusInt; dz <= radiusInt; dz++) {
                boolean hasCornerInside = false;
                boolean hasCornerOutside = false;

                for (double ox = -0.5; ox <= 0.5; ox += 1.0) {
                    for (double oz = -0.5; oz <= 0.5; oz += 1.0) {
                        double cornerDist = Math.sqrt((dx + ox) * (dx + ox) + (dz + oz) * (dz + oz));
                        if (cornerDist <= radius) {
                            hasCornerInside = true;
                        } else {
                            hasCornerOutside = true;
                        }
                    }
                }

                if (hasCornerInside && hasCornerOutside) {
                    double x = center.x + dx;
                    double z = center.z + dz;

                    Box box = new Box(
                            x - 0.5, baseY, z - 0.5,
                            x + 0.5, baseY + 1, z + 0.5
                    );

                    drawBoxWithCross(box, color, fillColor, 2f);
                }
            }
        }
    }

    public void drawBoxWithCross(Box box, int lineColor, int fillColor, float lineWidth) {
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;

        drawQuad(new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), fillColor, false);
        drawQuad(new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), fillColor, false);
        drawQuad(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), fillColor, false);
        drawQuad(new Vec3d(x1, y1, z2), new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), fillColor, false);
        drawQuad(new Vec3d(x1, y1, z1), new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), fillColor, false);
        drawQuad(new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y2, z1), fillColor, false);

        drawLine(new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x1, y1, z2), new Vec3d(x1, y1, z1), lineColor, lineWidth, false);
        drawLine(new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), lineColor, lineWidth, false);
        drawLine(new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), lineColor, lineWidth, false);

        int crossColor = ColorUtil.multAlpha(lineColor, 0.6f);
        float crossWidth = lineWidth * 0.8f;

        drawLine(new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z2), crossColor, crossWidth, false);
        drawLine(new Vec3d(x2, y1, z1), new Vec3d(x1, y1, z2), crossColor, crossWidth, false);

        drawLine(new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z2), crossColor, crossWidth, false);
        drawLine(new Vec3d(x2, y2, z1), new Vec3d(x1, y2, z2), crossColor, crossWidth, false);

        drawLine(new Vec3d(x1, y1, z1), new Vec3d(x2, y2, z1), crossColor, crossWidth, false);
        drawLine(new Vec3d(x2, y1, z1), new Vec3d(x1, y2, z1), crossColor, crossWidth, false);

        drawLine(new Vec3d(x1, y1, z2), new Vec3d(x2, y2, z2), crossColor, crossWidth, false);
        drawLine(new Vec3d(x2, y1, z2), new Vec3d(x1, y2, z2), crossColor, crossWidth, false);

        drawLine(new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z2), crossColor, crossWidth, false);
        drawLine(new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z1), crossColor, crossWidth, false);

        drawLine(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z2), crossColor, crossWidth, false);
        drawLine(new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z1), crossColor, crossWidth, false);
    }

    public void drawBoxWithCrossFull(Box box, int lineColor, int fillColor, float lineWidth) {
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;

        drawQuad(new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), fillColor, false);
        drawQuad(new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), fillColor, false);
        drawQuad(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), fillColor, false);
        drawQuad(new Vec3d(x1, y1, z2), new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), fillColor, false);
        drawQuad(new Vec3d(x1, y1, z1), new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), fillColor, false);
        drawQuad(new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y2, z1), fillColor, false);

        drawQuad(new Vec3d(x1, y1, z2), new Vec3d(x2, y1, z2), new Vec3d(x2, y1, z1), new Vec3d(x1, y1, z1), fillColor, false);
        drawQuad(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), new Vec3d(x1, y2, z1), new Vec3d(x1, y1, z1), fillColor, false);
        drawQuad(new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), fillColor, false);
        drawQuad(new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), fillColor, false);
        drawQuad(new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), new Vec3d(x1, y1, z2), new Vec3d(x1, y1, z1), fillColor, false);
        drawQuad(new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), fillColor, false);

        drawLine(new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x1, y1, z2), new Vec3d(x1, y1, z1), lineColor, lineWidth, false);
        drawLine(new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), lineColor, lineWidth, false);
        drawLine(new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), lineColor, lineWidth, false);
        drawLine(new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), lineColor, lineWidth, false);

        int crossColor = ColorUtil.multAlpha(lineColor, 0.6f);
        float crossWidth = lineWidth * 0.8f;

        drawLine(new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z2), crossColor, crossWidth, false);
        drawLine(new Vec3d(x2, y1, z1), new Vec3d(x1, y1, z2), crossColor, crossWidth, false);

        drawLine(new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z2), crossColor, crossWidth, false);
        drawLine(new Vec3d(x2, y2, z1), new Vec3d(x1, y2, z2), crossColor, crossWidth, false);

        drawLine(new Vec3d(x1, y1, z1), new Vec3d(x2, y2, z1), crossColor, crossWidth, false);
        drawLine(new Vec3d(x2, y1, z1), new Vec3d(x1, y2, z1), crossColor, crossWidth, false);

        drawLine(new Vec3d(x1, y1, z2), new Vec3d(x2, y2, z2), crossColor, crossWidth, false);
        drawLine(new Vec3d(x2, y1, z2), new Vec3d(x1, y2, z2), crossColor, crossWidth, false);

        drawLine(new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z2), crossColor, crossWidth, false);
        drawLine(new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z1), crossColor, crossWidth, false);

        drawLine(new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z2), crossColor, crossWidth, false);
        drawLine(new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z1), crossColor, crossWidth, false);
    }

    public void drawPlastShape(BlockPos playerPos, Vec3d smooth, int lineColor, int fillColor) {
        if (mc.player == null) return;

        float yaw = MathHelper.wrapDegrees(mc.player.getYaw());

        if (Math.abs(mc.player.getPitch()) > 60) {
            BlockPos blockPos = playerPos.up().offset(mc.player.getFacing(), 3);
            Vec3d pos1 = Vec3d.of(blockPos.east(3).south(3).down()).add(smooth);
            Vec3d pos2 = Vec3d.of(blockPos.west(2).north(2).up()).add(smooth);
            drawBoxWithCrossFull(new Box(pos1, pos2), lineColor, fillColor, 3);
        } else if (yaw <= -157.5F || yaw >= 157.5F) {
            BlockPos blockPos = playerPos.north(3).up();
            Vec3d pos1 = Vec3d.of(blockPos.down(2).east(3)).add(smooth);
            Vec3d pos2 = Vec3d.of(blockPos.up(3).west(2).south(2)).add(smooth);
            drawBoxWithCrossFull(new Box(pos1, pos2), lineColor, fillColor, 3);
        } else if (yaw <= -112.5F) {
            drawSidePlast(playerPos.east(5).south().down(), smooth, lineColor, fillColor, -1, true);
        } else if (yaw <= -67.5F) {
            BlockPos blockPos = playerPos.east(2).up();
            Vec3d pos1 = Vec3d.of(blockPos.down(2).south(3)).add(smooth);
            Vec3d pos2 = Vec3d.of(blockPos.up(3).north(2).east(2)).add(smooth);
            drawBoxWithCrossFull(new Box(pos1, pos2), lineColor, fillColor, 3);
        } else if (yaw <= -22.5F) {
            drawSidePlast(playerPos.east(5).down(), smooth, lineColor, fillColor, 1, false);
        } else if (yaw >= -22.5 && yaw <= 22.5) {
            BlockPos blockPos = playerPos.south(2).up();
            Vec3d pos1 = Vec3d.of(blockPos.down(2).east(3)).add(smooth);
            Vec3d pos2 = Vec3d.of(blockPos.up(3).west(2).south(2)).add(smooth);
            drawBoxWithCrossFull(new Box(pos1, pos2), lineColor, fillColor, 3);
        } else if (yaw <= 67.5F) {
            drawSidePlast(playerPos.west(4).down(), smooth, lineColor, fillColor, 1, true);
        } else if (yaw <= 112.5F) {
            BlockPos blockPos = playerPos.west(3).up();
            Vec3d pos1 = Vec3d.of(blockPos.down(2).south(3)).add(smooth);
            Vec3d pos2 = Vec3d.of(blockPos.up(3).north(2).east(2)).add(smooth);
            drawBoxWithCrossFull(new Box(pos1, pos2), lineColor, fillColor, 3);
        } else if (yaw <= 157.5F) {
            drawSidePlast(playerPos.west(4).south().down(), smooth, lineColor, fillColor, -1, false);
        }
    }

    private void drawSidePlast(BlockPos blockPos, Vec3d smooth, int lineColor, int fillColor, int i, boolean ff) {
        Vec3d vec3d = Vec3d.of(blockPos).add(smooth);
        int crossColor = ColorUtil.multAlpha(lineColor, 0.6f);

        List<Vec3d> horizontalPoints = new ArrayList<>();

        float x = ff ? i : -i;
        Vec3d current = vec3d;

        horizontalPoints.add(current);
        current = current.add(x, 0, 0);
        horizontalPoints.add(current);

        for (int f = 0; f < 4; f++) {
            current = current.add(0, 0, i);
            horizontalPoints.add(current);
            current = current.add(x, 0, 0);
            horizontalPoints.add(current);
        }

        current = current.add(0, 0, i);
        horizontalPoints.add(current);
        current = current.add(x * -2, 0, 0);
        horizontalPoints.add(current);

        for (int f = 0; f < 3; f++) {
            current = current.add(0, 0, i * -1);
            horizontalPoints.add(current);
            current = current.add(x * -1, 0, 0);
            horizontalPoints.add(current);
        }

        current = current.add(0, 0, i * -2);
        horizontalPoints.add(current);

        for (int p = 0; p < horizontalPoints.size() - 1; p++) {
            Vec3d p1 = horizontalPoints.get(p);
            Vec3d p2 = horizontalPoints.get(p + 1);
            drawLine(p1, p2, lineColor, 2f, false);
            drawLine(p1.add(0, 5, 0), p2.add(0, 5, 0), lineColor, 2f, false);
        }

        for (Vec3d point : horizontalPoints) {
            drawLine(point, point.add(0, 5, 0), lineColor, 2f, false);
        }

        for (int p = 0; p < horizontalPoints.size() - 1; p++) {
            Vec3d p1 = horizontalPoints.get(p);
            Vec3d p2 = horizontalPoints.get(p + 1);
            Vec3d p1Top = p1.add(0, 5, 0);
            Vec3d p2Top = p2.add(0, 5, 0);

            drawQuad(p1, p2, p2Top, p1Top, fillColor, false);
            drawQuad(p1Top, p2Top, p2, p1, fillColor, false);

            drawLine(p1, p2Top, crossColor, 1.6f, false);
            drawLine(p2, p1Top, crossColor, 1.6f, false);
        }

        current = vec3d;
        drawQuad(current, current.add(x, 0, 0), current.add(x, 0, i * 2), current.add(0, 0, i * 2), fillColor, false);
        drawQuad(current.add(0, 0, i * 2), current.add(x, 0, i * 2), current.add(x, 0, 0), current, fillColor, false);
        drawLine(current, current.add(x, 0, i * 2), crossColor, 1.6f, false);
        drawLine(current.add(x, 0, 0), current.add(0, 0, i * 2), crossColor, 1.6f, false);

        for (int f = 0; f < 3; f++) {
            current = current.add(x, 0, i);
            drawQuad(current, current.add(x, 0, 0), current.add(x, 0, i * 2), current.add(0, 0, i * 2), fillColor, false);
            drawQuad(current.add(0, 0, i * 2), current.add(x, 0, i * 2), current.add(x, 0, 0), current, fillColor, false);
            drawLine(current, current.add(x, 0, i * 2), crossColor, 1.6f, false);
            drawLine(current.add(x, 0, 0), current.add(0, 0, i * 2), crossColor, 1.6f, false);
        }
        current = current.add(x, 0, i);
        drawQuad(current, current.add(x, 0, 0), current.add(x, 0, i), current.add(0, 0, i), fillColor, false);
        drawQuad(current.add(0, 0, i), current.add(x, 0, i), current.add(x, 0, 0), current, fillColor, false);
        drawLine(current, current.add(x, 0, i), crossColor, 1.6f, false);
        drawLine(current.add(x, 0, 0), current.add(0, 0, i), crossColor, 1.6f, false);

        current = vec3d.add(0, 5, 0);
        drawQuad(current, current.add(0, 0, i * 2), current.add(x, 0, i * 2), current.add(x, 0, 0), fillColor, false);
        drawQuad(current.add(x, 0, 0), current.add(x, 0, i * 2), current.add(0, 0, i * 2), current, fillColor, false);
        drawLine(current, current.add(x, 0, i * 2), crossColor, 1.6f, false);
        drawLine(current.add(x, 0, 0), current.add(0, 0, i * 2), crossColor, 1.6f, false);

        for (int f = 0; f < 3; f++) {
            current = current.add(x, 0, i);
            drawQuad(current, current.add(0, 0, i * 2), current.add(x, 0, i * 2), current.add(x, 0, 0), fillColor, false);
            drawQuad(current.add(x, 0, 0), current.add(x, 0, i * 2), current.add(0, 0, i * 2), current, fillColor, false);
            drawLine(current, current.add(x, 0, i * 2), crossColor, 1.6f, false);
            drawLine(current.add(x, 0, 0), current.add(0, 0, i * 2), crossColor, 1.6f, false);
        }
        current = current.add(x, 0, i);
        drawQuad(current, current.add(0, 0, i), current.add(x, 0, i), current.add(x, 0, 0), fillColor, false);
        drawQuad(current.add(x, 0, 0), current.add(x, 0, i), current.add(0, 0, i), current, fillColor, false);
        drawLine(current, current.add(x, 0, i), crossColor, 1.6f, false);
        drawLine(current.add(x, 0, 0), current.add(0, 0, i), crossColor, 1.6f, false);
    }

    private double lerp(double start, double end, double delta) {
        return start + (end - start) * delta;
    }

    public void drawGradientQuad(Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4, int c1, int c2, int c3, int c4, boolean depth) {
        GradientQuad quad = new GradientQuad(p1, p2, p3, p4, c1, c2, c3, c4);
        if (depth) GRADIENT_QUAD_DEPTH.add(quad);
        else GRADIENT_QUAD.add(quad);
    }

    public void drawLineGradient(Vec3d start, Vec3d end, int colorStart, int colorEnd, float width, boolean depth) {
        Line line = new Line(null, start, end, colorStart, colorEnd, width);
        if (depth) LINE_DEPTH.add(line);
        else LINE.add(line);
    }

    public Vector3f getNormal(Vector3f start, Vector3f end) {
        Vector3f normal = new Vector3f(start).sub(end);
        float sqrt = MathHelper.sqrt(normal.lengthSquared());
        if (sqrt < 0.0001f) return new Vector3f(0, 1, 0);
        return normal.div(sqrt);
    }

    public void drawShape(BlockPos blockPos, VoxelShape voxelShape, int color, float width) {
        drawShape(blockPos, voxelShape, color, width, true, false);
    }

    public void drawShape(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
        if (SHAPE_BOXES.containsKey(voxelShape)) {
            SHAPE_BOXES.get(voxelShape).forEach(box -> {
                Box offsetBox = box.offset(blockPos);
                drawBox(offsetBox, color, width, true, fill, depth);
            });
            return;
        }
        SHAPE_BOXES.put(voxelShape, voxelShape.getBoundingBoxes());
    }

    public void drawShapeAlternative(BlockPos blockPos, VoxelShape voxelShape, int color, float width, boolean fill, boolean depth) {
        Vec3d vec3d = Vec3d.of(blockPos);

        if (SHAPE_OUTLINES.containsKey(voxelShape)) {
            Pair<List<Box>, List<Line>> pair = SHAPE_OUTLINES.get(voxelShape);
            if (fill) {
                pair.getLeft().forEach(box -> drawBox(box.offset(vec3d), color, width, false, true, depth));
            }
            pair.getRight().forEach(line -> drawLine(line.start.add(vec3d), line.end.add(vec3d), color, width, depth));
            return;
        }
        List<Line> lines = new ArrayList<>();
        voxelShape.forEachEdge((minX, minY, minZ, maxX, maxY, maxZ) ->
                lines.add(new Line(null, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), 0, 0, 0)));
        SHAPE_OUTLINES.put(voxelShape, new Pair<>(voxelShape.getBoundingBoxes(), lines));
    }

    public void drawBox(Box box, int color, float width) {
        drawBox(box, color, width, true, true, false);
    }

    public void drawBox(Box box, int color, float width, boolean line, boolean fill, boolean depth) {
        drawBox(null, box, color, width, line, fill, depth);
    }

    public void drawBox(MatrixStack.Entry entry, Box box, int color, float width, boolean line, boolean fill, boolean depth) {
        double x1 = box.minX;
        double y1 = box.minY;
        double z1 = box.minZ;
        double x2 = box.maxX;
        double y2 = box.maxY;
        double z2 = box.maxZ;

        if (fill) {
            int fillColor = ColorUtil.multAlpha(color, 0.3f);
            drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x2, y1, z1), new Vec3d(x2, y1, z2), new Vec3d(x1, y1, z2), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y2, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y1, z1), fillColor, depth);
            drawQuad(entry, new Vec3d(x2, y1, z1), new Vec3d(x2, y2, z1), new Vec3d(x2, y2, z2), new Vec3d(x2, y1, z2), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y1, z2), new Vec3d(x2, y1, z2), new Vec3d(x2, y2, z2), new Vec3d(x1, y2, z2), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y1, z1), new Vec3d(x1, y1, z2), new Vec3d(x1, y2, z2), new Vec3d(x1, y2, z1), fillColor, depth);
            drawQuad(entry, new Vec3d(x1, y2, z1), new Vec3d(x1, y2, z2), new Vec3d(x2, y2, z2), new Vec3d(x2, y2, z1), fillColor, depth);
        }

        if (line) {
            drawLine(entry, x1, y1, z1, x2, y1, z1, color, width, depth);
            drawLine(entry, x2, y1, z1, x2, y1, z2, color, width, depth);
            drawLine(entry, x2, y1, z2, x1, y1, z2, color, width, depth);
            drawLine(entry, x1, y1, z2, x1, y1, z1, color, width, depth);
            drawLine(entry, x1, y1, z2, x1, y2, z2, color, width, depth);
            drawLine(entry, x1, y1, z1, x1, y2, z1, color, width, depth);
            drawLine(entry, x2, y1, z2, x2, y2, z2, color, width, depth);
            drawLine(entry, x2, y1, z1, x2, y2, z1, color, width, depth);
            drawLine(entry, x1, y2, z1, x2, y2, z1, color, width, depth);
            drawLine(entry, x2, y2, z1, x2, y2, z2, color, width, depth);
            drawLine(entry, x2, y2, z2, x1, y2, z2, color, width, depth);
            drawLine(entry, x1, y2, z2, x1, y2, z1, color, width, depth);
        }
    }

    public void drawLine(MatrixStack.Entry entry, double minX, double minY, double minZ, double maxX, double maxY, double maxZ, int color, float width, boolean depth) {
        drawLine(entry, new Vec3d(minX, minY, minZ), new Vec3d(maxX, maxY, maxZ), color, color, width, depth);
    }

    public void drawLine(Vec3d start, Vec3d end, int color, float width, boolean depth) {
        drawLine(null, start, end, color, color, width, depth);
    }

    public void drawLine(MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width, boolean depth) {
        Line line = new Line(entry, start, end, colorStart, colorEnd, width);
        if (depth) LINE_DEPTH.add(line);
        else LINE.add(line);
    }

    public void drawQuad(Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color, boolean depth) {
        drawQuad(null, x, y, w, z, color, depth);
    }

    public void drawQuad(MatrixStack.Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color, boolean depth) {
        Quad quad = new Quad(entry, x, y, w, z, color);
        if (depth) QUAD_DEPTH.add(quad);
        else QUAD.add(quad);
    }

    public void resetCircleSmoothing() {
        smoothY = 0;
        smoothY2 = 0;
    }

    public record Line(MatrixStack.Entry entry, Vec3d start, Vec3d end, int colorStart, int colorEnd, float width) {
    }

    public record Quad(MatrixStack.Entry entry, Vec3d x, Vec3d y, Vec3d w, Vec3d z, int color) {
    }

    public record GradientQuad(Vec3d p1, Vec3d p2, Vec3d p3, Vec3d p4, int c1, int c2, int c3, int c4) {
    }
}