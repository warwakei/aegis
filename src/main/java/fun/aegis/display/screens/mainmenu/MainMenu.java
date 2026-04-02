package fun.aegis.display.screens.mainmenu;

import antidaunleak.api.UserProfile;
import fun.aegis.Aegis;
import fun.aegis.common.animation.Direction;
import fun.aegis.display.screens.mainmenu.altscreen.AltScreen;
import fun.aegis.display.screens.mainmenu.SnowSystem;
import fun.aegis.utils.client.text.TextAnimation;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.geometry.Render2D;
import fun.aegis.utils.display.gif.GifRender;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.common.animation.implement.Decelerate;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.awt.Color;

public class MainMenu extends Screen implements QuickImports {
    public static MainMenu INSTANCE = new MainMenu();
    public int x, y, width, height;
    private final TextAnimation textAnimation = new TextAnimation();
    private boolean altVisible = false;
    private final GifRender gifRender = new GifRender("minecraft:gif/backgrounds/mainmenutype1", 1);
    private final Decelerate altFadeAnimation = new Decelerate();
    private final Decelerate mainFadeAnimation = new Decelerate();
    private AltScreen altScreen;
    private long lastToggleTime = 0;
    private static final long TOGGLE_DELAY = 500;
    private static final UserProfile userProfile = UserProfile.getInstance();
    private final SnowSystem snowSystem = new SnowSystem(150, 0.02f);
    private static final String CHANGELOG = "Aegis Early Beta 0.5.0 Changelog: \n\n Very good grim speeds \n\n Added new rotations: HvH V2 & HvH V2X \n\n Fixed some bugs \n\n Added 2 new modes for Velocity: V2 & V2X & V2R \n\n\n\n\n\n\n\n\n Our discord: https://discord.gg/JpsKPfVTyf \n\n You running: Aegis Beta 0.5.0 (Free)";
    private java.util.Map<String, java.util.Map<String, Object>> clickableLinks = new java.util.HashMap<>();

    public MainMenu() {
        super(Text.of("MainMenu"));
        altFadeAnimation.setMs(250).setValue(1.0);
        mainFadeAnimation.setMs(250).setValue(1.0);
        mainFadeAnimation.setDirection(Direction.FORWARDS);
        altFadeAnimation.setDirection(Direction.BACKWARDS);
    }

