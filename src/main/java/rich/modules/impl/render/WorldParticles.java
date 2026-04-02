package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import rich.events.api.EventHandler;
import rich.events.impl.TickEvent;
import rich.events.impl.WorldRenderEvent;
import rich.modules.impl.render.worldparticles.Particle;
import rich.modules.impl.render.worldparticles.ParticleSpawner;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.ColorSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.Instance;
import rich.util.timer.StopWatch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class WorldParticles extends ModuleStructure {

    public static WorldParticles getInstance() {
        return Instance.get(WorldParticles.class);
    }

    final List<Particle> particles = new ArrayList<>();
    final StopWatch timer = new StopWatch();

    Vec3d lastPlayerPos = Vec3d.ZERO;
    Vec3d playerVelocity = Vec3d.ZERO;
    double playerSpeed = 0;

    public SelectSetting mode = new SelectSetting("Режим", "Тип частиц")
            .value("3D Кубы", "Корона", "Куб", "Доллар", "Сердце", "Молния", "Линия", "Ромб", "Снежинка", "Звезда", "Звезда 2", "Треугольник", "Свечение", "Рандом")
            .selected("Звезда");

    public SliderSettings cubeCount = new SliderSettings("Количество", "Количество частиц")
            .range(10.0f, 500.0f)
            .setValue(100.0f);

    public SliderSettings lifeTime = new SliderSettings("Время жизни", "Время жизни (сек)")
            .range(2.0f, 60.0f)
            .setValue(10.0f);

    public SliderSettings size = new SliderSettings("Размер", "Размер частиц")
            .range(0.1f, 1.5f)
            .setValue(1.5f);

    public SliderSettings glowSize = new SliderSettings("Свечение", "Размер свечения")
            .range(0.1f, 5.0f)
            .setValue(3f);

    public BooleanSetting physics = new BooleanSetting("Физика", "Частицы падают вниз")
            .setValue(false);

    public BooleanSetting randomColor = new BooleanSetting("Рандомный цвет", "Каждая частица имеет случайный цвет")
            .setValue(false);

    public BooleanSetting whiteOnSpawn = new BooleanSetting("Белые при спавне", "Частицы белые при появлении и плавно меняют цвет")
            .setValue(true);

    public BooleanSetting whiteCenter = new BooleanSetting("Белый центр", "Белый центр у текстурных частиц")
            .setValue(false)
            .visible(() -> !mode.getSelected().equals("3D Кубы"));

    public ColorSetting cubeColor = new ColorSetting("Цвет", "Цвет частиц")
            .value(0xFF896148)
            .visible(() -> !randomColor.isValue());

    public WorldParticles() {
        super("WorldParticles", "Летающие частицы в мире", ModuleCategory.RENDER);
        settings(mode, cubeCount, lifeTime, size, glowSize, physics, randomColor, whiteOnSpawn, whiteCenter, cubeColor);
    }

    @Override
    public void deactivate() {
        particles.clear();
        lastPlayerPos = Vec3d.ZERO;
        playerVelocity = Vec3d.ZERO;
        playerSpeed = 0;
    }

    private Particle.ParticleType getParticleType() {
        String selected = mode.getSelected();
        return switch (selected) {
            case "3D Кубы" -> Particle.ParticleType.CUBE_3D;
            case "Корона" -> Particle.ParticleType.CROWN;
            case "Куб" -> Particle.ParticleType.CUBE_BLAST;
            case "Доллар" -> Particle.ParticleType.DOLLAR;
            case "Сердце" -> Particle.ParticleType.HEART;
            case "Молния" -> Particle.ParticleType.LIGHTNING;
            case "Линия" -> Particle.ParticleType.LINE;
            case "Ромб" -> Particle.ParticleType.RHOMBUS;
            case "Снежинка" -> Particle.ParticleType.SNOWFLAKE;
            case "Звезда" -> Particle.ParticleType.STAR;
            case "Звезда 2" -> Particle.ParticleType.STAR_ALT;
            case "Треугольник" -> Particle.ParticleType.TRIANGLE;
            case "Свечение" -> Particle.ParticleType.GLOW;
            case "Рандом" -> Particle.ParticleType.RANDOM;
            default -> Particle.ParticleType.CUBE_3D;
        };
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        Vec3d currentPos = mc.player.getEntityPos();

        if (lastPlayerPos != Vec3d.ZERO) {
            playerVelocity = currentPos.subtract(lastPlayerPos);
            playerSpeed = playerVelocity.horizontalLength();
        }
        lastPlayerPos = currentPos;

        double despawnDistSq = ParticleSpawner.getDespawnDistanceSquared();
        for (Particle p : particles) {
            if (!p.isFadingOut()) {
                double distSq = p.getHorizontalDistanceSquaredTo(currentPos);
                if (distSq > despawnDistSq) {
                    p.startFadeOut();
                }
            }
        }

        int actualDelay = ParticleSpawner.calculateSpawnDelay(playerSpeed);

        if (particles.size() < cubeCount.getValue() && timer.finished(actualDelay)) {
            int spawnCount = ParticleSpawner.calculateSpawnCount(playerSpeed, particles.size(), (int) cubeCount.getValue());
            long lifeTimeMs = (long) (lifeTime.getValue() * 1000);
            Particle.ParticleType type = getParticleType();

            for (int i = 0; i < spawnCount && particles.size() < cubeCount.getValue(); i++) {
                Particle particle = ParticleSpawner.createParticle(currentPos, playerVelocity, playerSpeed, lifeTimeMs, type);
                particle.setPhysics(physics.isValue());
                particle.setSize(size.getValue());
                particles.add(particle);
            }

            timer.reset();
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (particles.isEmpty()) return;

        VertexConsumerProvider.Immediate immediate = mc.getBufferBuilders().getEntityVertexConsumers();
        MatrixStack matrices = e.getStack();
        Vec3d cameraPos = mc.gameRenderer.getCamera().getCameraPos();
        long now = System.currentTimeMillis();
        float cameraYaw = mc.gameRenderer.getCamera().getYaw();
        float cameraPitch = mc.gameRenderer.getCamera().getPitch();
        float rotation = (float) (now % 9000L) / 9000.0F * 360.0F;
        int baseColor = cubeColor.getColor();
        float glow = glowSize.getValue();
        boolean useRandomColor = randomColor.isValue();
        boolean useWhiteOnSpawn = whiteOnSpawn.isValue();
        boolean useWhiteCenter = whiteCenter.isValue();

        Iterator<Particle> iterator = particles.iterator();
        while (iterator.hasNext()) {
            Particle p = iterator.next();
            p.update(now);
            if (p.shouldRemove()) {
                iterator.remove();
            }
        }

        for (Particle p : particles) {
            double distSq = p.getDistanceSquaredTo(cameraPos);
            if (distSq < 150 * 150) {
                p.render(matrices, immediate, cameraPos, baseColor, rotation, cameraYaw, cameraPitch, glow, useRandomColor, useWhiteOnSpawn, useWhiteCenter);
            }
        }

        immediate.draw();
    }
}