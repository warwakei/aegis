package rich.modules.impl.movement;

import antidaunleak.api.annotation.Native;
import net.minecraft.block.Blocks;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import rich.events.api.EventHandler;
import rich.events.impl.InputEvent;
import rich.events.impl.TickEvent;
import rich.modules.impl.combat.aura.Angle;
import rich.modules.impl.combat.aura.AngleConnection;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.Instance;

import java.util.*;

/**
 * Wayfinder — полная автоматизация движения.
 *
 * Режимы:
 * - Navigation: обход препятствий, учёт льда, защита от падения
 * - Parkour: автоматический паркур через пустые пространства
 *
 * Функции:
 * - Raycast вперёд на N блоков для обнаружения препятствий
 * - Проверка блока под ногами (лёд с правильной коллизией)
 * - Проверка падения с края блока
 * - Поиск обхода слева/справа с оптимальным путём
 * - Авто-паркур с таймингами прыжка, спринта, приседания
 */
public class Wayfinder extends ModuleStructure {

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    // ===== Settings =====
    private final SelectSetting modeSetting = new SelectSetting("Режим", "Выберите режим работы")
            .value("Navigation", "Parkour")
            .selected("Navigation");

    private final SliderSettings obstacleRange = new SliderSettings("Дальность препятствий", "Максимальная дистанция raycast")
            .range(3.0f, 7.0f)
            .setValue(5.0f);

    private final BooleanSetting iceAwareness = new BooleanSetting("Учитывать лёд", "Корректировать движение на льду")
            .setValue(true);

    private final BooleanSetting fallProtection = new BooleanSetting("Защита от падения", "Не прыгать если впереди пропасть")
            .setValue(true);

    private final SliderSettings parkourSpeed = new SliderSettings("Скорость паркура", "Множитель скорости при паркуре")
            .range(0.8f, 1.2f)
            .setValue(1.0f)
            .visible(() -> modeSetting.isSelected("Parkour"));

    private final BooleanSetting parkourSneak = new BooleanSetting("Приседание на краю", "Приседать при приближении к краю")
            .setValue(false)
            .visible(() -> modeSetting.isSelected("Parkour"));

    private final BooleanSetting autoSprint = new BooleanSetting("Авто-спринт", "Автоматически использовать спринт")
            .setValue(true);

    public Wayfinder() {
        super("Wayfinder", "Automated navigation with obstacle avoidance and parkour", ModuleCategory.MOVEMENT);
        settings(modeSetting, obstacleRange, iceAwareness, fallProtection, parkourSpeed, parkourSneak, autoSprint);
    }

    public static Wayfinder getInstance() {
        return Instance.get(Wayfinder.class);
    }

    // ===== State =====
    private enum State { IDLE, SEARCHING, PATHFINDING, JUMPING, LANDING }
    private State currentState = State.IDLE;

    // Navigation
    private Vec3d avoidTarget = null;
    private float avoidYaw = 0;
    private int avoidTimer = 0;

    // Parkour
    private Vec3d jumpTarget = null;
    private int parkourTick = 0;
    private boolean wasOnGround = true;
    private boolean pendingJump = false;
    private boolean pendingSneak = false;
    private boolean pendingSprint = false;
    private boolean pendingForward = false;
    private boolean pendingLeft = false;
    private boolean pendingRight = false;

    // Ice tracking
    private boolean onIce = false;
    private double iceFriction = 0.98;

    // ===== Activation =====
    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void activate() {
        super.activate();
        resetState();
    }

    @Override
    @Native(type = Native.Type.VMProtectBeginMutation)
    public void deactivate() {
        super.deactivate();
        resetState();
    }

    private void resetState() {
        currentState = State.IDLE;
        avoidTarget = null;
        avoidTimer = 0;
        jumpTarget = null;
        parkourTick = 0;
        pendingJump = false;
        pendingSneak = false;
        pendingSprint = false;
        pendingForward = false;
        pendingLeft = false;
        pendingRight = false;
        onIce = false;
    }

    // ===== Main Tick =====
    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        boolean onGround = mc.player.isOnGround();
        wasOnGround = onGround;

