package rich.util.player;

import it.unimi.dsi.fastutil.objects.Object2DoubleArrayMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.LadderBlock;
import net.minecraft.block.TrapdoorBlock;
import net.minecraft.block.PowderSnowBlock;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.vehicle.AbstractBoatEntity;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShape;
import rich.IMinecraft;
import rich.util.move.MoveUtil;

@SuppressWarnings("deprecation")
public class PlayerSimulation implements Simulation, IMinecraft {

    public final PlayerEntity player;
    public final SimulatedPlayerInput input;
    public Vec3d pos;
    public Vec3d velocity;
    public Box boundingBox;
    public float yaw;
    public float pitch;
    public boolean sprinting;
    public float fallDistance;
    public int jumpingCooldown;
    public boolean isJumping;
    public boolean isFallFlying;
    public boolean onGround;
    public boolean horizontalCollision;
    public boolean verticalCollision;
    public boolean touchingWater;
    public boolean isSwimming;
    public boolean submergedInWater;
    private final Object2DoubleMap<TagKey<Fluid>> fluidHeight;
    private final HashSet<TagKey<Fluid>> submergedFluidTag;

    private int simulatedTicks = 0;
    private boolean clipLedged = false;

    private static final double STEP_HEIGHT = 0.5;

    public PlayerSimulation(PlayerEntity player,
                            SimulatedPlayerInput input,
                            Vec3d pos,
                            Vec3d velocity,
                            Box boundingBox,
                            float yaw,
                            float pitch,
                            boolean sprinting,
                            float fallDistance,
                            int jumpingCooldown,
                            boolean isJumping,
                            boolean isFallFlying,
                            boolean onGround,
                            boolean horizontalCollision,
                            boolean verticalCollision,
                            boolean touchingWater,
                            boolean isSwimming,
                            boolean submergedInWater,
                            Object2DoubleMap<TagKey<Fluid>> fluidHeight,
                            HashSet<TagKey<Fluid>> submergedFluidTag) {
        this.player = player;
        this.input = input;
        this.pos = pos;
        this.velocity = velocity;
        this.boundingBox = boundingBox;
        this.yaw = yaw;
        this.pitch = pitch;
        this.sprinting = sprinting;
        this.fallDistance = fallDistance;
        this.jumpingCooldown = jumpingCooldown;
        this.isJumping = isJumping;
        this.isFallFlying = isFallFlying;
        this.onGround = onGround;
        this.horizontalCollision = horizontalCollision;
        this.verticalCollision = verticalCollision;
        this.touchingWater = touchingWater;
        this.isSwimming = isSwimming;
        this.submergedInWater = submergedInWater;
        this.fluidHeight = fluidHeight;
        this.submergedFluidTag = submergedFluidTag;
    }

    public static PlayerSimulation simulateLocalPlayer(int ticks) {
        PlayerSimulation simulatedPlayer = PlayerSimulation.fromClientPlayer(PlayerSimulation.SimulatedPlayerInput.fromClientPlayer(mc.player.input.playerInput));

        for (int i = 0; i < ticks; i++) {
            simulatedPlayer.tick();
        }

        return simulatedPlayer;
    }

    public static PlayerSimulation simulateOtherPlayer(PlayerEntity player, int ticks) {
        PlayerSimulation simulatedPlayer = PlayerSimulation.fromOtherPlayer(player, PlayerSimulation.SimulatedPlayerInput.guessInput(player));

        for (int i = 0; i < ticks; i++) {
            simulatedPlayer.tick();
        }

        return simulatedPlayer;
    }

    public static PlayerSimulation fromClientPlayer(SimulatedPlayerInput input) {
        ClientPlayerEntity player = mc.player;
        return new PlayerSimulation(
                player,
                input,
                player.getEntityPos(),
                player.getVelocity(),
                player.getBoundingBox(),
                player.getYaw(),
                player.getPitch(),
                player.isSprinting(),
                (float) player.fallDistance,
                player.jumpingCooldown,
                player.jumping,
                player.isGliding(),
                player.isOnGround(),
                player.horizontalCollision,
                player.verticalCollision,
                player.isTouchingWater(),
                player.isSwimming(),
                player.isSubmergedInWater(),
                new Object2DoubleArrayMap<>(player.fluidHeight),
                new HashSet<>(player.submergedFluidTag)
        );
    }