    @Override
    public void tick() {
        super.tick();
        textAnimation.updateText();
        if (altScreen != null)
            altScreen.tick();
        snowSystem.update(width, height, 0.016f);

        if (altFadeAnimation.isFinished(Direction.BACKWARDS)) {
            altVisible = false;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        mc.options.getGuiScale().setValue(2);
        x = window.getScaledWidth();
        y = window.getScaledHeight();
        width = window.getScaledWidth() + 2;
        height = window.getScaledHeight() + 2;
        float cy = height / 2.0f, sy = cy - 25, sx = width / 2 - 50, bs = 21;

        gifRender.render(context.getMatrices(), 0, 0, width, height);
        image.setTexture("textures/mainmenu/backmenu.png")
                .render(ShapeProperties.create(context.getMatrices(), 0, 0, width, height).color(-1).build());

        snowSystem.render(context, 0.8f);

        Double mainAlpha = mainFadeAnimation.getOutput();
        int mainAlphaInt = (int) (255 * mainAlpha);

        if (mainAlpha > 0.01f) {
            if (!CHANGELOG.isEmpty()) {
                float changelogX = 8;
                float changelogY = 8;
                drawChangelogWithLinks(context, CHANGELOG, changelogX, changelogY, mainAlphaInt);
            }

            drawButton(context, sx, sy, 102, 18.5f, "Одиночная игра", mainAlphaInt);
            drawButton(context, sx, sy + bs, 102, 18.5f, "Сетевая игра", mainAlphaInt);
            drawButton(context, sx, sy + bs * 2, 102, 18.5f, "Аккаунты", mainAlphaInt);
            drawButton(context, sx, sy + bs * 3, 102, 18.5f, "", mainAlphaInt);
            drawButton(context, sx, sy + bs * 4, 50, 18.5f, "", mainAlphaInt);
            drawButton(context, sx + 52, sy + bs * 4, 50, 18.5f, "", mainAlphaInt);

            Fonts.getSize(21, Fonts.Type.ICONSTYPENEW).drawCenteredString(context.getMatrices(), "i", width / 2 - 24,
                    sy + bs + 49, applyAlpha(ColorAssist.getText(0.35f), mainAlphaInt));
            Fonts.getSize(22, Fonts.Type.ICONSTYPENEW).drawCenteredString(context.getMatrices(), "s", width / 2 + 27,
                    sy + bs + 49, applyAlpha(ColorAssist.getText(0.35f), mainAlphaInt));

            Fonts.getSize(68, Fonts.Type.ICONS).drawCenteredString(context.getMatrices(), "", width / 2, sy - 80,
                    applyAlpha(new Color(200, 200, 200).getRGB(), mainAlphaInt));

            Fonts.getSize(18, Fonts.Type.DEFAULT).drawCenteredString(context.getMatrices(),
                    "Aegis Client, вы сделали правильный выбор.", width / 2, sy - 40,
                    applyAlpha(new Color(200, 200, 200).getRGB(), mainAlphaInt));
            Fonts.getSize(12, Fonts.Type.DEFAULT).drawCenteredString(context.getMatrices(),
                    textAnimation.getCurrentText(), width / 2, sy - 25,
                    applyAlpha(new Color(200, 200, 200).getRGB(), mainAlphaInt));
            Fonts.getSize(12, Fonts.Type.DEFAULT).drawCenteredString(context.getMatrices(),
                    "© 2026 AegisClient. Все права защищены. 1488", width / 2 + 2, height - 7,
                    applyAlpha(ColorAssist.getText(0.35f), mainAlphaInt));

            rectangle
                    .render(ShapeProperties.create(context.getMatrices(), 8, height - 27, 20, 20).thickness(2).round(10)
                            .outlineColor(applyAlpha(new Color(100, 100, 100, 95).getRGB(), mainAlphaInt))
                            .color(applyAlpha(new Color(50, 50, 50, 55).getRGB(), mainAlphaInt),
                                    applyAlpha(new Color(50, 50, 50, 55).getRGB(), mainAlphaInt),
                                    applyAlpha(new Color(80, 80, 80, 95).getRGB(), mainAlphaInt),
                                    applyAlpha(new Color(80, 80, 80, 95).getRGB(), mainAlphaInt))
                            .build());

            Render2D.drawTexture(context, Identifier.of("minecraft", "textures/mainmenu/steve.png"), 9.5f,
                    height - 25.5f, 17, 7, 32, 32, 32, applyAlpha(new Color(0, 0, 0, 255).getRGB(), mainAlphaInt));

            rectangle.render(ShapeProperties.create(context.getMatrices(), 22, height - 13, 6, 6).thickness(2).round(3)
                    .outlineColor(applyAlpha(new Color(100, 100, 100, 95).getRGB(), mainAlphaInt))
                    .color(applyAlpha(new Color(50, 50, 50, 55).getRGB(), mainAlphaInt),
                            applyAlpha(new Color(50, 50, 50, 55).getRGB(), mainAlphaInt),
                            applyAlpha(new Color(80, 80, 80, 95).getRGB(), mainAlphaInt),
                            applyAlpha(new Color(80, 80, 80, 95).getRGB(), mainAlphaInt))
                    .build());
            rectangle.render(ShapeProperties.create(context.getMatrices(), 23, height - 12, 4, 4).round(2)
                    .color(applyAlpha(new Color(1, 235, 1, 155).getRGB(), mainAlphaInt)).build());

            Fonts.getSize(12, Fonts.Type.DEFAULT).drawString(context.getMatrices(),
                    "Username ▸ " + userProfile.profile("username"), 35, height - 21.5f,
                    applyAlpha(ColorAssist.getText(0.35f), mainAlphaInt));
            Fonts.getSize(12, Fonts.Type.DEFAULT).drawString(context.getMatrices(), "Versions ▸ Beta", 35,
                    height - 14.5f, applyAlpha(ColorAssist.getText(0.35f), mainAlphaInt));
            String text = "Build ▸ 0.5.0 Beta";
            float textWidth = Fonts.getSize(12, Fonts.Type.DEFAULT).getStringWidth(text);
            Fonts.getSize(12, Fonts.Type.DEFAULT).drawString(context.getMatrices(), text,
                    context.getScaledWindowWidth() - textWidth - 3, context.getScaledWindowHeight() - 5.5f,
                    applyAlpha(ColorAssist.getText(0.35f), mainAlphaInt));
        }

        Double altAlpha = altFadeAnimation.getOutput();
        if (altVisible || altAlpha > 0.01f) {
            float centerX = width / 2f - 80;
            float centerY = height / 2f - 105;

            if (altScreen == null) {
                altScreen = new AltScreen(centerX, centerY);
            } else {
                altScreen.updatePosition(centerX, centerY);
            }

            int altAlphaInt = (int) (255 * altAlpha);
            Color buttonColor = new Color(50, 50, 50, (int) (55 * altAlpha));
            Color outlineColor = new Color(100, 100, 100, (int) (95 * altAlpha));
            Color gradientColor = new Color(80, 80, 80, (int) (95 * altAlpha));
            Color textColor = new Color(200, 200, 200, altAlphaInt);
            Color bgColor = new Color(30, 30, 30, (int) (255 * altAlpha));

            altScreen.render(context, buttonColor, outlineColor, gradientColor, textColor, bgColor);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawButton(DrawContext ctx, float x, float y, float w, float h, String label, int alpha) {
        rectangle.render(ShapeProperties.create(ctx.getMatrices(), x, y, w, h).thickness(2).round(4)
                .outlineColor(applyAlpha(new Color(100, 100, 100, 95).getRGB(), alpha))
                .color(applyAlpha(new Color(50, 50, 50, 55).getRGB(), alpha),
                        applyAlpha(new Color(50, 50, 50, 55).getRGB(), alpha),
                        applyAlpha(new Color(80, 80, 80, 95).getRGB(), alpha),
                        applyAlpha(new Color(80, 80, 80, 95).getRGB(), alpha))
                .build());

        rectangle.render(ShapeProperties.create(ctx.getMatrices(), x, y, w, 1).thickness(2).round(5)
                .outlineColor(applyAlpha(new Color(100, 100, 100, 95).getRGB(), alpha))
                .color(applyAlpha(new Color(50, 50, 50, 5).getRGB(), alpha),
                        applyAlpha(new Color(50, 50, 50, 255).getRGB(), alpha),
                        applyAlpha(new Color(80, 80, 80, 255).getRGB(), alpha),
                        applyAlpha(new Color(80, 80, 80, 5).getRGB(), alpha))
                .build());

        if (!label.isEmpty()) {
            Fonts.getSize(16, Fonts.Type.DEFAULT).drawCenteredString(ctx.getMatrices(), label, width / 2, y + 7,
                    applyAlpha(new Color(200, 200, 200).getRGB(), alpha));
        }
    }

    private int applyAlpha(int color, int alpha) {
        Color c = new Color(color, true);
        int newAlpha = (int) ((c.getAlpha() / 255.0) * alpha);
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.min(255, newAlpha)).getRGB();
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double h, double v) {
        if (altVisible && altFadeAnimation.getOutput() > 0.5 && altScreen != null) {
            return altScreen.mouseScrolled(mx, my, v);
        }
        return super.mouseScrolled(mx, my, h, v);
    }

    @Override
    public boolean mouseDragged(double mx, double my, int btn, double dx, double dy) {
        if (altVisible && altFadeAnimation.getOutput() > 0.5 && altScreen != null) {
            return altScreen.mouseDragged(mx, my, btn);
        }
        return super.mouseDragged(mx, my, btn, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mx, double my, int btn) {
        if (altScreen != null)
            altScreen.mouseReleased();
        return super.mouseReleased(mx, my, btn);
    }

    @Override
    public boolean charTyped(char c, int m) {
        if (altVisible && altFadeAnimation.getOutput() > 0.5 && altScreen != null) {
            return altScreen.charTyped(c);
        }
        return super.charTyped(c, m);
    }

    @Override
    public boolean keyPressed(int k, int s, int m) {
        if (k == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            if (altVisible) {
                toggleAlt();
                return true;
            }
            return false;
        }

        if (altVisible && altFadeAnimation.getOutput() > 0.5 && altScreen != null && altScreen.keyPressed(k)) {
            return true;
        }

        return super.keyPressed(k, s, m);
    }

    private boolean isIn(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private void toggleAlt() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastToggleTime < TOGGLE_DELAY) {
            return;
        }
        lastToggleTime = currentTime;

        if (!altVisible) {
            altVisible = true;
            altFadeAnimation.setDirection(Direction.FORWARDS);
            altFadeAnimation.reset();
            mainFadeAnimation.setDirection(Direction.BACKWARDS);
            mainFadeAnimation.reset();
            if (altScreen != null)
                altScreen.reset();
        } else {
            altFadeAnimation.setDirection(Direction.BACKWARDS);
            altFadeAnimation.reset();
            mainFadeAnimation.setDirection(Direction.FORWARDS);
            mainFadeAnimation.reset();
            if (altScreen != null)
                altScreen.reset();
        }
    }

    private void drawChangelogWithLinks(DrawContext context, String text, float x, float y, int alpha) {
        String[] lines = text.split("\n");
        float currentY = y;

        for (String line : lines) {
            drawLineWithLinks(context, line, x, currentY, alpha);
            currentY += 14;
        }
    }

    private void drawLineWithLinks(DrawContext context, String line, float x, float y, int alpha) {
        String[] words = line.split(" ");
        float currentX = x;

        for (String word : words) {
            if (word.startsWith("http://") || word.startsWith("https://")) {
                float wordWidth = Fonts.getSize(14, Fonts.Type.DEFAULT).getStringWidth(word);
                Fonts.getSize(14, Fonts.Type.DEFAULT).drawString(context.getMatrices(), word, currentX, y,
                        applyAlpha(new Color(100, 150, 255).getRGB(), alpha));

                java.util.Map<String, Object> linkData = new java.util.HashMap<>();
                linkData.put("url", word);
                linkData.put("x", currentX);
                linkData.put("y", y);
                linkData.put("width", wordWidth);
                linkData.put("height", 14f);
                clickableLinks.put(word, linkData);

                currentX += wordWidth + 4;
            } else {
                float wordWidth = Fonts.getSize(14, Fonts.Type.DEFAULT).getStringWidth(word + " ");
                Fonts.getSize(14, Fonts.Type.DEFAULT).drawString(context.getMatrices(), word + " ", currentX, y,
                        applyAlpha(new Color(200, 200, 200).getRGB(), alpha));
                currentX += wordWidth;
            }
        }
    }

    @Override
    public boolean mouseClicked(double mx, double my, int btn) {
        for (java.util.Map.Entry<String, java.util.Map<String, Object>> entry : clickableLinks.entrySet()) {
            java.util.Map<String, Object> linkData = entry.getValue();
            float linkX = ((Number) linkData.get("x")).floatValue();
            float linkY = ((Number) linkData.get("y")).floatValue();
            float linkWidth = ((Number) linkData.get("width")).floatValue();
            float linkHeight = ((Number) linkData.get("height")).floatValue();

            if (isIn(mx, my, linkX, linkY, linkWidth, linkHeight)) {
                String url = (String) linkData.get("url");
                try {
                    java.awt.Desktop.getDesktop().browse(new java.net.URI(url));
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return true;
            }
        }

        float cy = height / 2.0f, sy = cy - 25, sx = width / 2 - 50, bs = 21;

        if (altVisible && altFadeAnimation.getOutput() > 0.5 && altScreen != null) {
            return altScreen.mouseClicked(mx, my, btn);
        }

        if (mainFadeAnimation.getOutput() > 0.5 && btn == 0) {
            if (isIn(mx, my, sx, sy, 102, 18.5f)) {
                mc.setScreen(new SelectWorldScreen(this));
                return true;
            }
            if (isIn(mx, my, sx, sy + bs, 102, 18.5f)) {
                mc.setScreen(new MultiplayerScreen(this));
                return true;
            }
            if (isIn(mx, my, sx, sy + bs * 2, 102, 18.5f)) {
                toggleAlt();
                return true;
            }
            if (isIn(mx, my, sx + 52, sy + bs * 4, 50, 18.5f)) {
                mc.setScreen(new OptionsScreen(this, mc.options));
                return true;
            }
            if (isIn(mx, my, sx, sy + bs * 4, 50, 18.5f)) {
                mc.stop();
                return true;
            }
        }

        return super.mouseClicked(mx, my, btn);
    }
}
