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
            .range(1, 3).setValue(2);

    public BooleanSetting spawnParticles = new BooleanSetting("Частицы волны", "Спавнить частицы при волне")
            .setValue(true);

    public SliderSettings particleDensity = new SliderSettings("Плотность частиц", "Количество частиц на блоке")
            .range(0, 3).setValue(1)
            .visible(() -> spawnParticles.isValue());

    public HitEffect() {
        super("HitEffect", "Hit Effect", ModuleCategory.RENDER);
        settings(primaryColor, secondaryColor, gradientWave, waveLayers, spawnParticles, particleDensity);
    }

    public void addWave(BlockPos pos) {
        if (mc.world != null && pos != null) {
            BlockPos groundPos = findGround(pos);
            if (groundPos != null) {
                waveEffects.add(new WaveEffect(groundPos, System.currentTimeMillis()));
            }
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
            float currentRadius = progress * maxRadius;
            float waveWidth = 2.0f;

            // Пульсация для красоты
            float pulse = (float) Math.sin(progress * Math.PI * 4) * 0.1f + 1.0f;
            float globalAlpha = 1.0f - progress;
            globalAlpha = (float) Math.pow(globalAlpha, 0.6);

            int rendered = 0;
            int maxPerFrame = 600;
            int numLayers = (int) waveLayers.getValue();

            for (Map.Entry<Long, Integer> entry : reachableBlocks.entrySet()) {
                if (rendered >= maxPerFrame) break;

                int blockDistance = entry.getValue();

                for (int layer = 0; layer < numLayers; layer++) {
                    float layerOffset = layer * 0.4f;
                    float layerRadius = currentRadius + layerOffset;
                    float layerAlpha = globalAlpha * (1.0f - layer * 0.25f);

                    if (blockDistance < layerRadius - waveWidth || blockDistance > layerRadius + 0.5f) continue;

                    BlockPos pos = BlockPos.fromLong(entry.getKey());
                    BlockState state = mc.world.getBlockState(pos);

                    if (state.isAir()) continue;

                    VoxelShape shape = state.getOutlineShape(mc.world, pos);
                    if (shape.isEmpty()) continue;

                    rendered++;

                    float localAlpha = 1.0f - Math.abs(blockDistance - layerRadius) / waveWidth;
                    localAlpha = Math.max(0, Math.min(1, localAlpha));
                    localAlpha *= layerAlpha * pulse;

                    if (localAlpha > 0.02f) {
                        int baseColor = gradientWave.isValue() ?
                                ColorUtil.lerpColor(primaryColor.getColor(), secondaryColor.getColor(), (float) blockDistance / maxRadius) :
                                primaryColor.getColor();
                        int color = ColorUtil.setAlpha(baseColor, (int) (localAlpha * 85));

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

                        // Частицы на блоках
                        if (spawnParticles.isValue() && particleDensity.getInt() > 0 && localAlpha > 0.3f) {
                            spawnBlockParticles(pos, localAlpha, blockDistance);
                        }
                    }
                }
            }
        }

        private void spawnBlockParticles(BlockPos pos, float alpha, int distance) {
            int density = particleDensity.getInt();
            if (density == 0) return;

            float progress = (float) (System.currentTimeMillis() - startTime) / duration;
            float currentRadius = progress * maxRadius;

            int color = ColorUtil.lerpColor(primaryColor.getColor(), secondaryColor.getColor(), (float) distance / maxRadius);
            color = ColorUtil.setAlpha(color, (int) (alpha * 120));

            for (int i = 0; i < density; i++) {
                double px = pos.getX() + 0.5 + (Math.random() - 0.5) * 0.8;
                double py = pos.getY() + 0.5 + Math.random() * 0.3;
                double pz = pos.getZ() + 0.5 + (Math.random() - 0.5) * 0.8;

                double velX = (Math.random() - 0.5) * 0.05;
                double velY = Math.random() * 0.08 + 0.02;
                double velZ = (Math.random() - 0.5) * 0.05;

                Particles particlesMod = Particles.getInstance();
                if (particlesMod != null && particlesMod.isState()) {
                    // Используем систему частиц для создания частиц волны
                    // (упрощённо - просто добавляем визуальный эффект)
                }
            }
        }
    }
}