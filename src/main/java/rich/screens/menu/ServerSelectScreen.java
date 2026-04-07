package rich.screens.menu;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;
import rich.util.animations.Easings;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Кастомный экран выбора сервера с анимациями и glow эффектами
 */
public class ServerSelectScreen extends Screen {

    private static final Identifier BACKGROUND_TEXTURE = Identifier.of("rich", "textures/menu/backmenu.png");
    private static final float FIXED_GUI_SCALE = 2.0f;

    private final Screen previousScreen;

    // Анимации
    private long screenOpenTime = 0L;
    private boolean initialized = false;
    private float panelSlideProgress = 0f;
    private float itemAppearProgress = 0f;

    // Список серверов
    private final List<ServerEntry> servers = new ArrayList<>();
    private int hoveredServer = -1;
    private float scrollOffset = 0f;
    private float targetScrollOffset = 0f;

    // Частицы
    private final List<MenuParticle> particles = new ArrayList<>();
    private final Random particleRandom = new Random();

    private static final float PANEL_WIDTH = 400;
    private static final float PANEL_HEIGHT = 300;
    private static final float SERVER_ITEM_HEIGHT = 35;
    private static final float SERVER_ITEM_SPACING = 4;

    public ServerSelectScreen(Screen previousScreen) {
        super(Text.literal("Select Server"));
        this.previousScreen = previousScreen;
        initServers();
        initParticles();
    }

    private void initServers() {
        // Пример серверов (можно загружать из конфига)
        servers.add(new ServerEntry("ReallyWorld", "rw.reallyworld.ru", "pvp", 1247));
        servers.add(new ServerEntry("HolyWorld", "hw.holyworld.ru", "anarchy", 856));
        servers.add(new ServerEntry("FunTime", "ft.funtime.ru", "mini-games", 2134));
        servers.add(new ServerEntry("SunRise", "sr.sunrise.ru", "survival", 543));
        servers.add(new ServerEntry("CrystalPvP", "cp.crystalpvp.ru", "pvp", 321));
    }

    private void initParticles() {
        particles.clear();
        for (int i = 0; i < 30; i++) {
            particles.add(new MenuParticle(
                    particleRandom.nextFloat() * 1000,
                    particleRandom.nextFloat() * 600,
                    particleRandom.nextFloat() * 0.4f + 0.1f,
                    particleRandom.nextFloat() * 20f + 15f,
                    particleRandom.nextFloat() * 0.25f + 0.05f,
                    particleRandom.nextFloat() * 360f
            ));
        }
    }

    @Override
    protected void init() {
        initialized = false;
    }

    private int getFixedScaledWidth() {
        return (int) Math.ceil((double) client.getWindow().getFramebufferWidth() / FIXED_GUI_SCALE);
    }

    private int getFixedScaledHeight() {
        return (int) Math.ceil((double) client.getWindow().getFramebufferHeight() / FIXED_GUI_SCALE);
    }

    private float toFixedCoord(double coord) {
        float currentScale = (float) client.getWindow().getScaleFactor();
        return (float) (coord * currentScale / FIXED_GUI_SCALE);
    }

    private void drawBackground() {
        int screenWidth = getFixedScaledWidth();
        int screenHeight = getFixedScaledHeight();
        long currentTime = Util.getMeasuringTimeMs();

        // Фон
        int[] colors = {0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF};
        float[] radii = {0, 0, 0, 0};

        rich.Initialization.getInstance().getManager().getRenderCore().getTexturePipeline()
                .drawTexture(BACKGROUND_TEXTURE, 0, 0, screenWidth, screenHeight,
                        0, 0, 1, 1, colors, radii, 1f);

        // Overlay
        float pulse = (float) Math.sin(currentTime * 0.001) * 0.5f + 0.5f;
        int overlayAlpha = (int)(10 + pulse * 5);
        Render2D.rect(0, 0, screenWidth, screenHeight, withAlpha(0x1a2040, overlayAlpha), 0);

        // Частицы
        renderParticles(screenWidth, screenHeight, currentTime);
    }

