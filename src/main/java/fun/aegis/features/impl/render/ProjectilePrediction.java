package fun.aegis.features.impl.render;

import fun.aegis.utils.features.aura.warp.Turns;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ChargedProjectilesComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.*;
import net.minecraft.entity.projectile.thrown.*;
import net.minecraft.item.*;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.RaycastContext;
import org.joml.Quaternionf;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.utils.display.font.FontRenderer;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.math.projection.Projection;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.display.geometry.Render2D;
import fun.aegis.utils.display.geometry.Render3D;
import fun.aegis.events.render.DrawEvent;
import fun.aegis.events.render.WorldRenderEvent;
import fun.aegis.utils.features.aura.utils.RaycastAngle;
import fun.aegis.utils.features.aura.warp.TurnsConnection;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectilePrediction extends Module {

    public static ProjectilePrediction getInstance() {
        return Instance.get(ProjectilePrediction.class);
    }

    final List<Point> points = new ArrayList<>();

    public ProjectilePrediction() {super("ProjectilePrediction", "Projectile Prediction", ModuleCategory.RENDER);}

    @EventHandler
    public void onDraw(DrawEvent e) {
        DrawContext context = e.getDrawContext();

        for (Point point : points) {
            Vec3d vec3d = Projection.worldSpaceToScreenSpace(point.pos);
            int ticks = point.ticks;

            if (!Projection.canSee(point.pos)) continue;

            FontRenderer font = Fonts.getSize(13);
            double time = ticks * 50 / 1000.0;
            String text = String.format("%.1f", time) + " сек";
            float textWidth = font.getStringWidth(text);
            float posX = (float) (vec3d.getX() + textWidth / 2 - 6);
            float posY = (float) (vec3d.getY() + 4);
            float padding = 3;
            float iconSize = 8;

            blur.render(ShapeProperties.create(context.getMatrices(),posX - textWidth + iconSize + padding, posY - padding, padding + textWidth + padding, 10)
                    .round(1.5F).color(ColorAssist.HALF_BLACK).build());
            font.drawString(context.getMatrices(), text, posX - textWidth + 8 + padding * 2, posY + 0.5F, -1);

            Render2D.defaultDrawStack(context, point.stack, posX - textWidth - padding + 2, posY - padding, true, false, 0.5F);
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.player == null || mc.world == null) return;
        points.clear();
        drawPredictionInHand(e.getStack(), mc.player.getHandItems(), TurnsConnection.INSTANCE.getRotation());
        getProjectiles().forEach(entity -> {
            Vec3d motion = entity.getVelocity();
            Vec3d pos = entity.getPos();
            Vec3d prevPos;
            for (int i = 0; i < 300; i++) {
                prevPos = pos;
                pos = pos.add(motion);
                motion = calculateMotion(entity, prevPos, motion);
                HitResult result = RaycastAngle.raycast(prevPos, pos, RaycastContext.ShapeType.COLLIDER, entity);

                if (!result.getType().equals(HitResult.Type.MISS)) {
                    pos = result.getPos();
                }

                Render3D.drawLine(prevPos, pos, ColorAssist.multAlpha(ColorAssist.fade(i), MathHelper.clamp(i / 25.0f, 0, 1)), 2, false);

                if (!result.getType().equals(HitResult.Type.MISS) || pos.y < -128) {
                    BreakingBad(entity, pos, i);
                    break;
                }
            }
        });
    }

    public void drawPredictionInHand(MatrixStack matrix, Iterable<ItemStack> stacks, Turns angle) {
        Item activeItem = mc.player.getActiveItem().getItem();
        for (ItemStack stack : stacks) {
            List<HitResult> results = switch (stack.getItem()) {
                case ExperienceBottleItem item -> checkTrajectory(new ExperienceBottleEntity(mc.world, mc.player, stack), 0.8, angle);
                case SplashPotionItem item -> checkTrajectory(new PotionEntity(mc.world, mc.player, stack), 0.55, angle);
                case TridentItem item when item.equals(activeItem) && mc.player.getItemUseTime() >= 10 -> checkTrajectory(new TridentEntity(mc.world, mc.player, stack), 2.5, angle);
                case SnowballItem item -> checkTrajectory(new SnowballEntity(mc.world, mc.player, stack), 1.5, angle);
                case EggItem item -> checkTrajectory(new EggEntity(mc.world, mc.player, stack), 1.5, angle);
                case EnderPearlItem item -> checkTrajectory(new EnderPearlEntity(mc.world, mc.player, stack), 1.5, angle);
                case BowItem item when item.equals(activeItem) && mc.player.isUsingItem() -> checkTrajectory(new ArrowEntity(mc.world, mc.player, stack, stack), 3 * MathHelper.clamp((mc.player.getItemUseTime() + tickCounter.getTickDelta(false)) / 20F,0F, 1F), angle);
                case CrossbowItem item when CrossbowItem.isCharged(stack) -> {
                    ChargedProjectilesComponent component = stack.get(DataComponentTypes.CHARGED_PROJECTILES);
                    List<HitResult> list = new ArrayList<>();
                    if (component != null) {
                        float velocity = component.getProjectiles().getFirst().isOf(Items.FIREWORK_ROCKET) ? 100 : 3;
                        list.add(checkTrajectory(angle.toVector(), new ArrowEntity(mc.world, mc.player, stack, stack), velocity));
                        if (component.getProjectiles().size() > 2) {
                            float pitchAbs = angle.getPitch() / 90;
                            float delta = pitchAbs * pitchAbs * pitchAbs * pitchAbs * pitchAbs;
                            float yaw = MathHelper.lerp(Math.abs(delta), 10, 90);
                            float pitch = MathHelper.lerp(delta, 0, 10);
                            list.add(checkTrajectory(angle.addYaw(-yaw).addPitch(-pitch).toVector(), new ArrowEntity(mc.world, mc.player, stack, stack), velocity));
                            list.add(checkTrajectory(angle.addYaw(yaw * 2).toVector(), new ArrowEntity(mc.world, mc.player, stack, stack), velocity));
                        }
                    }
                    yield list;
                }
                default -> null;
            };
            if (results != null) {
                results = results.stream().filter(Objects::nonNull).toList();
                if (!results.isEmpty()) renderProjectileResults(matrix, results);
            }
            return;
        }
    }

    public void renderProjectileResults(MatrixStack matrix, List<HitResult> results) {
        for (HitResult result : results) {
            Direction direction = getDirection(result);
            int color = result.getType().equals(HitResult.Type.ENTITY) ? Color.RED.getRGB() : ColorAssist.getClientColor();
            double width = 0.3;

            Quaternionf quaternionf = switch (direction) {
                case Direction.WEST, Direction.EAST -> RotationAxis.POSITIVE_Z.rotationDegrees(90);
                case Direction.SOUTH, Direction.NORTH -> RotationAxis.POSITIVE_X.rotationDegrees(90);
                default -> new Quaternionf();
            };

            matrix.push();
            matrix.translate(result.getPos());
            matrix.multiply(quaternionf);
            MatrixStack.Entry entry = matrix.peek().copy();
            for (int i = 0, size = 90; i <= size; i++) {
                Render3D.drawLine(entry, Calculate.cosSin(i, size, width), Calculate.cosSin(i + 1, size, width), color, color,1, false);
            }
            Render3D.drawLine(entry, new Vec3d(0, 0, -width), new Vec3d(0, 0, width), color, color,1, false);
            Render3D.drawLine(entry, new Vec3d(-width, 0, 0), new Vec3d(width, 0, 0), color, color,1, false);
            matrix.pop();
        }
    }

    public List<Entity> getProjectiles() {
        return PlayerInteractionHelper.streamEntities().filter(e -> (e instanceof PersistentProjectileEntity || e instanceof ThrownItemEntity || e instanceof ItemEntity) && !visible(e)).toList();
    }

    public List<HitResult> checkTrajectory(ProjectileEntity entity, double velocity, Turns angle) {
        return new ArrayList<>(Collections.singleton(checkTrajectory(angle.toVector(), entity, velocity)));
    }

    public HitResult checkTrajectory(Vec3d lookVec, ProjectileEntity entity, double velocity) {
        float sqrt = MathHelper.sqrt(lookVec.toVector3f().lengthSquared());
        Vec3d motion = switch (entity) {
            case ArrowEntity arrow when arrow.getItemStack().getItem().equals(Items.CROSSBOW) -> Vec3d.ZERO;
            default -> mc.player.getVelocity();
        };
        return traceTrajectory(mc.player.getEyePos().add(Calculate.interpolate(mc.player).subtract(mc.player.getPos())), lookVec.multiply(velocity / sqrt).add(motion), entity);
    }

    public HitResult calcTrajectory(ProjectileEntity e) {
        return traceTrajectory(e.getPos(), e.getVelocity(), e);
    }

    public HitResult traceTrajectory(Vec3d pos, Vec3d motion, ProjectileEntity entity) {
        Vec3d prevPos;
        for (int i = 0; i < 300; i++) {
            prevPos = pos;
            pos = pos.add(motion);
            motion = calculateMotion(entity, prevPos, motion);

            HitResult result = RaycastAngle.raycast(prevPos, pos, RaycastContext.ShapeType.COLLIDER, entity);
            if (!result.getType().equals(HitResult.Type.MISS)) {
                return result;
            }

            Vec3d finalPos = pos, finalPrevPos = prevPos;
            if (PlayerInteractionHelper.streamEntities().filter(ent -> ent instanceof LivingEntity living && living != entity.getOwner() && living.isAlive())
                    .anyMatch(ent -> ent.getBoundingBox().expand(0.3).intersects(finalPrevPos, finalPos))) {
                return new HitResult(pos) {
                    @Override
                    public Type getType() {
                        return Type.ENTITY;
                    }
                };
            }

            if (pos.y < -128) break;
        }
        return null;
    }

    public Vec3d calculateMotion(Entity entity, Vec3d prevPos, Vec3d motion) {
        boolean isInWater = mc.world.getFluidState(BlockPos.ofFloored(prevPos)).isIn(FluidTags.WATER);
        double multiply = switch (entity) {
            case TridentEntity trident -> 0.99;
            case PersistentProjectileEntity persistent when isInWater -> 0.6;
            default -> isInWater ? 0.8 : 0.99;
        };
        return motion.multiply(multiply).add(0, -entity.getFinalGravity(),0);
    }

    private void BreakingBad(Entity entity, Vec3d pos, int ticks) {
        switch (entity) {
            case ItemEntity item -> points.add(new Point(item.getStack(), pos, ticks));
            case ThrownItemEntity thrown -> points.add(new Point(thrown.getStack(), pos, ticks));
            case PersistentProjectileEntity persistent -> points.add(new Point(persistent.getItemStack(), pos, ticks));
            default -> {}
        }
    }

    private Direction getDirection(HitResult result) {
        if (result instanceof BlockHitResult blockHitResult) {
            return blockHitResult.getSide();
        }
        return Direction.getFacing(result.getPos().subtract(mc.player.getEyePos()).normalize());
    }

    private boolean visible(Entity entity) {
        boolean posChange = entity.getX() == entity.prevX && entity.getY() == entity.prevY && entity.getZ() == entity.prevZ;
        boolean itemEntityCheck = entity instanceof ItemEntity && (entity.isOnGround() || PlayerInteractionHelper.isBoxInBlock(entity.getBoundingBox().expand(2), Blocks.WATER));
        return posChange || itemEntityCheck;
    }

    private record Point(ItemStack stack, Vec3d pos, int ticks) {}
}
