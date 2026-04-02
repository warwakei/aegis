package rich.modules.impl.render.worldparticles;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import rich.IMinecraft;
import rich.util.ColorUtil;
import rich.util.animations.Animation;
import rich.util.animations.Direction;
import rich.util.animations.EaseInOutQuad;
import rich.util.render.сliemtpipeline.ClientPipelines;

import java.util.concurrent.ThreadLocalRandom;

public class Particle implements IMinecraft {

    public enum ParticleType {
        CUBE_3D,
        CROWN,
        CUBE_BLAST,
        DOLLAR,
        HEART,
        LIGHTNING,
        LINE,
        RHOMBUS,
        SNOWFLAKE,
        STAR,
        STAR_ALT,
        TRIANGLE,
        GLOW,
        RANDOM
    }

    private static final ParticleType[] RANDOM_TYPES = {
            ParticleType.CROWN,
            ParticleType.CUBE_BLAST,
            ParticleType.DOLLAR,
            ParticleType.HEART,
            ParticleType.LIGHTNING,
            ParticleType.LINE,
            ParticleType.RHOMBUS,
            ParticleType.SNOWFLAKE,
            ParticleType.STAR,
            ParticleType.STAR_ALT,
            ParticleType.TRIANGLE,
            ParticleType.GLOW
    };

    private static final Identifier TEXTURE_CROWN = Identifier.of("rich", "textures/world/crown.png");
    private static final Identifier TEXTURE_CUBE_BLAST = Identifier.of("rich", "textures/world/cubeblast1.png");
    private static final Identifier TEXTURE_DOLLAR = Identifier.of("rich", "textures/world/dollar.png");
    private static final Identifier TEXTURE_HEART = Identifier.of("rich", "textures/world/heart.png");
    private static final Identifier TEXTURE_LIGHTNING = Identifier.of("rich", "textures/world/lightning.png");
    private static final Identifier TEXTURE_LINE = Identifier.of("rich", "textures/world/line.png");
    private static final Identifier TEXTURE_RHOMBUS = Identifier.of("rich", "textures/world/rhombus.png");
    private static final Identifier TEXTURE_SNOWFLAKE = Identifier.of("rich", "textures/world/snowflake.png");
    private static final Identifier TEXTURE_STAR = Identifier.of("rich", "textures/world/star.png");
    private static final Identifier TEXTURE_STAR_ALT = Identifier.of("rich", "textures/world/star1.png");
    private static final Identifier TEXTURE_TRIANGLE = Identifier.of("rich", "textures/world/triangle.png");
    private static final Identifier TEXTURE_GLOW = Identifier.of("rich", "textures/world/dashbloom.png");

    private static final Identifier GLOW_BLOOM = Identifier.of("rich", "textures/world/dashbloom.png");
    private static final Identifier GLOW_BLOOM_SAMPLE = Identifier.of("rich", "textures/world/dashbloomsample.png");

    double x, y, z;
    double prevX, prevY, prevZ;
    double mX, mY, mZ;
    long start;
    float phase;
    Animation fadeInAnimation;
    Animation fadeOutAnimation;
    float cachedAlpha = 0.0F;
    long lastAlphaUpdate = 0L;
    boolean fadingOut = false;
    long lifeTimeMs;
    boolean forceFadeOut = false;

    private ParticleType type;
    private ParticleType actualType;
    private Identifier texture;
    private float rotation;
    private float rotationSpeed;
    private int randomColor;
    private long spawnTime;
    private boolean physicsEnabled = true;
    private float size = 0.5f;

    public Particle(double x, double y, double z, long lifeTimeMs) {
        this.start = System.currentTimeMillis();
        this.spawnTime = System.currentTimeMillis();
        this.phase = (float) (Math.random() * 100.0);
        this.x = x;
        this.y = y;
        this.z = z;
        this.prevX = x;
        this.prevY = y;
        this.prevZ = z;
        this.mX = (Math.random() - 0.5) * 0.04;
        this.mY = (Math.random() - 0.5) * 0.04;
        this.mZ = (Math.random() - 0.5) * 0.04;
        this.fadeInAnimation = new EaseInOutQuad().setMs(600).setValue(1.0);
        this.fadeInAnimation.setDirection(Direction.FORWARDS);
        this.fadeOutAnimation = new EaseInOutQuad().setMs(400).setValue(1.0);
        this.fadeOutAnimation.setDirection(Direction.FORWARDS);
        this.lifeTimeMs = lifeTimeMs;
        this.rotation = (float) (Math.random() * 360.0);
        this.rotationSpeed = (float) (Math.random() * 1.5 + 0.5);
        this.randomColor = generateRandomColor();
        this.type = ParticleType.CUBE_3D;
        this.actualType = ParticleType.CUBE_3D;
    }

