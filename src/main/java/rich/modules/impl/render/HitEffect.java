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
import rich.modules.module.setting.implement.ColorSetting;
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

    public ColorSetting colorSetting = new ColorSetting("Цвет", "Выберите цвет для эффекта")
            .setColor(new Color(137, 97, 72, 255).getRGB());

    public HitEffect() {
        super("HitEffect", "Hit Effect", ModuleCategory.RENDER);
        settings(colorSetting);
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
        private final long duration = 475;
        private final int maxRadius = 8;
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
            float waveWidth = 2.5f;

            float globalAlpha = 1.0f - progress;
            globalAlpha = (float) Math.pow(globalAlpha, 0.5);

            int rendered = 0;
            int maxPerFrame = 500;

            for (Map.Entry<Long, Integer> entry : reachableBlocks.entrySet()) {
                if (rendered >= maxPerFrame) break;

                int blockDistance = entry.getValue();

                if (blockDistance < currentRadius - waveWidth || blockDistance > currentRadius + 0.5f) continue;

                BlockPos pos = BlockPos.fromLong(entry.getKey());
                BlockState state = mc.world.getBlockState(pos);

                if (state.isAir()) continue;

                VoxelShape shape = state.getOutlineShape(mc.world, pos);
                if (shape.isEmpty()) continue;

                rendered++;

                float localAlpha = 1.0f - Math.abs(blockDistance - currentRadius) / waveWidth;
                localAlpha = Math.max(0, Math.min(1, localAlpha));
                localAlpha *= globalAlpha;

                if (localAlpha > 0.02f) {
                    int baseColor = colorSetting.getColor();
                    int color = ColorUtil.setAlpha(baseColor, (int) (localAlpha * 75));

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
                }
            }
        }
    }
}