        // Detect ice below
        onIce = checkIceBelow();
        if (onIce && iceAwareness.isValue()) {
            iceFriction = 0.986; // ice friction
        } else {
            iceFriction = 0.91; // normal friction
        }

        if (modeSetting.isSelected("Parkour")) {
            processParkour(onGround);
        } else {
            processNavigation(onGround);
        }
    }

    // ===== Navigation Mode =====
    @Native(type = Native.Type.VMProtectBeginUltra)
    private void processNavigation(boolean onGround) {
        // Reset avoidance timer
        if (avoidTimer > 0) {
            avoidTimer--;
            if (avoidTimer <= 0) {
                avoidTarget = null;
                avoidYaw = 0;
            }
        }

        // Check for obstacles ahead
        if (avoidTarget == null) {
            double range = obstacleRange.getValue();
            Vec3d obstacle = raycastObstacleAhead(range);

            if (obstacle != null) {
                // Find best path around
                avoidTarget = findBestPath(obstacle);
                avoidTimer = 40; // ticks to follow this path
            }
        }

        // Check fall risk
        if (fallProtection.isValue() && avoidTarget == null) {
            if (checkFallRiskAhead()) {
                // Slow down or sneak
                pendingSneak = true;
                pendingForward = true;
                return;
            }
        }
    }

    // ===== Parkour Mode =====
    @Native(type = Native.Type.VMProtectBeginMutation)
    private void processParkour(boolean onGround) {
        pendingJump = false;
        pendingSneak = false;
        pendingSprint = autoSprint.isValue();
        pendingForward = true;
        pendingLeft = false;
        pendingRight = false;

        float currentYaw = mc.player.getYaw();
        double range = obstacleRange.getValue();

        // Scan for the next block to land on
        Vec3d nextBlock = findNextParkourTarget(range);

        if (nextBlock == null) {
            // No valid target — just go forward
            return;
        }

        // Calculate direction to target
        Vec3d playerPos = mc.player.getEntityPos();
        Vec3d direction = nextBlock.subtract(playerPos).normalize();
        double horizontalDist = Math.sqrt(
                (nextBlock.x - playerPos.x) * (nextBlock.x - playerPos.x) +
                (nextBlock.z - playerPos.z) * (nextBlock.z - playerPos.z)
        );

        // Calculate required yaw to face the target
        float targetYaw = (float) Math.toDegrees(Math.atan2(direction.z, direction.x)) - 90.0f;
        float yawDiff = MathHelper.wrapDegrees(targetYaw - currentYaw);

        // If we need to rotate significantly, do it
        if (Math.abs(yawDiff) > 5.0f) {
            float newYaw = currentYaw + MathHelper.clamp(yawDiff, -30.0f, 30.0f);
            Angle newAngle = new Angle(newYaw, mc.player.getPitch());
            AngleConnection.INSTANCE.setRotation(newAngle);
        }

        // Determine if this is a gap jump
        boolean isGapJump = isGapAhead();

        if (isGapJump) {
            // GAP JUMP — need precise timing
            pendingJump = true;
            pendingSprint = true;
            pendingSneak = false;

            // Check if we need to sneak at the edge
            if (parkourSneak.isValue() && isAtEdge()) {
                pendingSneak = true;
            }

            // Determine lateral direction for gap
            if (yawDiff < -10.0f) {
                pendingRight = true;
            } else if (yawDiff > 10.0f) {
                pendingLeft = true;
            }

        } else {
            // Normal parkour step
            if (onGround) {
                // Check if we need to jump onto the block
                double heightDiff = nextBlock.y - playerPos.y;

                if (heightDiff > 0.5) {
                    // Block is above us — need to jump up
                    pendingJump = true;
                    pendingSprint = true;
                } else if (heightDiff < -0.5) {
                    // Block is below — might need to drop
                    pendingJump = false;
                } else {
                    // Same level — just walk
                    pendingJump = false;
                }
            }

            // Lateral adjustment to align with target
            if (Math.abs(yawDiff) > 15.0f) {
                if (yawDiff < 0) {
                    pendingRight = true;
                } else {
                    pendingLeft = true;
                }
            }
        }

        // Ice handling in parkour
        if (onIce && iceAwareness.isValue()) {
            // Start turning earlier on ice
            pendingSprint = true;
        }

        parkourTick++;
    }

    // ===== Input Event — apply our movement =====
    @EventHandler
    @Native(type = Native.Type.VMProtectBeginUltra)
    public void onInput(InputEvent e) {
        if (modeSetting.isSelected("Parkour")) {
            // Apply parkour movement
            e.setDirectional(
                    pendingForward,
                    false, // never go backward
                    pendingLeft,
                    pendingRight,
                    pendingSneak,
                    pendingSprint,
                    pendingJump
            );
        } else {
            // Navigation mode — only modify if avoiding
            if (avoidTarget != null && avoidTimer > 0) {
                // Steer toward avoid target
                Vec3d playerPos = mc.player.getEntityPos();
                Vec3d dir = avoidTarget.subtract(playerPos).normalize();
                float targetYaw = avoidYaw;
                float currentYaw = mc.player.getYaw();
                float yawDiff = MathHelper.wrapDegrees(targetYaw - currentYaw);

                boolean left = yawDiff > 5.0f;
                boolean right = yawDiff < -5.0f;

                e.setDirectionalLow(true, false, left, right);
            }
        }
    }

    // ===== Environment Checks =====

    /**
     * Raycast forward to find the nearest solid obstacle.
     * Returns the position of the obstacle, or null if clear.
     */
    @Native(type = Native.Type.VMProtectBeginUltra)
    private Vec3d raycastObstacleAhead(double range) {
        Vec3d startPos = mc.player.getEyePos();
        Vec3d lookVec = mc.player.getRotationVec(0);

        // Don't raycast straight — offset to body level
        Vec3d bodyStart = startPos.add(0, -0.5, 0);

        for (double d = 0.5; d < range; d += 0.5) {
            Vec3d checkPos = bodyStart.add(
                    lookVec.x * d,
                    lookVec.y * d,
                    lookVec.z * d
            );

            BlockPos blockPos = BlockPos.ofFloored(checkPos);
            var state = mc.world.getBlockState(blockPos);
            var shape = state.getCollisionShape(mc.world, blockPos);

            if (!shape.isEmpty() && shape != VoxelShapes.empty()) {
                // Check if it's a passable block (slabs, stairs)
                if (isFullySolid(state, blockPos)) {
                    return new Vec3d(blockPos.getX() + 0.5, blockPos.getY(), blockPos.getZ() + 0.5);
                }
            }
        }
        return null;
    }

    /**
     * Check if a block is fully solid (not a slab, stair, etc.)
     */
    @Native(type = Native.Type.VMProtectBeginUltra)
    private boolean isFullySolid(net.minecraft.block.BlockState state, BlockPos pos) {
        var block = state.getBlock();
        if (block == Blocks.AIR) return false;

        // Bottom slabs are walkable but not obstacles at head level
        if (state.contains(Properties.SLAB_TYPE) && state.get(Properties.SLAB_TYPE) == SlabType.BOTTOM) {
            return false;
        }

        // Carpets, pressure plates, etc.
        if (state.getCollisionShape(mc.world, pos).isEmpty()) return false;

        return true;
    }

    /**
     * Find the best path around an obstacle — checks left and right.
     * Returns the target position to steer toward.
     */
    @Native(type = Native.Type.VMProtectBeginUltra)
    private Vec3d findBestPath(Vec3d obstacle) {
        Vec3d playerPos = mc.player.getEntityPos();
        float playerYaw = mc.player.getYaw();

        // Check left and right of obstacle
        double perpendicularYaw = Math.toRadians(playerYaw + 90);

        Vec3d leftTarget = new Vec3d(
                obstacle.x + Math.cos(perpendicularYaw) * 2.0,
                playerPos.y,
                obstacle.z + Math.sin(perpendicularYaw) * 2.0
        );

        Vec3d rightTarget = new Vec3d(
                obstacle.x - Math.cos(perpendicularYaw) * 2.0,
                playerPos.y,
                obstacle.z - Math.sin(perpendicularYaw) * 2.0
        );

        // Score each path — prefer the one with fewer blocks in the way
        double leftScore = scorePath(leftTarget);
        double rightScore = scorePath(rightTarget);

        if (leftScore <= rightScore) {
            avoidYaw = (float) Math.toDegrees(Math.atan2(
                    leftTarget.z - playerPos.z, leftTarget.x - playerPos.x)) - 90.0f;
            return leftTarget;
        } else {
            avoidYaw = (float) Math.toDegrees(Math.atan2(
                    rightTarget.z - playerPos.z, rightTarget.x - playerPos.x)) - 90.0f;
            return rightTarget;
        }
    }

    /**
     * Score a path by checking how many obstacles are in the way.
     * Lower = better.
     */
    @Native(type = Native.Type.VMProtectBeginUltra)
    private double scorePath(Vec3d target) {
        Vec3d playerPos = mc.player.getEntityPos();
        Vec3d dir = target.subtract(playerPos).normalize();
        double dist = playerPos.distanceTo(target);
        double obstacles = 0;

        for (double d = 0.5; d < dist; d += 0.5) {
            Vec3d checkPos = playerPos.add(dir.x * d, dir.y * d, dir.z * d);
            BlockPos blockPos = BlockPos.ofFloored(checkPos);
            var state = mc.world.getBlockState(blockPos);
            var shape = state.getCollisionShape(mc.world, blockPos);
            if (!shape.isEmpty()) obstacles += 1;
        }

        return obstacles;
    }

    /**
     * Check if there's a fall risk ahead (no block to land on).
     */
    @Native(type = Native.Type.VMProtectBeginUltra)
    private boolean checkFallRiskAhead() {
        Vec3d playerPos = mc.player.getEntityPos();
        Vec3d lookVec = mc.player.getRotationVec(0);

        // Check 2 blocks ahead at ground level
        for (double d = 1.0; d < 3.0; d += 0.5) {
            Vec3d checkPos = playerPos.add(lookVec.x * d, -1.5, lookVec.z * d);
            BlockPos belowPos = BlockPos.ofFloored(checkPos);
            var state = mc.world.getBlockState(belowPos);

            if (state.isAir()) {
                // Check further down — is there anything to land on?
                boolean hasGround = false;
                for (int dy = -1; dy > -5; dy--) {
                    BlockPos checkBelow = belowPos.add(0, dy, 0);
                    if (!mc.world.getBlockState(checkBelow).isAir()) {
                        hasGround = true;
                        break;
                    }
                }
                if (!hasGround) return true;
            }
        }
        return false;
    }

    /**
     * Check if there's ice below the player.
     */
    @Native(type = Native.Type.VMProtectBeginUltra)
    private boolean checkIceBelow() {
        if (mc.player == null || mc.world == null) return false;
        BlockPos feetPos = mc.player.getBlockPos().down();
        var block = mc.world.getBlockState(feetPos).getBlock();
        return block == Blocks.ICE ||
                block == Blocks.PACKED_ICE ||
                block == Blocks.BLUE_ICE ||
                block == Blocks.FROSTED_ICE;
    }

    // ===== Parkour Helpers =====

    /**
     * Find the next valid block to land on in parkour mode.
     * Scans in a cone ahead of the player.
     */
    @Native(type = Native.Type.VMProtectBeginUltra)
    private Vec3d findNextParkourTarget(double maxRange) {
        Vec3d playerPos = mc.player.getEntityPos();
        float playerYaw = mc.player.getYaw();
        Vec3d lookVec = new Vec3d(
                -Math.sin(Math.toRadians(playerYaw)),
                0,
                Math.cos(Math.toRadians(playerYaw))
        );

        // Search in layers
        for (double d = 1.0; d < maxRange; d += 0.5) {
            // Check center line first
            Vec3d centerCheck = playerPos.add(lookVec.multiply(d));

            // Check if there's solid ground at or near this position
            for (int dy = 1; dy >= -3; dy--) {
                BlockPos checkPos = BlockPos.ofFloored(centerCheck.x, playerPos.y + dy, centerCheck.z);

                // Check if this block is solid and we can reach it
                var state = mc.world.getBlockState(checkPos);
                if (canStandOn(state, checkPos)) {
                    // Check if there's enough space above to land
                    if (hasSpaceAbove(checkPos, 2)) {
                        return new Vec3d(checkPos.getX() + 0.5, checkPos.getY() + 1.0, checkPos.getZ() + 0.5);
                    }
                }
            }

            // If center has no target, check left and right
            for (int side = -1; side <= 1; side += 2) {
                double sideways = side * 1.5;
                double perpYaw = playerYaw + 90;
                Vec3d sideCheck = playerPos.add(
                        lookVec.x * d + Math.cos(Math.toRadians(perpYaw)) * sideways,
                        0,
                        lookVec.z * d + Math.sin(Math.toRadians(perpYaw)) * sideways
                );

                for (int dy = 1; dy >= -3; dy--) {
                    BlockPos checkPos = BlockPos.ofFloored(sideCheck.x, playerPos.y + dy, sideCheck.z);
                    var state = mc.world.getBlockState(checkPos);
                    if (canStandOn(state, checkPos) && hasSpaceAbove(checkPos, 2)) {
                        return new Vec3d(checkPos.getX() + 0.5, checkPos.getY() + 1.0, checkPos.getZ() + 0.5);
                    }
                }
            }
        }

        return null;
    }

    /**
     * Check if player can stand on this block.
     */
    @Native(type = Native.Type.VMProtectBeginUltra)
    private boolean canStandOn(net.minecraft.block.BlockState state, BlockPos pos) {
        if (state.isAir()) return false;
        var shape = state.getCollisionShape(mc.world, pos);
        if (shape.isEmpty()) return false;

        // Don't stand on fragile blocks
        var block = state.getBlock();
        if (block == Blocks.TNT || block == Blocks.GRAVEL || block == Blocks.SAND) {
            return false;
        }

        return true;
    }

    /**
     * Check if there's enough empty space above a block.
     */
    @Native(type = Native.Type.VMProtectBeginUltra)
    private boolean hasSpaceAbove(BlockPos pos, int height) {
        for (int i = 1; i <= height; i++) {
            BlockPos checkPos = pos.up(i);
            if (!mc.world.getBlockState(checkPos).isAir()) return false;
        }
        return true;
    }

    /**
     * Check if there's a gap (empty space) ahead that requires a jump.
     */
    @Native(type = Native.Type.VMProtectBeginUltra)
    private boolean isGapAhead() {
        Vec3d playerPos = mc.player.getEntityPos();
        Vec3d lookVec = mc.player.getRotationVec(0);

        // Check 1-3 blocks ahead at ground level
        for (double d = 1.0; d < 3.0; d += 0.25) {
            Vec3d checkPos = playerPos.add(lookVec.x * d, -0.5, lookVec.z * d);
            BlockPos blockPos = BlockPos.ofFloored(checkPos);
            var state = mc.world.getBlockState(blockPos);

            if (state.isAir()) {
                // Check below — is there a floor?
                BlockPos belowPos = blockPos.down();
                var belowState = mc.world.getBlockState(belowPos);
                if (belowState.isAir()) {
                    return true; // gap detected
                }
            }
        }
        return false;
    }

    /**
     * Check if player is at the edge of a block.
     */
    @Native(type = Native.Type.VMProtectBeginUltra)
    private boolean isAtEdge() {
        if (mc.player == null) return false;

        Vec3d playerPos = mc.player.getEntityPos();
        Vec3d lookVec = mc.player.getRotationVec(0);

        // Check 0.5 blocks ahead at feet level
        Vec3d checkPos = playerPos.add(lookVec.x * 0.5, -0.5, lookVec.z * 0.5);
        BlockPos blockPos = BlockPos.ofFloored(checkPos);
        return mc.world.getBlockState(blockPos).isAir();
    }
}
