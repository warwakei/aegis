package rich.modules.impl.render;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.world.Heightmap;
import net.minecraft.world.chunk.WorldChunk;
import rich.events.api.EventHandler;
import rich.events.impl.WorldRenderEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.ColorSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.ColorUtil;
import rich.util.config.impl.blockesp.BlockESPConfig;
import rich.util.render.Render3D;
import rich.util.string.chat.ChatMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class BlockESP extends ModuleStructure {
    ColorSetting color = new ColorSetting("Цвет", "Цвет подсветки блоков").value(ColorUtil.getColor(255, 0, 0, 255));
    SliderSettings range = new SliderSettings("Радиус", "Радиус поиска блоков").range(1, 128).setValue(32);
    BooleanSetting notifyInChat = new BooleanSetting("Уведомления", "Показывать координаты найденных блоков в чате").setValue(false);

    Set<String> blocksToHighlight = new CopyOnWriteArraySet<>();
    Map<BlockPos, BlockState> renderBlocks = new HashMap<>();
    Set<BlockPos> notifiedBlocks = new CopyOnWriteArraySet<>();
    long lastScanTime = 0;
    int checkCounter = 0;

    public BlockESP() {
        super("BlockESP", "Block ESP", ModuleCategory.RENDER);
        settings(color, range, notifyInChat);
    }

    public Set<String> getBlocksToHighlight() {
        return blocksToHighlight;
    }

    @Override
    public void activate() {
        blocksToHighlight.clear();
        blocksToHighlight.addAll(BlockESPConfig.getInstance().getBlocks());
        notifiedBlocks.clear();
    }

    @Override
    public void deactivate() {
        renderBlocks.clear();
        notifiedBlocks.clear();
    }

    @EventHandler
    public void onRender3D(WorldRenderEvent event) {
        if (!state || mc.world == null || mc.player == null) {
            renderBlocks.clear();
            return;
        }
        if (blocksToHighlight.isEmpty()) {
            renderBlocks.clear();
            return;
        }
        BlockPos playerPos = mc.player.getBlockPos();
        long currentTime = System.nanoTime() / 1_000_000;
        if (currentTime - lastScanTime >= 2000) {
            renderBlocks.clear();
            int chunkRange = 2;
            int yRange = 48;
            for (int x = -chunkRange; x <= chunkRange; x++) {
                for (int z = -chunkRange; z <= chunkRange; z++) {
                    int chunkX = (playerPos.getX() >> 4) + x;
                    int chunkZ = (playerPos.getZ() >> 4) + z;
                    if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) continue;
                    WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(chunkX, chunkZ);
                    if (chunk == null) continue;
                    int cx = chunk.getPos().x << 4;
                    int cz = chunk.getPos().z << 4;
                    for (int bx = 0; bx < 16; bx++) {
                        for (int bz = 0; bz < 16; bz++) {
                            int minY = Math.max(mc.world.getBottomY(), playerPos.getY() - yRange);
                            int maxY = Math.min(mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, cx + bx, cz + bz), playerPos.getY() + yRange);
                            for (int by = minY; by <= maxY; by++) {
                                BlockPos pos = new BlockPos(cx + bx, by, cz + bz);
                                double dist = mc.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                                if (dist > range.getValue() * range.getValue()) continue;
                                Block block = mc.world.getBlockState(pos).getBlock();
                                String blockName = Registries.BLOCK.getId(block).toString();
                                if (blocksToHighlight.contains(blockName)) {
                                    renderBlocks.put(pos.toImmutable(), mc.world.getBlockState(pos));
                                    if (notifyInChat.isValue() && !notifiedBlocks.contains(pos)) {
                                        notifyBlockFound(pos, blockName);
                                        notifiedBlocks.add(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
            lastScanTime = currentTime;
            checkCounter = 0;
        }
        if (checkCounter % 5 == 0) {
            int nearChunkRange = 1;
            for (int x = -nearChunkRange; x <= nearChunkRange; x++) {
                for (int z = -nearChunkRange; z <= nearChunkRange; z++) {
                    int chunkX = (playerPos.getX() >> 4) + x;
                    int chunkZ = (playerPos.getZ() >> 4) + z;
                    if (!mc.world.getChunkManager().isChunkLoaded(chunkX, chunkZ)) continue;
                    WorldChunk chunk = mc.world.getChunkManager().getWorldChunk(chunkX, chunkZ);
                    if (chunk == null) continue;
                    int cx = chunk.getPos().x << 4;
                    int cz = chunk.getPos().z << 4;
                    for (int bx = 0; bx < 16; bx++) {
                        for (int bz = 0; bz < 16; bz++) {
                            int minY = Math.max(mc.world.getBottomY(), playerPos.getY() - 24);
                            int maxY = Math.min(mc.world.getTopY(Heightmap.Type.WORLD_SURFACE, cx + bx, cz + bz), playerPos.getY() + 24);
                            for (int by = minY; by <= maxY; by++) {
                                BlockPos pos = new BlockPos(cx + bx, by, cz + bz);
                                double dist = mc.player.squaredDistanceTo(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5);
                                if (dist > 4 * 4) continue;
                                Block block = mc.world.getBlockState(pos).getBlock();
                                String blockName = Registries.BLOCK.getId(block).toString();
                                if (blocksToHighlight.contains(blockName) && !renderBlocks.containsKey(pos)) {
                                    renderBlocks.put(pos.toImmutable(), mc.world.getBlockState(pos));
                                    if (notifyInChat.isValue() && !notifiedBlocks.contains(pos)) {
                                        notifyBlockFound(pos, blockName);
                                        notifiedBlocks.add(pos);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (checkCounter % 60 == 0) {
            renderBlocks.entrySet().removeIf(entry -> {
                BlockPos pos = entry.getKey();
                Block block = mc.world.getBlockState(pos).getBlock();
                String blockName = Registries.BLOCK.getId(block).toString();
                boolean shouldRemove = !blocksToHighlight.contains(blockName);
                if (shouldRemove) {
                    notifiedBlocks.remove(pos);
                }
                return shouldRemove;
            });
        }
        checkCounter++;
        renderBlocks.forEach((pos, blockState) -> {
            Render3D.drawBox(new Box(pos), color.getColor(), 1);
        });
    }

    private void notifyBlockFound(BlockPos pos, String blockName) {
        if (mc.player != null) {
            ChatMessage.brandmessage("Найден блок " + blockName + " -> " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ());
        }
    }
}