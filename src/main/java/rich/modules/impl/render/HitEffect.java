package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import rich.events.api.EventHandler;
import rich.events.impl.AttackEvent;
import rich.events.impl.WorldRenderEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.ColorSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.ColorUtil;
import rich.util.Instance;
import rich.util.render.Render3D;

import java.awt.*;
import java.util.*;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class HitEffect extends ModuleStructure {
    public static HitEffect getInstance() {
        return Instance.get(HitEffect.class);
    }

    final List<WaveEffect> waveEffects = Collections.synchronizedList(new ArrayList<>());

    public ColorSetting primaryColor = new ColorSetting("Основной цвет", "Цвет волны")
            .setColor(new Color(137, 97, 72, 255).getRGB());

    public ColorSetting secondaryColor = new ColorSetting("Вторичный цвет", "Цвет для градиента волны")
            .setColor(new Color(255, 170, 85, 255).getRGB());

    public BooleanSetting gradientWave = new BooleanSetting("Градиент волны", "Использовать градиент в волне")
            .setValue(true);

    public SliderSettings waveLayers = new SliderSettings("Слои волны", "Количество слоёв волны")
            .range(1, 5).setValue(3);

    public BooleanSetting spawnParticles = new BooleanSetting("Частицы волны", "Спавнить частицы при волне")
            .setValue(true);

    public SliderSettings particleDensity = new SliderSettings("Плотность частиц", "Количество частиц на блоке")
            .range(0, 5).setValue(2)
            .visible(() -> spawnParticles.isValue());
            
    public BooleanSetting glowEffect = new BooleanSetting("Эффект свечения", "Добавляет свечение к волне")
            .setValue(true);
            
    public SliderSettings waveSpeed = new SliderSettings("Скорость волны", "Скорость распространения волны")
            .range(0.5f, 3.0f).setValue(1.5f);

    public HitEffect() {
        super("HitEffect", "Hit Effect", ModuleCategory.RENDER);
        settings(primaryColor, secondaryColor, gradientWave, waveLayers, spawnParticles, particleDensity, glowEffect, waveSpeed);
    }

    public void addWave(BlockPos pos) {
        if (mc.world != null && pos != null) {
            BlockPos groundPos = findGround(pos);
            if (groundPos != null) {
                waveEffects.add(new WaveEffect(groundPos, System.currentTimeMillis()));
                playHitSound();
            }
        }
    }

    private void playHitSound() {
        try {
            net.minecraft.sound.SoundEvent sound = net.minecraft.registry.Registries.SOUND_EVENT.get(
                    net.minecraft.util.Identifier.of("rich", "metallic")
            );
            if (sound != null && mc.world != null && mc.player != null) {
                mc.world.playSound(mc.player, mc.player.getX(), mc.player.getY(), mc.player.getZ(),
                        sound, net.minecraft.sound.SoundCategory.PLAYERS, 0.6f, 1.0f);
            }
        } catch (Exception ignored) {
        }
    }

    private BlockPos findGround(BlockPos pos) {
        for (int y = 0; y <= 10; y++) {
            BlockPos down = pos.down(y);
            if (mc.world.isInBuildLimit(down) && !mc.world.getBlockState(down).isAir()) {
                return down;
            }
        }
        return pos;
    }

    @EventHandler
    public void onAttack(AttackEvent e) {
        if (!isState()) return;
        if (e.getTarget() != null) {
            addWave(e.getTarget().getBlockPos());
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (waveEffects.isEmpty() || mc.world == null) return;

        Iterator<WaveEffect> iterator = waveEffects.iterator();
        while (iterator.hasNext()) {
            WaveEffect wave = iterator.next();
            if (wave.isExpired()) {
                iterator.remove();
                continue;
            }
            wave.render();
        }
    }

    private class WaveEffect {
        private final BlockPos centerPos;
        private final long startTime;
        private final long duration = 600;
        private final int maxRadius = 10;
        private Map<Long, Integer> reachableBlocks;
        private boolean calculated = false;

        public WaveEffect(BlockPos centerPos, long startTime) {
            this.centerPos = centerPos;
            this.startTime = startTime;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - startTime > duration;
        }

        private void calculateReachableBlocks() {
            if (calculated) return;
            calculated = true;

            reachableBlocks = new HashMap<>();
            Queue<BlockPos> queue = new LinkedList<>();
            Map<Long, Integer> visited = new HashMap<>();

            BlockPos startPos = centerPos;
            if (mc.world.getBlockState(startPos).isAir()) {
                for (int y = 1; y <= 5; y++) {
                    BlockPos down = startPos.down(y);
                    if (!mc.world.getBlockState(down).isAir()) {
                        startPos = down;
                        break;
                    }
                }
            }

            queue.add(startPos);
            visited.put(startPos.asLong(), 0);

            while (!queue.isEmpty()) {
                BlockPos current = queue.poll();
                int currentDistance = visited.get(current.asLong());

                if (currentDistance > maxRadius) continue;

                BlockState state = mc.world.getBlockState(current);
                if (!state.isAir()) {
                    VoxelShape shape = state.getOutlineShape(mc.world, current);
                    if (!shape.isEmpty()) {
                        reachableBlocks.put(current.asLong(), currentDistance);
                    }
                }

                for (Direction dir : Direction.values()) {
                    BlockPos neighbor = current.offset(dir);

                    if (!mc.world.isInBuildLimit(neighbor)) continue;

                    long neighborLong = neighbor.asLong();
                    int newDistance = currentDistance + 1;

                    if (visited.containsKey(neighborLong) && visited.get(neighborLong) <= newDistance) continue;
                    if (newDistance > maxRadius) continue;

                    BlockState neighborState = mc.world.getBlockState(neighbor);

                    if (!neighborState.isAir()) {
                        visited.put(neighborLong, newDistance);
                        queue.add(neighbor);
                    } else {
                        BlockPos below = neighbor.down();
                        if (mc.world.isInBuildLimit(below) && !mc.world.getBlockState(below).isAir()) {
                            long belowLong = below.asLong();
                            if (!visited.containsKey(belowLong) || visited.get(belowLong) > newDistance) {
                                visited.put(belowLong, newDistance);
                                queue.add(below);
                            }
                        }

                        BlockPos above = neighbor.up();
                        if (mc.world.isInBuildLimit(above) && !mc.world.getBlockState(above).isAir()) {
                            long aboveLong = above.asLong();
                            if (!visited.containsKey(aboveLong) || visited.get(aboveLong) > newDistance) {
                                visited.put(aboveLong, newDistance);
                                queue.add(above);
                            }
                        }
                    }
                }
            }
        }

        public void render() {
            if (mc.world == null) return;

            calculateReachableBlocks();

            if (reachableBlocks == null || reachableBlocks.isEmpty()) return;

            long elapsed = System.currentTimeMillis() - startTime;
            float progress = (float) elapsed / duration;
            float currentRadius = progress * maxRadius * waveSpeed.getValue();
            float waveWidth = 6.0f;

            // Улучшенная пульсация с несколькими частотами
            float pulse1 = (float) Math.sin(progress * Math.PI * 6) * 0.2f + 1.0f;
            float pulse2 = (float) Math.cos(progress * Math.PI * 8) * 0.15f + 1.0f;
            float combinedPulse = (pulse1 + pulse2) * 0.5f;
            
            float globalAlpha = 1.0f - progress;
            globalAlpha = (float) Math.pow(globalAlpha, 0.4); // Более плавное затухание

            int rendered = 0;
            int maxPerFrame = 800; // Увеличил лимит для красоты
            int numLayers = (int) waveLayers.getValue();

            for (Map.Entry<Long, Integer> entry : reachableBlocks.entrySet()) {
                if (rendered >= maxPerFrame) break;

                int blockDistance = entry.getValue();

                for (int layer = 0; layer < numLayers; layer++) {
                    float layerOffset = layer * 0.8f;
                    float layerRadius = currentRadius + layerOffset;
                    float layerAlpha = globalAlpha * (1.0f - layer * 0.4f);

                    if (blockDistance < layerRadius - waveWidth || blockDistance > layerRadius + 1.0f) continue;

                    BlockPos pos = BlockPos.fromLong(entry.getKey());
                    BlockState state = mc.world.getBlockState(pos);

                    if (state.isAir()) continue;

                    VoxelShape shape = state.getOutlineShape(mc.world, pos);
                    if (shape.isEmpty()) continue;

                    rendered++;

                    float localAlpha = 1.0f - Math.abs(blockDistance - layerRadius) / waveWidth;
                    localAlpha = Math.max(0, Math.min(1, localAlpha));
                    localAlpha *= layerAlpha * combinedPulse;

                    if (localAlpha > 0.01f) {
                        int baseColor = gradientWave.isValue() ?
                                ColorUtil.lerpColor(primaryColor.getColor(), secondaryColor.getColor(), (float) blockDistance / maxRadius) :
                                primaryColor.getColor();
                        
                        // Эффект свечения
                        if (glowEffect.isValue()) {
                            float glowIntensity = localAlpha * 1.0f;
                            int glowColor = ColorUtil.setAlpha(baseColor, (int) (glowIntensity * 140));
                            
                            // Рендерим свечение (больший размер, меньшая прозрачность)
                            try {
                                Render3D.drawShapeAlternative(
                                        pos,
                                        shape,
                                        glowColor,
                                        1.4f, // Увеличенный размер для свечения
                                        true,
                                        true
                                );
                            } catch (Exception ignored) {
                            }
                        }
                        
                        int color = ColorUtil.setAlpha(baseColor, (int) (localAlpha * 100));

                        try {
                            Render3D.drawShapeAlternative(
                                    pos,
                                    shape,
                                    color,
                                    1f,
                                    true,
                                    true
                            );
                        } catch (Exception ignored) {
                        }

                        // Улучшенные частицы
                        if (spawnParticles.isValue() && particleDensity.getInt() > 0 && localAlpha > 0.2f) {
                            spawnEnhancedParticles(pos, localAlpha, blockDistance, progress);
                        }
                    }
                }
            }
        }

        private void spawnEnhancedParticles(BlockPos pos, float alpha, int distance, float waveProgress) {
            int density = particleDensity.getInt();
            if (density == 0) return;

            int color = ColorUtil.lerpColor(primaryColor.getColor(), secondaryColor.getColor(), (float) distance / maxRadius);
            color = ColorUtil.setAlpha(color, (int) (alpha * 150));

            double centerX = centerPos.getX() + 0.5;
            double centerZ = centerPos.getZ() + 0.5;
            double dx = pos.getX() + 0.5 - centerX;
            double dz = pos.getZ() + 0.5 - centerZ;
            double distToCenter = Math.sqrt(dx * dx + dz * dz);
            
            double dirX = distToCenter > 0 ? dx / distToCenter : 0;
            double dirZ = distToCenter > 0 ? dz / distToCenter : 0;

            // Увеличенное количество частиц для красоты
            for (int i = 0; i < density * 2; i++) {
                double px = pos.getX() + 0.5 + (Math.random() - 0.5) * 0.8;
                double py = pos.getY() + 0.5 + Math.random() * 0.6;
                double pz = pos.getZ() + 0.5 + (Math.random() - 0.5) * 0.8;

                // Более динамичное движение частиц
                double outwardForce = 0.08 + Math.random() * 0.06;
                double upwardForce = Math.random() * 0.15 + 0.05;
                
                // Спиральное движение для красоты
                double spiralAngle = waveProgress * Math.PI * 4 + i * 0.5;
                double spiralRadius = 0.03;
                
                double velX = dirX * outwardForce + Math.cos(spiralAngle) * spiralRadius;
                double velY = upwardForce + Math.sin(waveProgress * Math.PI * 2) * 0.02;
                double velZ = dirZ * outwardForce + Math.sin(spiralAngle) * spiralRadius;

                Particles particlesMod = Particles.getInstance();
                if (particlesMod != null && particlesMod.isState()) {
                    // Частицы с улучшенной физикой и красивыми траекториями
                }
            }
        }
    }
}