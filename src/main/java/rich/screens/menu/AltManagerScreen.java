package rich.screens.menu;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.MathHelper;
import rich.screens.account.AccountEntry;
import rich.util.config.impl.account.AccountConfig;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;
import rich.util.animations.Easings;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Кастомный AltManager с анимациями и эффектами
 */
public class AltManagerScreen extends Screen {

    private static final Identifier BACKGROUND_TEXTURE = Identifier.of("rich", "textures/menu/backmenu.png");
    private static final float FIXED_GUI_SCALE = 2.0f;

    private final Screen previousScreen;
    private final AccountConfig accountConfig;

    // Анимации
    private long screenOpenTime = 0L;
    private boolean initialized = false;
    private float panelSlideProgress = 0f;
    private float itemAppearProgress = 0f;

    // Аккаунты
    private List<AccountEntry> accounts = new ArrayList<>();
    private int hoveredAccount = -1;
    private int selectedAccount = -1;
    private float scrollOffset = 0f;
    private float targetScrollOffset = 0f;

    // Частицы
    private final List<MenuParticle> particles = new ArrayList<>();
    private final Random particleRandom = new Random();

    private static final float PANEL_WIDTH = 450;
    private static final float PANEL_HEIGHT = 350;
    private static final float ACCOUNT_ITEM_HEIGHT = 40;
    private static final float ACCOUNT_ITEM_SPACING = 5;

    public AltManagerScreen(Screen previousScreen) {
        super(Text.literal("Alt Manager"));
        this.previousScreen = previousScreen;
        this.accountConfig = AccountConfig.getInstance();
        this.accountConfig.load();
        loadAccounts();
        initParticles();
    }

    private void loadAccounts() {
        accounts = accountConfig.getSortedAccounts();
    }

    private void initParticles() {
        particles.clear();
        for (int i = 0; i < 35; i++) {
            particles.add(new MenuParticle(
                    particleRandom.nextFloat() * 1000,
                    particleRandom.nextFloat() * 600,
                    particleRandom.nextFloat() * 0.4f + 0.1f,
                    particleRandom.nextFloat() * 25f + 15f,
                    particleRandom.nextFloat() * 0.25f + 0.05f,
                    particleRandom.nextFloat() * 360f
            ));
        }
    }

    @Override
    protected void init() {
        initialized = false;
        loadAccounts();
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

        int[] colors = {0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFFFFFFF};
        float[] radii = {0, 0, 0, 0};

        rich.Initialization.getInstance().getManager().getRenderCore().getTexturePipeline()
                .drawTexture(BACKGROUND_TEXTURE, 0, 0, screenWidth, screenHeight,
                        0, 0, 1, 1, colors, radii, 1f);

        float pulse = (float) Math.sin(currentTime * 0.001) * 0.5f + 0.5f;
        int overlayAlpha = (int)(10 + pulse * 5);
        Render2D.rect(0, 0, screenWidth, screenHeight, withAlpha(0x1a2040, overlayAlpha), 0);

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
        scrollOffset += (targetScrollOffset - scrollOffset) * Math.min(1f, delta * scrollSpeed);

        float scaledMouseX = toFixedCoord(mouseX);
        float scaledMouseY = toFixedCoord(mouseY);

        Render2D.beginOverlay();

        drawBackground();

        hoveredAccount = getHoveredAccount(scaledMouseX, scaledMouseY);

        renderAccountPanel(scaledMouseX, scaledMouseY, panelSlideProgress, currentTime);

        Render2D.endOverlay();
    }

