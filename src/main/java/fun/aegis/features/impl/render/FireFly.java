package fun.aegis.features.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.aegis.events.player.TickEvent;
import fun.aegis.events.render.WorldRenderEvent;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.math.time.StopWatch;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.BlockState;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Vector3d;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FireFly extends Module {
    public static FireFly getInstance() {
        return Instance.get(FireFly.class);
    }

    List<FireFlyEntity> particles = new ArrayList<>();
    Random random = new Random();

    SliderSettings count = new SliderSettings("Количество", "Количество светлячков").setValue(100).range(10, 300);
    SliderSettings speed = new SliderSettings("Скорость", "Скорость движения").setValue(0.08f).range(0.01f, 0.3f);
    SliderSettings radius = new SliderSettings("Радиус спавна", "Радиус появления светлячков").setValue(20f).range(5f, 40f);
    SliderSettings trailLength = new SliderSettings("Длина шлейфа", "Длина хвоста светлячка").setValue(15f).range(5f, 40f);
    SliderSettings gravity = new SliderSettings("Гравитация", "Сила притяжения вниз").setValue(0.002f).range(0f, 0.01f);
    BooleanSetting randomColor = new BooleanSetting("Рандомный цвет", "Случайные цвета светлячков");
    BooleanSetting collision = new BooleanSetting("Коллизия", "Столкновение с блоками");

    Identifier texture = Identifier.of("textures/features/firefly/glow.png");

    public FireFly() {
        super("FireFly", "FireFly", ModuleCategory.RENDER);
        randomColor.setValue(true);
        collision.setValue(true);
        setup(count, speed, radius, trailLength, gravity, randomColor, collision);
    }

    @Override
    public void activate() {
        super.activate();
        particles.clear();
    }

    @Override
    public void deactivate() {
        super.deactivate();
        particles.clear();
    }


    private void spawnParticle() {
        if (mc.player == null || mc.world == null) return;

        double distance = 3 + random.nextDouble() * (radius.getValue() - 3);
        double yawRad = Math.toRadians(random.nextDouble() * 360);
        double xOffset = -Math.sin(yawRad) * distance;
        double zOffset = Math.cos(yawRad) * distance;
        double yOffset = random.nextDouble() * 8;

        double spawnX = mc.player.getX() + xOffset;
        double spawnY = mc.player.getY() + yOffset;
        double spawnZ = mc.player.getZ() + zOffset;

        BlockPos spawnPos = BlockPos.ofFloored(spawnX, spawnY, spawnZ);
        if (!mc.world.getBlockState(spawnPos).isAir()) return;

        double velocitySpeed = speed.getValue() * 0.5;
        double velocityYaw = Math.toRadians(random.nextDouble() * 360);
        double velocityPitch = Math.toRadians(-20 + random.nextDouble() * 40);

        Vector3d initialVelocity = new Vector3d(
                -Math.sin(velocityYaw) * Math.cos(velocityPitch) * velocitySpeed,
                Math.sin(velocityPitch) * velocitySpeed * 0.3,
                Math.cos(velocityYaw) * Math.cos(velocityPitch) * velocitySpeed
        );

        int color = randomColor.isValue() ? randomColor() : ColorAssist.getClientColor();
        particles.add(new FireFlyEntity(new Vector3d(spawnX, spawnY, spawnZ), initialVelocity, color));
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null) return;

        particles.removeIf(particle ->
                particle.timer.elapsedTime() > 10000 ||
                        particle.position.distanceSquared(mc.player.getX(), mc.player.getY(), mc.player.getZ()) >= 2500 ||
                        particle.isDead()
        );

        int targetCount = (int) count.getValue();
        while (particles.size() < targetCount) {
            spawnParticle();
        }

        for (FireFlyEntity particle : particles) {
            particle.update();
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent event) {
        if (mc.player == null || mc.world == null || particles.isEmpty()) return;

        MatrixStack matrix = event.getStack();
        Camera camera = mc.getEntityRenderDispatcher().camera;
        Vec3d cameraPos = camera.getPos();

        RenderSystem.enableBlend();
        RenderSystem.disableCull();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);
        RenderSystem.setShaderTexture(0, texture);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);

        for (FireFlyEntity particle : particles) {
            double distSq = particle.getPosition().distanceSquared(cameraPos.x, cameraPos.y, cameraPos.z);
            if (distSq > 2500) continue;
            
            renderTrail(matrix, buffer, particle, camera, cameraPos);
            renderParticle(matrix, buffer, particle, camera, cameraPos);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());

        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableBlend();
    }


    private void renderTrail(MatrixStack matrix, BufferBuilder buffer, FireFlyEntity particle, Camera camera, Vec3d cameraPos) {
        List<Vector3d> trail = particle.getTrail();
        if (trail.size() < 2) return;

        int baseAlpha = particle.getAlpha();
        int color = particle.getColor();

        for (int i = 0; i < trail.size(); i++) {
            Vector3d pos = trail.get(i);
            float fade = (float) i / (float) trail.size();
            float size = 0.12f * fade;
            int trailAlpha = (int) (baseAlpha * fade * 0.6f);
            int trailColor = ColorAssist.multAlpha(color, trailAlpha / 255f);
            renderBillboard(matrix, buffer, pos, size, trailColor, camera, cameraPos);
        }
    }

    private void renderParticle(MatrixStack matrix, BufferBuilder buffer, FireFlyEntity particle, Camera camera, Vec3d cameraPos) {
        int baseAlpha = particle.getAlpha();
        int pulseAlpha = particle.getPulseAlpha();
        int finalAlpha = Math.min(baseAlpha, pulseAlpha);
        int color = particle.getColor();
        Vector3d pos = particle.getPosition();

        float glowSize = 0.28f;
        int glowColor = ColorAssist.multAlpha(color, (finalAlpha * 0.5f) / 255f);
        renderBillboard(matrix, buffer, pos, glowSize, glowColor, camera, cameraPos);

        float mainSize = 0.18f;
        int mainColor = ColorAssist.multAlpha(color, finalAlpha / 255f);
        renderBillboard(matrix, buffer, pos, mainSize, mainColor, camera, cameraPos);

        float coreSize = 0.08f;
        int coreColor = ColorAssist.getColor(255, 255, 255, finalAlpha);
        renderBillboard(matrix, buffer, pos, coreSize, coreColor, camera, cameraPos);
    }

    private void renderBillboard(MatrixStack matrix, BufferBuilder buffer, Vector3d pos, float size, int color, Camera camera, Vec3d cameraPos) {
        // Вычисляем позицию относительно камеры
        double x = pos.x - cameraPos.x;
        double y = pos.y - cameraPos.y;
        double z = pos.z - cameraPos.z;

        // Используем существующий матрикс стек вместо создания нового
        matrix.push();
        matrix.translate(x, y, z);
        
        // Применяем ротацию для billboard эффекта
        matrix.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrix.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(camera.getYaw() + 180.0F));

        Matrix4f mat = matrix.peek().getPositionMatrix();

        // Рендерим квад
        buffer.vertex(mat, -size, -size, 0).texture(0, 0).color(color);
        buffer.vertex(mat, -size, size, 0).texture(0, 1).color(color);
        buffer.vertex(mat, size, size, 0).texture(1, 1).color(color);
        buffer.vertex(mat, size, -size, 0).texture(1, 0).color(color);
        
        matrix.pop();
    }

    private int randomColor() {
        float hue = random.nextFloat() * 0.15f + 0.1f;
        return Color.HSBtoRGB(hue, 0.8f, 1.0f);
    }

    @Getter
    private class FireFlyEntity {
        private final StopWatch timer = new StopWatch();
        private final int color;
        private final Vector3d position;
        private final Vector3d velocity;
        private final List<Vector3d> trail = new ArrayList<>();
        private float alpha = 0f;
        private boolean dead = false;
        private double targetY;
        private long directionChangeTime = 0;

        public FireFlyEntity(Vector3d position, Vector3d velocity, int color) {
            this.position = position;
            this.velocity = velocity;
            this.color = color;
            this.targetY = position.y + (Math.random() - 0.5) * 3;
            this.trail.add(new Vector3d(position.x, position.y, position.z));
        }


        public void update() {
            if (mc.world == null) return;

            if (System.currentTimeMillis() - directionChangeTime > 2000 + random.nextInt(3000)) {
                directionChangeTime = System.currentTimeMillis();
                double newYaw = Math.toRadians(random.nextDouble() * 360);
                double newSpeed = speed.getValue() * (0.5 + random.nextDouble() * 0.5);
                velocity.x = -Math.sin(newYaw) * newSpeed;
                velocity.z = Math.cos(newYaw) * newSpeed;
                targetY = position.y + (random.nextDouble() - 0.5) * 4;
            }

            double yDiff = targetY - position.y;
            velocity.y += yDiff * 0.01;
            velocity.y -= gravity.getValue();

            velocity.x += (Math.random() - 0.5) * 0.008;
            velocity.y += (Math.random() - 0.5) * 0.005;
            velocity.z += (Math.random() - 0.5) * 0.008;

            double maxSpeed = speed.getValue();
            velocity.x = MathHelper.clamp(velocity.x, -maxSpeed, maxSpeed);
            velocity.y = MathHelper.clamp(velocity.y, -maxSpeed * 0.5, maxSpeed * 0.5);
            velocity.z = MathHelper.clamp(velocity.z, -maxSpeed, maxSpeed);

            velocity.x *= 0.98;
            velocity.y *= 0.95;
            velocity.z *= 0.98;

            if (collision.isValue()) {
                double nextX = position.x + velocity.x;
                double nextY = position.y + velocity.y;
                double nextZ = position.z + velocity.z;

                BlockPos nextPos = BlockPos.ofFloored(nextX, nextY, nextZ);
                BlockState nextState = mc.world.getBlockState(nextPos);

                if (!nextState.isAir()) {
                    if (!mc.world.getBlockState(BlockPos.ofFloored(nextX, position.y, position.z)).isAir()) {
                        velocity.x = -velocity.x * 0.5;
                    }
                    if (!mc.world.getBlockState(BlockPos.ofFloored(position.x, nextY, position.z)).isAir()) {
                        velocity.y = -velocity.y * 0.3;
                        targetY = position.y + (velocity.y > 0 ? -2 : 2);
                    }
                    if (!mc.world.getBlockState(BlockPos.ofFloored(position.x, position.y, nextZ)).isAir()) {
                        velocity.z = -velocity.z * 0.5;
                    }
                } else {
                    position.x = nextX;
                    position.y = nextY;
                    position.z = nextZ;
                }
            } else {
                position.x += velocity.x;
                position.y += velocity.y;
                position.z += velocity.z;
            }

            trail.add(new Vector3d(position.x, position.y, position.z));
            int maxTrailLen = (int) trailLength.getValue();
            if (trail.size() > maxTrailLen) {
                trail.subList(0, trail.size() - maxTrailLen).clear();
            }

            long elapsed = timer.elapsedTime();
            long fadeInDuration = 800;
            long fadeOutStart = 10000 - 1000;

            if (elapsed < fadeInDuration) {
                alpha = Math.min(255f, alpha + 8f);
            } else if (elapsed > fadeOutStart) {
                alpha = Math.max(0f, alpha - 5f);
                if (alpha <= 0) dead = true;
            }
        }

        public int getAlpha() {
            return (int) alpha;
        }

        public int getPulseAlpha() {
            double pulse = (Math.sin(timer.elapsedTime() / 400.0 + position.x) + 1.0) / 2.0;
            pulse = pulse * 0.5 + 0.5;
            return (int) (pulse * 255);
        }
    }
}