    public Particle(double x, double y, double z, double mx, double my, double mz, long lifeTimeMs) {
        this(x, y, z, lifeTimeMs);
        this.mX = mx;
        this.mY = my;
        this.mZ = mz;
    }

    private int generateRandomColor() {
        int[] colors = {
                0xFFFF0000, 0xFFFF7F00, 0xFFFFFF00, 0xFF00FF00,
                0xFF00FFFF, 0xFF0000FF, 0xFF8B00FF, 0xFFFF00FF,
                0xFFFF1493, 0xFFFFFFFF, 0xFF00FF7F, 0xFFFF6347
        };
        return colors[ThreadLocalRandom.current().nextInt(colors.length)];
    }

    public Particle setType(ParticleType type) {
        this.type = type;
        if (type == ParticleType.RANDOM) {
            this.actualType = RANDOM_TYPES[ThreadLocalRandom.current().nextInt(RANDOM_TYPES.length)];
        } else {
            this.actualType = type;
        }
        this.texture = getTextureForType(this.actualType);
        return this;
    }

    public Particle setPhysics(boolean enabled) {
        this.physicsEnabled = enabled;
        return this;
    }

    public Particle setSize(float size) {
        this.size = size;
        return this;
    }

    private Identifier getTextureForType(ParticleType type) {
        return switch (type) {
            case CROWN -> TEXTURE_CROWN;
            case CUBE_BLAST -> TEXTURE_CUBE_BLAST;
            case DOLLAR -> TEXTURE_DOLLAR;
            case HEART -> TEXTURE_HEART;
            case LIGHTNING -> TEXTURE_LIGHTNING;
            case LINE -> TEXTURE_LINE;
            case RHOMBUS -> TEXTURE_RHOMBUS;
            case SNOWFLAKE -> TEXTURE_SNOWFLAKE;
            case STAR -> TEXTURE_STAR;
            case STAR_ALT -> TEXTURE_STAR_ALT;
            case TRIANGLE -> TEXTURE_TRIANGLE;
            case GLOW -> TEXTURE_GLOW;
            default -> null;
        };
    }

    public double getDistanceSquaredTo(Vec3d pos) {
        double dx = this.x - pos.x;
        double dy = this.y - pos.y;
        double dz = this.z - pos.z;
        return dx * dx + dy * dy + dz * dz;
    }

    public double getHorizontalDistanceSquaredTo(Vec3d pos) {
        double dx = this.x - pos.x;
        double dz = this.z - pos.z;
        return dx * dx + dz * dz;
    }

    public void startFadeOut() {
        if (!fadingOut) {
            fadingOut = true;
            forceFadeOut = true;
            this.fadeOutAnimation.setDirection(Direction.BACKWARDS);
        }
    }

