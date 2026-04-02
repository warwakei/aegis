package rich.modules.impl.combat;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
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
import rich.events.api.EventHandler;
import rich.events.impl.EntitySpawnEvent;
import rich.events.impl.InputEvent;
import rich.events.impl.PacketEvent;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.MultiSelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.inventory.InventoryUtils;
import rich.util.inventory.SwapExecutor;
import rich.util.inventory.SwapSettings;
import rich.util.repository.friend.FriendUtils;
import rich.util.string.PlayerInteractionHelper;

import java.util.List;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AutoCrystal extends ModuleStructure {

    private final MultiSelectSetting protections = new MultiSelectSetting("Защита", "Что не взрывать")
            .value("Себя", "Друзей", "Ресурсы")
            .selected("Себя", "Друзей", "Ресурсы");

    private final SliderSettings itemRange = new SliderSettings("Дистанция до ресурсов", "Минимальное расстояние до ресурсов")
            .range(1.0f, 12.0f)
            .setValue(6.0f);

    private final BooleanSetting legitMode = new BooleanSetting("Легит режим", "Останавливать движение перед свапом")
            .setValue(false);

    private final SliderSettings swapDelay = new SliderSettings("Задержка свапа", "Мс перед свапом кристалла")
            .range(0, 200)
            .setValue(50)
            .visible(() -> legitMode.isValue());

    private final SwapExecutor swapExecutor = new SwapExecutor();
    private BlockPos obsPosition;
    private boolean waitingForCrystal;

    public AutoCrystal() {
        super("AutoCrystal", "Auto Crystal", ModuleCategory.COMBAT);
        settings(protections, itemRange, legitMode, swapDelay);
    }

    @Override
    public void activate() {
        obsPosition = null;
        waitingForCrystal = false;
    }

    @Override
    public void deactivate() {
        swapExecutor.cancel();
        obsPosition = null;
        waitingForCrystal = false;
    }

    @EventHandler
    public void onPacket(PacketEvent e) {
        if (mc.player == null || mc.world == null) return;
        if (swapExecutor.isRunning() || waitingForCrystal) return;

        if (e.getPacket() instanceof PlayerInteractBlockC2SPacket interact && interact.getSequence() != 0) {
            BlockPos interactPos = interact.getBlockHitResult().getBlockPos();
            BlockPos spawnPos = interactPos.offset(interact.getBlockHitResult().getSide());

            BlockPos blockPos = null;
            if (mc.world.getBlockState(spawnPos).getBlock().equals(Blocks.OBSIDIAN)) {
                blockPos = spawnPos;
            } else if (mc.world.getBlockState(interactPos).getBlock().equals(Blocks.OBSIDIAN)) {
                blockPos = interactPos;
            }

            if (blockPos == null) return;

            Slot crystalSlot = findCrystalSlot();
            if (crystalSlot == null) return;

            if (!isSafePosition(blockPos)) return;

            final BlockPos finalBlockPos = blockPos;
            obsPosition = blockPos;
            waitingForCrystal = true;

            SwapSettings settings = legitMode.isValue()
                    ? new SwapSettings()
                    .stopMovement(true)
                    .stopSprint(true)
                    .preSwapDelay(swapDelay.getInt(), swapDelay.getInt() + 30)
                    .postSwapDelay(20, 50)
                    : SwapSettings.instant();

            swapExecutor.execute(() -> {
                placeCrystal(crystalSlot, finalBlockPos);
            }, settings, () -> {
                scheduleReset();
            });
        }
    }

    private void placeCrystal(Slot crystalSlot, BlockPos blockPos) {
        if (crystalSlot == null || blockPos == null) return;

        int currentSlot = InventoryUtils.currentSlot();

        if (crystalSlot.id >= 36 && crystalSlot.id <= 44) {
            int hotbarSlot = crystalSlot.id - 36;
            InventoryUtils.selectSlot(hotbarSlot);

            PlayerInteractionHelper.sendSequencedPacket(i ->
                    new PlayerInteractBlockC2SPacket(
                            Hand.MAIN_HAND,
                            new BlockHitResult(blockPos.toCenterPos(), Direction.UP, blockPos, false),
                            i
                    )
            );

            InventoryUtils.selectSlot(currentSlot);
        } else {
            InventoryUtils.swapHotbar(crystalSlot.id, currentSlot);

            PlayerInteractionHelper.sendSequencedPacket(i ->
                    new PlayerInteractBlockC2SPacket(
                            Hand.MAIN_HAND,
                            new BlockHitResult(blockPos.toCenterPos(), Direction.UP, blockPos, false),
                            i
                    )
            );

            InventoryUtils.swapHotbar(crystalSlot.id, currentSlot);
            InventoryUtils.closeScreen();
        }
    }

    private void scheduleReset() {
        // Сбрасываем через 6 тиков если кристалл не появился
        resetTicks = 6;
    }

    private int resetTicks = 0;

    @EventHandler
    public void onEntitySpawnEvent(EntitySpawnEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (e.getEntity() instanceof EndCrystalEntity crystal) {
            if (obsPosition != null && obsPosition.equals(crystal.getBlockPos().down())) {
                if (isSafeToDamage(crystal)) {
                    mc.interactionManager.attackEntity(mc.player, crystal);
                }
                resetState();
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        swapExecutor.tick();

        if (resetTicks > 0) {
            resetTicks--;
            if (resetTicks == 0) {
                resetState();
            }
        }
    }

    @EventHandler
    public void onInput(InputEvent e) {
        if (swapExecutor.isBlocking()) {
            e.setDirectionalLow(false, false, false, false);
            e.setJumping(false);
        }
    }

    private void resetState() {
        obsPosition = null;
        waitingForCrystal = false;
        resetTicks = 0;
    }

    private Slot findCrystalSlot() {
        return InventoryUtils.findSlot(Items.END_CRYSTAL);
    }

    private boolean isSafePosition(BlockPos pos) {
        if (protections.isSelected("Себя")) {
            if (mc.player.getY() > pos.getY()) {
                return false;
            }
        }

        if (protections.isSelected("Друзей")) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player) continue;
                if (FriendUtils.isFriend(player)) {
                    if (player.getY() > pos.getY()) {
                        return false;
                    }
                }
            }
        }

        if (protections.isSelected("Ресурсы")) {
            Vec3d crystalPos = pos.up().toCenterPos();
            double range = itemRange.getValue();
            Box box = new Box(
                    crystalPos.x - range, crystalPos.y - range, crystalPos.z - range,
                    crystalPos.x + range, crystalPos.y + range, crystalPos.z + range
            );
            List<Entity> entities = mc.world.getOtherEntities(mc.player, box);

            for (Entity entity : entities) {
                if (entity instanceof ItemEntity) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isSafeToDamage(EndCrystalEntity crystal) {
        BlockPos crystalBlock = crystal.getBlockPos().down();

        if (protections.isSelected("Себя")) {
            if (mc.player.getY() > crystalBlock.getY()) {
                return false;
            }
        }

        if (protections.isSelected("Друзей")) {
            for (PlayerEntity player : mc.world.getPlayers()) {
                if (player == mc.player) continue;
                if (FriendUtils.isFriend(player)) {
                    if (player.getY() > crystalBlock.getY()) {
                        return false;
                    }
                }
            }
        }

        if (protections.isSelected("Ресурсы")) {
            Vec3d crystalPos = crystal.getEntityPos();
            double range = itemRange.getValue();
            Box box = new Box(
                    crystalPos.x - range, crystalPos.y - range, crystalPos.z - range,
                    crystalPos.x + range, crystalPos.y + range, crystalPos.z + range
            );
            List<Entity> entities = mc.world.getOtherEntities(mc.player, box);

            for (Entity entity : entities) {
                if (entity instanceof ItemEntity) {
                    return false;
                }
            }
        }

        return true;
    }
}