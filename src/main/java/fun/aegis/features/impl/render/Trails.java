package fun.aegis.features.impl.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import fun.aegis.events.player.TickEvent;
import fun.aegis.events.render.WorldLoadEvent;
import fun.aegis.events.render.WorldRenderEvent;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.features.module.setting.implement.ColorSetting;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.geometry.Render3D;
import fun.aegis.utils.math.time.StopWatch;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gl.ShaderProgramKeys;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.joml.Matrix4f;

import java.awt.*;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class Trails extends Module {

    public static Trails getInstance() {
        return Instance.get(Trails.class);
    }

    final BooleanSetting xp = new BooleanSetting("XP бутылки", "Отображать трейлы для XP бутылок").setValue(false);
    final SelectSetting pearls = new SelectSetting("Эндер-жемчуг", "Режим отображения для эндер-жемчуга")
            .value("Частицы", "Выкл").selected("Частицы");
    final SelectSetting arrows = new SelectSetting("Стрелы", "Режим отображения для стрел")
            .value("Частицы", "Выкл").selected("Частицы");
    final SelectSetting players = new SelectSetting("Игроки", "Режим отображения для игроков")
            .value("Частицы", "Kagune", "Tail", "Выкл").selected("Частицы"); 
    final BooleanSetting onlySelf = new BooleanSetting("Только я", "Отображать только для себя")
            .setValue(false).visible(() -> !players.isSelected("Выкл"));
    final BooleanSetting hideFirstPerson = new BooleanSetting("От 1 лица", "Скрывать от первого лица")
            .setValue(true).visible(() -> !players.isSelected("Выкл"));

    final SliderSettings tailLength = new SliderSettings("Tail Length", "Длина хвоста")
            .setValue(250F).range(150F, 350F)
            .visible(() -> players.isSelected("Tail"));

    final SelectSetting particleMode = new SelectSetting("Тип частиц", "Тип отображаемых частиц")
            .value("Звезды", "Блум", "Сакура", "Луна", "Спарк", "Треугольник", "Куб", "Крест").selected("Спарк")
            .visible(() -> players.isSelected("Частицы"));
    final SelectSetting physics = new SelectSetting("Физика", "Физика частиц")
            .value("Падение", "Парение").selected("Парение")
            .visible(() -> players.isSelected("Частицы"));
    final SliderSettings particleScale = new SliderSettings("Размер частиц", "Размер частиц")
            .setValue(3).range(1, 10)
            .visible(() -> players.isSelected("Частицы"));
    final SliderSettings amount = new SliderSettings("Количество", "Количество частиц за спавн")
            .setValue(3).range(1, 10)
            .visible(() -> players.isSelected("Частицы"));
    final SliderSettings lifeTime = new SliderSettings("Время жизни", "Время жизни частиц в секундах")
            .setValue(2).range(1, 10)
            .visible(() -> players.isSelected("Частицы"));

    final SliderSettings kaguneLength = new SliderSettings("Kagune Length", "Длина хвоста")
            .setValue(60f).range(20f, 120f)
            .visible(() -> players.isSelected("Kagune"));
    final SliderSettings kaguneSize = new SliderSettings("Kagune Size", "Размер частиц")
            .setValue(0.25f).range(0.05f, 0.8f)
            .visible(() -> players.isSelected("Kagune"));
    final SliderSettings kaguneAlpha = new SliderSettings("Kagune Alpha", "Прозрачность")
            .setValue(0.85f).range(0.1f, 1.0f)
            .visible(() -> players.isSelected("Kagune"));
    final SliderSettings kaguneSmooth = new SliderSettings("Kagune Smooth", "Плавность следования")
            .setValue(0.3f).range(0.1f, 0.9f)
            .visible(() -> players.isSelected("Kagune"));

    final SelectSetting colorMode = new SelectSetting("Режим цвета", "Режим цвета")
            .value("Sync", "Custom").selected("Sync");
    final ColorSetting customColor = new ColorSetting("Кастом цвет", "Кастомный цвет")
            .value(0xFF50b4b4).visible(() -> colorMode.isSelected("Custom"));

    final Identifier STAR_TEXTURE = Identifier.of("textures/new_particles/star.png");
    final Identifier BLOOM_TEXTURE = Identifier.of("textures/new_particles/glow.png");
    final Identifier SAKURA_TEXTURE = Identifier.of("textures/new_particles/feather.png");
    final Identifier MOON_TEXTURE = Identifier.of("textures/new_particles/moon.png");
    final Identifier SPARK_TEXTURE = Identifier.of("textures/new_particles/spark.png");
    final Identifier TRIANGLE_TEXTURE = Identifier.of("textures/new_particles/triangle.png");
    final Identifier CUBE_TEXTURE = Identifier.of("textures/new_particles/cube.png");
    final Identifier MCROSS_TEXTURE = Identifier.of("textures/new_particles/mcross.png");
    final List<Particle> particles = new ArrayList<>();

    final Map<java.util.UUID, Deque<KagunePoint>> kaguneTrails = new HashMap<>();
    final Map<java.util.UUID, Long> kaguneLastMoveTime = new HashMap<>();
    final Map<java.util.UUID, Vec3d> kaguneLastPos = new HashMap<>();
    final Map<java.util.UUID, Float> kaguneVisibility = new HashMap<>();
    final List<TailPoint> tailPoints = new ArrayList<>();
    final Random random = new Random();
    private int tickCounter = 0;
    
    private static final long KAGUNE_FADE_DURATION = 500L;

    public Trails() {
        super("Trails", "Trails", ModuleCategory.RENDER);
        setup(xp, pearls, arrows, players, onlySelf, hideFirstPerson, 
              tailLength, particleMode, physics, particleScale, amount, lifeTime,
              kaguneLength, kaguneSize, kaguneAlpha, kaguneSmooth,
              colorMode, customColor);
    }

    @Override
    public void deactivate() {
        super.deactivate();
        particles.clear();
        tailPoints.clear();
        kaguneTrails.clear();
        kaguneLastMoveTime.clear();
        kaguneLastPos.clear();
        kaguneVisibility.clear();
        tickCounter = 0;
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        particles.clear();
        tailPoints.clear();
        kaguneTrails.clear();
        kaguneLastMoveTime.clear();
        kaguneLastPos.clear();
        kaguneVisibility.clear();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        tickCounter++;
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.player == null || mc.world == null) return;

        MatrixStack stack = e.getStack();
        float tickDelta = e.getPartialTicks();

        if (tickCounter % 3 == 0) {
            for (Entity en : mc.world.getEntities()) {
                if (en instanceof ExperienceBottleEntity && xp.isValue()) {
                    calcTrajectory(en);
                }
            }
        }

        int playerCount = 0;
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (playerCount++ > 20) break;
            
            if (player != mc.player && onlySelf.isValue()) continue;

            boolean isMoving = player.getVelocity().horizontalLengthSquared() > 0.001 
                    || Math.abs(player.getY() - player.prevY) > 0.01;

            if (players.isSelected("Частицы") && isMoving) {
                if (tickCounter % 2 == 0) {
                    spawnParticles(player);
                }
            }

            if (players.isSelected("Kagune")) {
                updateKaguneTrail(player);
            }
        }

        if (players.isSelected("Tail") && mc.gameRenderer.getCamera().isThirdPerson()) {
            renderTail(stack, tickDelta);
        }

        if (players.isSelected("Kagune")) {
            renderKagune(tickDelta);
        }

        if (players.isSelected("Частицы") && !particles.isEmpty()) {
            renderParticles();
        }

        long maxLife = lifeTime.getInt() * 1000L;
        if (tickCounter % 10 == 0) {
            particles.removeIf(p -> System.currentTimeMillis() - p.time > maxLife);
        }
    }

    private void spawnParticles(PlayerEntity player) {
        boolean isFirstPerson = hideFirstPerson.isValue() 
                && mc.options.getPerspective().isFirstPerson() 
                && player == mc.player;
        if (isFirstPerson) return;

        int count = amount.getInt();
        for (int i = 0; i < count; i++) {
            double px = player.getX() + randomFloat(-0.25f, 0.25f);
            double py = player.getY() + randomFloat(0.2f, 1.4f);
            double pz = player.getZ() + randomFloat(-0.25f, 0.25f);

            Color col = getColor(i);
            particles.add(new Particle(px, py, pz, col));
        }
    }

    private void renderParticles() {
        boolean isFirstPerson = hideFirstPerson.isValue() && mc.options.getPerspective().isFirstPerson();

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.setShaderTexture(0, getParticleTexture());
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        
        float pitch = camera.getPitch();
        float yaw = camera.getYaw();
        float cosPitch = (float) Math.cos(Math.toRadians(pitch));
        float sinPitch = (float) Math.sin(Math.toRadians(pitch));
        float cosYaw = (float) Math.cos(Math.toRadians(yaw + 180));
        float sinYaw = (float) Math.sin(Math.toRadians(yaw + 180));

        int rendered = 0;
        int vertexCount = 0;
        long lifeMs = lifeTime.getInt() * 1000L;
        
        for (Particle p : particles) {
            if (rendered++ > 200) break;

            p.update();

            float scale = particleScale.getValue() / 10f;
            long age = System.currentTimeMillis() - p.time;
            if (age > lifeMs) continue;
            
            float alpha = (1f - (float)age / lifeMs) * 0.9f;
            if (alpha <= 0) continue;

            float dx = (float)(p.x - camPos.x);
            float dy = (float)(p.y - camPos.y);
            float dz = (float)(p.z - camPos.z);
            
            float[] offsets = {-scale, scale};
            int color = ColorAssist.replAlpha(p.color.getRGB(), (int) (alpha * 255));
            
            for (float ox : offsets) {
                for (float oy : offsets) {
                    float vx = ox * cosYaw - oy * sinPitch * sinYaw;
                    float vy = oy * cosPitch;
                    float vz = ox * sinYaw + oy * sinPitch * cosYaw;

                    buffer.vertex(dx + vx, dy + vy, dz + vz)
                            .texture(ox > 0 ? 1 : 0, oy > 0 ? 1 : 0)
                            .color(color);
                }
            }
            vertexCount += 4;
        }

        if (vertexCount > 0) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }


    private void updateKaguneTrail(PlayerEntity player) {
        Deque<KagunePoint> deque = kaguneTrails.computeIfAbsent(player.getUuid(), id -> new ArrayDeque<>());
        long now = System.currentTimeMillis();

        Vec3d targetPos = new Vec3d(
                MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true), player.prevX, player.getX()),
                MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true), player.prevY, player.getY()) + player.getHeight() * 0.5,
                MathHelper.lerp(mc.getRenderTickCounter().getTickDelta(true), player.prevZ, player.getZ())
        );

        boolean isMoving = player.getVelocity().horizontalLengthSquared() > 0.0001
                || Math.abs(player.getY() - player.prevY) > 0.01;
        
        Vec3d lastPos = kaguneLastPos.get(player.getUuid());
        if (!isMoving && lastPos != null) {
            isMoving = lastPos.squaredDistanceTo(targetPos) > 0.0001;
        }
        
        if (isMoving) {
            kaguneLastMoveTime.put(player.getUuid(), now);
        }
        kaguneLastPos.put(player.getUuid(), targetPos);

        Long lastMoveTime = kaguneLastMoveTime.get(player.getUuid());
        boolean recentlyMoved = lastMoveTime != null && (now - lastMoveTime) < 150;

        float currentVisibility = kaguneVisibility.getOrDefault(player.getUuid(), 1f);
        float targetVisibility = (isMoving || recentlyMoved) ? 1f : 0f;
        float fadeSpeed = 0.08f;
        
        if (currentVisibility < targetVisibility) {
            currentVisibility = Math.min(targetVisibility, currentVisibility + fadeSpeed * 2f);
        } else if (currentVisibility > targetVisibility) {
            currentVisibility = Math.max(targetVisibility, currentVisibility - fadeSpeed);
        }
        kaguneVisibility.put(player.getUuid(), currentVisibility);

        KagunePoint head = deque.peekFirst();
        
        if (head != null) {
            double distance = head.pos.distanceTo(targetPos);
            double maxGap = 0.15;
            
            if (distance > maxGap) {
                int subdivisions = (int) Math.ceil(distance / maxGap);
                for (int i = 1; i <= subdivisions; i++) {
                    float t = (float) i / subdivisions;
                    Vec3d interpPos = lerpVec(head.pos, targetPos, t);
                    deque.addFirst(new KagunePoint(interpPos, now));
                }
            } else if (distance > 0.005) {
                float smooth = kaguneSmooth.getValue();
                Vec3d smoothedPos = lerpVec(head.pos, targetPos, smooth);
                deque.addFirst(new KagunePoint(smoothedPos, now));
            } else if (isMoving || recentlyMoved) {
                deque.addFirst(new KagunePoint(targetPos, now));
            }
        } else {
            deque.addFirst(new KagunePoint(targetPos, now));
        }

        int maxPoints = kaguneLength.getInt();
        while (deque.size() > maxPoints) {
            deque.removeLast();
        }
    }

    private void renderKagune(float tickDelta) {
        if (kaguneTrails.isEmpty()) return;

        boolean isFirstPerson = hideFirstPerson.isValue() && mc.options.getPerspective().isFirstPerson();
        if (isFirstPerson) return;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShaderTexture(0, BLOOM_TEXTURE);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_TEX_COLOR);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_TEXTURE_COLOR);
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();
        
        float pitch = camera.getPitch();
        float yaw = camera.getYaw();
        float cosPitch = (float) Math.cos(Math.toRadians(pitch));
        float sinPitch = (float) Math.sin(Math.toRadians(pitch));
        float cosYaw = (float) Math.cos(Math.toRadians(yaw + 180));
        float sinYaw = (float) Math.sin(Math.toRadians(yaw + 180));

        int vertexCount = 0;
        int trailCount = 0;
        
        for (Map.Entry<java.util.UUID, Deque<KagunePoint>> entry : kaguneTrails.entrySet()) {
            if (trailCount++ > 15) break;
            
            java.util.UUID playerId = entry.getKey();
            Deque<KagunePoint> deque = entry.getValue();
            List<KagunePoint> points = new ArrayList<>(deque);
            int total = points.size();
            if (total < 2) continue;

            float visibility = kaguneVisibility.getOrDefault(playerId, 1f);
            if (visibility <= 0.01f) continue;

            int renderPoints = Math.max(total / 2, 15);
            
            for (int i = 0; i < renderPoints; i++) {
                float exactIndex = (float) i / (renderPoints - 1) * (total - 1);
                int idx1 = (int) Math.floor(exactIndex);
                int idx2 = Math.min(idx1 + 1, total - 1);
                float localT = exactIndex - idx1;
                
                KagunePoint p1 = points.get(idx1);
                KagunePoint p2 = points.get(idx2);
                
                Vec3d pos = lerpVec(p1.pos, p2.pos, localT);

                float positionFactor = (float) i / (float) (renderPoints - 1);
                float baseAlpha = (float) Math.pow(1f - positionFactor, 1.5f);
                float fadePower = 1f + positionFactor * 2f;
                float visibilityFade = (float) Math.pow(visibility, fadePower);
                float alpha = kaguneAlpha.getValue() * baseAlpha * visibilityFade;
                
                if (alpha <= 0.01f) continue;

                float sizeFactor = 1f - positionFactor * 0.6f;
                float size = kaguneSize.getValue() * sizeFactor;

                int baseColor = colorMode.isSelected("Sync")
                        ? ColorAssist.fade(8, (int)(positionFactor * total * 8), ColorAssist.getClientColor(), ColorAssist.getClientColor(0.5f))
                        : customColor.getColor();

                float dx = (float)(pos.x - camPos.x);
                float dy = (float)(pos.y - camPos.y);
                float dz = (float)(pos.z - camPos.z);
                
                float glowSize = size * 2.5f;
                int glowColor = ColorAssist.replAlpha(baseColor, (int) (alpha * 0.3f * 255));
                
                float[] glowOffsets = {-glowSize, glowSize};
                for (float ox : glowOffsets) {
                    for (float oy : glowOffsets) {
                        float vx = ox * cosYaw - oy * sinPitch * sinYaw;
                        float vy = oy * cosPitch;
                        float vz = ox * sinYaw + oy * sinPitch * cosYaw;
                        buffer.vertex(dx + vx, dy + vy, dz + vz).texture(ox > 0 ? 1 : 0, oy > 0 ? 1 : 0).color(glowColor);
                    }
                }

                int coreColor = ColorAssist.replAlpha(baseColor, (int) (alpha * 255));
                float[] coreOffsets = {-size, size};
                for (float ox : coreOffsets) {
                    for (float oy : coreOffsets) {
                        float vx = ox * cosYaw - oy * sinPitch * sinYaw;
                        float vy = oy * cosPitch;
                        float vz = ox * sinYaw + oy * sinPitch * cosYaw;
                        buffer.vertex(dx + vx, dy + vy, dz + vz).texture(ox > 0 ? 1 : 0, oy > 0 ? 1 : 0).color(coreColor);
                    }
                }
                vertexCount += 8;
            }
        }

        if (vertexCount > 0) {
            BufferRenderer.drawWithGlobalProgram(buffer.end());
        }
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }

    private void renderTail(MatrixStack stack, float tickDelta) {
        if (mc.player == null) return;

        tailPoints.removeIf(point -> point.time.finished(tailLength.getValue()));

        Vec3d playerPos = new Vec3d(
                MathHelper.lerp(tickDelta, mc.player.prevX, mc.player.getX()),
                MathHelper.lerp(tickDelta, mc.player.prevY, mc.player.getY()),
                MathHelper.lerp(tickDelta, mc.player.prevZ, mc.player.getZ())
        );

        if (tailPoints.isEmpty() || tailPoints.get(tailPoints.size() - 1).pos.squaredDistanceTo(playerPos) > 0.001) {
            tailPoints.add(new TailPoint(playerPos));
        }

        if (tailPoints.size() < 2) return;

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SrcFactor.SRC_ALPHA, GlStateManager.DstFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.setShader(ShaderProgramKeys.POSITION_COLOR);

        BufferBuilder buffer = Tessellator.getInstance().begin(VertexFormat.DrawMode.TRIANGLE_STRIP, VertexFormats.POSITION_COLOR);
        Camera camera = mc.gameRenderer.getCamera();
        Vec3d camPos = camera.getPos();

        float playerHeight = mc.player.getHeight();
        int size = tailPoints.size();

        for (int i = 0; i < size - 1; i++) {
            TailPoint p1 = tailPoints.get(i);
            TailPoint p2 = tailPoints.get(i + 1);

            int color1 = ColorAssist.fade(3, i, ColorAssist.getClientColor(), ColorAssist.getClientColor(0.3f));
            int color2 = ColorAssist.fade(3, i + 1, ColorAssist.getClientColor(), ColorAssist.getClientColor(0.3f));
            float alpha1 = Math.min((float) i / (float) size, 1F);
            float alpha2 = Math.min((float) (i + 1) / (float) size, 1F);
            int finalColor1 = ColorAssist.replAlpha(color1, alpha1 / 2F);
            int finalColor2 = ColorAssist.replAlpha(color2, alpha2 / 2F);

            // Правильные координаты: от ног до головы
            float x1 = (float)(p1.pos.x - camPos.x);
            float y1_bottom = (float)(p1.pos.y - camPos.y);
            float y1_top = (float)(p1.pos.y + playerHeight - camPos.y);
            float z1 = (float)(p1.pos.z - camPos.z);
            
            float x2 = (float)(p2.pos.x - camPos.x);
            float y2_bottom = (float)(p2.pos.y - camPos.y);
            float y2_top = (float)(p2.pos.y + playerHeight - camPos.y);
            float z2 = (float)(p2.pos.z - camPos.z);

            // Рендерим полоску от ног до головы
            buffer.vertex(x1, y1_bottom, z1).color(finalColor1);
            buffer.vertex(x1, y1_top, z1).color(finalColor1);
            buffer.vertex(x2, y2_bottom, z2).color(finalColor2);
            buffer.vertex(x2, y2_top, z2).color(finalColor2);
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        RenderSystem.depthMask(true);
        RenderSystem.disableBlend();
    }


    private void calcTrajectory(Entity e) {
        double motionX = e.getVelocity().x;
        double motionY = e.getVelocity().y;
        double motionZ = e.getVelocity().z;
        double x = e.getX();
        double y = e.getY();
        double z = e.getZ();
        Vec3d lastPos = new Vec3d(x, y, z);

        for (int i = 0; i < 300; i++) {
            lastPos = new Vec3d(x, y, z);
            x += motionX;
            y += motionY;
            z += motionZ;

            if (mc.world.getBlockState(BlockPos.ofFloored(x, y, z)).getBlock() == Blocks.WATER) {
                motionX *= 0.8;
                motionY *= 0.8;
                motionZ *= 0.8;
            } else {
                motionX *= 0.99;
                motionY *= 0.99;
                motionZ *= 0.99;
            }

            if (e instanceof ArrowEntity) {
                motionY -= 0.05;
            } else {
                motionY -= 0.03f;
            }

            Vec3d pos = new Vec3d(x, y, z);

            HitResult hitResult = mc.world.raycast(new RaycastContext(lastPos, pos, RaycastContext.ShapeType.OUTLINE, RaycastContext.FluidHandling.NONE, mc.player));
            if (hitResult != null && hitResult.getType() != HitResult.Type.MISS) {
                break;
            }

            if (y <= -65) break;
            if (e.getVelocity().lengthSquared() < 0.0001) continue;

            int alpha = (int) MathHelper.clamp((255f * (i / 8f)), 0, 255);
            int lineColor = colorMode.isSelected("Sync")
                    ? ColorAssist.replAlpha(ColorAssist.getClientColor(), alpha)
                    : ColorAssist.replAlpha(customColor.getColor(), alpha);

            Render3D.drawLine(lastPos, pos, lineColor, 2f, true);
        }
    }


    private Color getColor(int index) {
        if (colorMode.isSelected("Sync")) {
            return new Color(ColorAssist.getClientColor());
        }
        return new Color(customColor.getColor());
    }

    private Identifier getParticleTexture() {
        return switch (particleMode.getSelected()) {
            case "Блум" -> BLOOM_TEXTURE;
            case "Звезды" -> STAR_TEXTURE;
            case "Сакура" -> SAKURA_TEXTURE;
            case "Луна" -> MOON_TEXTURE;
            case "Треугольник" -> TRIANGLE_TEXTURE;
            case "Куб" -> CUBE_TEXTURE;
            case "Крест" -> MCROSS_TEXTURE;
            default -> SPARK_TEXTURE;
        };
    }

    private float randomFloat(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }

    private Vec3d lerpVec(Vec3d a, Vec3d b, float t) {
        t = MathHelper.clamp(t, 0f, 1f);
        return new Vec3d(
                MathHelper.lerp(t, a.x, b.x),
                MathHelper.lerp(t, a.y, b.y),
                MathHelper.lerp(t, a.z, b.z)
        );
    }


    private record KagunePoint(Vec3d pos, long createdAt) {}

    private static class TailPoint {
        final Vec3d pos;
        final StopWatch time = new StopWatch();

        TailPoint(Vec3d pos) {
            this.pos = pos;
        }
    }

    private class Particle {
        double x, y, z;
        double motionX, motionY, motionZ;
        long time;
        Color color;
        float rotation;
        float rotationSpeed;

        Particle(double x, double y, double z, Color color) {
            this.x = x;
            this.y = y;
            this.z = z;
            float speed = 0.02f;
            this.motionX = randomFloat(-speed, speed);
            this.motionY = randomFloat(-speed, speed);
            this.motionZ = randomFloat(-speed, speed);
            this.time = System.currentTimeMillis();
            this.color = color;
            this.rotation = randomFloat(0, 360);
            this.rotationSpeed = randomFloat(-3f, 3f);
        }

        void update() {
            x += motionX;
            y += motionY;
            z += motionZ;

            rotation += rotationSpeed;

            if (physics.isSelected("Падение")) {
                motionY -= 0.001f;
            }

            motionX *= 0.98;
            motionY *= 0.98;
            motionZ *= 0.98;
            rotationSpeed *= 0.99f;

            if (mc.world != null) {
                Block block = mc.world.getBlockState(BlockPos.ofFloored(x, y - 0.1, z)).getBlock();
                if (block != Blocks.AIR && block != Blocks.WATER && block != Blocks.LAVA) {
                    motionY = Math.abs(motionY) * 0.3;
                }
            }
        }
    }
}