    public static PlayerSimulation fromOtherPlayer(PlayerEntity player, SimulatedPlayerInput input) {
        return new PlayerSimulation(
                player,
                input,
                player.getEntityPos(),
                player.getEntityPos().subtract(new Vec3d(player.lastX, player.lastY, player.lastZ)),
                player.getBoundingBox(),
                player.getYaw(),
                player.getPitch(),
                player.isSprinting(),
                (float) player.fallDistance,
                player.jumpingCooldown,
                player.jumping,
                player.isGliding(),
                player.isOnGround(),
                player.horizontalCollision,
                player.verticalCollision,
                player.isTouchingWater(),
                player.isSwimming(),
                player.isSubmergedInWater(),
                new Object2DoubleArrayMap<>(player.fluidHeight),
                new HashSet<>(player.submergedFluidTag)
        );
    }

    @Override
    public Vec3d pos() {
        return player.getEntityPos();
    }

    @Override
    public void tick() {
        simulatedTicks++;
        clipLedged = false;
        if (pos.y <= -70) {
            return;
        }
        input.update();
        checkWaterState();
        updateSubmergedInWaterState();
        updateSwimming();

        if (jumpingCooldown > 0) {
            jumpingCooldown--;
        }
        isJumping = input.playerInput.jump();
        double newX = velocity.x;
        double newY = velocity.y;
        double newZ = velocity.z;
        if (Math.abs(velocity.x) < 0.003) newX = 0.0;
        if (Math.abs(velocity.y) < 0.003) newY = 0.0;
        if (Math.abs(velocity.z) < 0.003) newZ = 0.0;
        if (onGround) {
            isFallFlying = false;
        }
        velocity = new Vec3d(newX, newY, newZ);

        if (isJumping) {
            double fluidLevel = isInLava() ? getFluidHeight(FluidTags.LAVA) : getFluidHeight(FluidTags.WATER);
            boolean inWater = isTouchingWater() && fluidLevel > 0.0;
            double swimHeight = getSwimHeight();
            if (inWater && (!onGround || fluidLevel > swimHeight)) {
                swimUpward(FluidTags.WATER);
            } else if (isInLava() && (!onGround || fluidLevel > swimHeight)) {
                swimUpward(FluidTags.LAVA);
            } else if ((onGround || (inWater && fluidLevel <= swimHeight)) && jumpingCooldown == 0) {
                jump();
                if (player.equals(mc.player)) {
                    jumpingCooldown = 10;
                }
            }
        }

        float sidewaysSpeed = input.movementSideways * 0.98f;
        float forwardSpeed = input.movementForward * 0.98f;
        float upwardsSpeed = 0.0f;

        if (hasStatusEffect(StatusEffects.SLOW_FALLING) || hasStatusEffect(StatusEffects.LEVITATION)) {
            onLanding();
        }

        travel(new Vec3d(sidewaysSpeed, upwardsSpeed, forwardSpeed));
    }

