package rich.screens.clickgui.profile;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;
import org.slf4j.LoggerFactory;

import javax.imageio.ImageIO;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;

public final class ProfileImages {
    public enum Kind {
        // размеры под 2x от реального размера на экране (1 GUI-юнит = 2 device-пикселя):
        // аватар 23x23 -> 46x46 px, фон карточки 70x30 -> 140x60 px
        AVATAR(96, 96), BACKGROUND(280, 120);

        final int width;
        final int height;
        final String fileName;
        final Identifier textureId;

        Kind(int width, int height) {
            this.width = width;
            this.height = height;
            fileName = name().toLowerCase(Locale.ROOT) + ".png";
            textureId = Identifier.of("rich", "profile/" + fileName);
        }
    }

    private static final EnumMap<Kind, Identifier> TEXTURES = new EnumMap<>(Kind.class);
    private static final EnumSet<Kind> LOADED = EnumSet.noneOf(Kind.class);

    private ProfileImages() {}

    private static Path path(Kind kind) {
        return FabricLoader.getInstance().getGameDir().resolve("AegisNeo/appearance").resolve(kind.fileName);
    }

    public static void select(Kind kind, Path source) throws IOException {
        saveImage(source, path(kind), kind);
        invalidate(kind);
    }

    public static void reset(Kind kind) throws IOException {
        Files.deleteIfExists(path(kind));
        invalidate(kind);
    }

    private static void invalidate(Kind kind) {
        Identifier existing = TEXTURES.remove(kind);
        if (existing != null) MinecraftClient.getInstance().getTextureManager().destroyTexture(existing);
        LOADED.remove(kind);
    }

    public static Identifier getTexture(Kind kind) {
        if (LOADED.add(kind) && Files.isRegularFile(path(kind))) {
            NativeImage image = null;
            ProfileTexture texture = null;
            try (var input = Files.newInputStream(path(kind))) {
                image = NativeImage.read(input);
                texture = new ProfileTexture(() -> "Aegis profile " + kind.fileName, image);
                image = null; // The texture now owns the decoded image.
                MinecraftClient.getInstance().getTextureManager().registerTexture(kind.textureId, texture);
                TEXTURES.put(kind, kind.textureId);
            } catch (IOException | RuntimeException e) {
                if (texture != null) texture.close();
                if (image != null) image.close();
                LoggerFactory.getLogger(ProfileImages.class).warn("Cannot load profile image {}", kind.fileName, e);
            }
        }
        return TEXTURES.get(kind);
    }

    /**
     * Обрезает картинку под пропорции слота и уменьшает её до целевого размера.
     * Мелкие исходники не растягиваются: апскейл запекал мыло в файл, а потом
     * картинка ещё раз сжималась при отрисовке.
     */
    private static BufferedImage normalize(BufferedImage source, Kind kind) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        double targetAspect = (double) kind.width / kind.height;
        double sourceAspect = (double) sourceWidth / sourceHeight;

        int cropWidth;
        int cropHeight;
        if (sourceAspect > targetAspect) {
            cropHeight = sourceHeight;
            cropWidth = Math.max(1, (int) Math.round(sourceHeight * targetAspect));
        } else {
            cropWidth = sourceWidth;
            cropHeight = Math.max(1, (int) Math.round(sourceWidth / targetAspect));
        }
        int cropX = (sourceWidth - cropWidth) / 2;
        int cropY = (sourceHeight - cropHeight) / 2;

        double scale = Math.min(1.0,
                Math.min((double) kind.width / cropWidth, (double) kind.height / cropHeight));
        int width = Math.max(1, (int) Math.round(cropWidth * scale));
        int height = Math.max(1, (int) Math.round(cropHeight * scale));

        BufferedImage cropped = new BufferedImage(cropWidth, cropHeight, BufferedImage.TYPE_INT_ARGB);
        var graphics = cropped.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
            graphics.drawImage(source, -cropX, -cropY, null);
        } finally {
            graphics.dispose();
        }

        return downscale(cropped, width, height);
    }

    /** Пошаговое уменьшение вдвое + финальный шаг: так не теряется детализация. */
    private static BufferedImage downscale(BufferedImage source, int width, int height) {
        BufferedImage current = source;
        while (current.getWidth() / 2 >= width && current.getHeight() / 2 >= height) {
            current = drawScaled(current, current.getWidth() / 2, current.getHeight() / 2);
        }
        if (current.getWidth() == width && current.getHeight() == height) {
            return current;
        }
        return drawScaled(current, width, height);
    }

    private static BufferedImage drawScaled(BufferedImage source, int width, int height) {
        BufferedImage result = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        var graphics = result.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.drawImage(source, 0, 0, width, height, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    static void saveImage(Path source, Path destination, Kind kind) throws IOException {
        if (!Files.isRegularFile(source) || Files.size(source) > 24L * 1024 * 1024) {
            throw new IOException("Нужен файл PNG или JPEG размером до 24 МБ");
        }

        BufferedImage original;
        try (var input = ImageIO.createImageInputStream(source.toFile())) {
            if (input == null) throw new IOException("Не удалось открыть файл");
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new IOException("Выберите изображение PNG или JPEG");
            var reader = readers.next();
            try {
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                if (!format.equals("png") && !format.equals("jpeg")) throw new IOException("Выберите PNG или JPEG");
                reader.setInput(input, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > 8192 || height > 8192 || (long) width * height > 24_000_000L) {
                    throw new IOException("Изображение слишком большое (до 24 мегапикселей)");
                }
                original = reader.read(0);
            } finally {
                reader.dispose();
            }
        }

        BufferedImage normalized = normalize(original, kind);

        Path parent = destination.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temp = Files.createTempFile(parent, "profile-", ".tmp");
        try {
            if (!ImageIO.write(normalized, "png", temp.toFile())) throw new IOException("Не удалось сохранить PNG");
            try {
                Files.move(temp, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temp, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }
}
