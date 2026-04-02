package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.entity.projectile.TridentEntity;
import net.minecraft.entity.projectile.thrown.ThrownEntity;
import net.minecraft.util.math.Vec3d;
import rich.events.api.EventHandler;
import rich.events.impl.AttackEvent;
import rich.events.impl.TickEvent;
import rich.events.impl.WorldRenderEvent;
import rich.modules.impl.render.particles.Particle3D;
import rich.modules.impl.render.particles.TotemEmitter;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.ColorSetting;
import rich.modules.module.setting.implement.MultiSelectSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.Instance;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Particles extends ModuleStructure {

    public static Particles getInstance() {
        return Instance.get(Particles.class);
    }

    final List<Particle3D> particles = new ArrayList<>();
    final List<TotemEmitter> totemEmitters = new ArrayList<>();

    public SelectSetting mode = new SelectSetting("Режим", "Тип партиклов")
            .value("Кубы", "Корона", "Куб", "Доллар", "Сердце", "Молния", "Линия", "Ромб", "Снежинка", "Звезда", "Звезда 2", "Треугольник", "Рандом")
            .selected("Звезда");

    public SelectSetting glowMode = new SelectSetting("Свечение", "Тип эффекта свечения")
            .value("Bloom", "Bloom Sample", "Оба")
            .selected("Bloom Sample");

    public MultiSelectSetting triggers = new MultiSelectSetting("Триггеры", "Когда спавнить партиклы")
            .value("Удар", "Тотем", "Ходьба", "Бросаемый предмет")
            .selected("Удар", "Тотем", "Ходьба", "Бросаемый предмет");

    public SliderSettings amount = new SliderSettings("Количество", "Кол-во партиклов при ударе")
            .range(10, 40).setValue(40);

    public SliderSettings walkAmount = new SliderSettings("Кол-во при ходьбе", "Кол-во партиклов в секунду при ходьбе")
            .range(10, 30).setValue(30).visible(() -> triggers.isSelected("Ходьба"));

    public SliderSettings spread = new SliderSettings("Разброс", "Сила разброса частиц в стороны")
            .range(0.5f, 3.0f).setValue(1.0f);

    public SliderSettings speed = new SliderSettings("Скорость", "Скорость движения частиц")
            .range(0.1f, 3.0f).setValue(2.0f);

    public SliderSettings lifeTime = new SliderSettings("Время жизни", "Время жизни частиц в секундах")
            .range(0.5f, 10f).setValue(2.5f);

    public SliderSettings size = new SliderSettings("Размер", "Размер частиц")
            .range(0.1f, 1.0f).setValue(1f);

    public BooleanSetting randomColor = new BooleanSetting("Рандомный цвет", "Каждый партикл получает случайный цвет")
            .setValue(false);

    public ColorSetting color = new ColorSetting("Цвет", "Цвет партиклов")
            .value(0xFF896148)
            .visible(() -> !randomColor.isValue());

    private static final float GLOW_SIZE = 7.5f;
    private static final int TOTEM_DURATION = 20;
    private static final float GRAVITY_STRENGTH = 0.04f;

    private static final int[] RANDOM_COLORS = {
            0xFFFF0000,
            0xFFFF7F00,
            0xFFFFFF00,
            0xFF00FF00,
            0xFF00FFFF,
            0xFF0000FF,
            0xFF8B00FF,
            0xFFFF00FF,
            0xFFFF1493,
            0xFFFFFFFF,
            0xFF00FF7F,
            0xFFFF6347
    };

    private float walkParticleAccumulator = 0;

    public Particles() {
        super("Particles", "Custom particles system", ModuleCategory.RENDER);
        settings(mode, glowMode, triggers, amount, walkAmount, spread, speed, lifeTime, size, randomColor, color);
    }

    @Override
    public void deactivate() {
        particles.clear();
        totemEmitters.clear();
        walkParticleAccumulator = 0;
    }

    private int getParticleColor() {
        if (randomColor.isValue()) {
            return RANDOM_COLORS[ThreadLocalRandom.current().nextInt(RANDOM_COLORS.length)];
        }
        return color.getColor();
    }

    private float getGravity() {
        return (1.0f - 0.9f) * GRAVITY_STRENGTH;
    }

    private float getSpeedMultiplier() {
        return speed.getValue();
    }

    private Particle3D.ParticleMode getParticleMode() {
        String selected = mode.getSelected();
        return switch (selected) {
            case "Кубы" -> Particle3D.ParticleMode.CUBES;
            case "Корона" -> Particle3D.ParticleMode.CROWN;
            case "Куб" -> Particle3D.ParticleMode.CUBE_BLAST;
            case "Доллар" -> Particle3D.ParticleMode.DOLLAR;
            case "Сердце" -> Particle3D.ParticleMode.HEART;
            case "Молния" -> Particle3D.ParticleMode.LIGHTNING;
            case "Линия" -> Particle3D.ParticleMode.LINE;
            case "Ромб" -> Particle3D.ParticleMode.RHOMBUS;
            case "Снежинка" -> Particle3D.ParticleMode.SNOWFLAKE;
            case "Звезда" -> Particle3D.ParticleMode.STAR;
            case "Звезда 2" -> Particle3D.ParticleMode.STAR_ALT;
            case "Треугольник" -> Particle3D.ParticleMode.TRIANGLE;
            case "Рандом" -> Particle3D.ParticleMode.RANDOM;
            default -> Particle3D.ParticleMode.CUBES;
        };
    }

    private Particle3D.GlowMode getGlowMode() {
        String selected = glowMode.getSelected();
        return switch (selected) {
            case "Bloom" -> Particle3D.GlowMode.BLOOM;
            case "Bloom Sample" -> Particle3D.GlowMode.BLOOM_SAMPLE;
            case "Оба" -> Particle3D.GlowMode.BOTH;
            default -> Particle3D.GlowMode.BOTH;
        };
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (triggers.isSelected("Ходьба")) {
            handleWalkParticles();
        }

        if (triggers.isSelected("Бросаемый предмет")) {
            handleProjectileParticles();
        }

        Iterator<TotemEmitter> emitterIterator = totemEmitters.iterator();
        while (emitterIterator.hasNext()) {
            TotemEmitter emitter = emitterIterator.next();
            emitter.tick();

            if (emitter.isAlive()) {
                spawnTotemParticlesBurst(emitter.getEntity(), emitter.getProgress());
            } else {
                emitterIterator.remove();
            }
        }

        Iterator<Particle3D> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle3D p = iterator.next();
            p.update();
            if (p.isDead()) {
                iterator.remove();
            }
        }
    }

    private void handleWalkParticles() {
        double velocitySq = mc.player.getVelocity().lengthSquared();
        boolean isMoving = velocitySq > 0.0001 && !mc.player.isSneaking();

        if (!isMoving) {
            walkParticleAccumulator = 0;
            return;
        }

        float particlesPerSecond = walkAmount.getValue();
        float particlesPerTick = particlesPerSecond / 20f;

        walkParticleAccumulator += particlesPerTick;

        int particlesToSpawn = (int) walkParticleAccumulator;
        walkParticleAccumulator -= particlesToSpawn;

        if (particlesToSpawn <= 0) return;

        float yaw = mc.player.getYaw();
        double radian = Math.toRadians(yaw + 90);
        double offsetX = Math.cos(radian) * 0.5;
        double offsetZ = Math.sin(radian) * 0.5;

        float spreadValue = spread.getValue() * 0.05f;
        float speedMult = getSpeedMultiplier();

        for (int i = 0; i < particlesToSpawn; i++) {
            double px = mc.player.getX() - offsetX + (Math.random() - 0.5) * 0.3;
            double py = mc.player.getY() + 0.3 + Math.random() * (mc.player.getHeight() - 0.3);
            double pz = mc.player.getZ() - offsetZ + (Math.random() - 0.5) * 0.3;

            Vec3d pos = new Vec3d(px, py, pz);

            double velX = (Math.random() - 0.5) * spreadValue * speedMult;
            double velY = (Math.random() - 0.5) * spreadValue * 0.5 * speedMult;
            double velZ = (Math.random() - 0.5) * spreadValue * speedMult;

            Vec3d velocity = new Vec3d(velX, velY, velZ);

            particles.add(new Particle3D(
                    pos,
                    velocity,
                    getParticleColor(),
                    size.getValue() * 0.6f,
                    lifeTime.getValue() * 0.5f
            ).setGravity(getGravity()).setVelocityMultiplier(0.99f).setMode(getParticleMode()).setGlowMode(getGlowMode()));
        }
    }

    private void handleProjectileParticles() {
        float spreadValue = spread.getValue() * 0.03f;
        float speedMult = getSpeedMultiplier();

        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof ThrownEntity || entity instanceof ArrowEntity || entity instanceof TridentEntity) {
                ProjectileEntity projectile = (ProjectileEntity) entity;

                double prevX = projectile.lastX;
                double prevY = projectile.lastY;
                double prevZ = projectile.lastZ;

                double currentX = projectile.getX();
                double currentY = projectile.getY();
                double currentZ = projectile.getZ();

                boolean isMoving = Math.abs(currentX - prevX) > 0.01 || Math.abs(currentY - prevY) > 0.01 || Math.abs(currentZ - prevZ) > 0.01;

                if (isMoving || projectile.getVelocity().lengthSquared() > 0.01) {
                    for (int i = 0; i < 2; i++) {
                        double px = projectile.getX() + (Math.random() - 0.5) * 0.5;
                        double py = projectile.getY() + Math.random() * projectile.getHeight();
                        double pz = projectile.getZ() + (Math.random() - 0.5) * 0.5;

                        Vec3d pos = new Vec3d(px, py, pz);

                        double velX = (Math.random() - 0.5) * 2 * spreadValue * speedMult;
                        double velY = (Math.random() - 0.5) * 2 * spreadValue * speedMult;
                        double velZ = (Math.random() - 0.5) * 2 * spreadValue * speedMult;

                        Vec3d velocity = new Vec3d(velX, velY, velZ);

                        particles.add(new Particle3D(
                                pos,
                                velocity,
                                getParticleColor(),
                                size.getValue() * 0.5f,
                                lifeTime.getValue() * 0.3f
                        ).setGravity(getGravity()).setVelocityMultiplier(0.99f).setMode(getParticleMode()).setGlowMode(getGlowMode()));
                    }
                }
            }
        }
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        if (!triggers.isSelected("Удар") || e.getTarget() == null) return;

        Entity target = e.getTarget();

        float spreadValue = spread.getValue() * 0.15f;
        float speedMult = getSpeedMultiplier();

        int count = amount.getInt();
        for (int i = 0; i < count; i++) {
            double px = target.getX();
            double py = target.getY() + (Math.random() * target.getHeight());
            double pz = target.getZ();

            Vec3d pos = new Vec3d(px, py, pz);

            Vec3d velocity = new Vec3d(
                    (Math.random() - 0.5) * 2 * spreadValue * speedMult,
                    (Math.random() - 0.5) * 2 * spreadValue * speedMult,
                    (Math.random() - 0.5) * 2 * spreadValue * speedMult
            );

            particles.add(new Particle3D(
                    pos,
                    velocity,
                    getParticleColor(),
                    size.getValue(),
                    lifeTime.getValue()
            ).setGravity(getGravity()).setVelocityMultiplier(0.99f).setMode(getParticleMode()).setGlowMode(getGlowMode()));
        }
    }

    public void onTotemPop(Entity entity) {
        if (!triggers.isSelected("Тотем")) return;

        totemEmitters.add(new TotemEmitter(entity, TOTEM_DURATION));
    }

    private void spawnTotemParticlesBurst(Entity entity, float progress) {
        if (entity == null || entity.isRemoved()) return;

        float spreadMultiplier = 1.0f - (progress * 0.5f);
        float spreadValue = spread.getValue();
        float speedMult = getSpeedMultiplier();

        for (int i = 0; i < 4; i++) {
            double d = Math.random() * 2.0 - 1.0;
            double e = Math.random() * 2.0 - 1.0;
            double f = Math.random() * 2.0 - 1.0;

            if (d * d + e * e + f * f <= 1.0) {
                double px = entity.getX() + d * entity.getWidth() * 0.5;
                double py = entity.getBodyY(0.5) + e * entity.getHeight() * 0.5;
                double pz = entity.getZ() + f * entity.getWidth() * 0.5;

                Vec3d pos = new Vec3d(px, py, pz);

                double velocityScale = spreadValue * 0.18 * spreadMultiplier * speedMult;

                double initialUpward;
                if (Math.random() < 0.4) {
                    initialUpward = (0.15 + Math.random() * 0.2) * speedMult;
                } else {
                    initialUpward = (0.03 + Math.random() * 0.07) * speedMult;
                }

                Vec3d velocity = new Vec3d(
                        d * velocityScale,
                        initialUpward,
                        f * velocityScale
                );

                int[] totemColors = {
                        0xFF7CFC00,
                        0xFFFFD700,
                        0xFF32CD32,
                        0xFFFFA500,
                        0xFF00FF00,
                        0xFFADFF2F
                };
                int particleColor = totemColors[(int) (Math.random() * totemColors.length)];

                particles.add(new Particle3D(
                        pos,
                        velocity,
                        particleColor,
                        size.getValue() * 0.8f,
                        lifeTime.getValue() * 0.8f
                ).setGravity(getGravity()).setVelocityMultiplier(0.98f).setMode(getParticleMode()).setGlowMode(getGlowMode()));
            }
        }
    }

    @EventHandler
    public void onRender3D(WorldRenderEvent e) {
        if (particles.isEmpty()) return;

        MatrixStack stack = e.getStack();
        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        float partialTicks = e.getPartialTicks();

        for (Particle3D p : particles) {
            p.render(stack, immediate, GLOW_SIZE, partialTicks);
        }

        immediate.draw();
    }
}