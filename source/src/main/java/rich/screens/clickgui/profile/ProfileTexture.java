package rich.screens.clickgui.profile;

import com.mojang.blaze3d.systems.CommandEncoder;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.textures.FilterMode;
import com.mojang.blaze3d.textures.GpuTexture;
import com.mojang.blaze3d.textures.TextureFormat;
import net.minecraft.client.texture.AbstractTexture;
import net.minecraft.client.texture.NativeImage;

import java.util.function.Supplier;

/**
 * Текстура профиля с полной цепочкой мипмапов.
 * <p>
 * Обычная {@code NativeImageBackedTexture} создаёт текстуру с одним мип-уровнем,
 * поэтому при уменьшении 256 -> 46 пикселей GPU берёт один билинейный сэмпл
 * из полного разрешения и получает рябь/кашу. Здесь мы заранее строим мип-цепочку
 * (бокс-фильтр на CPU) и просим у сэмплера трилинейную фильтрацию — тогда
 * уменьшение идёт корректно, как у ванильных атласов.
 */
final class ProfileTexture extends AbstractTexture {

    ProfileTexture(Supplier<String> label, NativeImage image) {
        int width = image.getWidth();
        int height = image.getHeight();
        // Количество мипов считаем по МЕНЬШЕЙ стороне: GpuTexture.getHeight(level)
        // это height >> level без clamp, поэтому на уровне от большей стороны
        // высота становится 0 и writeToTexture падает.
        int mipLevels = Math.max(1, 32 - Integer.numberOfLeadingZeros(Math.min(width, height)));

        GpuDevice device = RenderSystem.getDevice();
        this.glTexture = device.createTexture(
                label,
                GpuTexture.USAGE_COPY_DST | GpuTexture.USAGE_TEXTURE_BINDING,
                TextureFormat.RGBA8,
                width, height, 1, mipLevels);
        this.glTextureView = device.createTextureView(this.glTexture);
        // последний true включает мипмап-фильтрацию (LINEAR_MIPMAP_LINEAR)
        this.sampler = RenderSystem.getSamplerCache().get(FilterMode.LINEAR, true);

        CommandEncoder encoder = device.createCommandEncoder();
        encoder.writeToTexture(this.glTexture, image, 0, 0, 0, 0, width, height, 0, 0);

        NativeImage current = image;
        for (int level = 1; level < mipLevels; level++) {
            int levelWidth = Math.max(1, width >> level);
            int levelHeight = Math.max(1, height >> level);
            NativeImage smaller = downsample(current, levelWidth, levelHeight);
            encoder.writeToTexture(this.glTexture, smaller, level, 0, 0, 0, levelWidth, levelHeight, 0, 0);
            if (current != image) current.close();
            current = smaller;
        }
        if (current != image) current.close();
    }

    /**
     * Бокс-фильтр по исходному изображению, усреднение с учётом альфы,
     * чтобы на прозрачных краях не появлялась тёмная кайма.
     */
    private static NativeImage downsample(NativeImage source, int width, int height) {
        NativeImage target = new NativeImage(width, height, false);
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();

        for (int y = 0; y < height; y++) {
            int y0 = Math.min(sourceHeight - 1, y * sourceHeight / height);
            int y1 = Math.min(sourceHeight, Math.max(y0 + 1, (y + 1) * sourceHeight / height));
            for (int x = 0; x < width; x++) {
                int x0 = Math.min(sourceWidth - 1, x * sourceWidth / width);
                int x1 = Math.min(sourceWidth, Math.max(x0 + 1, (x + 1) * sourceWidth / width));

                int alphaSum = 0;
                int redSum = 0;
                int greenSum = 0;
                int blueSum = 0;
                int count = 0;

                for (int srcY = y0; srcY < y1; srcY++) {
                    for (int srcX = x0; srcX < x1; srcX++) {
                        int argb = source.getColorArgb(srcX, srcY);
                        int alpha = (argb >>> 24) & 0xFF;
                        alphaSum += alpha;
                        redSum += ((argb >> 16) & 0xFF) * alpha;
                        greenSum += ((argb >> 8) & 0xFF) * alpha;
                        blueSum += (argb & 0xFF) * alpha;
                        count++;
                    }
                }

                int alpha = count == 0 ? 0 : alphaSum / count;
                int red = alphaSum == 0 ? 0 : redSum / alphaSum;
                int green = alphaSum == 0 ? 0 : greenSum / alphaSum;
                int blue = alphaSum == 0 ? 0 : blueSum / alphaSum;
                target.setColorArgb(x, y, (alpha << 24) | (red << 16) | (green << 8) | blue);
            }
        }
        return target;
    }
}
