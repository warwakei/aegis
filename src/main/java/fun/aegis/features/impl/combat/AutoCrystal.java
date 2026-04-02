package fun.aegis.features.impl.combat;

import fun.aegis.common.repository.friend.FriendUtils;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.MultiSelectSetting;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import fun.aegis.utils.interactions.inv.InventoryFlowManager;
import fun.aegis.utils.interactions.inv.InventoryTask;
import fun.aegis.utils.math.script.Script;
import fun.aegis.utils.math.time.StopWatch;
import fun.aegis.events.player.EntitySpawnEvent;
import fun.aegis.events.player.TickEvent;
import fun.aegis.utils.client.managers.event.EventHandler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.Comparator;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoCrystal extends Module {
    private final Script script = new Script();
    private final StopWatch placeCooldown = new StopWatch();
    private final StopWatch breakCooldown = new StopWatch();
    private final StopWatch obsidianCooldown = new StopWatch();

    private final SliderSettings range = new SliderSettings("Дистанция", "Дистанция до таргета")
            .range(3.0f, 15.0f)
            .setValue(8.0f);

    private final SliderSettings placeDelay = new SliderSettings("Задержка размещения", "Задержка между размещениями кристаллов (мс)")
            .range(50f, 500f)
            .setValue(100f);

    private final SliderSettings breakDelay = new SliderSettings("Задержка разрушения", "Задержка между разрушениями кристаллов (мс)")
            .range(50f, 500f)
            .setValue(50f);

    private final BooleanSetting autoObsidian = new BooleanSetting("Авто обсидиан", "Автоматически ставить обсидиан рядом с врагом")
            .setValue(false);

    private final SliderSettings obsidianDelay = new SliderSettings("Задержка обсидиана", "Задержка между размещениями обсидиана (мс)")
            .range(50f, 500f)
            .setValue(100f)
            .visible(() -> autoObsidian.isValue());

    private final MultiSelectSetting protections = new MultiSelectSetting("Защита", "Что не взрывать")
            .value("Себя", "Друзей", "Ресурсы")
            .selected("Себя", "Друзей");

    private final SliderSettings itemRange = new SliderSettings("Дистанция до ресурсов", "Минимальное расстояние до ресурсов")
            .range(1.0f, 12.0f)
            .setValue(6.0f)
            .visible(() -> protections.isSelected("Ресурсы"));

    public AutoCrystal() {
        super("AutoCrystal", "Auto Crystal", ModuleCategory.COMBAT);
        setup(range, placeDelay, breakDelay, autoObsidian, obsidianDelay, protections, itemRange);
    }

    @Override
    public void activate() {
        placeCooldown.reset();
        breakCooldown.reset();
        obsidianCooldown.reset();
        super.activate();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        script.update();
        
        if (PlayerInteractionHelper.nullCheck()) return;
        
        LivingEntity target = Aura.getInstance().getTarget();
        if (target == null) return;
        
        double distToTarget = mc.player.distanceTo(target);
        if (distToTarget > range.getValue()) return;
        
        // Ставим обсидиан если нужно
        if (autoObsidian.isValue() && obsidianCooldown.finished((long) obsidianDelay.getValue())) {
            if (placeObsidian(target)) {
                obsidianCooldown.reset();
            }
        }
        
        // Основной цикл: проверяем кристаллы и ставим/ломаем
        if (placeCooldown.finished((long) placeDelay.getValue())) {
            processCrystals(target);
            placeCooldown.reset();
        }
    }

    private void processCrystals(LivingEntity target) {
        if (mc.player == null) return;
        
        BlockPos targetPos = target.getBlockPos();
        
        // Ищем обсидиан/бедрок рядом с врагом (по X-Z координатам)
        BlockPos[] searchPositions = {
            targetPos,
            targetPos.north(),
            targetPos.south(),
            targetPos.east(),
            targetPos.west(),
            targetPos.north().east(),
            targetPos.north().west(),
            targetPos.south().east(),
            targetPos.south().west()
        };
        
        for (BlockPos pos : searchPositions) {
            if (!isValidCrystalBase(pos)) continue;
            
            BlockPos crystalPos = pos.up();
            EndCrystalEntity existingCrystal = getCrystalAt(crystalPos);
            
            if (existingCrystal != null) {
                // Есть кристалл - ломаем его
                if (isSafeToBreak(existingCrystal)) {
                    mc.interactionManager.attackEntity(mc.player, existingCrystal);
                    breakCooldown.reset();
                }
            } else {
                // Нет кристалла - ставим свой
                if (!mc.world.getBlockState(crystalPos).isReplaceable()) {
                    continue;
                }
                Slot crystal = InventoryTask.getSlot(Items.END_CRYSTAL);
                if (crystal != null) {
                    placeCrystalAt(crystal, pos);
                }
            }
            return; // Обрабатываем только один блок за тик
        }
    }

    private boolean placeObsidian(LivingEntity target) {
        if (mc.player == null) return false;
        
        BlockPos targetPos = target.getBlockPos();
        
        // Ищем место для обсидиана рядом с врагом (по X-Z)
        BlockPos[] searchPositions = {
            targetPos,
            targetPos.north(),
            targetPos.south(),
            targetPos.east(),
            targetPos.west(),
            targetPos.north().east(),
            targetPos.north().west(),
            targetPos.south().east(),
            targetPos.south().west()
        };
        
        for (BlockPos pos : searchPositions) {
            if (isValidCrystalBase(pos)) {
                continue; // Уже есть обсидиан/бедрок
            }
            
            if (canPlaceObsidian(pos)) {
                Slot obsidian = InventoryTask.getSlot(Items.OBSIDIAN);
                if (obsidian != null) {
                    placeBlockAt(obsidian, pos);
                    return true;
                }
            }
        }
        
        return false;
    }

    private boolean isSafeToBreak(EndCrystalEntity crystal) {
        if (!protections.isSelected("Себя")) {
            return true;
        }
        
        // Если защита себя включена, проверяем X-Z расстояние
        // Взрыв кристалла наносит урон в радиусе, поэтому проверяем горизонтальное расстояние
        double distXZ = Math.sqrt(
            Math.pow(crystal.getX() - mc.player.getX(), 2) + 
            Math.pow(crystal.getZ() - mc.player.getZ(), 2)
        );
        return distXZ > 2.0; // Ломаем только если кристалл достаточно далеко по X-Z
    }

    private EndCrystalEntity getCrystalAt(BlockPos pos) {
        Box box = new Box(pos);
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof EndCrystalEntity && entity.getBoundingBox().intersects(box)) {
                return (EndCrystalEntity) entity;
            }
        }
        return null;
    }

    private boolean isValidCrystalBase(BlockPos pos) {
        return mc.world.getBlockState(pos).getBlock() == Blocks.OBSIDIAN ||
               mc.world.getBlockState(pos).getBlock() == Blocks.BEDROCK;
    }

    private boolean canPlaceObsidian(BlockPos pos) {
        return mc.world.getBlockState(pos).isReplaceable();
    }

    private void placeCrystalAt(Slot crystal, BlockPos basePos) {
        script.addTickStep(0, () -> {
            InventoryFlowManager.addTask(() -> {
                InventoryTask.swapHand(crystal, Hand.MAIN_HAND, false);
                PlayerInteractionHelper.sendSequencedPacket(i -> 
                    new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, 
                        new BlockHitResult(basePos.toCenterPos(), Direction.UP, basePos, false), i)
                );
                InventoryTask.swapHand(crystal, Hand.MAIN_HAND, false, true);
            });
        });
    }

    private void placeBlockAt(Slot block, BlockPos pos) {
        script.addTickStep(0, () -> {
            InventoryFlowManager.addTask(() -> {
                InventoryTask.swapHand(block, Hand.MAIN_HAND, false);
                PlayerInteractionHelper.sendSequencedPacket(i -> 
                    new PlayerInteractBlockC2SPacket(Hand.MAIN_HAND, 
                        new BlockHitResult(pos.toCenterPos(), Direction.UP, pos, false), i)
                );
                InventoryTask.swapHand(block, Hand.MAIN_HAND, false, true);
            });
        });
    }
}
