package fun.aegis.utils.display.gif;

import fun.aegis.utils.display.shape.implement.Image;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.math.frame.FrameRateCounter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;

public class GifRender {
    private int currentFrame = 0;
    private float frameTime = 0;
    private final float frameDuration = 0.0015f;
    private final String[] frames;
    private final Image image;

    public GifRender(String path, int frameCount) {
        this.frames = new String[frameCount];
        for (int i = 0; i < frameCount; i++) {
            frames[i] = String.format("minecraft:gif/backgrounds/mainmenutype1/%05d.png", i + 1);
        }
        this.image = new Image();
    }

    public void render(MatrixStack matrix, float x, float y, float width, float height) {
        if (!MinecraftClient.getInstance().isWindowFocused()) {
            return;
        }
        frameTime += FrameRateCounter.INSTANCE.getFps() > 0 ? 1.0f / FrameRateCounter.INSTANCE.getFps() : 0.006f;
        if (frameTime >= frameDuration) {
            currentFrame = (currentFrame + 1) % frames.length;
            frameTime = 0;
        }
        image.setTexture(frames[currentFrame]).render(ShapeProperties.create(matrix, x, y, width, height).color(-1).build());
    }
}
