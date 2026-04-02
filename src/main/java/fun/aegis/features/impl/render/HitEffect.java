package fun.aegis.features.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.display.geometry.Render3D;
import fun.aegis.events.render.WorldRenderEvent;

import java.util.*;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class HitEffect extends Module {
    public static HitEffect getInstance() {
        return Instance.get(HitEffect.class);
    }

    final List<WaveEffect> waveEffects = new ArrayList<>();

    public HitEffect() {
        super("HitEffect", "Hit Effect", ModuleCategory.RENDER);
    }

    public void addWave(BlockPos pos) {
        if (mc.world != null) {
            waveEffects.add(new WaveEffect(pos, System.currentTimeMillis()));
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
        private final long duration = 2000;
        private final int maxRadius = 15;

        public WaveEffect(BlockPos centerPos, long startTime) {
            this.centerPos = centerPos;
            this.startTime = startTime;
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - startTime > duration;
        }

        public void render() {
            if (mc.world == null) return;

            long elapsed = System.currentTimeMillis() - startTime;
            float progress = (float) elapsed / duration;
            float currentRadius = progress * maxRadius;
            float waveWidth = 3.0f;

            float globalAlpha = 1.0f - progress;
            globalAlpha = (float) Math.pow(globalAlpha, 0.5);

            int rendered = 0;
            int maxPerFrame = 350;

            for (int x = -maxRadius; x <= maxRadius; x++) {
                for (int z = -maxRadius; z <= maxRadius; z++) {
                    if (rendered >= maxPerFrame) return;

                    double distance = Math.sqrt(x * x + z * z);

                    if (distance < currentRadius - waveWidth || distance > currentRadius + 0.5) continue;

                    BlockPos checkPos = centerPos.add(x, 0, z);
                    BlockPos renderPos = findBlock(checkPos);

                    if (renderPos == null) continue;

                    BlockState state = mc.world.getBlockState(renderPos);
                    if (state.isAir()) continue;

                    VoxelShape shape = state.getOutlineShape(mc.world, renderPos);
                    if (shape.isEmpty()) continue;

                    rendered++;

                    float localAlpha = 1.0f - (float) Math.abs(distance - currentRadius) / waveWidth;
                    localAlpha = Math.max(0, Math.min(1, localAlpha));
                    localAlpha *= globalAlpha;

                    if (localAlpha > 0.02f) {
                        int baseColor = ColorAssist.getClientColor();
                        int color = ColorAssist.setAlpha(baseColor, (int) (localAlpha * 255));

                        float heightWave = (float) (Math.sin(distance * 0.35 - progress * Math.PI * 2.8) * 0.7 + 0.7);
                        heightWave *= globalAlpha;
                        int heightOffset = Math.max(0, (int) (heightWave * 3));

                        float lineWidth = 2.2f + (1.0f - localAlpha) * 2.5f;

                        try {
                            Render3D.drawShapeAlternative(
                                    renderPos.up(heightOffset),
                                    shape,
                                    color,
                                    lineWidth,
                                    true,
                                    true
                            );
                        } catch (Exception ignored) {
                        }
                    }
                }
            }
        }

        private BlockPos findBlock(BlockPos start) {
            if (!mc.world.isInBuildLimit(start)) return null;

            BlockState state = mc.world.getBlockState(start);
            if (!state.isAir()) return start;

            for (int y = 1; y <= 10; y++) {
                BlockPos down = start.down(y);
                if (mc.world.isInBuildLimit(down)) {
                    BlockState downState = mc.world.getBlockState(down);
                    if (!downState.isAir()) return down;
                }

                BlockPos up = start.up(y);
                if (mc.world.isInBuildLimit(up)) {
                    BlockState upState = mc.world.getBlockState(up);
                    if (!upState.isAir()) return up;
                }
            }

            return null;
        }
    }
}
