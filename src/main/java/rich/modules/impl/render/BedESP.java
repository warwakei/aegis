package rich.modules.impl.render;

import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
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
import rich.util.render.Render3D;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class BedESP extends ModuleStructure {
    
    ColorSetting color = new ColorSetting("Цвет", "Цвет подсветки кроватей").value(ColorUtil.getColor(255, 0, 0, 255));
    SliderSettings range = new SliderSettings("Радиус", "Радиус поиска кроватей").range(1, 128).setValue(64);
    BooleanSetting showDistance = new BooleanSetting("Дистанция", "Показывать дистанцию до кровати").setValue(true);

    Map<BlockPos, BlockState> renderBeds = new HashMap<>();
    Set<BlockPos> checkedBeds = new CopyOnWriteArraySet<>();
    long lastScanTime = 0;
    int checkCounter = 0;
    
    int myTeamColor = -1;
    long lastColorCheckTime = 0;

    public BedESP() {
        super("BedESP", "Bed ESP", ModuleCategory.RENDER);
        settings(color, range, showDistance);
    }

    @Override
    public void activate() {
        renderBeds.clear();
        checkedBeds.clear();
        myTeamColor = -1;
        lastColorCheckTime = 0;
    }

    @Override
    public void deactivate() {
        renderBeds.clear();
        checkedBeds.clear();
    }
    
    /**
     * Получает цвет команды игрока по шлему
     * Упрощённая версия - проверяет только наличие кожаного шлема
     */
    private int getMyTeamColor() {
        if (mc.player == null) return -1;
        
        ItemStack helmet = mc.player.getEquippedStack(EquipmentSlot.HEAD);
        if (helmet.isEmpty()) return -1;
        if (helmet.getItem() != Items.LEATHER_HELMET) return -1;
        
        // Возвращаем -1 так как определение цвета через компонент недоступно
        // Просто проверяем что у нас кожаный шлем
        return 0;
    }
    
    /**
     * Проверяет является ли кровать кроватью противника
     */
    private boolean isEnemyBed(BlockPos pos) {
        if (mc.world == null) return false;
        
        BlockState state = mc.world.getBlockState(pos);
        if (!(state.getBlock() instanceof BedBlock)) return false;
        
        // Проверяем цвет кровати (в BedWars кровати это окрашенные блоки)
        // Кровать имеет цвет в зависимости от типа (red_bed, blue_bed, etc.)
        String bedName = state.getBlock().getTranslationKey();
        
        // Если это не кровать - пропускаем
        if (!bedName.contains("bed")) return false;
        
        // Если у нас нет цвета команды - считаем что это враг
        if (myTeamColor == -1) return true;
        
        // Определяем цвет кровати по названию
        int bedColor = getBedColorFromName(bedName);
        
        // Если цвета не совпадают - это вражеская кровать
        return bedColor != -1 && bedColor != myTeamColor;
    }
    
    /**
     * Получает цвет кровати из названия блока
     */
    private int getBedColorFromName(String bedName) {
        if (bedName.contains("red")) return ColorUtil.getColor(255, 0, 0, 255);
        if (bedName.contains("blue")) return ColorUtil.getColor(0, 0, 255, 255);
        if (bedName.contains("yellow")) return ColorUtil.getColor(255, 255, 0, 255);
        if (bedName.contains("green")) return ColorUtil.getColor(0, 255, 0, 255);
        if (bedName.contains("white")) return ColorUtil.getColor(255, 255, 255, 255);
        if (bedName.contains("black")) return ColorUtil.getColor(0, 0, 0, 255);
        if (bedName.contains("gray")) return ColorUtil.getColor(128, 128, 128, 255);
        if (bedName.contains("cyan")) return ColorUtil.getColor(0, 255, 255, 255);
        if (bedName.contains("purple")) return ColorUtil.getColor(128, 0, 128, 255);
        if (bedName.contains("orange")) return ColorUtil.getColor(255, 165, 0, 255);
        if (bedName.contains("pink")) return ColorUtil.getColor(255, 192, 203, 255);
        if (bedName.contains("brown")) return ColorUtil.getColor(139, 69, 19, 255);
        if (bedName.contains("lime")) return ColorUtil.getColor(50, 205, 50, 255);
        if (bedName.contains("light_gray")) return ColorUtil.getColor(211, 211, 211, 255);
        if (bedName.contains("magenta")) return ColorUtil.getColor(255, 0, 255, 255);
        return -1;
    }

    @EventHandler
    public void onRender3D(WorldRenderEvent event) {
        if (!state || mc.world == null || mc.player == null) {
            renderBeds.clear();
            return;
        }
        
        // Обновляем цвет команды раз в 5 секунд
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastColorCheckTime >= 5000) {
            myTeamColor = getMyTeamColor();
            lastColorCheckTime = currentTime;
        }
        
        BlockPos playerPos = mc.player.getBlockPos();
        long nanoTime = System.nanoTime() / 1_000_000;
        
        // Полное сканирование каждые 2 секунды
        if (nanoTime - lastScanTime >= 2000) {
            renderBeds.clear();
            checkedBeds.clear();
            
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
                                
                                if (isEnemyBed(pos)) {
                                    renderBeds.put(pos.toImmutable(), mc.world.getBlockState(pos));
                                }
                            }
                        }
                    }
                }
            }
            
            lastScanTime = nanoTime;
            checkCounter = 0;
        }
        
        // Быстрая проверка ближней зоны
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
                                
                                if (isEnemyBed(pos) && !renderBeds.containsKey(pos)) {
                                    renderBeds.put(pos.toImmutable(), mc.world.getBlockState(pos));
                                }
                            }
                        }
                    }
                }
            }
        }
        
        // Очистка удалённых кроватей
        if (checkCounter % 60 == 0) {
            renderBeds.entrySet().removeIf(entry -> {
                BlockPos pos = entry.getKey();
                if (!isEnemyBed(pos)) {
                    checkedBeds.remove(pos);
                    return true;
                }
                return false;
            });
        }
        
        checkCounter++;
        
        // Рендер кроватей
        renderBeds.forEach((pos, blockState) -> {
            Box box = new Box(pos);
            Render3D.drawBox(box, color.getColor(), 1);
        });
    }
}