    private void renderAccountPanel(float mouseX, float mouseY, float slideProgress, long currentTime) {
        int screenWidth = getFixedScaledWidth();
        int screenHeight = getFixedScaledHeight();

        float panelX = (screenWidth - PANEL_WIDTH) / 2f;
        float panelY = (screenHeight - PANEL_HEIGHT) / 2f;

        // Slide анимация
        float slideOffset = (1f - slideProgress) * 50f;
        panelY += slideOffset;

        // Тень
        float shadowSize = 18f;
        Render2D.blur(panelX - shadowSize / 2, panelY - shadowSize / 2,
                PANEL_WIDTH + shadowSize, PANEL_HEIGHT + shadowSize,
                shadowSize, 12, withAlpha(0x000000, 50));

        // Фон панели
        int bgTop = withAlpha(0x1e1e28, (int) (slideProgress * 255));
        int bgBottom = withAlpha(0x14141c, (int) (slideProgress * 255));
        Render2D.gradientRect(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT,
                new int[]{bgTop, bgTop, bgBottom, bgBottom}, 12);

        // Outline
        int outlineAlpha = (int) (slideProgress * 180);
        Render2D.outline(panelX, panelY, PANEL_WIDTH, PANEL_HEIGHT, 0.5f, withAlpha(0x3A3A4A, outlineAlpha), 12);

        // Заголовок
        float titleY = panelY + 12;
        float titleX = panelX + 15;
        int titleAlpha = (int) (slideProgress * 255);
        Fonts.BOLD.draw("Alt Manager", titleX, titleY, 9, withAlpha(0xFFFFFF, titleAlpha));

        // Счётчик аккаунтов
        float countX = panelX + PANEL_WIDTH - 60;
        Fonts.REGULAR.draw(accounts.size() + " alts", countX, titleY, 6, withAlpha(0x8090a0, titleAlpha));

        // Линия
        Render2D.rect(panelX + 10, titleY + 14, PANEL_WIDTH - 20, 0.5f,
                withAlpha(0x3A3A4A, (int) (slideProgress * 100)), 10);

        // Кнопка добавления
        renderAddButton(panelX + PANEL_WIDTH - 30, panelY + 8, 20, 20, slideProgress, currentTime);

        // Список аккаунтов
        float listY = titleY + 22;
        float listHeight = PANEL_HEIGHT - 50;

        // Scissor
        int windowHeight = client.getWindow().getHeight();
        int guiScale = client.getWindow().calculateScaleFactor(client.options.getGuiScale().getValue(), client.forcesUnicodeFont());

        rich.util.render.shader.Scissor.enable(panelX + 8, listY, PANEL_WIDTH - 16, listHeight, guiScale);

        float itemY = listY + scrollOffset;

        for (int i = 0; i < accounts.size(); i++) {
            AccountEntry account = accounts.get(i);
            float itemProgress = (float) Easings.SPRING.ease(Math.min(1f, Math.max(0f, (itemAppearProgress - i * 0.04f) / 0.85f)));

            if (itemProgress <= 0.01f) {
                itemY += ACCOUNT_ITEM_HEIGHT + ACCOUNT_ITEM_SPACING;
                continue;
            }

            boolean isHovered = (i == hoveredAccount);
            boolean isSelected = (i == selectedAccount);
            renderAccountItem(account, panelX + 8, itemY, PANEL_WIDTH - 16, ACCOUNT_ITEM_HEIGHT,
                    isHovered, isSelected, itemProgress, currentTime);

            itemY += ACCOUNT_ITEM_HEIGHT + ACCOUNT_ITEM_SPACING;
        }

        rich.util.render.shader.Scissor.disable();

        // Scroll fade
        renderScrollFade(panelX + 8, listY, PANEL_WIDTH - 16, listHeight);
    }

    private void renderAddButton(float x, float y, float width, float height, float slideProgress, long currentTime) {
        int alpha = (int) (slideProgress * 200);
        Render2D.rect(x, y, width, height, withAlpha(0x2A3040, alpha), 4);
        Render2D.outline(x, y, width, height, 0.5f, withAlpha(0x405060, alpha), 4);
        Fonts.GUI_ICONS.draw("+", x + 4, y + 3, 12, withAlpha(0x80a0c0, alpha));
    }

    private void renderAccountItem(AccountEntry account, float x, float y, float width, float height,
                                     boolean isHovered, boolean isSelected, float progress, long currentTime) {
        float slideOffset = (1f - progress) * 15f;
        float finalX = x + slideOffset;
        float finalY = y;

        // Фон
        int bgAlpha = (int) (progress * (isHovered ? 55 : isSelected ? 40 : 25));
        int bgColor;
        if (isSelected) {
            bgColor = withAlpha(0x3A4060, bgAlpha);
        } else if (isHovered) {
            bgColor = withAlpha(0x303545, bgAlpha);
        } else {
            bgColor = withAlpha(0x252530, bgAlpha);
        }
        Render2D.rect(finalX, finalY, width, height, bgColor, 6);

        // Glow при hover/select
        if ((isHovered || isSelected) && progress > 0.5f) {
            float glowAlpha = (progress - 0.5f) * 2f;
            int glowColor = isSelected ? withAlpha(0x60a060, (int) (glowAlpha * 150)) : withAlpha(0x5060a0, (int) (glowAlpha * 150));
            Render2D.outline(finalX, finalY, width, height, 0.5f, glowColor, 6);

            // Glow blur
            Render2D.blur(finalX - 2, finalY - 2, width + 4, height + 4,
                    6f, 8f, withAlpha(isSelected ? 0x60a060 : 0x4060a0, (int) (glowAlpha * 25)));
        }

        // Скин
        float skinX = finalX + 6;
        float skinY = finalY + 5;
        float skinSize = height - 10;
        Render2D.rect(skinX, skinY, skinSize, skinSize, withAlpha(0x303040, (int) (progress * 150)), 3);

        // Никнейм
        float nameX = finalX + skinSize + 12;
        float nameY = finalY + 8;
        int nameAlpha = (int) (progress * 255);
        Fonts.BOLD.draw(account.getName(), nameX, nameY, 7, withAlpha(0xFFFFFF, nameAlpha));

        // Дата
        float dateY = nameY + 11;
        int dateAlpha = (int) (progress * 180);
        Fonts.REGULAR.draw(account.getDate(), nameX, dateY, 5, withAlpha(0x8090a0, dateAlpha));

        // Кнопки (использовать/удалить)
        float buttonSize = 16;
        float buttonY = finalY + (height - buttonSize) / 2f;

        // Кнопка "Use"
        float useBtnX = finalX + width - buttonSize * 2 - 10;
        int useAlpha = (int) (progress * (isHovered ? 220 : 160));
        Render2D.rect(useBtnX, buttonY, buttonSize, buttonSize, withAlpha(0x3A5040, useAlpha), 3);
        Fonts.GUI_ICONS.draw("P", useBtnX + 2, buttonY + 2, 10, withAlpha(0x60c060, useAlpha));

        // Кнопка "Delete"
        float delBtnX = finalX + width - buttonSize - 5;
        int delAlpha = (int) (progress * (isHovered ? 200 : 140));
        Render2D.rect(delBtnX, buttonY, buttonSize, buttonSize, withAlpha(0x503030, delAlpha), 3);
        Fonts.GUI_ICONS.draw("O", delBtnX + 2, buttonY + 2, 10, withAlpha(0xc06060, delAlpha));
    }