    private void travel(Vec3d movementInput) {
        if (isSwimming && !player.hasVehicle()) {
            double g = getRotationVector().y;
            double h = (g < -0.2) ? 0.085 : 0.06;
            BlockPos posAbove = new BlockPos(MathHelper.floor(pos.x),
                    MathHelper.floor(pos.y + 1.0 - 0.1),
                    MathHelper.floor(pos.z));
            if (g <= 0.0 || input.playerInput.jump() ||
                    !player.getEntityWorld().getBlockState(posAbove).getFluidState().isEmpty()) {
                velocity = velocity.add(0.0, (g - velocity.y) * h, 0.0);
            }
        }

        double beforeTravelVelocityY = velocity.y;
        double d = 0.08;
        boolean falling = velocity.y <= 0.0;
        if (velocity.y <= 0.0 && hasStatusEffect(StatusEffects.SLOW_FALLING)) {
            d = 0.01;
            onLanding();
        }

        if (isTouchingWater() && player.shouldSwimInFluids()) {
            double e = pos.y;
            float f = isSprinting() ? 0.9f : 0.8f;
            float g = 0.02f;
            float h = (float) getAttributeValue(EntityAttributes.WATER_MOVEMENT_EFFICIENCY);
            if (!onGround) {
                h *= 0.5f;
            }
            if (h > 0.0f) {
                f += (0.54600006f - f) * h / 3.0f;
                g += (getMovementSpeed() - g) * h / 3.0f;
            }
            if (hasStatusEffect(StatusEffects.DOLPHINS_GRACE)) {
                f = 0.96f;
            }
            updateVelocity(g, movementInput);
            move(velocity);
            Vec3d tempVel = velocity;
            if (horizontalCollision && isClimbing()) {
                tempVel = new Vec3d(tempVel.x, 0.2, tempVel.z);
            }
            velocity = tempVel.multiply(f, 0.8, f);
            Vec3d vec3d2 = player.applyFluidMovingSpeed(d, falling, velocity);
            velocity = vec3d2;
            if (horizontalCollision && doesNotCollide(vec3d2.x, vec3d2.y + 0.6 - pos.y + e, vec3d2.z)) {
                velocity = new Vec3d(vec3d2.x, 0.3, vec3d2.z);
            }
        } else if (isInLava() && player.shouldSwimInFluids()) {
            double e = pos.y;
            updateVelocity(0.02f, movementInput);
            move(velocity);
            if (getFluidHeight(FluidTags.LAVA) <= getSwimHeight()) {
                velocity = velocity.multiply(0.5, 0.8, 0.5);
                velocity = player.applyFluidMovingSpeed(d, falling, velocity);
            } else {
                velocity = velocity.multiply(0.5);
            }
            if (!player.hasNoGravity()) {
                velocity = velocity.add(0.0, -d / 4.0, 0.0);
            }
            if (horizontalCollision && doesNotCollide(velocity.x, velocity.y + 0.6 - pos.y + e, velocity.z)) {
                velocity = new Vec3d(velocity.x, 0.3, velocity.z);
            }
        } else if (isFallFlying) {
            double k;
            Vec3d e = velocity;
            if (e.y > -0.5) {
                fallDistance = 1.0f;
            }
            Vec3d vec3d3 = getRotationVector();
            float f = pitch * ((float) Math.PI / 180f);
            double g = Math.sqrt(vec3d3.x * vec3d3.x + vec3d3.z * vec3d3.z);
            double horizontalSpeed = velocity.horizontalLength();
            double i = vec3d3.length();
            float j = MathHelper.cos(f);
            j = (float) (j * (j * Math.min(1.0, i / 0.4)));
            e = velocity.add(0.0, d * (-1.0 + j * 0.75), 0.0);
            if (e.y < 0.0 && g > 0.0) {
                k = e.y * -0.1 * j;
                e = e.add(vec3d3.x * k / g, k, vec3d3.z * k / g);
            }
            if (f < 0.0f && g > 0.0) {
                k = horizontalSpeed * (-MathHelper.sin(f)) * 0.04;
                e = e.add(-vec3d3.x * k / g, k * 3.2, -vec3d3.z * k / g);
            }
            if (g > 0.0) {
                e = e.add((vec3d3.x / g * horizontalSpeed - e.x) * 0.1, 0.0, (vec3d3.z / g * horizontalSpeed - e.z) * 0.1);
            }
            velocity = e.multiply(0.99, 0.98, 0.99);
            move(velocity);
        } else {
            BlockPos blockPos = getVelocityAffectingPos();
            float p = player.getEntityWorld().getBlockState(blockPos).getBlock().getSlipperiness();
            float f = onGround ? p * 0.91f : 0.91f;
            Vec3d vec3d6 = applyMovementInput(movementInput, p);
            double q = vec3d6.y;
            if (hasStatusEffect(StatusEffects.LEVITATION)) {
                StatusEffectInstance levitation = getStatusEffect(StatusEffects.LEVITATION);
                if (levitation != null) {
                    q += (0.05 * (levitation.getAmplifier() + 1) - vec3d6.y) * 0.2;
                }
            } else if (player.getEntityWorld().isClient() && !player.getEntityWorld().isChunkLoaded(blockPos)) {
                q = (pos.y > player.getEntityWorld().getBottomY()) ? -0.1 : 0.0;
            } else if (!player.hasNoGravity()) {
                q -= d;
            }
            if (player.hasNoDrag()) {
                velocity = new Vec3d(vec3d6.x, q, vec3d6.z);
            } else {
                velocity = new Vec3d(vec3d6.x * f, q * 0.9800000190734863, vec3d6.z * f);
            }
        }

        if (player.getAbilities().flying && !player.hasVehicle()) {
            velocity = new Vec3d(velocity.x, beforeTravelVelocityY * 0.6, velocity.z);
            onLanding();
        }
    }

