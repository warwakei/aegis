package rich.modules.impl.render;

import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import rich.IMinecraft;
import rich.events.api.EventHandler;
import rich.events.impl.WorldRenderEvent;
import rich.modules.impl.combat.Aura;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.ColorSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.animations.Animation;
import rich.util.animations.Direction;
import rich.util.animations.OutBack;
import rich.util.render.сliemtpipeline.ClientPipelines;
import rich.util.render.Render3D;
import rich.util.timer.StopWatch;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class TargetESP extends ModuleStructure implements IMinecraft {

    private static TargetESP instance;

    public static TargetESP getInstance() {
        return instance;
    }

    Animation espAnim = new OutBack().setMs(300).setValue(1);
    StopWatch stopWatch = new StopWatch();

    SelectSetting mode = new SelectSetting("Режим", "Тип TargetESP")
            .value("Rhomb", "Ghost", "Chain", "Crystals", "Circle")
            .selected("Rhomb");

    SliderSettings crystalRotationSpeed = new SliderSettings("Скорость вращения кристаллов",
            "Скорость вращения кристаллов")
            .range(0.1f, 2.0f)
            .visible(() -> mode.isSelected("Crystals"));

    ColorSetting color1 = new ColorSetting("Цвет 1", "Первый цвет градиента")
            .setColor(new Color(255, 101, 57, 255).getRGB());

    ColorSetting color2 = new ColorSetting("Цвет 2", "Второй цвет градиента")
            .setColor(new Color(255, 50, 150, 255).getRGB());

    ColorSetting color3 = new ColorSetting("Цвет 3", "Третий цвет для Ghost")
            .setColor(new Color(150, 50, 255, 255).getRGB())
            .visible(() -> mode.isSelected("Ghost"));

    private Vec3d smoothedPos = null;
    private LivingEntity lastTarget = null;
    private float movingValue = 0;
    private float hurtProgress = 0;

    private Entity lastRenderedTarget = null;
    private final List<Crystal> crystalList = new ArrayList<>();
    private float rotationAngle = 0;

    private long lastFrameTime = System.currentTimeMillis();
    private static final float TARGET_FPS = 60f;
    private static final float TARGET_FRAME_TIME = 1000f / TARGET_FPS;

    public TargetESP() {
        super("TargetEsp", "Target Esp", ModuleCategory.RENDER);
        instance = this;

        crystalRotationSpeed.setValue(0.5f);

        settings(mode, crystalRotationSpeed, color1, color2, color3);
    }

    private float getDeltaTime() {
        long currentTime = System.currentTimeMillis();
        float deltaMs = currentTime - lastFrameTime;
        lastFrameTime = currentTime;
        deltaMs = Math.max(1f, Math.min(deltaMs, 100f));
        return deltaMs / TARGET_FRAME_TIME;
    }

    @EventHandler
    public void onRender3D(WorldRenderEvent e) {
        float deltaTime = getDeltaTime();

        LivingEntity target = null;

        if (Aura.getInstance() != null && Aura.getInstance().isState()) {
            target = Aura.target;
        }

        if (target == null) {
            smoothedPos = null;
            lastTarget = null;
            espAnim.setDirection(Direction.BACKWARDS);
            Render3D.resetCircleSmoothing();
            return;
        }

        espAnim.setDirection(Direction.FORWARDS);
        float alpha = espAnim.getOutput().floatValue();
        if (alpha <= 0.01f)
            return;

        movingValue += 2f * deltaTime;
        if (movingValue > 360000)
            movingValue = 0;

        float hurtDecay = 0.1f * deltaTime;
        hurtProgress = target.hurtTime > 0 ? (float) target.hurtTime / 10f : Math.max(0, hurtProgress - hurtDecay);

        Render3D.updateTargetEsp(deltaTime);

        if (mode.isSelected("Circle")) {
            renderCircle(e.getStack(), target, alpha);
            return;
        }

        MatrixStack stack = e.getStack();
        VertexConsumerProvider.Immediate provider = mc.getBufferBuilders().getEntityVertexConsumers();

        Vec3d camPos = mc.gameRenderer.getCamera().getCameraPos();
        float partialTicks = e.getPartialTicks();
        Vec3d targetPos = target.getLerpedPos(partialTicks);

        if (lastTarget != target || smoothedPos == null) {
            smoothedPos = targetPos;
            lastTarget = target;
        } else {
            float smoothingFactor = Math.min(1.0f, partialTicks * 1.5f);
            double dx = targetPos.x - smoothedPos.x;
            double dy = targetPos.y - smoothedPos.y;
            double dz = targetPos.z - smoothedPos.z;
            smoothedPos = new Vec3d(
                    smoothedPos.x + dx * smoothingFactor,
                    smoothedPos.y + dy * smoothingFactor,
                    smoothedPos.z + dz * smoothingFactor);
        }

        stack.push();
        stack.translate(
                smoothedPos.x - camPos.x,
                smoothedPos.y - camPos.y,
                smoothedPos.z - camPos.z);

        if (mode.isSelected("Rhomb")) {
            renderRhomb(stack, provider, target, alpha);
        } else if (mode.isSelected("Ghost")) {
            renderGhost(stack, provider, target, alpha);
        } else if (mode.isSelected("Chain")) {
            renderChain(stack, provider, target, alpha, deltaTime);
        } else if (mode.isSelected("Crystals")) {
            if (crystalList.isEmpty() || lastRenderedTarget != target) {
                createCrystals(target);
                lastRenderedTarget = target;
            }
            renderCrystals(stack, provider, target, alpha, deltaTime);
        }

        provider.draw();

        stack.pop();
    }

    private void renderCircle(MatrixStack stack, LivingEntity target, float alpha) {
        int baseColor1 = color1.getColor();
        int baseColor2 = color2.getColor();

        if (hurtProgress > 0) {
            baseColor1 = lerpColor(baseColor1, 0xFFFF0000, hurtProgress);
            baseColor2 = lerpColor(baseColor2, 0xFFFF0000, hurtProgress);
        }

        Render3D.drawCircle(stack, target, alpha, hurtProgress, baseColor1, baseColor2);
    }

    private void renderChain(MatrixStack stack, VertexConsumerProvider provider, LivingEntity target, float alpha,
            float deltaTime) {
        VertexConsumer consumer = provider
                .getBuffer(ClientPipelines.CHAIN_ESP.apply(Identifier.of("rich", "images/world/chain.png")));

        float animValue = (System.currentTimeMillis() % 360000) / 1000f * 60f;

        float gradusX = (float) (20 * Math.min(1 + Math.sin(Math.toRadians(animValue)), 1));
        float gradusZ = (float) (20 * (Math.min(1 + Math.sin(Math.toRadians(animValue)), 2) - 1));
        float width = target.getWidth() * 3;

        int linksStep = 18;
        int totalAngle = 360 * 2;
        float chainSizeVal = 8;
        float down = 1.5f;
        float chainScale = 0.5f;

        int alphaVal = MathHelper.clamp((int) (alpha * 128), 0, 128);

        int baseColor1 = color1.getColor();
        int baseColor2 = color2.getColor();

        if (hurtProgress > 0) {
            baseColor1 = lerpColor(baseColor1, 0xFFFF0000, hurtProgress);
            baseColor2 = lerpColor(baseColor2, 0xFFFF0000, hurtProgress);
        }

        int c1 = withAlpha(baseColor1, alphaVal);
        int c2 = withAlpha(baseColor2, alphaVal);

        float rotationValue = (System.currentTimeMillis() % 720000) / 1000f * 30f;

        for (int chain = 0; chain < 2; chain++) {
            float val = 1.2f - 0.5f * (chain == 0 ? 1.0f : 0.9f);

            stack.push();
            stack.translate(0, target.getHeight() / 2.0f, 0);
            stack.scale(chainScale, chainScale, chainScale);

            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(chain == 0 ? gradusX : -gradusX));
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(chain == 0 ? gradusZ : -gradusZ));

            float x = 0, y = -0.5f, z = 0;
            Matrix4f matrix = stack.peek().getPositionMatrix();

            int modif = linksStep / 2;
            for (int i = 0; i < totalAngle; i += modif) {
                float offsetX = (chain == 0 ? gradusX : -gradusX) / 100F;
                float offsetZ = (chain == 0 ? -gradusZ : gradusZ) / 100F;

                float prevSin = (float) (x + offsetX
                        + Math.sin(Math.toRadians(i - modif + rotationValue)) * width * val);
                float prevCos = (float) (z + offsetZ
                        + Math.cos(Math.toRadians(i - modif + rotationValue)) * width * val);

                float sin = (float) (x + offsetX + Math.sin(Math.toRadians(i + rotationValue)) * width * val);
                float cos = (float) (z + offsetZ + Math.cos(Math.toRadians(i + rotationValue)) * width * val);

                float u0 = 1f / 360f * (float) (i - modif) * chainSizeVal;
                float u1 = 1f / 360f * (float) i * chainSizeVal;

                consumer.vertex(matrix, prevSin, y, prevCos).texture(u0, 0).color(c1);
                consumer.vertex(matrix, sin, y, cos).texture(u1, 0).color(c1);
                consumer.vertex(matrix, sin, y + down, cos).texture(u1, 0.99f).color(c2);
                consumer.vertex(matrix, prevSin, y + down, prevCos).texture(u0, 0.99f).color(c2);
            }

            stack.pop();
        }
    }

    private void renderRhomb(MatrixStack stack, VertexConsumerProvider provider, LivingEntity target, float alpha) {
        VertexConsumer consumer = provider
                .getBuffer(ClientPipelines.ROMB_ESP.apply(Identifier.of("rich", "images/world/cube.png")));

        Quaternionf camRot = mc.gameRenderer.getCamera().getRotation();

        stack.translate(0, target.getHeight() / 2f, 0);
        stack.multiply(camRot);

        float timeRotation = (System.currentTimeMillis() % 6283) / 1000f;
        stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) Math.sin(timeRotation) * 360));

        float size = 0.5f;
        stack.scale(size, size, 1);

        int c1 = withAlpha(color1.getColor(), (int) (255 * alpha));
        int c2 = withAlpha(color2.getColor(), (int) (255 * alpha));

        Vector3f[] quad = {
                new Vector3f(-1, -1, 0),
                new Vector3f(-1, 1, 0),
                new Vector3f(1, 1, 0),
                new Vector3f(1, -1, 0)
        };

        var m = stack.peek();
        consumer.vertex(m, quad[0].x, quad[0].y, 0).texture(0, 0).color(c2);
        consumer.vertex(m, quad[1].x, quad[1].y, 0).texture(0, 1).color(c1);
        consumer.vertex(m, quad[2].x, quad[2].y, 0).texture(1, 1).color(c2);
        consumer.vertex(m, quad[3].x, quad[3].y, 0).texture(1, 0).color(c1);
    }

    private void renderGhost(MatrixStack stack, VertexConsumerProvider consumers, LivingEntity target, float alpha) {
        VertexConsumer consumer = consumers
                .getBuffer(ClientPipelines.GHOSTS_ESP.apply(Identifier.of("rich", "images/particle/ghost-glow.png")));

        stack.translate(0, target.getHeight() * 0.5f, 0);

        particle(stack, consumer, (sin, cos) -> new Vec3d(sin, cos, -cos), alpha, 0);
        particle(stack, consumer, (sin, cos) -> new Vec3d(-sin, sin, -cos), alpha, 1);
        particle(stack, consumer, (sin, cos) -> new Vec3d(-sin, -sin, cos), alpha, 2);
    }

    private void particle(MatrixStack stack, VertexConsumer consumer, Transformation transformation, float alpha,
            int colorIndex) {
        double radius = 0.7f;
        double distance = 11;

        float particleSize = 0.5f;
        int alphaFactor = 15;

        long elapsed = System.currentTimeMillis();

        int baseColor;
        switch (colorIndex) {
            case 0 -> baseColor = color1.getColor();
            case 1 -> baseColor = color2.getColor();
            default -> baseColor = color3.getColor();
        }

        for (int i = 0; i < 40 * alpha; i++) {
            stack.push();

            double angle = 0.15 * ((elapsed * 0.5) - (i * distance)) / 30.0;

            double sin = Math.sin(angle) * radius;
            double cos = Math.cos(angle) * radius;

            Vec3d trans = transformation.make(sin, cos);
            stack.translate(trans.x, trans.y, trans.z);

            stack.multiply(mc.gameRenderer.getCamera().getRotation());

            float spinRotation = (elapsed * 0.1f) - (i * 10f);
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(spinRotation));

            stack.translate(particleSize / 2f, particleSize / 2f, 0);

            float x = (float) i / (float) 40;
            int lerpedColor = lerpColor(baseColor, getNextColor(colorIndex), x);

            int c1 = withAlpha(lerpedColor, (int) ((255 - i * alphaFactor) * alpha));
            int c2 = withAlpha(lerpedColor, (int) ((255 - i * alphaFactor) * alpha));

            var m = stack.peek();
            consumer.vertex(m, 0, -particleSize, 0).texture(0, 0).color(c2);
            consumer.vertex(m, -particleSize, -particleSize, 0).texture(0, 1).color(c1);
            consumer.vertex(m, -particleSize, 0, 0).texture(1, 1).color(c2);
            consumer.vertex(m, 0, 0, 0).texture(1, 0).color(c1);

            stack.pop();
        }
    }

    private void createCrystals(Entity target) {
        crystalList.clear();
        crystalList.add(new Crystal(new Vec3d(0, 0.85, 0.8), new Vec3d(-49, 0, 40)));
        crystalList.add(new Crystal(new Vec3d(0.2, 0.85, -0.675), new Vec3d(35, 0, -30)));
        crystalList.add(new Crystal(new Vec3d(0.6, 1.35, 0.6), new Vec3d(-30, 0, 35)));
        crystalList.add(new Crystal(new Vec3d(-0.74, 1.05, 0.4), new Vec3d(-25, 0, -30)));
        crystalList.add(new Crystal(new Vec3d(0.74, 0.95, -0.4), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(new Vec3d(-0.475, 0.85, -0.375), new Vec3d(30, 0, -25)));
        crystalList.add(new Crystal(new Vec3d(0, 1.35, -0.6), new Vec3d(45, 0, 0)));
        crystalList.add(new Crystal(new Vec3d(0.85, 0.7, 0.1), new Vec3d(-30, 0, 30)));
        crystalList.add(new Crystal(new Vec3d(-0.7, 1.35, -0.3), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(new Vec3d(-0.3, 1.35, 0.55), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(new Vec3d(-0.5, 0.7, 0.7), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(new Vec3d(0.5, 0.7, 0.7), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(new Vec3d(-0.7, 0.75, 0), new Vec3d(0, 0, 0)));
        crystalList.add(new Crystal(new Vec3d(-0.2, 0.65, -0.7), new Vec3d(0, 0, 0)));
    }

    private void renderCrystals(MatrixStack stack, VertexConsumerProvider provider, LivingEntity target, float alpha,
            float deltaTime) {
        if (target == null || crystalList.isEmpty()) {
            return;
        }

        rotationAngle += crystalRotationSpeed.getValue() * deltaTime;
        rotationAngle = rotationAngle % 360;

        stack.push();
        stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotationAngle));

        int baseColor = color1.getColor();
        if (hurtProgress > 0) {
            baseColor = lerpColor(baseColor, 0xFFFF0000, hurtProgress);
        }

        for (Crystal crystal : crystalList) {
            crystal.render(stack, provider, alpha, baseColor);
        }

        stack.pop();
    }

    private class Crystal {
        private final Vec3d position;
        private final Vec3d rotation;
        private final float rotationSpeed;

        public Crystal(Vec3d position, Vec3d rotation) {
            this.position = position;
            this.rotation = rotation;
            this.rotationSpeed = 0.5f + (float) (Math.random() * 1.5f);
        }

        public void render(MatrixStack stack, VertexConsumerProvider provider, float alpha, int baseColor) {
            stack.push();
            stack.translate(position.x, position.y, position.z);

            float timeSeconds = (System.currentTimeMillis() % 31416) / 1000f;
            float pulsation = 1.0f + (float) (Math.sin(timeSeconds * 2f) * 0.1f);
            stack.scale(pulsation, pulsation, pulsation);

            float selfRotation = (System.currentTimeMillis() % 36000) / 100.0f * rotationSpeed;
            stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees((float) rotation.x));
            stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees((float) rotation.y + selfRotation));
            stack.multiply(RotationAxis.POSITIVE_Z.rotationDegrees((float) rotation.z));

            float userAlpha = 0.3f;

            VertexConsumer filledConsumer = provider.getBuffer(ClientPipelines.CRYSTAL_FILLED);
            drawFilledCrystal(stack, filledConsumer, baseColor, userAlpha * 0.85f, alpha);

            VertexConsumer glowConsumer = provider.getBuffer(ClientPipelines.CRYSTAL_GLOW);
            stack.push();
            stack.scale(1.15f, 1.15f, 1.15f);
            drawFilledCrystal(stack, glowConsumer, baseColor, userAlpha * 0.25f, alpha);
            stack.pop();

            stack.push();
            stack.scale(1.3f, 1.3f, 1.3f);
            drawFilledCrystal(stack, glowConsumer, baseColor, userAlpha * 0.1f, alpha);
            stack.pop();

            drawBloomEffect(stack, provider, baseColor, alpha);

            stack.pop();
        }

        private void drawFilledCrystal(MatrixStack stack, VertexConsumer consumer, int baseColor, float alphaMultiplier,
                float anim) {
            float s = 0.05f;
            float h_prism = s * 1.0f;
            float h_pyramid = s * 1.5f;
            int numSides = 8;

            List<Vector3f> topVertices = new ArrayList<>();
            List<Vector3f> bottomVertices = new ArrayList<>();

            for (int i = 0; i < numSides; i++) {
                float angle = (float) (2 * Math.PI * i / numSides);
                float x = (float) (s * Math.cos(angle));
                float z = (float) (s * Math.sin(angle));
                topVertices.add(new Vector3f(x, h_prism / 2, z));
                bottomVertices.add(new Vector3f(x, -h_prism / 2, z));
            }

            Vector3f vTop = new Vector3f(0, h_prism / 2 + h_pyramid, 0);
            Vector3f vBottom = new Vector3f(0, -h_prism / 2 - h_pyramid, 0);

            int finalAlpha = (int) (alphaMultiplier * 255 * anim);
            int finalColor = withAlpha(baseColor, finalAlpha);
            int darkerColor = withAlpha(darkenColor(baseColor, 0.7f), finalAlpha);
            int lighterColor = withAlpha(lightenColor(baseColor, 1.2f), finalAlpha);

            Matrix4f matrix = stack.peek().getPositionMatrix();

            for (int i = 0; i < numSides; i++) {
                Vector3f v1 = bottomVertices.get(i);
                Vector3f v2 = bottomVertices.get((i + 1) % numSides);
                Vector3f v3 = topVertices.get((i + 1) % numSides);
                Vector3f v4 = topVertices.get(i);

                int sideColor = (i % 2 == 0) ? finalColor : darkerColor;
                drawQuadFilled(matrix, consumer, v1, v2, v3, v4, sideColor);
            }

            for (int i = 0; i < numSides; i++) {
                Vector3f v1 = topVertices.get(i);
                Vector3f v2 = topVertices.get((i + 1) % numSides);

                int pyramidColor = (i % 2 == 0) ? lighterColor : finalColor;
                drawTriangleFilled(matrix, consumer, vTop, v2, v1, pyramidColor);
            }

            for (int i = 0; i < numSides; i++) {
                Vector3f v1 = bottomVertices.get(i);
                Vector3f v2 = bottomVertices.get((i + 1) % numSides);

                int pyramidColor = (i % 2 == 0) ? darkerColor : finalColor;
                drawTriangleFilled(matrix, consumer, vBottom, v1, v2, pyramidColor);
            }
        }

        private void drawTriangleFilled(Matrix4f matrix, VertexConsumer consumer, Vector3f v1, Vector3f v2, Vector3f v3,
                int color) {
            consumer.vertex(matrix, v1.x, v1.y, v1.z).color(color);
            consumer.vertex(matrix, v2.x, v2.y, v2.z).color(color);
            consumer.vertex(matrix, v3.x, v3.y, v3.z).color(color);
            consumer.vertex(matrix, v3.x, v3.y, v3.z).color(color);
        }

        private void drawQuadFilled(Matrix4f matrix, VertexConsumer consumer, Vector3f v1, Vector3f v2, Vector3f v3,
                Vector3f v4, int color) {
            consumer.vertex(matrix, v1.x, v1.y, v1.z).color(color);
            consumer.vertex(matrix, v2.x, v2.y, v2.z).color(color);
            consumer.vertex(matrix, v3.x, v3.y, v3.z).color(color);
            consumer.vertex(matrix, v4.x, v4.y, v4.z).color(color);
        }

        private void drawBloomEffect(MatrixStack stack, VertexConsumerProvider provider, int baseColor, float anim) {
            VertexConsumer bloomConsumer = provider
                    .getBuffer(ClientPipelines.BLOOM_ESP.apply(Identifier.of("rich", "images/particle/glow.png")));

            int bloomAlpha = (int) (0.3f * 60 * anim);
            int bloomColor = withAlpha(baseColor, bloomAlpha);
            float bloomSize = 0.05f * 15.0f;

            Quaternionf camRot = mc.gameRenderer.getCamera().getRotation();
            int segments = 6;

            for (int i = 0; i < segments; i++) {
                stack.push();
                float angle = (360.0f / segments) * i;
                stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
                stack.multiply(camRot);

                Matrix4f matrix = stack.peek().getPositionMatrix();

                bloomConsumer.vertex(matrix, -bloomSize / 2, -bloomSize / 2, 0).texture(0, 1).color(bloomColor);
                bloomConsumer.vertex(matrix, bloomSize / 2, -bloomSize / 2, 0).texture(1, 1).color(bloomColor);
                bloomConsumer.vertex(matrix, bloomSize / 2, bloomSize / 2, 0).texture(1, 0).color(bloomColor);
                bloomConsumer.vertex(matrix, -bloomSize / 2, bloomSize / 2, 0).texture(0, 0).color(bloomColor);

                stack.pop();
            }

            for (int i = 0; i < segments; i++) {
                stack.push();
                float angle = (360.0f / segments) * i;
                stack.multiply(RotationAxis.POSITIVE_X.rotationDegrees(90));
                stack.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(angle));
                stack.multiply(camRot);

                Matrix4f matrix = stack.peek().getPositionMatrix();

                bloomConsumer.vertex(matrix, -bloomSize / 2, -bloomSize / 2, 0).texture(0, 1).color(bloomColor);
                bloomConsumer.vertex(matrix, bloomSize / 2, -bloomSize / 2, 0).texture(1, 1).color(bloomColor);
                bloomConsumer.vertex(matrix, bloomSize / 2, bloomSize / 2, 0).texture(1, 0).color(bloomColor);
                bloomConsumer.vertex(matrix, -bloomSize / 2, bloomSize / 2, 0).texture(0, 0).color(bloomColor);

                stack.pop();
            }
        }
    }

    private int darkenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (int) (((color >> 16) & 0xFF) * factor);
        int g = (int) (((color >> 8) & 0xFF) * factor);
        int b = (int) ((color & 0xFF) * factor);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int lightenColor(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, (int) (((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int) (((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int) ((color & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int getNextColor(int colorIndex) {
        return switch (colorIndex) {
            case 0 -> color2.getColor();
            case 1 -> color3.getColor();
            default -> color1.getColor();
        };
    }

    private int lerpColor(int c1, int c2, float t) {
        int a1 = (c1 >> 24) & 0xFF;
        int r1 = (c1 >> 16) & 0xFF;
        int g1 = (c1 >> 8) & 0xFF;
        int b1 = c1 & 0xFF;

        int a2 = (c2 >> 24) & 0xFF;
        int r2 = (c2 >> 16) & 0xFF;
        int g2 = (c2 >> 8) & 0xFF;
        int b2 = c2 & 0xFF;

        int a = (int) (a1 + (a2 - a1) * t);
        int r = (int) (r1 + (r2 - r1) * t);
        int g = (int) (g1 + (g2 - g1) * t);
        int b = (int) (b1 + (b2 - b1) * t);

        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int withAlpha(int color, int alpha) {
        alpha = Math.max(0, Math.min(255, alpha));
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    @FunctionalInterface
    private interface Transformation {
        Vec3d make(double sin, double cos);
    }
}