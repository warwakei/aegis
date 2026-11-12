package rich.modules.impl.combat;

import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.thrown.EnderPearlEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import rich.events.api.EventHandler;
import rich.events.impl.GameLeftEvent;
import rich.events.impl.TickEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.inventory.InventoryUtils;
import rich.util.repository.friend.FriendUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class PearlFollow extends ModuleStructure {
    private final SliderSettings range = new SliderSettings("Дистанция", "Радиус обнаружения чужой перки")
            .range(8, 48).setValue(24);
    private final Map<UUID, Integer> handled = new HashMap<>();
    private ClientWorld world;
    private int tick;
    private int nextThrow;

    private record Supply(Hand hand, int hotbar, int inventorySlot, ItemStack stack) {}

    public PearlFollow() {
        super("PearlFollow", "Бросает жемчуг к предсказанной точке приземления перки противника", ModuleCategory.COMBAT);
        settings(range);
    }

    @Override public void deactivate() { clear(); }

    @EventHandler public void onLeave(GameLeftEvent event) { clear(); }

    private void clear() {
        handled.clear();
        world = null;
        tick = 0;
        nextThrow = 0;
    }

    @EventHandler
    public void onTick(TickEvent event) {
        var player = mc.player;
        if (player == null || mc.world == null || mc.interactionManager == null) {
            clear();
            return;
        }
        if (world != mc.world) {
            clear();
            world = mc.world;
        }
        tick++;
        if (tick % 200 == 0) handled.entrySet().removeIf(entry -> tick - entry.getValue() > 200);
        double radius = range.getValue();
        var pearls = world.getEntitiesByClass(EnderPearlEntity.class, player.getBoundingBox().expand(radius),
                pearl -> pearl.age <= 8 && !handled.containsKey(pearl.getUuid()));
        if (pearls.isEmpty()) return;

        boolean available = mc.currentScreen == null && player.currentScreenHandler == player.playerScreenHandler
                && !player.isUsingItem() && !player.isSpectator() && tick >= nextThrow;
        Supply supply = available ? findSupply() : null;

        pearls.sort(Comparator.comparingDouble(pearl -> pearl.squaredDistanceTo(player)));
        int attempts = 0;
        for (EnderPearlEntity pearl : pearls) {
            if (!(pearl.getOwner() instanceof PlayerEntity owner)) continue;
            if (supply == null || owner == player || player.isTeammate(owner) || FriendUtils.isFriend(owner)
                    || owner.squaredDistanceTo(player) > radius * radius) {
                handled.put(pearl.getUuid(), tick);
                continue;
            }
            if (pearl.getVelocity().lengthSquared() < 0.01) continue;
            if (++attempts > 3) break;
            handled.put(pearl.getUuid(), tick);

            var target = PearlTrajectory.predict(pearl.getEntityPos(), pearl.getVelocity(), this::inWater,
                    (from, to) -> raycast(from, to, pearl));
            if (target == null) continue;
            Vec3d origin = player.getEyePos().add(0, -0.1, 0);
            Vec3d movement = player.getMovement();
            Vec3d inherited = new Vec3d(movement.x, player.isOnGround() ? 0 : movement.y, movement.z);
            var aim = PearlTrajectory.solve(origin, inherited, target, this::inWater, (from, to) -> raycast(from, to, player));
            if (aim == null) continue;

            throwPearl(supply, aim);
            return;
        }
    }

    private Supply findSupply() {
        var player = mc.player;
        if (canUse(player.getMainHandStack())) return new Supply(Hand.MAIN_HAND, -1, -1, player.getMainHandStack());
        if (canUse(player.getOffHandStack())) return new Supply(Hand.OFF_HAND, -1, -1, player.getOffHandStack());
        var inventory = player.getInventory();
        for (int slot = 0; slot < 36; slot++) {
            var stack = inventory.getStack(slot);
            if (!canUse(stack)) continue;
            if (slot < 9) return new Supply(Hand.MAIN_HAND, slot, -1, stack);
            if (!player.playerScreenHandler.getCursorStack().isEmpty()) return null;
            int id = player.playerScreenHandler.getSlotIndex(inventory, slot).orElse(-1);
            if (id >= 0 && player.playerScreenHandler.getSlot(id).canTakeItems(player)) {
                return new Supply(Hand.MAIN_HAND, -1, id, stack);
            }
        }
        return null;
    }

    private boolean canUse(ItemStack stack) {
        return stack.isOf(Items.ENDER_PEARL) && !mc.player.getItemCooldownManager().isCoolingDown(stack);
    }

    private void throwPearl(Supply supply, PearlTrajectory.Aim aim) {
        var player = mc.player;
        var handler = player.playerScreenHandler;
        int selected = player.getInventory().getSelectedSlot();
        float yaw = player.getYaw(), pitch = player.getPitch();
        boolean swapped = false, aimed = false;
        try {
            if (supply.hotbar() >= 0) InventoryUtils.selectSlot(supply.hotbar());
            if (supply.inventorySlot() >= 0) {
                mc.interactionManager.clickSlot(handler.syncId, supply.inventorySlot(), selected, SlotActionType.SWAP, player);
                swapped = ItemStack.areItemsAndComponentsEqual(player.getMainHandStack(), supply.stack());
            }
            if (!canUse(player.getStackInHand(supply.hand()))) return;
            player.setYaw(aim.yaw());
            player.setPitch(aim.pitch());
            aimed = true;
            nextThrow = tick + 10;
            var result = mc.interactionManager.interactItem(player, supply.hand());
            if (result instanceof ActionResult.Success success && success.swingSource() == ActionResult.SwingSource.CLIENT) {
                player.swingHand(supply.hand());
            }
        } finally {
            player.setYaw(yaw);
            player.setPitch(pitch);
            if (supply.hotbar() >= 0) InventoryUtils.selectSlot(selected);
            if (swapped) mc.interactionManager.clickSlot(handler.syncId, supply.inventorySlot(), selected, SlotActionType.SWAP, player);
            if (aimed && mc.getNetworkHandler() != null) {
                // The server applies angles from the use-item packet even when the local camera is restored.
                mc.getNetworkHandler().sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, player.isOnGround(), player.horizontalCollision));
            }
        }
    }

    private boolean inWater(Vec3d point) {
        return world.getFluidState(BlockPos.ofFloored(point)).isIn(FluidTags.WATER);
    }

    private PearlTrajectory.Landing raycast(Vec3d from, Vec3d to, net.minecraft.entity.Entity projectile) {
        var hit = world.raycast(new RaycastContext(from, to, RaycastContext.ShapeType.COLLIDER,
                RaycastContext.FluidHandling.NONE, projectile));
        return hit.getType() == HitResult.Type.BLOCK ? new PearlTrajectory.Landing(hit.getPos(), hit.getBlockPos()) : null;
    }
}
