package rich.screens.clickgui.impl.background.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import rich.screens.clickgui.profile.ProfileImages;
import rich.util.config.impl.account.AccountConfig;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;
import rich.util.render.gif.GifRender;

import java.awt.*;

public class AvatarRenderer {
    public static final float X = 12.5f, Y = 12.5f, WIDTH = 70, HEIGHT = 30;

    private static final MinecraftClient mc = MinecraftClient.getInstance();

    public void render(DrawContext context, float bgX, float bgY, float alphaMultiplier) {
        int alpha = (int) (255 * alphaMultiplier);
        int alphaFon = (int) (105 * alphaMultiplier);
        int alphaText = (int) (200 * alphaMultiplier);

        AccountConfig accountConfig = AccountConfig.getInstance();
        String username = accountConfig != null ? accountConfig.getActiveAccountName() : mc.getSession().getUsername();
        if (username == null || username.isEmpty()) {
            username = mc.getSession().getUsername();
        }

        // фон с гифкой тут
        context.getMatrices().pushMatrix();
        var background = ProfileImages.getTexture(ProfileImages.Kind.BACKGROUND);
        if (background != null) Render2D.texture(background, bgX + X, bgY + Y, WIDTH, HEIGHT, 1f, 7, applyAlpha(-1, alpha));
        else GifRender.drawBackground(bgX + X, bgY + Y, WIDTH, HEIGHT, 7, applyAlpha(-1, alpha));
        Render2D.rect(bgX + 15f, bgY + 15f, 25, 25, new Color(22, 24, 32, alpha).getRGB(), 15);
        var avatar = ProfileImages.getTexture(ProfileImages.Kind.AVATAR);
        if (avatar != null) Render2D.texture(avatar, bgX + 16f, bgY + 16f, 23, 23, 1f, 15, applyAlpha(-1, alpha));
        else GifRender.drawAvatar(bgX + 16f, bgY + 16f, 23, 23, 15, applyAlpha(-1, alpha));
        Render2D.rect(bgX + 33, bgY + 33, 5, 5, new Color(0, 255, 0, alpha).getRGB(), 10);
        context.getMatrices().popMatrix();

        Render2D.rect(bgX + 12.5f, bgY + 12.5f, 70, 30, new Color(8, 10, 16, alphaFon).getRGB(), 7);

        // ник без ножниц а то мерцает
        float textX = bgX + 44;
        float textY = bgY + 22;
        float maxTextWidth = 35f;

        // длинный ник режем тут
        String displayName = username;
        float nameWidth = Fonts.BOLD.getWidth(displayName, 6);
        if (nameWidth > maxTextWidth) {
            while (Fonts.BOLD.getWidth(displayName + "…", 6) > maxTextWidth && displayName.length() > 1) {
                displayName = displayName.substring(0, displayName.length() - 1);
            }
            displayName += "…";
        }

        Fonts.BOLD.draw(displayName, textX, textY, 6, new Color(255, 255, 255, alphaText).getRGB());
        Fonts.BOLD.draw("+", bgX + 75f, bgY + 15f, 5, applyAlpha(-1, alphaText));
    }

    private int applyAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }
}
