package fun.aegis.commands.defaults;

import fun.aegis.Aegis;
import fun.aegis.utils.client.managers.api.command.Command;
import fun.aegis.utils.client.managers.api.command.argument.IArgConsumer;
import fun.aegis.utils.client.managers.api.command.exception.CommandException;
import fun.aegis.utils.client.managers.api.command.exception.CommandNotEnoughArgumentsException;
import fun.aegis.utils.client.managers.api.command.helpers.TabCompleteHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraft.block.BlockState;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

public class ClipCommand extends Command {

    public ClipCommand() {
        super("clip");
    }

    @Override
    public void execute(String label, IArgConsumer args) throws CommandException {
        if (!args.hasAny()) {
            throw new CommandNotEnoughArgumentsException(1);
        }

        String action = args.getString().toLowerCase(Locale.US);
        
        switch (action) {
            case "vclip":
                handleVClip(args);
                break;
            case "hclip":
                handleHClip(args);
                break;
            case "up":
                handleUp(args);
                break;
            case "down":
                handleDown(args);
                break;
            case "forward":
                handleForward(args);
                break;
            case "back":
                handleBack(args);
                break;
            case "help":
                handleHelp();
                break;
            default:
                logDirect(Formatting.RED + "Неизвестное действие: " + action);
                handleHelp();
                break;
        }
    }

    private void handleVClip(IArgConsumer args) throws CommandException {
        args.requireMin(1);
        double distance = Double.parseDouble(args.getString());
        PlayerEntity player = mc.player;
        
        if (player != null) {
            player.setPosition(player.getX(), player.getY() + distance + 0.1, player.getZ());
            logDirect(Formatting.GREEN + "Вертикальный вклип на " + distance + " блоков");
        }
    }

    private void handleHClip(IArgConsumer args) throws CommandException {
        args.requireMin(1);
        double distance = Double.parseDouble(args.getString());
        PlayerEntity player = mc.player;
        
        if (player != null) {
            double yaw = Math.toRadians(player.getYaw());
            player.setPosition(player.getX() - Math.sin(yaw) * distance, player.getY() + 0.1, player.getZ() + Math.cos(yaw) * distance);
            logDirect(Formatting.GREEN + "Горизонтальный вклип на " + distance + " блоков");
        }
    }

    private void handleUp(IArgConsumer args) throws CommandException {
        args.requireMin(1);
        double distance = Double.parseDouble(args.getString());
        PlayerEntity player = mc.player;
        
        if (player != null) {
            Vec3d safePos = findSafePositionUp(player.getPos(), distance);
            if (safePos != null) {
                player.setPosition(safePos.x, safePos.y, safePos.z);
                logDirect(Formatting.GREEN + "Вклип вверх на " + distance + " блоков (найдено безопасное место)");
            } else {
                player.setPosition(player.getX(), player.getY() + distance + 0.1, player.getZ());
                logDirect(Formatting.YELLOW + "Вклип вверх на " + distance + " блоков (безопасное место не найдено, использована стандартная позиция)");
            }
        }
    }

    private void handleDown(IArgConsumer args) throws CommandException {
        args.requireMin(1);
        double distance = Double.parseDouble(args.getString());
        PlayerEntity player = mc.player;
        
        if (player != null) {
            Vec3d safePos = findSafePositionDown(player.getPos(), distance);
            if (safePos != null) {
                player.setPosition(safePos.x, safePos.y, safePos.z);
                logDirect(Formatting.GREEN + "Вклип вниз на " + distance + " блоков (найдено безопасное место)");
            } else {
                player.setPosition(player.getX(), player.getY() - distance + 0.1, player.getZ());
                logDirect(Formatting.YELLOW + "Вклип вниз на " + distance + " блоков (безопасное место не найдено, использована стандартная позиция)");
            }
        }
    }

    private void handleForward(IArgConsumer args) throws CommandException {
        args.requireMin(1);
        double distance = Double.parseDouble(args.getString());
        PlayerEntity player = mc.player;
        
        if (player != null) {
            double yaw = Math.toRadians(player.getYaw());
            player.setPosition(player.getX() - Math.sin(yaw) * distance, player.getY() + 0.1, player.getZ() + Math.cos(yaw) * distance);
            logDirect(Formatting.GREEN + "Вклип вперед на " + distance + " блоков");
        }
    }

