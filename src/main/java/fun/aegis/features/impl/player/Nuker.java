package fun.aegis.features.impl.player;

import antidaunleak.api.annotation.Native;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;

import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.client.managers.event.types.EventType;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import fun.aegis.utils.display.geometry.Render3D;
import fun.aegis.utils.math.task.TaskPriority;
import fun.aegis.events.player.RotationUpdateEvent;
import fun.aegis.events.render.WorldRenderEvent;
import fun.aegis.utils.features.aura.utils.MathAngle;
import fun.aegis.utils.features.aura.warp.TurnsConfig;
import fun.aegis.utils.features.aura.warp.TurnsConnection;

import java.util.Comparator;
import java.util.Objects;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Nuker extends Module {
    public BlockPos pos;
    private VoxelShape shape;

    private final BooleanSetting rotateSetting = new BooleanSetting("Поворот", "Ломать блоки ниже игрока").setValue(true);
    private final BooleanSetting downSetting = new BooleanSetting("Вниз", "Ломать блоки ниже игрока").setValue(true);
    private final SliderSettings radiusSetting = new SliderSettings("Радиус", "Ломает блоки в радиусе вокруг вас").setValue(3).range(1, 6);

    public Nuker() {
        super("Nuker", ModuleCategory.PLAYER);
        setup(rotateSetting, downSetting, radiusSetting);
    }

    
    @EventHandler

    public void onWorldRender(WorldRenderEvent e) {
        if (pos != null && shape != null && !shape.isEmpty())
            Render3D.drawShape(pos, shape, ColorAssist.getClientColor(), 2);
    }

    
    @EventHandler
    public void onRotationUpdate(RotationUpdateEvent e) {
        if (e.getType() == EventType.PRE) {
            pos = PlayerInteractionHelper.getCube(mc.player.getBlockPos(), radiusSetting.getInt(), radiusSetting.getInt(), downSetting.isValue())
                    .stream().filter(this::validBlock).min(Comparator.comparingDouble(this::blockPriority)).orElse(null);

            if (pos != null) {
                if (rotateSetting.isValue()) TurnsConnection.INSTANCE.rotateTo(MathAngle.calculateAngle(pos.toCenterPos()), TurnsConfig.DEFAULT, TaskPriority.HIGH_IMPORTANCE_1, this);
                shape = mc.world.getBlockState(pos).getOutlineShape(mc.world, pos);
                mc.interactionManager.updateBlockBreakingProgress(pos, Direction.UP);
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }

    
    private double blockPriority(BlockPos pos) {
        return switch (mc.world.getBlockState(pos).getBlock().getTranslationKey().replace("block.minecraft.", "")) {
            case "ancient_debris" -> 0;
            case "diamond_ore" -> 1;
            case "emerald_ore" -> 2;
            case "gold_ore" -> 3;
            case "iron_ore" -> 4;
            case "lapis_ore" -> 5;
            case "redstone_ore" -> 6;
            default -> mc.player.squaredDistanceTo(pos.toCenterPos());
        };
    }

    
    private boolean validBlock(BlockPos pos) {
        BlockState state = Objects.requireNonNull(mc.world).getBlockState(pos);
        return !PlayerInteractionHelper.isAir(state) && state.getBlock() != Blocks.WATER && state.getBlock() != Blocks.LAVA && state.getBlock() != Blocks.BEDROCK && state.getBlock() != Blocks.BARRIER;
    }
}