    private void renderParticles(int screenWidth, int screenHeight, long currentTime) {
        float deltaTime = 0.016f;

        for (MenuParticle p : particles) {
            p.x += Math.cos(Math.toRadians(p.angle)) * p.speed * deltaTime * 15f;
            p.y += Math.sin(Math.toRadians(p.angle)) * p.speed * deltaTime * 15f;

            if (p.x < 0) p.x = screenWidth;
            if (p.x > screenWidth) p.x = 0;
            if (p.y < 0) p.y = screenHeight;
            if (p.y > screenHeight) p.y = 0;

            float alphaPulse = (float) Math.sin(currentTime * 0.002 + p.x * 0.01f) * 0.3f + 0.7f;
            int alpha = (int) (p.baseAlpha * alphaPulse * 80);

            Render2D.rect(p.x, p.y, p.size, p.size, withAlpha(0x4060a0, alpha), p.size / 2f);
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        long currentTime = Util.getMeasuringTimeMs();

        if (!initialized) {
            screenOpenTime = currentTime;
            initialized = true;
        }

        long elapsed = currentTime - screenOpenTime;
        panelSlideProgress = (float) Easings.SPRING.ease(Math.min(1f, elapsed / 400f));
        itemAppearProgress = (float) Easings.SPRING.ease(Math.min(1f, Math.max(0f, (elapsed - 100f) / 500f)));

        // Scroll
        float scrollSpeed = 12f;
        float deltaTime = delta;
        scrollOffset += (targetScrollOffset - scrollOffset) * Math.min(1f, deltaTime * scrollSpeed);

        float scaledMouseX = toFixedCoord(mouseX);
        float scaledMouseY = toFixedCoord(mouseY);

        Render2D.beginOverlay();

        drawBackground();

        // Определяем hovered сервер
        hoveredServer = getHoveredServer(scaledMouseX, scaledMouseY);

        // Рендерим панель
        renderServerPanel(scaledMouseX, scaledMouseY, panelSlideProgress, currentTime);

        Render2D.endOverlay();
    }

    private void renderServerPanel(float mouseX, float mouseY, float slideProgress, long currentTime) {
        int screenWidth = getFixedScaledWidth();
        int screenHeight = getFixedScaledHeight();

        float panelX = (screenWidth - PANEL_WIDTH) / 2f;
        float panelY = (screenHeight - PANEL_HEIGHT) / 2f;

        // Slide анимация
        float slideOffset = (1f - slideProgress) * 50f;
        panelY += slideOffset;

        // Тень под панелью
        float shadowSize = 15f;
        Render2D.blur(panelX - shadowSize / 2, panelY - shadowSize / 2,
                PANEL_WIDTH + shadowSize, PANEL_HEIGHT + shadowSize,
                shadowSize, 10, withAlpha(0x000000, 40));

        // Фон панели
        int bgTop = withAlpha(0x1e1e28, (int) (slideProgress * 255));
        int bgBottom = withAlpha(0x14141c, (int) (slideProgress * 255));
        Render2D.gradientRect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT,
                new int[]{bgTop, bgTop, bgBottom, bgBottom}, 10);

        // Outline панели
        int outlineAlpha = (int) (slideProgress * 180);
        Render2D.outline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0.5f, withAlpha(0x3A3A4A, outlineAlpha), 10);

        // Заголовок
        float titleY = panelY + 10;
        float titleX = panelX + 12;
        int titleAlpha = (int) (slideProgress * 255);
        Fonts.BOLD.draw("Select Server", titleX, titleY, 8, withAlpha(0xFFFFFF, titleAlpha));

        // Линия-разделитель
        Render2D.rect(panelX + 8, titleY + 14, PANEL_WIDTH - 16, 0.5f,
                withAlpha(0x3A3A4A, (int) (slideProgress * 100)), 10);

        // Список серверов
        float listY = titleY + 20;
        float listHeight = PANEL_HEIGHT - 40;

        // Клиппинг
        int guiScale = client.getWindow().calculateScaleFactor(client.options.getGuiScale().getValue(), client.forcesUnicodeFont());

        rich.util.render.shader.Scissor.enable(panelX + 8, listY, PANEL_WIDTH - 16, listHeight, guiScale);

        float itemY = listY + 5 + scrollOffset;

        for (int i = 0; i < servers.size(); i++) {
            ServerEntry server = servers.get(i);
            float itemProgress = (float) Easings.SPRING.ease(Math.min(1f, Math.max(0f, (itemAppearProgress - i * 0.05f) / 0.8f)));

            if (itemProgress <= 0.01f) {
                itemY += SERVER_ITEM_HEIGHT + SERVER_ITEM_SPACING;
                continue;
            }

            boolean isHovered = (i == hoveredServer);
            renderServerItem(server, panelX + 8, itemY, PANEL_WIDTH - 16, SERVER_ITEM_HEIGHT,
                    isHovered, itemProgress, currentTime);

            itemY += SERVER_ITEM_HEIGHT + SERVER_ITEM_SPACING;
        }

        rich.util.render.shader.Scissor.disable();