    private Vec3d applyMovementInput(Vec3d movementInput, float slipperiness) {
        updateVelocity(getMovementSpeed(slipperiness), movementInput);
        velocity = applyClimbingSpeed(velocity);
        move(velocity);
        Vec3d result = velocity;
        BlockPos posBlock = posToBlockPos(pos);
        BlockState state = getState(posBlock);
        if ((horizontalCollision || isJumping) &&
                (isClimbing() || (state != null && state.isOf(Blocks.POWDER_SNOW) &&
                        PowderSnowBlock.canWalkOnPowderSnow(player)))) {
            result = new Vec3d(result.x, 0.2, result.z);
        }
        return result;
    }

    private void updateVelocity(float speed, Vec3d movementInput) {
        Vec3d vec = Entity.movementInputToVelocity(movementInput, speed, yaw);
        velocity = velocity.add(vec);
    }

    private float getMovementSpeed(float slipperiness) {
        return onGround ? getMovementSpeed() * (0.21600002f / (slipperiness * slipperiness * slipperiness))
                : getAirStrafingSpeed();
    }

    private float getAirStrafingSpeed() {
        float speed = 0.02f;
        if (input.playerInput.sprint()) {
            return speed + 0.005999999865889549f;
        }
        return speed;
    }

    private float getMovementSpeed() {
        return 0.10000000149011612f;
    }

    private void move(Vec3d movement) {
        Vec3d modifiedMovement = movement;
        modifiedMovement = adjustMovementForSneaking(modifiedMovement);
        Vec3d adjustedMovement = adjustMovementForCollisions(modifiedMovement);
        if (adjustedMovement.lengthSquared() > 1.0E-7) {
            pos = pos.add(adjustedMovement);
            boundingBox = player.getDimensions(player.getPose()).getBoxAt(pos);
        }
        boolean xCollision = !MathHelper.approximatelyEquals(movement.x, adjustedMovement.x);
        boolean zCollision = !MathHelper.approximatelyEquals(movement.z, adjustedMovement.z);
        horizontalCollision = xCollision || zCollision;
        verticalCollision = (movement.y != adjustedMovement.y);
        onGround = verticalCollision && movement.y < 0.0;
        if (!isTouchingWater()) {
            checkWaterState();
        }
        if (onGround) {
            onLanding();
        } else if (movement.y < 0) {
            fallDistance -= (float) movement.y;
        }
        Vec3d currentVel = velocity;
        if (horizontalCollision || verticalCollision) {
            velocity = new Vec3d(xCollision ? 0.0 : currentVel.x,
                    onGround ? 0.0 : currentVel.y,
                    zCollision ? 0.0 : currentVel.z);
        }
    }

