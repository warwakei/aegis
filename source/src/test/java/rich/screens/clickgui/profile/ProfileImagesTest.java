package rich.screens.clickgui.profile;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.Assert.*;

public class ProfileImagesTest {
    @Rule public final TemporaryFolder temp = new TemporaryFolder();

    @Test public void portraitIsCenterCroppedWithoutStretching() throws IOException {
        Path source = temp.getRoot().toPath().resolve("portrait.png");
        Path saved = temp.getRoot().toPath().resolve("appearance/avatar.png");
        var image = new BufferedImage(300, 100, BufferedImage.TYPE_INT_ARGB);
        var graphics = image.createGraphics();
        try {
            graphics.setColor(Color.RED);
            graphics.fillRect(0, 0, 300, 100);
            graphics.setColor(Color.BLUE);
            graphics.fillRect(100, 0, 100, 100);
        } finally {
            graphics.dispose();
        }
        ImageIO.write(image, "PNG", source.toFile());
        ProfileImages.saveImage(source, saved, ProfileImages.Kind.AVATAR);
        var avatar = ImageIO.read(saved.toFile());
        assertEquals(256, avatar.getWidth());
        assertEquals(256, avatar.getHeight());
        assertEquals(Color.BLUE.getRGB(), avatar.getRGB(20, 128));
        assertEquals(Color.BLUE.getRGB(), avatar.getRGB(235, 128));
    }

    @Test public void jpegBannerIsStoredAsPersistentPng() throws IOException {
        Path source = temp.getRoot().toPath().resolve("banner.jpg");
        Path saved = temp.getRoot().toPath().resolve("appearance/background.png");
        ImageIO.write(new BufferedImage(1280, 720, BufferedImage.TYPE_INT_RGB), "JPEG", source.toFile());
        ProfileImages.saveImage(source, saved, ProfileImages.Kind.BACKGROUND);
        Files.delete(source);
        var banner = ImageIO.read(saved.toFile());
        assertEquals(560, banner.getWidth());
        assertEquals(240, banner.getHeight());
    }

    @Test public void invalidSelectionDoesNotOverwriteExistingImage() throws IOException {
        Path source = temp.getRoot().toPath().resolve("invalid.png");
        Path saved = temp.getRoot().toPath().resolve("avatar.png");
        ImageIO.write(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), "PNG", saved.toFile());
        byte[] original = Files.readAllBytes(saved);
        Files.writeString(source, "Not an image");
        assertThrows(IOException.class, () -> ProfileImages.saveImage(source, saved, ProfileImages.Kind.AVATAR));
        assertArrayEquals(original, Files.readAllBytes(saved));
    }
}
