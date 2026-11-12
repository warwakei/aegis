package rich.modules.impl.player;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.slf4j.LoggerFactory;
import rich.events.api.EventHandler;
import rich.events.api.types.Priority;
import rich.events.impl.GameLeftEvent;
import rich.events.impl.InputEvent;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.inventory.InventoryUtils;
import rich.util.string.chat.ChatMessage;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public final class SchematicPrinter extends ModuleStructure {
    private static final Direction[] HORIZONTAL = {Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST};
    private static final ClassValue<Boolean> INTERACTIVE = new ClassValue<>() {
        @Override protected Boolean computeValue(Class<?> type) {
            // Signatures survive remapping, unlike the names of vanilla's block-use methods.
            for (Class<?> current = type; current != null && current != AbstractBlock.class; current = current.getSuperclass()) {
                for (var method : current.getDeclaredMethods()) {
                    Class<?>[] parameters = method.getParameterTypes();
                    if (ActionResult.class.isAssignableFrom(method.getReturnType()) && parameters.length > 0
                            && parameters[parameters.length - 1] == BlockHitResult.class) return true;
                }
            }
            return false;
        }
    };

    private final SliderSettings delay = new SliderSettings("Задержка", "Пауза между установками в тиках").range(2, 20).setValue(4);
    private final BooleanSetting walk = new BooleanSetting("Подходить к блокам", "Подходит по ровному полу без прыжков и спусков").setValue(false);
    private final Map<BlockPos, Integer> retryAfter = new HashMap<>();
    private LitematicaBridge bridge;
    private ClientWorld world;
    private BlockPos walkTarget;
    private int tick;
    private int nextScan;

    private record Material(Hand hand, int slot, ItemStack stack) {}
    private record Target(BlockPos pos, BlockState state, Material material) {}

    public SchematicPrinter() {
        super("SchematicPrinter", "Ставит блоки видимой схемы Litematica из хотбара или второй руки", ModuleCategory.PLAYER);
        settings(delay, walk);
    }

    @Override public void deactivate() { clear(); }

    @EventHandler public void onLeave(GameLeftEvent event) { clear(); }

    private void clear() {
        retryAfter.clear();
        world = null;
        walkTarget = null;
        tick = 0;
        nextScan = 0;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        if (mc.player == null || mc.world == null || mc.interactionManager == null) {
            clear();
            return;
        }
        if (world != mc.world) {
            clear();
            world = mc.world;
        }
        if (mc.currentScreen != null || mc.player.currentScreenHandler != mc.player.playerScreenHandler
                || mc.player.isSpectator() || mc.player.isUsingItem() || mc.options.useKey.isPressed()
                || mc.options.attackKey.isPressed()) {
            walkTarget = null;
            return;
        }

        if (!FabricLoader.getInstance().isModLoaded("litematica")) {
            ChatMessage.brandmessage("SchematicPrinter: нужна Litematica для Minecraft 1.21.11");
            setState(false);
            return;
        }
        try {
            if (bridge == null) bridge = new LitematicaBridge();
            var schematic = bridge.visibleWorld();
            if (schematic == null) {
                walkTarget = null;
                return;
            }
            if (++tick < nextScan) return;
            nextScan = tick + delay.getInt();
            retryAfter.values().removeIf(until -> until <= tick);
            placeOrApproach(schematic, bridge.currentLayer());
        } catch (ReflectiveOperationException | LinkageError e) {
            ChatMessage.brandmessage("SchematicPrinter: эта версия Litematica несовместима");
            LoggerFactory.getLogger(SchematicPrinter.class).warn("Cannot read Litematica schematic", e);
            bridge = null;
            setState(false);
        }
    }

    private void placeOrApproach(net.minecraft.world.World schematic, LitematicaBridge.Layer layer) throws ReflectiveOperationException {
        walkTarget = null;
        Map<Item, Material> materials = availableMaterials();
        if (materials.isEmpty()) return;
        double reach = mc.player.getBlockInteractionRange();
        Vec3d eye = mc.player.getEyePos();
        BlockPos center = mc.player.getBlockPos();
        int scan = Math.min(8, (int) Math.ceil(reach) + (walk.isValue() ? 3 : 1));
        int vertical = Math.min(6, (int) Math.ceil(reach) + 1);
        var targets = new ArrayList<Target>();
        for (BlockPos pos : BlockPos.iterate(center.add(-scan, -vertical, -scan), center.add(scan, vertical, scan))) {
            if (!world.isChunkLoaded(pos) || !schematic.isChunkLoaded(pos) || retryAfter.containsKey(pos)) continue;
            BlockState expected = schematic.getBlockState(pos);
            if (expected.isAir() || expected.hasBlockEntity() || expected.contains(Properties.DOUBLE_BLOCK_HALF)
                    || expected.contains(Properties.BED_PART)) continue;
            Material material = materials.get(expected.getBlock().asItem());
            if (material == null || ((BlockItem) material.stack().getItem()).getBlock() != expected.getBlock()
                    || !world.getBlockState(pos).isAir() || !world.getWorldBorder().contains(pos) || !layer.includes(pos)) continue;
            if (expected.getBlock() instanceof FallingBlock && FallingBlock.canFallThrough(world.getBlockState(pos.down()))) continue;
            targets.add(new Target(pos.toImmutable(), expected, material));
        }
        targets.sort(Comparator.comparingDouble(target -> Vec3d.ofCenter(target.pos()).squaredDistanceTo(eye)));
        for (Target target : targets) {
            if (Vec3d.ofCenter(target.pos()).squaredDistanceTo(eye) > (reach + 1) * (reach + 1)) break;
            var hit = findHit(target, eye, reach);
            if (hit == null) continue;
            retryAfter.put(target.pos(), tick + 20);
            place(target.material(), hit);
            return;
        }
        if (walk.isValue() && mc.player.isOnGround()) {
            for (Target target : targets) {
                double distance = Vec3d.ofCenter(target.pos()).distanceTo(eye);
                if (distance <= reach || distance > reach + 4) continue;
                walkTarget = approach(center, target.pos());
                if (walkTarget != null) break;
            }
        }
    }

    private Map<Item, Material> availableMaterials() {
        Map<Item, Material> materials = new HashMap<>();
        var inventory = mc.player.getInventory();
        int selected = inventory.getSelectedSlot();
        addMaterial(materials, Hand.MAIN_HAND, selected, inventory.getStack(selected));
        addMaterial(materials, Hand.OFF_HAND, -1, mc.player.getOffHandStack());
        for (int slot = 0; slot < 9; slot++) addMaterial(materials, Hand.MAIN_HAND, slot, inventory.getStack(slot));
        return materials;
    }

    private static void addMaterial(Map<Item, Material> materials, Hand hand, int slot, ItemStack stack) {
        if (!stack.isEmpty() && stack.getItem() instanceof BlockItem) materials.putIfAbsent(stack.getItem(), new Material(hand, slot, stack));
    }

    private BlockHitResult findHit(Target target, Vec3d eye, double reach) {
        if (!target.state().canPlaceAt(world, target.pos()) || !world.canPlace(target.state(), target.pos(), ShapeContext.of(mc.player))) return null;
        for (Direction side : Direction.values()) {
            BlockPos support = target.pos().offset(side.getOpposite());
            var anchor = world.getBlockState(support);
            if (anchor.isAir() || (!mc.player.isSneaking() && INTERACTIVE.get(anchor.getBlock().getClass()))) continue;
            Vec3d face = Vec3d.ofCenter(support).add(side.getOffsetX() * 0.499, side.getOffsetY() * 0.499, side.getOffsetZ() * 0.499);
            if (eye.squaredDistanceTo(face) > reach * reach) continue;
            var hit = world.raycast(new RaycastContext(eye, face, RaycastContext.ShapeType.OUTLINE,
                    RaycastContext.FluidHandling.NONE, mc.player));
            if (hit.getType() != HitResult.Type.BLOCK || !hit.getBlockPos().equals(support) || hit.getSide() != side) continue;
            Material material = target.material();
            var context = new ItemPlacementContext(mc.player, material.hand(), material.stack(), hit);
            if (context.getBlockPos().equals(target.pos()) && context.canPlace()
                    && target.state().equals(target.state().getBlock().getPlacementState(context))) return hit;
        }
        return null;
    }

    private void place(Material material, BlockHitResult hit) {
        int saved = mc.player.getInventory().getSelectedSlot();
        try {
            if (material.slot() >= 0 && material.slot() != saved) InventoryUtils.selectSlot(material.slot());
            var result = mc.interactionManager.interactBlock(mc.player, material.hand(), hit);
            if (result instanceof ActionResult.Success success && success.swingSource() == ActionResult.SwingSource.CLIENT) {
                mc.player.swingHand(material.hand());
            }
        } finally {
            if (material.slot() >= 0 && material.slot() != saved) InventoryUtils.selectSlot(saved);
        }
    }

    private BlockPos approach(BlockPos from, BlockPos target) {
        BlockPos best = null;
        double distance = from.getSquaredDistance(target);
        for (Direction direction : HORIZONTAL) {
            BlockPos candidate = from.offset(direction);
            double nextDistance = candidate.getSquaredDistance(target);
            if (nextDistance < distance && standable(candidate)) {
                best = candidate;
                distance = nextDistance;
            }
        }
        return best;
    }

    private boolean standable(BlockPos pos) {
        if (!world.isChunkLoaded(pos) || !world.getWorldBorder().contains(pos)) return false;
        BlockPos floor = pos.down();
        var state = world.getBlockState(floor);
        return !state.isOf(Blocks.MAGMA_BLOCK) && state.isFullCube(world, floor)
                && world.getBlockState(pos).isAir() && world.getBlockState(pos.up()).isAir();
    }

    @EventHandler(Priority.LOW)
    public void onInput(InputEvent event) {
        if (!walk.isValue() || walkTarget == null || world != mc.world || mc.player == null || !mc.player.isOnGround()
                || mc.currentScreen != null || !standable(walkTarget)) return;
        var input = event.getInput();
        if (input.forward() || input.backward() || input.left() || input.right() || input.jump() || input.sneak()) return;
        Vec3d move = Vec3d.ofBottomCenter(walkTarget).subtract(mc.player.getEntityPos());
        if (move.horizontalLengthSquared() < 0.0225) return;
        mc.player.setYaw((float) Math.toDegrees(Math.atan2(-move.x, move.z)));
        event.setDirectional(true, false, false, false, false, false, false);
    }
}