    private Vec3d adjustMovementForCollisions(Vec3d movement) {
        Box box = new Box(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3).offset(pos);
        List<VoxelShape> collisionShapes = Collections.emptyList();
        Vec3d adjusted;
        if (movement.lengthSquared() == 0.0) {
            adjusted = movement;
        } else {
            adjusted = Entity.adjustMovementForCollisions(player, movement, box, player.getEntityWorld(), collisionShapes);
        }
        boolean xCollide = movement.x != adjusted.x;
        boolean yCollide = movement.y != adjusted.y;
        boolean zCollide = movement.z != adjusted.z;
        boolean stepPossible = onGround || (yCollide && movement.y < 0.0);
        if (player.getStepHeight() > 0.0f && stepPossible && (xCollide || zCollide)) {
            Vec3d stepAdjust = Entity.adjustMovementForCollisions(player,
                    new Vec3d(movement.x, player.getStepHeight(), movement.z),
                    box, player.getEntityWorld(), collisionShapes);
            Vec3d stepOffset = Entity.adjustMovementForCollisions(player,
                    new Vec3d(0.0, player.getStepHeight(), 0.0),
                    box.stretch(movement.x, 0.0, movement.z), player.getEntityWorld(), collisionShapes);
            Vec3d combined = Entity.adjustMovementForCollisions(player,
                    new Vec3d(movement.x, 0.0, movement.z),
                    box.offset(stepOffset), player.getEntityWorld(), collisionShapes).add(stepOffset);
            if (stepOffset.y < player.getStepHeight() && combined.horizontalLengthSquared() > stepAdjust.horizontalLengthSquared()) {
                stepAdjust = combined;
            }
            if (stepAdjust.horizontalLengthSquared() > adjusted.horizontalLengthSquared()) {
                return stepAdjust.add(Entity.adjustMovementForCollisions(player,
                        new Vec3d(0.0, -stepAdjust.y + movement.y, 0.0),
                        box.offset(stepAdjust), player.getEntityWorld(), collisionShapes));
            }
        }
        return adjusted;
    }

    private void onLanding() {
        fallDistance = 0.0f;
    }

    public void jump() {
        velocity = velocity.add(0.0, getJumpVelocity() - velocity.y, 0.0);
        if (isSprinting()) {
            float rad = (float) Math.toRadians(yaw);
            velocity = velocity.add(-MathHelper.sin(rad) * 0.2, 0.0, MathHelper.cos(rad) * 0.2);
        }
    }

    private Vec3d applyClimbingSpeed(Vec3d motion) {
        if (!isClimbing()) {
            return motion;
        }
        onLanding();
        double clampedX = MathHelper.clamp(motion.x, -0.15000000596046448, 0.15000000596046448);
        double clampedZ = MathHelper.clamp(motion.z, -0.15000000596046448, 0.15000000596046448);
        double clampedY = Math.max(motion.y, -0.15000000596046448);
        if (clampedY < 0.0 && !getState(posToBlockPos(pos)).isOf(Blocks.SCAFFOLDING) && player.isHoldingOntoLadder()) {
            clampedY = 0.0;
        }
        return new Vec3d(clampedX, clampedY, clampedZ);
    }

    public boolean isClimbing() {
        BlockPos posBlock = posToBlockPos(pos);
        BlockState state = getState(posBlock);
        if (state.isIn(BlockTags.CLIMBABLE)) {
            return true;
        } else return state.getBlock() instanceof TrapdoorBlock && canEnterTrapdoor(posBlock, state);
    }

    private boolean canEnterTrapdoor(BlockPos pos, BlockState state) {
        if (!state.get(TrapdoorBlock.OPEN)) {
            return false;
        }
        BlockState below = player.getEntityWorld().getBlockState(pos.down());
        return below.isOf(Blocks.LADDER) && below.get(LadderBlock.FACING).equals(state.get(TrapdoorBlock.FACING));
    }

