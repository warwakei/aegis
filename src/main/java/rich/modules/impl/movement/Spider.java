package rich.modules.impl.movement;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import rich.events.api.EventHandler;
import rich.events.api.types.EventType;
import rich.events.impl.RotationUpdateEvent;
import rich.events.impl.TickEvent;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.AngleConfig;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.impl.combat.aura.MathAngle;
import rich.modules.impl.combat.aura.rotations.SnapAngle;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.SelectSetting;
import rich.util.inventory.InventoryUtils;
import rich.util.math.TaskPriority;
import rich.util.player.PlayerSimulation;
import rich.util.string.PlayerInteractionHelper;
import rich.util.timer.StopWatch;

import java.util.stream.Stream;

@Setter
@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@SuppressWarnings("deprecation")
public class Spider extends ModuleStructure {
    StopWatch stopWatch = new StopWatch();

    SelectSetting mode = new SelectSetting("Режим", "Выбирает режим")
            .value("SpookyTime", "FunTime", "Slime Block", "Water Bucket")
            .selected("Slime Block");

    @NonFinal
    int cooldown;

    @NonFinal
    boolean startSetPitch = false;

    public Spider() {
        super("Spider", ModuleCategory.MOVEMENT);
        settings(mode);
    }

    private Block getBlockState(BlockPos blockPos) {
        return mc.world.getBlockState(blockPos).getBlock();
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        if (mode.isSelected("Slime Block")) {
            mc.options.jumpKey.setPressed(false);
        }
    }

    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        if (mode.isSelected("FunTime")) {
            handleFunTimeMode();
        }

        if (mode.isSelected("Water Bucket")) {
            handleWaterBucketMode();
        }

        if (mode.isSelected("SpookyTime") && stopWatch.finished(310)) {
            handleSpookyTimeMode();
        }

