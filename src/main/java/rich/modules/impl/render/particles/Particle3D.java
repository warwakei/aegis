package rich.modules.impl.render.particles;

import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import rich.IMinecraft;
import rich.modules.impl.render.worldparticles.ParticleRenderer;
import rich.util.ColorUtil;
import rich.util.animations.Animation;
import rich.util.animations.Direction;
import rich.util.animations.EaseInOutQuad;
import rich.util.render.сliemtpipeline.ClientPipelines;

import java.util.concurrent.ThreadLocalRandom;

public class Particle3D implements IMinecraft {

    public enum ParticleMode {
        CUBES,
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
        RANDOM
    }

    public enum GlowMode {
        BLOOM,
        BLOOM_SAMPLE,
        BOTH
    }

    private static final ParticleMode[] RANDOM_MODES = {
            ParticleMode.CUBES,
            ParticleMode.CROWN,
            ParticleMode.CUBE_BLAST,
            ParticleMode.DOLLAR,
            ParticleMode.HEART,
            ParticleMode.LIGHTNING,
            ParticleMode.LINE,
            ParticleMode.RHOMBUS,
            ParticleMode.SNOWFLAKE,
            ParticleMode.STAR,
            ParticleMode.STAR_ALT,
            ParticleMode.TRIANGLE
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

    private static final Identifier GLOW_BLOOM = Identifier.of("rich", "textures/world/dashbloom.png");
    private static final Identifier GLOW_BLOOM_SAMPLE = Identifier.of("rich", "textures/world/dashbloomsample.png");

    private double x, y, z;
    private double lastX, lastY, lastZ;
    private double velocityX, velocityY, velocityZ;
    private long start;
    private float phase;
    private int color;
    private float scale;
    private long lifeTimeMs;
    private float rotation;

    private Animation fadeInAnimation;
    private Animation fadeOutAnimation;
    private float cachedAlpha = 0.0F;
    private long lastAlphaUpdate = 0L;
    private boolean fadingOut = false;

    private float gravityStrength = 0.04f;
    private float velocityMultiplier = 0.98f;
    private boolean collidesWithWorld = true;

    private ParticleMode mode = ParticleMode.CUBES;
    private ParticleMode actualMode = ParticleMode.CUBES;
    private GlowMode glowMode = GlowMode.BOTH;
    private boolean spinning = true;

    public Particle3D(Vec3d pos, Vec3d velocity, int color, float scale, float maxAgeSeconds) {
        this.start = System.currentTimeMillis();
        this.phase = (float) (Math.random() * 100.0);
        this.rotation = (float) (Math.random() * 360.0);
        this.x = pos.x;
        this.y = pos.y;
        this.z = pos.z;
        this.lastX = pos.x;
        this.lastY = pos.y;
        this.lastZ = pos.z;
        this.velocityX = velocity.x;
        this.velocityY = velocity.y;
        this.velocityZ = velocity.z;
        this.color = color;
        this.scale = scale;
        this.lifeTimeMs = (long) (maxAgeSeconds * 1000);

        this.fadeInAnimation = new EaseInOutQuad().setMs(150).setValue(1.0);
        this.fadeInAnimation.setDirection(Direction.FORWARDS);
        this.fadeOutAnimation = new EaseInOutQuad().setMs(250).setValue(1.0);
        this.fadeOutAnimation.setDirection(Direction.FORWARDS);
    }

    public Particle3D setGravity(float gravity) {
        this.gravityStrength = gravity;
        return this;
    }

    public Particle3D setVelocityMultiplier(float mult) {
        this.velocityMultiplier = mult;
        return this;
    }

    public Particle3D setCollision(boolean collision) {
        this.collidesWithWorld = collision;
        return this;
    }

    public Particle3D setMode(ParticleMode mode) {
        this.mode = mode;
        if (mode == ParticleMode.RANDOM) {
            this.actualMode = RANDOM_MODES[ThreadLocalRandom.current().nextInt(RANDOM_MODES.length)];
        } else {
            this.actualMode = mode;
        }
        return this;
    }

    public Particle3D setGlowMode(GlowMode glowMode) {
        this.glowMode = glowMode;
        return this;
    }

    public Particle3D setSpinning(boolean spinning) {
        this.spinning = spinning;
        return this;
    }

    public void update() {
        long now = System.currentTimeMillis();

        this.lastX = this.x;
        this.lastY = this.y;
        this.lastZ = this.z;

        this.velocityY -= gravityStrength;

        if (collidesWithWorld && mc.world != null) {
            if (this.isHit(this.x + this.velocityX, this.y, this.z)) {
                this.velocityX *= -0.8;
            } else {
                this.x += this.velocityX;
            }
            if (this.isHit(this.x, this.y + this.velocityY, this.z)) {
                this.velocityX *= 0.999;
                this.velocityZ *= 0.999;
                this.velocityY *= -0.7;
            } else {
                this.y += this.velocityY;
            }
            if (this.isHit(this.x, this.y, this.z + this.velocityZ)) {
                this.velocityZ *= -0.8;
            } else {
                this.z += this.velocityZ;
            }
        } else {
            this.x += this.velocityX;
            this.y += this.velocityY;
            this.z += this.velocityZ;
        }

        this.velocityX /= 0.999999;
        this.velocityZ /= 0.999999;

        if (spinning) {
            this.rotation += 2.0f;
        }

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

    private boolean isHit(double px, double py, double pz) {
        if (mc.world == null) return false;
        BlockPos pos = BlockPos.ofFloored(px, py, pz);
        return mc.world.getBlockState(pos).isFullCube(mc.world, pos);
    }

    public boolean isDead() {
        return fadingOut && this.cachedAlpha <= 0.0F;
    }

    public float getAlpha() {
        return this.cachedAlpha;
    }

    private Identifier getTexture() {
        return switch (actualMode) {
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
            default -> null;
        };
    }

    public void render(MatrixStack matrices, VertexConsumerProvider immediate, float glowSize, float partialTicks) {
        float alpha = this.getAlpha();
        if (alpha <= 0.0F) return;

        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
        float cameraYaw = mc.gameRenderer.getCamera().getYaw();
        float cameraPitch = mc.gameRenderer.getCamera().getPitch();

        double interpX = MathHelper.lerp(partialTicks, this.lastX, this.x);
        double interpY = MathHelper.lerp(partialTicks, this.lastY, this.y);
        double interpZ = MathHelper.lerp(partialTicks, this.lastZ, this.z);

        float relX = (float) (interpX - cameraPos.x);
        float relY = (float) (interpY - cameraPos.y);
        float relZ = (float) (interpZ - cameraPos.z);

        if (actualMode == ParticleMode.CUBES) {
            renderCube(matrices, immediate, relX, relY, relZ, alpha, glowSize, cameraYaw, cameraPitch);
        } else {
            renderTextured(matrices, immediate, relX, relY, relZ, alpha, glowSize, cameraYaw, cameraPitch);
        }
    }

    private void renderCube(MatrixStack matrices, VertexConsumerProvider immediate,
                            float relX, float relY, float relZ, float alpha, float glowSize,
                            float cameraYaw, float cameraPitch) {
        long now = System.currentTimeMillis();
        float rotationAnim = (float) (now % 9000L) / 9000.0F * 360.0F;

        float alpha02 = alpha * 0.2F;
        float alpha04 = alpha * 0.4F;
        int glowCol = ColorUtil.multAlpha(color, alpha);
        float size = scale * 0.25f;
        float cubeGlow1 = size * glowSize;
        float cubeGlow2 = size * (glowSize / 3.0f);

        float rotY = rotationAnim + this.phase;
        float rotX = rotationAnim * 0.5F;

        matrices.push();
        matrices.translate(relX, relY, relZ);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(rotY));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(rotX));
        Matrix4f mat = matrices.peek().getPositionMatrix();
        ParticleRenderer.drawCube(immediate.getBuffer(ParticleRenderer.getQuadsLayer()), mat, ColorUtil.multAlpha(color, alpha02), size);
        ParticleRenderer.drawLines(immediate.getBuffer(ParticleRenderer.getLinesLayer()), mat, ColorUtil.multAlpha(color, alpha04), size);
        matrices.pop();

        matrices.push();
        matrices.translate(relX, relY, relZ);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-cameraYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cameraPitch));
        Matrix4f gMat = matrices.peek().getPositionMatrix();

        renderGlowEffect(immediate, gMat, glowCol, alpha, cubeGlow1, cubeGlow2);

        matrices.pop();
    }

    private void renderTextured(MatrixStack matrices, VertexConsumerProvider immediate,
                                float relX, float relY, float relZ, float alpha, float glowSize,
                                float cameraYaw, float cameraPitch) {
        Identifier texture = getTexture();
        if (texture == null) return;

        int glowCol = ColorUtil.multAlpha(color, alpha);
        float size = scale * 0.5f;

        int r = (glowCol >> 16) & 0xFF;
        int g = (glowCol >> 8) & 0xFF;
        int b = glowCol & 0xFF;
        int a = (int) (255 * alpha);

        matrices.push();
        matrices.translate(relX, relY, relZ);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-cameraYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cameraPitch));

        if (spinning) {
            matrices.multiply(RotationAxis.POSITIVE_Z.rotationDegrees(rotation));
        }

        Matrix4f mat = matrices.peek().getPositionMatrix();

        RenderLayer layer = ClientPipelines.WORLD_PARTICLES_GLOW.apply(texture);
        VertexConsumer buffer = immediate.getBuffer(layer);

        float half = size / 2.0f;
        buffer.vertex(mat, -half, -half, 0).texture(0, 0).color(r, g, b, a);
        buffer.vertex(mat, -half, half, 0).texture(0, 1).color(r, g, b, a);
        buffer.vertex(mat, half, half, 0).texture(1, 1).color(r, g, b, a);
        buffer.vertex(mat, half, -half, 0).texture(1, 0).color(r, g, b, a);

        matrices.pop();

        matrices.push();
        matrices.translate(relX, relY, relZ);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-cameraYaw));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(cameraPitch));
        Matrix4f gMat = matrices.peek().getPositionMatrix();

        float glowSizePrimary = size * glowSize * 0.5f;
        float glowSizeSecondary = size * glowSize * 0.2f;
        renderGlowEffect(immediate, gMat, glowCol, alpha, glowSizePrimary, glowSizeSecondary);

        matrices.pop();
    }

    private void renderGlowEffect(VertexConsumerProvider immediate, Matrix4f matrix, int color, float alpha, float sizePrimary, float sizeSecondary) {
        switch (glowMode) {
            case BLOOM -> {
                RenderLayer layerBloom = ClientPipelines.WORLD_PARTICLES_GLOW.apply(GLOW_BLOOM);
                ParticleRenderer.drawGlow(immediate.getBuffer(layerBloom), matrix, color, (int) (80.0F * alpha), sizePrimary);
            }
            case BLOOM_SAMPLE -> {
                RenderLayer layerSample = ClientPipelines.WORLD_PARTICLES_GLOW.apply(GLOW_BLOOM_SAMPLE);
                ParticleRenderer.drawGlow(immediate.getBuffer(layerSample), matrix, color, (int) (140.0F * alpha), sizeSecondary);
            }
            case BOTH -> {
                RenderLayer layerBloom = ClientPipelines.WORLD_PARTICLES_GLOW.apply(GLOW_BLOOM);
                RenderLayer layerSample = ClientPipelines.WORLD_PARTICLES_GLOW.apply(GLOW_BLOOM_SAMPLE);
                ParticleRenderer.drawGlow(immediate.getBuffer(layerBloom), matrix, color, (int) (80.0F * alpha), sizePrimary);
                ParticleRenderer.drawGlow(immediate.getBuffer(layerSample), matrix, color, (int) (140.0F * alpha), sizeSecondary);
            }
        }
    }
}