    private Vec3d adjustMovementForSneaking(Vec3d movement) {
        if (movement.y <= 0.0 && method_30263()) {
            double dx = movement.x;
            double dz = movement.z;
            double step = 0.05;
            while (dx != 0.0 && player.getEntityWorld().isSpaceEmpty(player, boundingBox.offset(dx, -STEP_HEIGHT, 0.0))) {
                if (dx < step && dx >= -step) {
                    dx = 0.0;
                    break;
                }
                dx += (dx > 0 ? -step : step);
            }
            while (dz != 0.0 && player.getEntityWorld().isSpaceEmpty(player, boundingBox.offset(0.0, -STEP_HEIGHT, dz))) {
                if (dz < step && dz >= -step) {
                    dz = 0.0;
                    break;
                }
                dz += (dz > 0 ? -step : step);
            }
            while (dx != 0.0 && dz != 0.0 && player.getEntityWorld().isSpaceEmpty(player, boundingBox.offset(dx, -STEP_HEIGHT, dz))) {
                dx = (dx < step && dx >= -step) ? 0.0 : (dx > 0 ? dx - step : dx + step);
                if (dz < step && dz >= -step) {
                    dz = 0.0;
                    break;
                }
                dz += (dz > 0 ? -step : step);
            }
            if (movement.x != dx || movement.z != dz) {
                clipLedged = true;
            }
            if (shouldClipAtLedge()) {
                movement = new Vec3d(dx, movement.y, dz);
            }
        }
        return movement;
    }

    protected boolean shouldClipAtLedge() {
        return input.playerInput.sneak() || input.forceSafeWalk;
    }

    private boolean method_30263() {
        return onGround || (fallDistance < STEP_HEIGHT &&
                !player.getEntityWorld().isSpaceEmpty(player, boundingBox.offset(0.0, fallDistance - STEP_HEIGHT, 0.0)));
    }

    private boolean isSprinting() {
        return sprinting;
    }

    private float getJumpVelocity() {
        return 0.42f * getJumpVelocityMultiplier() + getJumpBoostVelocityModifier();
    }

    private float getJumpBoostVelocityModifier() {
        if (hasStatusEffect(StatusEffects.JUMP_BOOST)) {
            StatusEffectInstance boost = getStatusEffect(StatusEffects.JUMP_BOOST);
            return 0.1f * (boost.getAmplifier() + 1);
        }
        return 0f;
    }

    private float getJumpVelocityMultiplier() {
        float multiplier1 = 0f;
        Block block = getState(posToBlockPos(pos)).getBlock();
        if (block != null) {
            multiplier1 = block.getJumpVelocityMultiplier();
        }
        float multiplier2 = 0f;
        Block block2 = getState(getVelocityAffectingPos()).getBlock();
        if (block2 != null) {
            multiplier2 = block2.getJumpVelocityMultiplier();
        }
        return (multiplier1 == 1.0f) ? multiplier2 : multiplier1;
    }

    private boolean doesNotCollide(double offsetX, double offsetY, double offsetZ) {
        return doesNotCollide(boundingBox.offset(offsetX, offsetY, offsetZ));
    }

    private boolean doesNotCollide(Box box) {
        return player.getEntityWorld().isSpaceEmpty(player, box) && !player.getEntityWorld().containsFluid(box);
    }

    private void swimUpward(TagKey<Fluid> fluidTag) {
        velocity = velocity.add(0.0, 0.03999999910593033, 0.0);
    }

    private BlockPos getVelocityAffectingPos() {
        return BlockPos.ofFloored(pos.x, boundingBox.minY - 0.5000001, pos.z);
    }

    private double getSwimHeight() {
        return (player.getStandingEyeHeight() < 0.4) ? 0.0 : 0.4;
    }

    private boolean isTouchingWater() {
        return touchingWater;
    }

    public boolean isInLava() {
        return fluidHeight.getDouble(FluidTags.LAVA) > 0.0;
    }