    public void update(long now) {
        if (mc.world == null) return;

        this.prevX = this.x;
        this.prevY = this.y;
        this.prevZ = this.z;

        double velMagSq = this.mX * this.mX + this.mY * this.mY + this.mZ * this.mZ;
        if (velMagSq > 1.0E-4) {
            if (this.isHit(this.x + this.mX, this.y, this.z)) {
                this.mX *= -0.8;
            } else {
                this.x = this.x + this.mX;
            }
            if (this.isHit(this.x, this.y + this.mY, this.z)) {
                this.mY *= -0.8;
            } else {
                this.y = this.y + this.mY;
            }
            if (this.isHit(this.x, this.y, this.z + this.mZ)) {
                this.mZ *= -0.8;
            } else {
                this.z = this.z + this.mZ;
            }
        } else {
            this.x = this.x + this.mX;
            this.y = this.y + this.mY;
            this.z = this.z + this.mZ;
        }

        this.mX *= 0.99;
        this.mY *= 0.99;
        this.mZ *= 0.99;

        if (physicsEnabled) {
            this.mY -= 0.0002;
        }

        this.rotation += this.rotationSpeed;

        if (!fadingOut && now - this.start > lifeTimeMs) {
            fadingOut = true;
            this.fadeOutAnimation.setDirection(Direction.BACKWARDS);
        }

        if (now - this.lastAlphaUpdate > 16L) {
            if (fadingOut) {
                this.cachedAlpha = this.fadeOutAnimation.getOutput().floatValue();
            } else {
                this.cachedAlpha = this.fadeInAnimation.getOutput().floatValue();
            }
            this.lastAlphaUpdate = now;
        }
    }

    public float getAlpha() {
        return this.cachedAlpha;
    }

    private boolean isHit(double px, double py, double pz) {
        if (mc.world == null) return false;
        BlockPos pos = BlockPos.ofFloored(px, py, pz);
        return mc.world.getBlockState(pos).isFullCube(mc.world, pos);
    }

    public boolean shouldRemove() {
        return fadingOut && this.cachedAlpha <= 0.0F;
    }

    public boolean isFadingOut() {
        return fadingOut;
    }

    public int getColor(int baseColor, boolean useRandomColor, boolean whiteOnSpawn) {
        if (useRandomColor) {
            return ColorUtil.multAlpha(randomColor, cachedAlpha);
        }

        if (whiteOnSpawn) {
            long currentTime = System.currentTimeMillis();
            long transitionDuration = 7000;
            long timeSinceSpawn = currentTime - spawnTime;

            if (timeSinceSpawn < transitionDuration) {
                float progress = (float) timeSinceSpawn / transitionDuration;

                int targetR = (baseColor >> 16) & 0xFF;
                int targetG = (baseColor >> 8) & 0xFF;
                int targetB = baseColor & 0xFF;
                int targetA = (baseColor >> 24) & 0xFF;

                int r = (int) (255 + ((targetR - 255) * progress));
                int g = (int) (255 + ((targetG - 255) * progress));
                int b = (int) (255 + ((targetB - 255) * progress));

                int color = (targetA << 24) | (r << 16) | (g << 8) | b;
                return ColorUtil.multAlpha(color, cachedAlpha);
            }
        }

        return ColorUtil.multAlpha(baseColor, cachedAlpha);
    }

    public void render(MatrixStack matrices, VertexConsumerProvider immediate, Vec3d cameraPos,
                       int baseColor, float globalRotation, float cameraYaw, float cameraPitch,
                       float glowSize, boolean useRandomColor, boolean whiteOnSpawn, boolean whiteCenter) {

        float alpha = this.getAlpha();
        if (alpha <= 0.0F) return;

        float partialTicks = mc.getRenderTickCounter().getTickProgress(true);
        double interpX = prevX + (x - prevX) * partialTicks;
        double interpY = prevY + (y - prevY) * partialTicks;
        double interpZ = prevZ + (z - prevZ) * partialTicks;

        float relX = (float) (interpX - cameraPos.x);
        float relY = (float) (interpY - cameraPos.y);
        float relZ = (float) (interpZ - cameraPos.z);

        int color = getColor(baseColor, useRandomColor, whiteOnSpawn);

        if (actualType == ParticleType.CUBE_3D) {
            renderCube3D(matrices, immediate, relX, relY, relZ, alpha, color, globalRotation, cameraYaw, cameraPitch, glowSize);
        } else {
            renderTextured(matrices, immediate, relX, relY, relZ, alpha, color, cameraYaw, cameraPitch, glowSize, whiteCenter);
        }
    }

