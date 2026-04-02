package rich.util.math;

import lombok.Getter;
import lombok.experimental.UtilityClass;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.ColorHelper;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix3x2fStack;
import org.joml.Vector3d;
import rich.IMinecraft;

import java.util.concurrent.ThreadLocalRandom;

import static net.minecraft.util.math.MathHelper.lerp;

@UtilityClass
public class MathUtils implements IMinecraft {
    public double PI2 = Math.PI * 2;

    @Getter
    private static float contextAlpha = 1.0f;

    public boolean isHovered(double mouseX, double mouseY, double x, double y, double width, double height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public static float clamp(float num, float min, float max) {
        return num < min ? min : Math.min(num, max);
    }

    public double computeGcd() {
        return (Math.pow(mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0)) * 1.2;
    }

    public int getRandom(int min, int max) {
        return (int) getRandom((float) min, (float) max + 1);
    }

    public float getRandom(float min, float max) {
        return (float) getRandom(min, (double) max);
    }

    public double getRandom(double min, double max) {
        if (min == max) {
            return min;
        } else {
            if (min > max) {
                double d = min;
                min = max;
                max = d;
            }
            return ThreadLocalRandom.current().nextDouble(min, max);
        }
    }

    public void scale(Matrix3x2fStack stack, float x, float y, float scaleX, float scaleY, Runnable data) {
        float sumScale = scaleX * scaleY;
        if (sumScale != 1 && sumScale > 0) {
            float prevAlpha = contextAlpha;
            contextAlpha = sumScale;

            stack.pushMatrix();
            stack.translate(x, y);
            stack.scale(scaleX, scaleY);
            stack.translate(-x, -y);
            data.run();
            stack.popMatrix();

            contextAlpha = prevAlpha;
        } else if (sumScale >= 1) {
            data.run();
        }
    }

    public float textScrolling(float textWidth) {
        int speed = (int) (textWidth * 75);
        return (float) MathHelper.clamp((System.currentTimeMillis() % speed * Math.PI / speed), 0, 1) * textWidth;
    }

    public double round(double num, double increment) {
        double rounded = Math.round(num / increment) * increment;
        return Math.round(rounded * 100.0) / 100.0;
    }

    public int floorNearestMulN(int x, int n) {
        return n * (int) Math.floor((double) x / (double) n);
    }

    public int getRed(int hex) {
        return hex >> 16 & 255;
    }

    public int getGreen(int hex) {
        return hex >> 8 & 255;
    }

    public int getBlue(int hex) {
        return hex & 255;
    }

    public int getAlpha(int hex) {
        return hex >> 24 & 255;
    }

    public int applyOpacity(int color, float opacity) {
        return ColorHelper.getArgb((int) (getAlpha(color) * opacity / 255), getRed(color), getGreen(color), getBlue(color));
    }

    public int applyContextAlpha(int color) {
        int a = (int) (getAlpha(color) * contextAlpha);
        return ColorHelper.getArgb(a, getRed(color), getGreen(color), getBlue(color));
    }

    public Vec3d cosSin(int i, int size, double width) {
        int index = Math.min(i, size);
        float cos = (float) (Math.cos(index * MathUtils.PI2 / size) * width);
        float sin = (float) (-Math.sin(index * MathUtils.PI2 / size) * width);
        return new Vec3d(cos, 0, sin);
    }

    public double absSinAnimation(double input) {
        return Math.abs(1 + Math.sin(input)) / 2;
    }

    public Vector3d interpolate(Vector3d prevPos, Vector3d pos) {
        return new Vector3d(interpolate(prevPos.x, pos.x), interpolate(prevPos.y, pos.y), interpolate(prevPos.z, pos.z));
    }

    public static float interpolate(float prev, float to, float value) {
        return prev + (to - prev) * value;
    }

    public Vec3d interpolate(Vec3d prevPos, Vec3d pos) {
        return new Vec3d(interpolate(prevPos.x, pos.x), interpolate(prevPos.y, pos.y), interpolate(prevPos.z, pos.z));
    }

    public Vec3d interpolate(Entity entity) {
        if (entity == null) return Vec3d.ZERO;
        return new Vec3d(
                interpolate(entity.lastX, entity.getX()),
                interpolate(entity.lastY, entity.getY()),
                interpolate(entity.lastZ, entity.getZ())
        );
    }

    public float interpolate(float prev, float orig) {
        return lerp(tickCounter.getTickProgress(false), prev, orig);
    }

    public double interpolate(double prev, double orig) {
        return lerp(tickCounter.getTickProgress(false), prev, orig);
    }

    public float interpolateSmooth(double smooth, float prev, float orig) {
        return (float) lerp(tickCounter.getFixedDeltaTicks() / smooth, prev, orig);
    }
}