    private void checkWaterState() {
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof AbstractBoatEntity boat) {
            if (!boat.isSubmergedInWater()) {
                touchingWater = false;
                return;
            }
        }
        if (updateMovementInFluid(FluidTags.WATER, 0.014)) {
            onLanding();
            touchingWater = true;
        } else {
            touchingWater = false;
        }
    }

    private void updateSwimming() {
        if (isSwimming) {
            isSwimming = isSprinting() && isTouchingWater() && !player.hasVehicle();
        } else {
            isSwimming = isSprinting() && isSubmergedInWater() &&
                    !player.hasVehicle() &&
                    player.getEntityWorld().getFluidState(posToBlockPos(pos)).isIn(FluidTags.WATER);
        }
    }

    private void updateSubmergedInWaterState() {
        submergedInWater = submergedFluidTag.contains(FluidTags.WATER);
        submergedFluidTag.clear();
        double eyeLevel = getEyeY() - 0.1111111119389534;
        Entity vehicle = player.getVehicle();
        if (vehicle instanceof AbstractBoatEntity boat) {
            if (!boat.isSubmergedInWater() &&
                    boat.getBoundingBox().maxY >= eyeLevel &&
                    boat.getBoundingBox().minY <= eyeLevel) {
                return;
            }
        }
        BlockPos posEye = BlockPos.ofFloored(pos.x, eyeLevel, pos.z);
        FluidState fluidState = player.getEntityWorld().getFluidState(posEye);
        double height = posEye.getY() + fluidState.getHeight(player.getEntityWorld(), posEye);
        if (height > eyeLevel) {
            submergedFluidTag.addAll(fluidState.streamTags().toList());
        }
    }

    private double getEyeY() {
        return pos.y + player.getStandingEyeHeight();
    }

    public boolean isSubmergedInWater() {
        return submergedInWater && isTouchingWater();
    }

    private double getFluidHeight(TagKey<Fluid> tag) {
        return fluidHeight.getDouble(tag);
    }

    private boolean updateMovementInFluid(TagKey<Fluid> tag, double speed) {
        if (isRegionUnloaded()) {
            return false;
        }
        Box box = boundingBox.contract(0.001);
        int i = MathHelper.floor(box.minX);
        int j = MathHelper.ceil(box.maxX);
        int k = MathHelper.floor(box.minY);
        int l = MathHelper.ceil(box.maxY);
        int m = MathHelper.floor(box.minZ);
        int n = MathHelper.ceil(box.maxZ);
        double d = 0.0;
        boolean pushedByFluids = true;
        boolean foundFluid = false;
        Vec3d fluidVelocity = Vec3d.ZERO;
        int count = 0;
        BlockPos.Mutable mutable = new BlockPos.Mutable();
        for (int p = i; p < j; p++) {
            for (int q = k; q < l; q++) {
                for (int r = m; r < n; r++) {
                    mutable.set(p, q, r);
                    FluidState fluidState = player.getEntityWorld().getFluidState(mutable);
                    if (fluidState.isIn(tag)) {
                        double e = q + fluidState.getHeight(player.getEntityWorld(), mutable);
                        if (e >= box.minY) {
                            foundFluid = true;
                            d = Math.max(e - box.minY, d);
                            if (pushedByFluids) {
                                Vec3d vel = fluidState.getVelocity(player.getEntityWorld(), mutable);
                                if (d < 0.4) {
                                    vel = vel.multiply(d);
                                }
                                fluidVelocity = fluidVelocity.add(vel);
                                count++;
                            }
                        }
                    }
                }
            }
        }
        if (fluidVelocity.length() > 0.0) {
            if (count > 0) {
                fluidVelocity = fluidVelocity.multiply(1.0 / count);
            }
            fluidVelocity = fluidVelocity.multiply(speed);
            if (Math.abs(velocity.x) < 0.003 && Math.abs(velocity.z) < 0.003 &&
                    fluidVelocity.length() < 0.0045) {
                fluidVelocity = fluidVelocity.normalize().multiply(0.0045);
            }
            velocity = velocity.add(fluidVelocity);
        }
        fluidHeight.put(tag, d);
        return foundFluid;
    }

    private boolean isRegionUnloaded() {
        Box box = boundingBox.expand(1.0);
        int i = MathHelper.floor(box.minX);
        int j = MathHelper.ceil(box.maxX);
        int k = MathHelper.floor(box.minZ);
        int l = MathHelper.ceil(box.maxZ);
        return !player.getEntityWorld().isRegionLoaded(i, k, j, l);
    }

    private Vec3d getRotationVector() {
        return getRotationVector(pitch, yaw);
    }

    private Vec3d getRotationVector(float pitch, float yaw) {
        float f = (float) (pitch * Math.PI / 180.0);
        float g = (float) (-yaw * Math.PI / 180.0);
        float h = MathHelper.cos(g);
        float i = MathHelper.sin(g);
        float j = MathHelper.cos(f);
        float k = MathHelper.sin(f);
        return new Vec3d(i * j, -k, h * j);
    }

    public boolean hasStatusEffect(RegistryEntry<StatusEffect> effect) {
        StatusEffectInstance instance = player.getStatusEffect(effect);
        return instance != null && instance.getDuration() >= simulatedTicks;
    }

    private StatusEffectInstance getStatusEffect(RegistryEntry<StatusEffect> effect) {
        StatusEffectInstance instance = player.getStatusEffect(effect);
        if (instance == null || instance.getDuration() < simulatedTicks) {
            return null;
        }
        return instance;
    }

    public double getAttributeValue(RegistryEntry<EntityAttribute> attribute) {
        return player.getAttributes().getValue(attribute);
    }

    @Override
    public PlayerSimulation clone() {
        return new PlayerSimulation(
                player,
                input,
                pos,
                velocity,
                boundingBox,
                yaw,
                pitch,
                sprinting,
                fallDistance,
                jumpingCooldown,
                isJumping,
                isFallFlying,
                onGround,
                horizontalCollision,
                verticalCollision,
                touchingWater,
                isSwimming,
                submergedInWater,
                new Object2DoubleArrayMap<>(fluidHeight),
                new HashSet<>(submergedFluidTag)
        );
    }

    public BlockPos posToBlockPos(Vec3d pos) {
        return new BlockPos(MathHelper.floor(pos.x), MathHelper.floor(pos.y), MathHelper.floor(pos.z));
    }

    public BlockState getState(BlockPos pos) {
        return player.getEntityWorld().getBlockState(pos);
    }

    public static class SimulatedPlayerInput extends Input {
        public boolean forceSafeWalk = false;
        public float movementForward;
        public float movementSideways;
        public PlayerInput playerInput;
        public static final double MAX_WALKING_SPEED = 0.121;

        public SimulatedPlayerInput(PlayerInput input) {
            this.playerInput = input;
        }

        public void update() {
            if (playerInput.forward() != playerInput.backward()) {
                movementForward = playerInput.forward() ? 1.0f : -1.0f;
            } else {
                movementForward = 0.0f;
            }
            if (playerInput.left() == playerInput.right()) {
                movementSideways = 0.0f;
            } else {
                movementSideways = playerInput.left() ? 1.0f : -1.0f;
            }
            if (playerInput.sneak()) {
                movementSideways *= 0.3f;
                movementForward *= 0.3f;
            }
        }

        @Override
        public String toString() {
            return "SimulatedPlayerInput(forwards={" + playerInput.forward() + "}, backwards={" + playerInput.backward() +
                    "}, left={" + playerInput.left() + "}, right={" + playerInput.right() + "}, jumping={" + playerInput.jump() +
                    "}, sprinting=" + playerInput.sprint() + ", slowDown=" + playerInput.sneak() + ")";
        }

        public static SimulatedPlayerInput fromClientPlayer(PlayerInput input) {
            return new SimulatedPlayerInput(input);
        }

        public static SimulatedPlayerInput guessInput(PlayerEntity entity) {
            Vec3d velocity = entity.getEntityPos().subtract(new Vec3d(entity.lastX, entity.lastY, entity.lastZ));
            double horizontalVelocity = velocity.horizontalLengthSquared();
            PlayerInput input = new PlayerInput(false, false, false, false, !entity.isOnGround(), entity.isSneaking(), horizontalVelocity >= MAX_WALKING_SPEED * MAX_WALKING_SPEED);
            if (horizontalVelocity > 0.05 * 0.05) {
                double velocityAngle = MoveUtil.getDegreesRelativeToView(velocity, entity.getYaw());
                double wrappedAngle = MathHelper.wrapDegrees(velocityAngle);
                input = MoveUtil.getDirectionalInputForDegrees(input, wrappedAngle);
            }
            return new SimulatedPlayerInput(input);
        }
    }
}