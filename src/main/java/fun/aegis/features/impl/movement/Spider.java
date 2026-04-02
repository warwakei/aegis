package fun.aegis.features.impl.movement;

import antidaunleak.api.annotation.Native;
import fun.aegis.utils.features.aura.warp.Turns;
import fun.aegis.utils.client.chat.ChatMessage;
import fun.aegis.utils.interactions.inv.InventoryFlowManager;
import fun.aegis.utils.interactions.inv.InventoryResult;
import fun.aegis.utils.interactions.inv.InventoryTask;
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

import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.client.managers.event.types.EventType;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.SelectSetting;
import fun.aegis.events.player.PostTickEvent;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import fun.aegis.utils.interactions.simulate.PlayerSimulation;
import fun.aegis.utils.math.time.StopWatch;
import fun.aegis.utils.math.task.TaskPriority;
import fun.aegis.utils.math.script.Script;
import fun.aegis.events.player.RotationUpdateEvent;
import fun.aegis.utils.features.aura.utils.MathAngle;
import fun.aegis.utils.features.aura.warp.TurnsConfig;
import fun.aegis.utils.features.aura.warp.TurnsConnection;
import fun.aegis.utils.features.aura.rotations.impl.SnapAngle;

import java.util.function.IntPredicate;
import java.util.stream.Stream;

import static net.minecraft.world.gen.chunk.DebugChunkGenerator.getBlockState;

@Setter
@Getter
@SuppressWarnings("unused")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Spider extends Module {
    Script script = new Script();
    StopWatch stopWatch = new StopWatch();

    SelectSetting mode = new SelectSetting("Режим", "Выбирает режим")
            .value("SpookyTime", "FunTime", "Slime Block", "Water Bucket").selected("Slime Block");

    public Spider() {
        super("Spider", ModuleCategory.MOVEMENT);
        setup(mode);
    }
    private int useItemSequence = 0;
    private double lastWaterY = 0;
    private long lastWaterPlaceTime = 0;

    @NonFinal
    int cooldown;
    @NonFinal
    boolean startSetPitch = false;
    private Block getBlockState(BlockPos blockPos) {
        return mc.world.getBlockState(blockPos).getBlock();
    }

    @Override
    public void deactivate() {
        if (mode.isSelected("Slime Block")) {
            mc.options.jumpKey.setPressed(false);
        }
    }
    @EventHandler
    public void onPostTick(PostTickEvent e) {

        if (mode.isSelected("FunTime")) {
            if (mc.options.jumpKey.isPressed()) return;
            Box playerBox = mc.player.getBoundingBox().expand(-1e-3);
            Box box = new Box(playerBox.minX, playerBox.minY, playerBox.minZ, playerBox.maxX, playerBox.minY + 0.5, playerBox.maxZ);
            if (stopWatch.finished(400) && PlayerInteractionHelper.isBox(box, this::hasCollision)) {
                box = new Box(playerBox.minX - 0.3, playerBox.minY + 1, playerBox.minZ - 0.3, playerBox.maxX, playerBox.maxY, playerBox.maxZ);
                if (PlayerInteractionHelper.isBox(box, this::hasCollision)) {
                    mc.player.setOnGround(true);
                    mc.player.velocity.y = 0.6;
                } else {
                    mc.player.setOnGround(true);
                    mc.player.jump();
                }
            }
        }

//        if (mode.isSelected("Test") && stopWatch.finished(200) && mc.player.isTouchingWater()) {
//            mc.player.setVelocity(0, 0.45F, 0);
//            stopWatch.reset();
//        }

        if (mode.isSelected("Water Bucket")) {
            if (mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET && mc.player.horizontalCollision) {
                mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                mc.player.swingHand(Hand.MAIN_HAND);
                mc.player.setVelocity(mc.player.getVelocity().x, 0.3, mc.player.getVelocity().z);
            }
        }

        if (mode.isSelected("SpookyTime") && stopWatch.finished(310)) {
            if (mc.player.getMainHandStack().getItem() == Items.WATER_BUCKET && mc.player.horizontalCollision) {

                mc.player.networkHandler.sendPacket(new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, 0, mc.player.getYaw(), mc.player.getPitch()));
                mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                mc.player.setVelocity(mc.player.getVelocity().x, 0.35, mc.player.getVelocity().z);
            }
            stopWatch.reset();
        }

        if (mode.isSelected("Slime Block")) {
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

                int slimeSlot = InventoryTask.getHotbarSlotId(i ->
                        mc.player.getInventory().getStack(i).getItem() == Items.SLIME_BLOCK
                );

                if (slimeSlot != -1) {
                    mc.player.getInventory().setSelectedSlot(slimeSlot);
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
    }


    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (e.getType() == EventType.PRE) {
            if (mode.isSelected("Slime Block")) {
                boolean offHand = mc.player.getOffHandStack().getItem() instanceof BlockItem;
                int slotId = InventoryTask.getHotbarSlotId(i -> mc.player.getInventory().getStack(i).getItem() instanceof BlockItem);
                BlockPos blockPos = findPos();
                if (script.isFinished() && (offHand || slotId != -1) && !blockPos.equals(BlockPos.ORIGIN)) {
                    ItemStack stack = offHand ? mc.player.getOffHandStack() : mc.player.getInventory().getStack(slotId);
                    Hand hand = offHand ? Hand.OFF_HAND : Hand.MAIN_HAND;
                    Vec3d vec = blockPos.toCenterPos();
                    Direction direction = Direction.getFacing(vec.x - mc.player.getX(), vec.y - mc.player.getY(), vec.z - mc.player.getZ());
                    Turns angle = MathAngle.calculateAngle(vec.subtract(new Vec3d(direction.getVector()).multiply(0.1F)));
                    Turns.VecRotation vecRotation = new Turns.VecRotation(angle, angle.toVector());
                    TurnsConnection.INSTANCE.rotateTo(vecRotation, mc.player, 1, new TurnsConfig(new SnapAngle(), true, true), TaskPriority.HIGH_IMPORTANCE_1, this);
                    if (canPlace(stack)) {
                        int prev = mc.player.inventory.selectedSlot;
                        if (!offHand) mc.player.inventory.selectedSlot = slotId;
                        mc.interactionManager.interactBlock(mc.player, hand, new BlockHitResult(vec, direction.getOpposite(), blockPos, false));
                        mc.player.networkHandler.sendPacket(new HandSwingC2SPacket(hand));
                        if (!offHand) mc.player.inventory.selectedSlot = prev;
                    }
                }
            }
        }
    }


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
        return Stream.of(blockPos.west(), blockPos.east(), blockPos.south(), blockPos.north()).filter(pos -> mc.world.getBlockState(pos).isSolid()).findFirst().orElse(BlockPos.ORIGIN);
    }

    private BlockPos getPlaceableWaterBlock() {
        BlockPos below = BlockPos.ofFloored(mc.player.getPos().add(0, -1.3, 0));
        if (mc.world.getBlockState(below).isSolidBlock(mc.world, below)) {
            return below;
        }

        for (Direction dir : Direction.values()) {
            BlockPos side = below.offset(dir);
            if (mc.world.getBlockState(side).isSolidBlock(mc.world, side)) {
                return side;
            }
        }

        return null;
    }


    private BlockPos getBlockPos() {
        return BlockPos.ofFloored(PlayerSimulation.simulateLocalPlayer(1).pos.add(0, -1e-3, 0));
    }
    private boolean hasCollision(BlockPos blockPos) {
        return !mc.world.getBlockState(blockPos).getCollisionShape(mc.world, blockPos).isEmpty();
    }
}