    private void renderCube3D(MatrixStack matrices, VertexConsumerProvider immediate,
                              float relX, float relY, float relZ, float alpha, int color,
                              float globalRotation, float cameraYaw, float cameraPitch, float glowSize) {

        float alpha02 = alpha * 0.2F;
        float alpha04 = alpha * 0.4F;
        int glowCol = color;
        float cubeSize = size * 0.5f;
        float cubeGlow1 = cubeSize * glowSize;
        float cubeGlow2 = cubeSize * (glowSize / 3.0f);

        float rotY = globalRotation + this.phase;
        float rotX = globalRotation * 0.5F;

        matrices.push();
        matrices.translate(relX, relY, relZ);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotY));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotX));
        Matrix4f mat = matrices.peek().getPositionMatrix();
        ParticleRenderer.drawCube(immediate.getBuffer(ParticleRenderer.getQuadsLayer()), mat, ColorUtil.multAlpha(color, alpha02), cubeSize);
        ParticleRenderer.drawLines(immediate.getBuffer(ParticleRenderer.getLinesLayer()), mat, ColorUtil.multAlpha(color, alpha04), cubeSize);
        matrices.pop();

        matrices.push();
        matrices.translate(relX, relY, relZ);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-cameraYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cameraPitch));
        Matrix4f gMat = matrices.peek().getPositionMatrix();
        ParticleRenderer.drawGlow(immediate.getBuffer(ParticleRenderer.getGlowLayer()), gMat, glowCol, (int) (80.0F * alpha), cubeGlow1);
        ParticleRenderer.drawGlow(immediate.getBuffer(ParticleRenderer.getGlowLayerSecondary()), gMat, glowCol, (int) (140.0F * alpha), cubeGlow2);
        matrices.pop();
    }

    private void renderTextured(MatrixStack matrices, VertexConsumerProvider immediate,
                                float relX, float relY, float relZ, float alpha, int color,
                                float cameraYaw, float cameraPitch, float glowSize, boolean whiteCenter) {

        if (texture == null) return;

        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;
        int a = (int) (255 * alpha);

        float textureSize = size * 0.5f;

        matrices.push();
        matrices.translate(relX, relY, relZ);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-cameraYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cameraPitch));
        matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));

        Matrix4f mat = matrices.peek().getPositionMatrix();

        RenderLayer layer = ClientPipelines.WORLD_PARTICLES_GLOW.apply(texture);
        VertexConsumer buffer = immediate.getBuffer(layer);

        float half = textureSize / 2.0f;
        buffer.vertex(mat, -half, -half, 0).texture(0, 0).color(r, g, b, a);
        buffer.vertex(mat, -half, half, 0).texture(0, 1).color(r, g, b, a);
        buffer.vertex(mat, half, half, 0).texture(1, 1).color(r, g, b, a);
        buffer.vertex(mat, half, -half, 0).texture(1, 0).color(r, g, b, a);

        if (whiteCenter) {
            float centerSize = half * 0.5f;
            int whiteA = (int) (200 * alpha);
            buffer.vertex(mat, -centerSize, -centerSize, 0.001f).texture(0, 0).color(255, 255, 255, whiteA);
            buffer.vertex(mat, -centerSize, centerSize, 0.001f).texture(0, 1).color(255, 255, 255, whiteA);
            buffer.vertex(mat, centerSize, centerSize, 0.001f).texture(1, 1).color(255, 255, 255, whiteA);
            buffer.vertex(mat, centerSize, -centerSize, 0.001f).texture(1, 0).color(255, 255, 255, whiteA);
        }

        matrices.pop();

        matrices.push();
        matrices.translate(relX, relY, relZ);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-cameraYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cameraPitch));
        Matrix4f gMat = matrices.peek().getPositionMatrix();

        float glowSizePrimary = textureSize * glowSize * 0.5f;
        float glowSizeSecondary = textureSize * glowSize * 0.2f;

        RenderLayer layerBloom = ClientPipelines.WORLD_PARTICLES_GLOW.apply(GLOW_BLOOM);
        RenderLayer layerSample = ClientPipelines.WORLD_PARTICLES_GLOW.apply(GLOW_BLOOM_SAMPLE);
        ParticleRenderer.drawGlow(immediate.getBuffer(layerBloom), gMat, color, (int) (80.0F * alpha), glowSizePrimary);
        ParticleRenderer.drawGlow(immediate.getBuffer(layerSample), gMat, color, (int) (140.0F * alpha), glowSizeSecondary);

        matrices.pop();
    }
}