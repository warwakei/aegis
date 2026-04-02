package fun.aegis.features.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.aegis.events.player.TickEvent;
import fun.aegis.events.render.WorldRenderEvent;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.features.module.setting.implement.ColorSetting;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.client.managers.event.EventHandler;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WorldParticles extends Module {

    BooleanSetting fireFlies = new BooleanSetting("FireFlies", "Светлячки с шлейфом").setValue(true);
    SliderSettings fireFliesCount = new SliderSettings("FFCount", "Количество светлячков").setValue(30).range(20, 200).visible(fireFlies::isValue);
    SliderSettings fireFliesSize = new SliderSettings("FFSize", "Размер светлячков").setValue(1f).range(0.1f, 2.0f).visible(fireFlies::isValue);
    SliderSettings fireFliesTrailLength = new SliderSettings("FFTrailLength", "Длина шлейфа светлячков").setValue(10).range(5, 30).visible(fireFlies::isValue);

    SelectSetting mode = new SelectSetting("Mode", "Тип частиц")
            .value("Звезды", "Блум", "Сакура", "Луна", "Спарк", "Треугольник", "Куб", "Крест")
            .selected("Звезды");

    SliderSettings count = new SliderSettings("Count", "Количество частиц").setValue(100).range(20, 800).visible(() -> !mode.isSelected("Off"));
    SliderSettings size = new SliderSettings("Size", "Размер частиц").setValue(1f).range(0.1f, 6.0f).visible(() -> !mode.isSelected("Off"));

    SelectSetting colorMode = new SelectSetting("ColorMode", "Режим цвета").value("Sync", "Custom").selected("Sync");
    ColorSetting customColor = new ColorSetting("Color", "Кастомный цвет")
            .value(0xFF37B7DA)
            .visible(() -> colorMode.isSelected("Custom"));

    SelectSetting physics = new SelectSetting("Physics", "Физика частиц")
            .value("Парение", "Падение")
            .selected("Парение")
            .visible(() -> !mode.isSelected("Off"));

    Identifier FIREFLY_TEXTURE = Identifier.of("textures/particles/firefly.png");

    final Identifier STAR_TEXTURE = Identifier.of("textures/new_particles/star.png");
    final Identifier BLOOM_TEXTURE = Identifier.of("textures/new_particles/glow.png");
    final Identifier SAKURA_TEXTURE = Identifier.of("textures/new_particles/feather.png");
    final Identifier MOON_TEXTURE = Identifier.of("textures/new_particles/moon.png");
    final Identifier SPARK_TEXTURE = Identifier.of("textures/new_particles/spark.png");
    final Identifier TRIANGLE_TEXTURE = Identifier.of("textures/new_particles/triangle.png");
    final Identifier CUBE_TEXTURE = Identifier.of("textures/new_particles/cube.png");
    final Identifier MCROSS_TEXTURE = Identifier.of("textures/new_particles/mcross.png");

    ArrayList<ParticleBase> fireFlyParticles = new ArrayList<>();
    ArrayList<ParticleBase> particles = new ArrayList<>();

    Random random = new Random();

    public WorldParticles() {
        super("WorldParticles", "WorldParticles", ModuleCategory.RENDER);
        setup(fireFlies, fireFliesCount, fireFliesSize, fireFliesTrailLength, mode, count, size, colorMode, customColor, physics);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        fireFlyParticles.clear();
        particles.clear();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        fireFlyParticles.removeIf(ParticleBase::tick);
        particles.removeIf(ParticleBase::tick);

        if (fireFlies.isValue()) {
            for (int i = fireFlyParticles.size(); i < fireFliesCount.getInt(); i++) {
                fireFlyParticles.add(new FireFly(
                        (float) (mc.player.getX() + randomFloat(-25f, 25f)),
                        (float) (mc.player.getY() + randomFloat(2f, 15f)),
                        (float) (mc.player.getZ() + randomFloat(-25f, 25f)),
                        randomFloat(-0.2f, 0.2f),
                        randomFloat(-0.1f, 0.1f),
                        randomFloat(-0.2f, 0.2f)
                ));
            }
        }

        if (!mode.isSelected("Off")) {
            boolean drop = physics.isSelected("Падение");
            for (int i = particles.size(); i < count.getInt(); i++) {
                particles.add(new ParticleBase(
                        (float) (mc.player.getX() + randomFloat(-48f, 48f)),
                        (float) (mc.player.getY() + randomFloat(2f, 48f)),
                        (float) (mc.player.getZ() + randomFloat(-48f, 48f)),
                        drop ? 0f : randomFloat(-0.4f, 0.4f),
                        drop ? randomFloat(-0.2f, -0.05f) : randomFloat(-0.1f, 0.1f),
                        drop ? 0f : randomFloat(-0.4f, 0.4f)
                ));
            }
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.player == null || mc.world == null) return;

        MatrixStack stack = e.getStack();
        float tickDelta = e.getPartialTicks();

        if (fireFlies.isValue() && !fireFlyParticles.isEmpty()) {
            stack.push();
            renderParticleList(stack, tickDelta, FIREFLY_TEXTURE, fireFlyParticles);
            stack.pop();
        }

        if (!mode.isSelected("Off") && !particles.isEmpty()) {
            stack.push();
            renderParticleList(stack, tickDelta, getModeTexture(), particles);
            stack.pop();
        }
    }

    private void renderParticleList(MatrixStack stack, float tickDelta, Identifier texture, List<ParticleBase> list) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        BufferBuilder bufferBuilder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        for (ParticleBase p : list) {
            p.render(bufferBuilder, tickDelta);
        }
        BufferRenderer.drawWithGlobalProgram(bufferBuilder.end());

        RenderSystem.depthMask(true);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableDepthTest();
        RenderSystem.disableBlend();
    }

    private Identifier getModeTexture() {
        if (mode.isSelected("Блум")) return BLOOM_TEXTURE;
        if (mode.isSelected("Звезды")) return STAR_TEXTURE;
        if (mode.isSelected("Сакура")) return SAKURA_TEXTURE;
        if (mode.isSelected("Луна")) return MOON_TEXTURE;
        if (mode.isSelected("Спарк")) return SPARK_TEXTURE;
        if (mode.isSelected("Треугольник")) return TRIANGLE_TEXTURE;
        if (mode.isSelected("Куб")) return CUBE_TEXTURE;
        if (mode.isSelected("Крест")) return MCROSS_TEXTURE;

        return STAR_TEXTURE;
    }

    private Color getParticleColor(int index) {
        if (colorMode.isSelected("Sync")) {
            return new Color(ColorAssist.fade(index));
        }
        return new Color(customColor.getColor());
    }

    private float randomFloat(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private class TrailSegment {
        private final Vec3d from;
        private final Vec3d to;
        private final Color color;
        private int ticks;
        private int prevTicks;
        private final int maxTicks;

        private TrailSegment(Vec3d from, Vec3d to, Color color, int maxTicks) {
            this.from = from;
            this.to = to;
            this.color = color;
            this.maxTicks = maxTicks;
            this.ticks = maxTicks;
        }

        private Vec3d interpolate(float pt) {
            double x = from.x + ((to.x - from.x) * pt) - mc.getEntityRenderDispatcher().camera.getPos().getX();
            double y = from.y + ((to.y - from.y) * pt) - mc.getEntityRenderDispatcher().camera.getPos().getY();
            double z = from.z + ((to.z - from.z) * pt) - mc.getEntityRenderDispatcher().camera.getPos().getZ();
            return new Vec3d(x, y, z);
        }

        private double animation(float pt) {
            return (this.prevTicks + (this.ticks - this.prevTicks) * pt) / (double) maxTicks;
        }

        private boolean update() {
            this.prevTicks = this.ticks;
            return this.ticks-- <= 0;
        }
    }

    private class FireFly extends ParticleBase {
        private final List<TrailSegment> trails = new ArrayList<>();

        private FireFly(float posX, float posY, float posZ, float motionX, float motionY, float motionZ) {
            super(posX, posY, posZ, motionX, motionY, motionZ);
        }

        @Override
        public boolean tick() {
            if (mc.player == null || mc.world == null) return true;

            if (mc.player.squaredDistanceTo(posX, posY, posZ) > 100) age -= 4;
            else if (!mc.world.getBlockState(new BlockPos((int) posX, (int) posY, (int) posZ)).isAir()) age -= 8;
            else age--;

            if (age < 0) return true;

            trails.removeIf(TrailSegment::update);

            prevposX = posX;
            prevposY = posY;
            prevposZ = posZ;

            posX += motionX;
            posY += motionY;
            posZ += motionZ;

            trails.add(new TrailSegment(
                    new Vec3d(prevposX, prevposY, prevposZ),
                    new Vec3d(posX, posY, posZ),
                    getParticleColor(age * 10),
                    fireFliesTrailLength.getInt()
            ));

            motionX *= 0.99f;
            motionY *= 0.99f;
            motionZ *= 0.99f;

            return false;
        }

        @Override
        public void render(BufferBuilder bufferBuilder, float tickDelta) {
            if (trails.isEmpty()) return;

            Camera camera = mc.gameRenderer.getCamera();
            float quadSize = fireFliesSize.getValue();

            for (TrailSegment ctx : trails) {
                Vec3d pos = ctx.interpolate(tickDelta);

                MatrixStack matrices = new MatrixStack();
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
                matrices.translate(pos.x, pos.y, pos.z);
                matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
                matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

                Matrix4f matrix = matrices.peek().getPositionMatrix();

                int alpha = (int) (255 * ((float) age / (float) maxAge) * ctx.animation(tickDelta));
                alpha = Math.max(0, Math.min(255, alpha));

                int finalColor = ColorAssist.replAlpha(ctx.color.getRGB(), alpha);

                bufferBuilder.vertex(matrix, 0, -quadSize, 0).texture(0f, 1f).color(finalColor);
                bufferBuilder.vertex(matrix, -quadSize, -quadSize, 0).texture(1f, 1f).color(finalColor);
                bufferBuilder.vertex(matrix, -quadSize, 0, 0).texture(1f, 0).color(finalColor);
                bufferBuilder.vertex(matrix, 0, 0, 0).texture(0, 0).color(finalColor);
            }
        }
    }

    private class ParticleBase {
        protected float prevposX, prevposY, prevposZ, posX, posY, posZ, motionX, motionY, motionZ;
        protected int age, maxAge;

        private ParticleBase(float posX, float posY, float posZ, float motionX, float motionY, float motionZ) {
            this.posX = posX;
            this.posY = posY;
            this.posZ = posZ;
            this.prevposX = posX;
            this.prevposY = posY;
            this.prevposZ = posZ;
            this.motionX = motionX;
            this.motionY = motionY;
            this.motionZ = motionZ;
            this.age = (int) randomFloat(100, 300);
            this.maxAge = age;
        }

        public boolean tick() {
            if (mc.player == null) return true;

            if (mc.player.squaredDistanceTo(posX, posY, posZ) > 4096) age -= 8;
            else age--;

            if (age < 0) return true;

            prevposX = posX;
            prevposY = posY;
            prevposZ = posZ;

            posX += motionX;
            posY += motionY;
            posZ += motionZ;

            motionX *= 0.9f;
            if (physics.isSelected("Парение")) {
                motionY *= 0.9f;
            }
            motionZ *= 0.9f;

            motionY -= 0.001f;

            return false;
        }

        public void render(BufferBuilder bufferBuilder, float tickDelta) {
            Camera camera = mc.gameRenderer.getCamera();

            Color c = getParticleColor(age * 2);

            Vec3d pos = interpolatePos(
                    prevposX, prevposY, prevposZ,
                    posX, posY, posZ,
                    tickDelta
            );

            MatrixStack matrices = new MatrixStack();
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));
            matrices.translate(pos.x, pos.y, pos.z);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));

            Matrix4f matrix = matrices.peek().getPositionMatrix();

            int alpha = (int) (255 * ((float) age / (float) maxAge));
            alpha = Math.max(0, Math.min(255, alpha));

            int finalColor = ColorAssist.replAlpha(c.getRGB(), alpha);
            float quadSize = size.getValue();

            bufferBuilder.vertex(matrix, 0, -quadSize, 0).texture(0f, 1f).color(finalColor);
            bufferBuilder.vertex(matrix, -quadSize, -quadSize, 0).texture(1f, 1f).color(finalColor);
            bufferBuilder.vertex(matrix, -quadSize, 0, 0).texture(1f, 0).color(finalColor);
            bufferBuilder.vertex(matrix, 0, 0, 0).texture(0, 0).color(finalColor);
        }

        private Vec3d interpolatePos(float prevX, float prevY, float prevZ, float x, float y, float z, float tickDelta) {
            double ix = prevX + (x - prevX) * tickDelta;
            double iy = prevY + (y - prevY) * tickDelta;
            double iz = prevZ + (z - prevZ) * tickDelta;

            Vec3d cam = mc.getEntityRenderDispatcher().camera.getPos();
            return new Vec3d(ix - cam.x, iy - cam.y, iz - cam.z);
        }
    }
}
