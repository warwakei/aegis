package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import rich.events.api.EventHandler;
import rich.events.impl.WorldRenderEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.ColorSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.Instance;
import rich.util.ColorUtil;
import rich.util.render.Render3D;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class BlockOverlay extends ModuleStructure {
    public static BlockOverlay getInstance() {
        return Instance.get(BlockOverlay.class);
    }

    ColorSetting color = new ColorSetting("Цвет", "Цвет подсветки блока")
            .value(ColorUtil.getColor(109, 252, 255, 230));
    
    BooleanSetting magneticLines = new BooleanSetting("Магнитные линии", "Красивый эффект с линиями к курсору")
            .setValue(true);
    
    SliderSettings lineCount = new SliderSettings("Количество линий", "Сколько линий рисовать")
            .range(10, 100)
            .setValue(40)
            .visible(() -> magneticLines.isValue());
    
    SliderSettings lineSpeed = new SliderSettings("Скорость анимации", "Скорость движения линий")
            .range(0.1f, 5.0f)
            .setValue(1.5f)
            .visible(() -> magneticLines.isValue());

    private final List<MagneticLine> lines = new ArrayList<>();
    private final Random random = new Random();
    private long lastUpdateTime = 0;

    public BlockOverlay() {
        super("BlockOverlay", "Block Overlay", ModuleCategory.RENDER);
        settings(color, magneticLines, lineCount, lineSpeed);
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (mc.crosshairTarget instanceof BlockHitResult result && result.getType().equals(HitResult.Type.BLOCK)) {
            BlockPos pos = result.getBlockPos();
            
            // светим блок
            Render3D.drawShapeAlternative(pos, mc.world.getBlockState(pos).getOutlineShape(mc.world, pos), 
                    color.getColor(), 1.5f, true, true);
            
            // магнит линии
            if (magneticLines.isValue()) {
                renderMagneticLines(e.getStack(), pos, result.getPos());
            }
        } else {
            // не смотрим чистим линии
            lines.clear();
        }
    }
    
    private void renderMagneticLines(MatrixStack matrices, BlockPos blockPos, Vec3d hitPos) {
        long currentTime = System.currentTimeMillis();
        
        // апдейт линий раз в 50мс
        if (currentTime - lastUpdateTime > 50) {
            updateLines(blockPos, hitPos);
            lastUpdateTime = currentTime;
        }
        
        // рисуем линии
        for (MagneticLine line : lines) {
            line.render(matrices);
        }
    }
    
    private void updateLines(BlockPos blockPos, Vec3d hitPos) {
        int targetCount = lineCount.getInt();
        
        // докидываем линии
        while (lines.size() < targetCount) {
            lines.add(createRandomLine(blockPos));
        }
        
        // режем лишние
        while (lines.size() > targetCount) {
            lines.remove(lines.size() - 1);
        }
        
        // двигаем линии
        for (MagneticLine line : lines) {
            line.update(hitPos, lineSpeed.getValue());
        }
    }
    
    private MagneticLine createRandomLine(BlockPos blockPos) {
        // рандом точка в блоке
        double x = blockPos.getX() + random.nextDouble();
        double y = blockPos.getY() + random.nextDouble();
        double z = blockPos.getZ() + random.nextDouble();
        
        return new MagneticLine(new Vec3d(x, y, z), color.getColor());
    }
    
    private static class MagneticLine {
        private Vec3d start;
        private Vec3d end;
        private final int color;
        private float alpha = 1.0f;
        private final Random random = new Random();
        
        public MagneticLine(Vec3d start, int color) {
            this.start = start;
            this.end = start;
            this.color = color;
        }
        
        public void update(Vec3d target, float speed) {
            // тянем к курсору
            Vec3d direction = target.subtract(end).normalize().multiply(speed * 0.1);
            end = end.add(direction);
            
            // чуть рандома для живости
            double randomX = (random.nextDouble() - 0.5) * 0.02;
            double randomY = (random.nextDouble() - 0.5) * 0.02;
            double randomZ = (random.nextDouble() - 0.5) * 0.02;
            end = end.add(randomX, randomY, randomZ);
            
            // мигаем прозрачностью
            alpha = (float) (0.3 + 0.7 * Math.sin(System.currentTimeMillis() * 0.005 + start.x + start.z));
        }
        
        public void render(MatrixStack matrices) {
            // рисуем светяшку
            int finalColor = ColorUtil.setAlpha(color, (int) (alpha * 255));
            
            // главная линия
            Render3D.drawLine(start, end, finalColor, 2.0f, true);
            
            // свечение вокруг
            int glowColor = ColorUtil.setAlpha(color, (int) (alpha * 100));
            Render3D.drawLine(start, end, glowColor, 4.0f, true);
            
            // яркое ядрышко
            int coreColor = ColorUtil.setAlpha(Color.WHITE.getRGB(), (int) (alpha * 200));
            Render3D.drawLine(start, end, coreColor, 1.0f, true);
        }
    }
}