        if (mode.isSelected("Slime Block")) {
            handleSlimeBlock();
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleFunTimeMode() {
        if (mc.options.jumpKey.isPressed()) return;
        Box playerBox = mc.player.getBoundingBox().expand(-1e-3);
        Box box = new Box(playerBox.minX, playerBox.minY, playerBox.minZ, playerBox.maxX, playerBox.minY + 0.5, playerBox.maxZ);
        if (stopWatch.finished(400) && PlayerInteractionHelper.isBox(box, this::hasCollision)) {
            box = new Box(playerBox.minX - 0.3, playerBox.minY + 1, playerBox.minZ - 0.3, playerBox.maxX, playerBox.maxY, playerBox.maxZ);
            if (PlayerInteractionHelper.isBox(box, this::hasCollision)) {
                mc.player.setOnGround(true);
                mc.player.setVelocity(mc.player.getVelocity().x, 0.6, mc.player.getVelocity().z);
            } else {
                mc.player.setOnGround(true);
                mc.player.jump();
            }
        }
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private void handleWaterBucketMode() {
        if (mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET && mc.player.horizontalCollision) {
            mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
            mc.player.swingHand(Hand.MAIN_HAND);
            mc.player.setVelocity(mc.player.getVelocity().x, 0.3, mc.player.getVelocity().z);
        }
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleSpookyTimeMode() {
        if (mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET && mc.player.horizontalCollision) {
            mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
            mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            mc.player.setVelocity(mc.player.getVelocity().x, 0.35, mc.player.getVelocity().z);
        }
        stopWatch.reset();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void handleSlimeBlock() {
        BlockPos playerPos = mc.player.getBlockPos();
        BlockPos[] adjacentBlocks = {
                playerPos.east(),
                playerPos.west(),
                playerPos.north(),
                playerPos.south()
        };

        boolean hasAdjacentSlime = false;
        for (BlockPos pos : adjacentBlocks) {
            if (getBlockState(pos) == Blocks.SLIME_BLOCK) {
                hasAdjacentSlime = true;
                break;
            }
        }

        if (!hasAdjacentSlime || !mc.player.horizontalCollision || mc.player.getVelocity().y <= -1) {
            return;
        }

        HitResult crosshair = mc.crosshairTarget;
        if (crosshair instanceof BlockHitResult blockHit) {
            Direction face = blockHit.getSide();
            BlockPos targetPos = blockHit.getBlockPos();

            if (getBlockState(targetPos) == Blocks.AIR) {
                return;
            }

            int slimeSlot = findHotbarSlot(Items.SLIME_BLOCK);

            if (slimeSlot != -1) {
                InventoryUtils.selectSlot(slimeSlot);
                startSetPitch = true;
                mc.player.setPitch(54);

                BlockHitResult interaction = new BlockHitResult(
                        blockHit.getPos(),
                        face,
                        targetPos,
                        false
                );

                mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, interaction);
                mc.player.swingHand(Hand.MAIN_HAND);

                if (cooldown >= 0.5) {
                    mc.player.setVelocity(mc.player.getVelocity().x, 0.63, mc.player.getVelocity().z);
                    cooldown = 0;
                } else {
                    cooldown++;
                }
            }
        }
    }

    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (e.getType() != EventType.PRE) return;
        if (mc.player == null || mc.world == null) return;
        if (!mode.isSelected("Slime Block")) return;

        boolean offHand = mc.player.getOffHandStack().getItem() instanceof BlockItem;
        int slotId = findHotbarBlockSlot();
        BlockPos blockPos = findPos();

        if ((offHand || slotId != -1) && !blockPos.equals(BlockPos.ORIGIN)) {
            ItemStack stack = offHand ? mc.player.getOffHandStack() : mc.player.getInventory().getStack(slotId);
            Hand hand = offHand ? Hand.OFF_HAND : Hand.MAIN_HAND;
            Vec3d vec = blockPos.toCenterPos();
            Direction direction = Direction.getFacing(vec.x - mc.player.getX(), vec.y - mc.player.getY(), vec.z - mc.player.getZ());
            Angle angle = MathAngle.calculateAngle(vec.subtract(new Vec3d(direction.getVector()).multiply(0.1F)));
            Angle.VecRotation vecRotation = new Angle.VecRotation(angle, angle.toVector());
            AngleConnection.INSTANCE.rotateTo(vecRotation, mc.player, 1, new AngleConfig(new SnapAngle(), true, true), TaskPriority.HIGH_IMPORTANCE_1, this);

            if (canPlace(stack)) {
                int prev = mc.player.getInventory().getSelectedSlot();
                if (!offHand) InventoryUtils.selectSlot(slotId);
                mc.interactionManager.interactBlock(mc.player, hand, new BlockHitResult(vec, direction.getOpposite(), blockPos, false));
                mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
                if (!offHand) InventoryUtils.selectSlot(prev);
            }
        }
    }

    private int findHotbarSlot(net.minecraft.item.Item item) {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() == item) {
                return i;
            }
        }
        return -1;
    }

    private int findHotbarBlockSlot() {
        for (int i = 0; i < 9; i++) {
            if (mc.player.getInventory().getStack(i).getItem() instanceof BlockItem) {
                return i;
            }
        }
        return -1;
    }

    @Native(type = Native.Type.VMProtectBeginMutation)
    private boolean canPlace(ItemStack stack) {
        BlockPos blockPos = getBlockPos();
        if (blockPos.getY() >= mc.player.getBlockY()) return false;
        BlockItem blockItem = (BlockItem) stack.getItem();
        VoxelShape shape = blockItem.getBlock().getDefaultState().getCollisionShape(mc.world, blockPos);
        if (shape.isEmpty()) return false;
        Box box = shape.getBoundingBox().offset(blockPos);
        return !box.intersects(mc.player.getBoundingBox()) && box.intersects(PlayerSimulation.simulateLocalPlayer(4).boundingBox);
    }

    private BlockPos findPos() {
        BlockPos blockPos = getBlockPos();
        if (mc.world.getBlockState(blockPos).isSolid()) return BlockPos.ORIGIN;
        return Stream.of(blockPos.west(), blockPos.east(), blockPos.south(), blockPos.north())
                .filter(pos -> mc.world.getBlockState(pos).isSolid())
                .findFirst()
                .orElse(BlockPos.ORIGIN);
    }

    private BlockPos getBlockPos() {
        return BlockPos.ofFloored(PlayerSimulation.simulateLocalPlayer(1).pos.add(0, -1e-3, 0));
    }

    private boolean hasCollision(BlockPos blockPos) {
        return !mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty();
    }
}