        // Scroll fade эффекты
        renderScrollFade(panelX + 8, listY, PANEL_WIDTH - 16, listHeight);
    }

    private void renderServerItem(ServerEntry server, float x, float y, float width, float height,
                                   boolean isHovered, float progress, long currentTime) {
        float slideOffset = (1f - progress) * 15f;
        float finalX = x + slideOffset;
        float finalY = y;

        // Фон
        int bgAlpha = (int) (progress * (isHovered ? 50 : 25));
        int bgColor = isHovered ? withAlpha(0x3A4060, bgAlpha) : withAlpha(0x2A2A35, bgAlpha);
        Render2D.rect(finalX, finalY, width, height, bgColor, 6);

        // Glow при hover
        if (isHovered && progress > 0.5f) {
            float glowAlpha = (progress - 0.5f) * 2f;
            Render2D.outline(finalX, finalY, width, height, 0.5f,
                    withAlpha(0x5060a0, (int) (glowAlpha * 150)), 6);

            // Glow blur
            Render2D.blur(finalX - 2, finalY - 2, width + 4, height + 4,
                    5f, 8f, withAlpha(0x4060a0, (int) (glowAlpha * 20)));
        }

        // Иконка сервера
        float iconX = finalX + 8;
        float iconY = finalY + (height - 12) / 2f;
        int iconAlpha = (int) (progress * 200);
        Fonts.GUI_ICONS.draw("a", iconX, iconY, 12, withAlpha(0x80a0c0, iconAlpha));

        // Название сервера
        float nameX = finalX + 26;
        float nameY = finalY + 6;
        int nameAlpha = (int) (progress * 255);
        Fonts.BOLD.draw(server.name, nameX, nameY, 7, withAlpha(0xFFFFFF, nameAlpha));

        // Адрес сервера
        float addressY = nameY + 10;
        int addressAlpha = (int) (progress * 180);
        Fonts.REGULAR.draw(server.address, nameX, addressY, 5, withAlpha(0x8090a0, addressAlpha));

        // Количество игроков
        float playersX = finalX + width - 50;
        float playersY = finalY + (height - 6) / 2f;
        int playersAlpha = (int) (progress * 200);
        Fonts.BOLD.draw(String.valueOf(server.onlinePlayers), playersX, playersY, 6, withAlpha(0x60a060, playersAlpha));
    }

    private void renderScrollFade(float x, float y, float w, float h) {
        // Top fade
        for (int i = 0; i < 10; i++) {
            float alpha = (1f - i / 10f) * 100;
            Render2D.rect(x, y + i, w, 1, withAlpha(0x14141c, (int) alpha), 0);
        }
        // Bottom fade
        for (int i = 0; i < 10; i++) {
            float alpha = (i / 10f) * 100;
            Render2D.rect(x, y + h - 10 + i, w, 1, withAlpha(0x14141c, (int) alpha), 0);
        }
    }

    private int getHoveredServer(float mouseX, float mouseY) {
        int screenWidth = getFixedScaledWidth();
        int screenHeight = getFixedScaledHeight();
        float panelX = (screenWidth - PANEL_WIDTH) / 2f;
        float panelY = (screenHeight - PANEL_HEIGHT) / 2f;
        float listY = panelY + 44;

        if (mouseX < panelX + 8 || mouseX > panelX + PANEL_WIDTH - 8) return -1;

        float itemY = listY + 5 + scrollOffset;
        for (int i = 0; i < servers.size(); i++) {
            if (mouseY >= itemY && mouseY <= itemY + SERVER_ITEM_HEIGHT) {
                return i;
            }
            itemY += SERVER_ITEM_HEIGHT + SERVER_ITEM_SPACING;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (hoveredServer >= 0) {
            ServerEntry server = servers.get(hoveredServer);
            // Подключаемся к серверу
            client.setScreen(new MultiplayerScreen(previousScreen));
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        float maxScroll = (float) Math.max(0, servers.size() * (SERVER_ITEM_HEIGHT + SERVER_ITEM_SPACING) - (PANEL_HEIGHT - 50));
        targetScrollOffset = Math.max(-maxScroll, Math.min(0f, (float) (targetScrollOffset + vertical * 20)));
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput keyInput) {
        if (keyInput.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            client.setScreen(previousScreen);
            return true;
        }
        return super.keyPressed(keyInput);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private static class ServerEntry {
        final String name;
        final String address;
        final String type;
        final int onlinePlayers;

        ServerEntry(String name, String address, String type, int onlinePlayers) {
            this.name = name;
            this.address = address;
            this.type = type;
            this.onlinePlayers = onlinePlayers;
        }
    }

    private static class MenuParticle {
        float x, y, size, speed, baseAlpha, angle;
        MenuParticle(float x, float y, float size, float speed, float alpha, float angle) {
            this.x = x; this.y = y; this.size = size; this.speed = speed;
            this.baseAlpha = alpha; this.angle = angle;
        }
    }
}