    private void handleBack(IArgConsumer args) throws CommandException {
        args.requireMin(1);
        double distance = Double.parseDouble(args.getString());
        PlayerEntity player = mc.player;
        
        if (player != null) {
            double yaw = Math.toRadians(player.getYaw());
            player.setPosition(player.getX() + Math.sin(yaw) * distance, player.getY() + 0.1, player.getZ() - Math.cos(yaw) * distance);
            logDirect(Formatting.GREEN + "Вклип назад на " + distance + " блоков");
        }
    }

    private void handleHelp() {
        logDirect(Formatting.GOLD + "Использование: " + Formatting.WHITE + ".clip <vclip/hclip/up/down/forward/back/help> [расстояние]");
        logDirect(Formatting.GOLD + "Примеры:");
        logDirect(Formatting.YELLOW + ".clip vclip 10 " + Formatting.GRAY + "- вклип вверх на 10 блоков");
        logDirect(Formatting.YELLOW + ".clip hclip 5 " + Formatting.GRAY + "- горизонтальный вклип на 5 блоков");
        logDirect(Formatting.YELLOW + ".clip up 3 " + Formatting.GRAY + "- вклип вверх на 3 блока");
    }

    @Override
    public Stream<String> tabComplete(String label, IArgConsumer args) throws CommandException {
        if (args.hasExactlyOne()) {
            return new TabCompleteHelper()
                    .sortAlphabetically()
                    .prepend("vclip", "hclip", "up", "down", "forward", "back", "help")
                    .filterPrefix(args.getString())
                    .stream();
        }
        return Stream.empty();
    }

    private Vec3d findSafePositionUp(Vec3d startPos, double maxDistance) {
        World world = mc.player.getWorld();
        Box playerBox = mc.player.getBoundingBox();
        
        for (double y = startPos.y + 1.0; y <= startPos.y + maxDistance; y += 0.5) {
            Vec3d testPos = new Vec3d(startPos.x, y, startPos.z);
            Box testBox = playerBox.offset(testPos.x - startPos.x, y - startPos.y, testPos.z - startPos.z);
            
            if (isSafePosition(world, testBox, testPos)) {
                return testPos;
            }
        }
        return null;
    }
    
    private Vec3d findSafePositionDown(Vec3d startPos, double maxDistance) {
        World world = mc.player.getWorld();
        Box playerBox = mc.player.getBoundingBox();
        
        for (double y = startPos.y - 1.0; y >= startPos.y - maxDistance; y -= 0.5) {
            Vec3d testPos = new Vec3d(startPos.x, y, startPos.z);
            Box testBox = playerBox.offset(testPos.x - startPos.x, y - startPos.y, testPos.z - startPos.z);
            
            if (isSafePosition(world, testBox, testPos)) {
                return testPos;
            }
        }
        return null;
    }
    
    private boolean isSafePosition(World world, Box box, Vec3d pos) {
        if (!world.isSpaceEmpty(mc.player, box)) {
            return false;
        }
        
        BlockPos blockPos = BlockPos.ofFloored(pos);
        BlockState state = world.getBlockState(blockPos);
        
        if (state.isAir() || !state.getFluidState().isEmpty()) {
            return true;
        }
        
        return false;
    }

    @Override
    public String getShortDesc() {
        return "Позволяет перемещаться (вклипываться) на расстояние.";
    }

    @Override
    public List<String> getLongDesc() {
        return Arrays.asList(
                "Эта команда позволяет мгновенно перемещаться на указанное расстояние в различных направлениях.",
                "",
                "Использование:",
                "> clip vclip <distance> - Вертикальный вклип",
                "> clip hclip <distance> - Горизонтальный вклип",
                "> clip up <distance> - Вклип вверх",
                "> clip down <distance> - Вклип вниз",
                "> clip forward <distance> - Вклип вперед",
                "> clip back <distance> - Вклип назад",
                "> clip help - Показать справку"
        );
    }
}