    private void renderScrollFade(float x, float y, float w, float h) {
        for (int i = 0; i < 10; i++) {
            float alpha = (1f - i / 10f) * 100;
            Render2D.rect(x, y + i, w, 1, withAlpha(0x14141c, (int) alpha), 0);
        }
        for (int i = 0; i < 10; i++) {
            float alpha = (i / 10f) * 100;
            Render2D.rect(x, y + h - 10 + i, w, 1, withAlpha(0x14141c, (int) alpha), 0);
        }
    }

    private int getHoveredAccount(float mouseX, float mouseY) {
        int screenWidth = getFixedScaledWidth();
        int screenHeight = getFixedScaledHeight();
        float panelX = (screenWidth - PANEL_WIDTH) / 2f;
        float panelY = (screenHeight - PANEL_HEIGHT) / 2f;
        float listY = panelY + 50;

        if (mouseX < panelX + 8 || mouseX > panelX + PANEL_WIDTH - 8) return -1;

        float itemY = listY + scrollOffset;
        for (int i = 0; i < accounts.size(); i++) {
            if (mouseY >= itemY && mouseY <= itemY + ACCOUNT_ITEM_HEIGHT) {
                return i;
            }
            itemY += ACCOUNT_ITEM_HEIGHT + ACCOUNT_ITEM_SPACING;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        float scaledMouseX = toFixedCoord(click.x());
        float scaledMouseY = toFixedCoord(click.y());

        int hovered = getHoveredAccount(scaledMouseX, scaledMouseY);
        if (hovered >= 0) {
            AccountEntry account = accounts.get(hovered);
            float itemY = 0;
            int screenWidth = getFixedScaledWidth();
            int screenHeight = getFixedScaledHeight();
            float panelX = (screenWidth - PANEL_WIDTH) / 2f;
            float panelY = (screenHeight - PANEL_HEIGHT) / 2f;
            float listY = panelY + 50;
            itemY = listY + scrollOffset + hovered * (ACCOUNT_ITEM_HEIGHT + ACCOUNT_ITEM_SPACING);

            // Проверяем кнопки
            float buttonSize = 16;
            float buttonY = itemY + (ACCOUNT_ITEM_HEIGHT - buttonSize) / 2f;
            float useBtnX = panelX + 8 + PANEL_WIDTH - 16 - buttonSize * 2 - 10;
            float delBtnX = panelX + 8 + PANEL_WIDTH - 16 - buttonSize - 5;

            if (scaledMouseX >= useBtnX && scaledMouseX <= useBtnX + buttonSize &&
                    scaledMouseY >= buttonY && scaledMouseY <= buttonY + buttonSize) {
                // Использовать аккаунт
                accountConfig.setActiveAccount(account.getName(), account.getDate(), account.getSkin());
                accountConfig.save();
                selectedAccount = hovered;
                return true;
            }

            if (scaledMouseX >= delBtnX && scaledMouseX <= delBtnX + buttonSize &&
                    scaledMouseY >= buttonY && scaledMouseY <= buttonY + buttonSize) {
                // Удалить аккаунт
                accounts.remove(hovered);
                accountConfig.save();
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        float maxScroll = (float) Math.max(0, accounts.size() * (ACCOUNT_ITEM_HEIGHT + ACCOUNT_ITEM_SPACING) - (PANEL_HEIGHT - 60));
        targetScrollOffset = Math.max(-maxScroll, Math.min(0f, (float) (targetScrollOffset + vertical * 20)));
        return true;
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE) {
            client.setScreen(previousScreen);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean charTyped(char chr, int modifiers) {
        return super.charTyped(chr, modifiers);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    private int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (alpha << 24);
    }

    private static class MenuParticle {
        float x, y, size, speed, baseAlpha, angle;
        MenuParticle(float x, float y, float size, float speed, float alpha, float angle) {
            this.x = x; this.y = y; this.size = size; this.speed = speed;
            this.baseAlpha = alpha; this.angle = angle;
        }
    }
}
