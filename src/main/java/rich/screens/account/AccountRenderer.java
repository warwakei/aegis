package rich.screens.account;

import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import rich.util.ColorUtil;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;
import rich.util.render.shader.Scissor;

import java.util.List;

public class AccountRenderer {

    private static final float BLUR_RADIUS = 15f;
    private static final float OUTLINE_THICKNESS = 1f;

    public void renderLeftPanelTop(float x, float y, float width, float height, float contentAlpha,
                                   String nicknameText, boolean nicknameFieldFocused,
                                   float scaledMouseX, float scaledMouseY, long currentTime) {

        int bgAlpha = (int) (contentAlpha * 120);
        int headerAlpha = (int) (contentAlpha * 150);
        int outlineAlpha = (int) (contentAlpha * 100);
        int blurAlpha = (int) (contentAlpha * 80);
        int titleAlpha = (int) (contentAlpha * 255);
        int titleTextAlpha = (int) (contentAlpha * 155);

        int bgTopLeft = withAlpha(0x0d0f14, bgAlpha);
        int bgTopRight = withAlpha(0x101218, bgAlpha);
        int bgBottomLeft = withAlpha(0x08090c, bgAlpha);
        int bgBottomRight = withAlpha(0x0d0f14, bgAlpha);
        int headerTopLeft = withAlpha(0x14171f, headerAlpha);
        int headerTopRight = withAlpha(0x181b24, headerAlpha);
        int headerBottomLeft = withAlpha(0x10131a, headerAlpha);
        int headerBottomRight = withAlpha(0x14171f, headerAlpha);
        int outlineColor = withAlpha(0x252a36, outlineAlpha);
        int blurTint = withAlpha(0x060810, blurAlpha);

        int[] bgColors = {bgTopLeft, bgTopRight, bgBottomRight, bgBottomLeft};
        Render2D.gradientRect(x, y, width, height, bgColors, 6);
        int[] headerColors = {headerTopLeft, headerTopRight, headerBottomRight, headerBottomLeft};
        Render2D.gradientRect(x, y, width, 22, headerColors, 6, 6, 0, 0);
        Render2D.outline(x, y, width, height, OUTLINE_THICKNESS, outlineColor, 6);
        Fonts.BOLD.drawCentered("Account Panel", x + width / 2f - 15, y + 7, 8f, withAlpha(0xFFFFFF, titleAlpha));

        Fonts.REGULARNEW.draw("Nickname", x + 5, y + 28, 5.5f, withAlpha(0xFFFFFF, titleTextAlpha));

        float fieldX = x + 5;
        float fieldY = y + 38;
        float fieldHeight = 14;
        float addButtonSize = 14;
        float buttonGap = 3;
        float fieldWidth = width - 10 - addButtonSize - buttonGap;

        renderNicknameField(fieldX, fieldY, fieldWidth, fieldHeight, contentAlpha, nicknameText, nicknameFieldFocused, currentTime);

        float addButtonX = fieldX + fieldWidth + buttonGap;
        boolean addButtonHovered = isMouseOver(scaledMouseX, scaledMouseY, addButtonX, fieldY, addButtonSize, addButtonSize);
        renderAddButton(addButtonX, fieldY, addButtonSize, contentAlpha, addButtonHovered, titleAlpha);

        float buttonWidth = width - 10;
        float buttonHeight = 16;

        float randomButtonX = x + 5;
        float randomButtonY = fieldY + fieldHeight + 6;
        boolean randomButtonHovered = isMouseOver(scaledMouseX, scaledMouseY, randomButtonX, randomButtonY, buttonWidth, buttonHeight);
        renderRandomButton(randomButtonX, randomButtonY, buttonWidth, buttonHeight, contentAlpha, randomButtonHovered, titleAlpha);

        float clearButtonX = x + 5;
        float clearButtonY = randomButtonY + buttonHeight + 5;
        boolean clearButtonHovered = isMouseOver(scaledMouseX, scaledMouseY, clearButtonX, clearButtonY, buttonWidth, buttonHeight);
        renderClearAllButton(clearButtonX, clearButtonY, buttonWidth, buttonHeight, contentAlpha, clearButtonHovered, titleAlpha);
    }

    private void renderNicknameField(float x, float y, float width, float height, float contentAlpha,
                                     String nicknameText, boolean focused, long currentTime) {
        int titleAlpha = (int) (contentAlpha * 255);
        int titleTextAlpha = (int) (contentAlpha * 155);
        int fieldBgAlpha = (int) (contentAlpha * 180);
        int fieldOutlineAlpha = focused ? (int) (contentAlpha * 180) : (int) (contentAlpha * 80);

        int fieldBgTop = withAlpha(0x0a0c10, fieldBgAlpha);
        int fieldBgBottom = withAlpha(0x080a0e, fieldBgAlpha);
        int[] fieldBgColors = {fieldBgTop, fieldBgTop, fieldBgBottom, fieldBgBottom};

        Render2D.gradientRect(x, y, width, height, fieldBgColors, 3);

        int fieldOutlineColor = focused ? withAlpha(0x3a4a5a, fieldOutlineAlpha) : withAlpha(0x252a36, fieldOutlineAlpha);
        Render2D.outline(x, y, width, height, 0.5f, fieldOutlineColor, 3);

        String displayText = nicknameText.isEmpty() && !focused ? "Enter nick..." : nicknameText;
        int textColor = nicknameText.isEmpty() && !focused ? withAlpha(0x606878, titleTextAlpha) : withAlpha(0xd0d4dc, titleAlpha);
        Fonts.TEST.draw(displayText, x + 4, y + 4.5f, 5.5f, textColor);

        if (focused && (currentTime / 500) % 2 == 0) {
            float cursorX = x + 4 + Fonts.TEST.getWidth(nicknameText, 5.5f);
            Render2D.rect(cursorX, y + 3, 0.5f, height - 6, withAlpha(0xd0d4dc, titleAlpha), 0);
        }
    }

    private void renderAddButton(float x, float y, float size, float contentAlpha, boolean hovered, int titleAlpha) {
        int btnAlpha = hovered ? (int) (contentAlpha * 180) : (int) (contentAlpha * 140);
        int btnTopLeft = withAlpha(0x14171f, btnAlpha);
        int btnTopRight = withAlpha(0x181b24, btnAlpha);
        int btnBottomLeft = withAlpha(0x10131a, btnAlpha);
        int btnBottomRight = withAlpha(0x14171f, btnAlpha);
        int[] btnColors = {btnTopLeft, btnTopRight, btnBottomRight, btnBottomLeft};

        Render2D.gradientRect(x, y, size, size, btnColors, 3);
        Render2D.outline(x, y, size, size, 0.5f, withAlpha(0x252a36, (int) (contentAlpha * 100)), 3);

        float plusCenterX = x + size / 2f;
        float plusCenterY = y + size / 2f;
        float plusSize = 5;
        float plusThickness = 1.2f;

        Render2D.rect(plusCenterX - plusSize / 2f, plusCenterY - plusThickness / 2f, plusSize, plusThickness, withAlpha(0xFFFFFF, titleAlpha), 0.5f);
        Render2D.rect(plusCenterX - plusThickness / 2f, plusCenterY - plusSize / 2f, plusThickness, plusSize, withAlpha(0xFFFFFF, titleAlpha), 0.5f);
    }

    private void renderRandomButton(float x, float y, float width, float height, float contentAlpha, boolean hovered, int titleAlpha) {
        int btnAlpha = hovered ? (int) (contentAlpha * 200) : (int) (contentAlpha * 140);
        int btnTopLeft = hovered ? withAlpha(0x1a1f28, btnAlpha) : withAlpha(0x14171f, btnAlpha);
        int btnTopRight = hovered ? withAlpha(0x1e232d, btnAlpha) : withAlpha(0x181b24, btnAlpha);
        int btnBottomLeft = hovered ? withAlpha(0x14181f, btnAlpha) : withAlpha(0x10131a, btnAlpha);
        int btnBottomRight = hovered ? withAlpha(0x1a1f28, btnAlpha) : withAlpha(0x14171f, btnAlpha);
        int[] btnColors = {btnTopLeft, btnTopRight, btnBottomRight, btnBottomLeft};

        Render2D.gradientRect(x, y, width, height, btnColors, 3);

        int outlineColor = hovered ? withAlpha(0x3a4a5a, (int) (contentAlpha * 150)) : withAlpha(0x252a36, (int) (contentAlpha * 100));
        Render2D.outline(x, y, width, height, 0.5f, outlineColor, 3);

        int textColor = hovered ? withAlpha(0xFFFFFF, titleAlpha) : withAlpha(0xd0d8e4, titleAlpha);
        Fonts.DEFAULT.draw("Random", x + 6, y + 5f, 5.5f, textColor);
        Fonts.ICONS.draw("R", x + 75, y + 3.5f, 10f, textColor);
    }

    private void renderClearAllButton(float x, float y, float width, float height, float contentAlpha, boolean hovered, int titleAlpha) {
        int btnAlpha = hovered ? (int) (contentAlpha * 200) : (int) (contentAlpha * 140);
        int btnTopLeft = hovered ? withAlpha(0x2a1a1a, btnAlpha) : withAlpha(0x1a1416, btnAlpha);
        int btnTopRight = hovered ? withAlpha(0x2e1e1e, btnAlpha) : withAlpha(0x1e1618, btnAlpha);
        int btnBottomLeft = hovered ? withAlpha(0x241414, btnAlpha) : withAlpha(0x161012, btnAlpha);
        int btnBottomRight = hovered ? withAlpha(0x2a1a1a, btnAlpha) : withAlpha(0x1a1416, btnAlpha);
        int[] btnColors = {btnTopLeft, btnTopRight, btnBottomRight, btnBottomLeft};

        Render2D.gradientRect(x, y, width, height, btnColors, 3);

        int outlineColor = hovered ? withAlpha(0x5a3a3a, (int) (contentAlpha * 150)) : withAlpha(0x352a2a, (int) (contentAlpha * 100));
        Render2D.outline(x, y, width, height, 0.5f, outlineColor, 3);

        int textColor = hovered ? withAlpha(0xff8080, titleAlpha) : withAlpha(0xd0a0a0, titleAlpha);
        Fonts.DEFAULT.draw("Clear All", x + 6, y + 5f, 5.5f, textColor);
        Fonts.GUI_ICONS.draw("O", x + 77, y + 2.5f, 11f, textColor);
    }

    public void renderLeftPanelBottom(float x, float y, float width, float height, float contentAlpha,
                                      String activeAccountName, String activeAccountDate, Identifier activeAccountSkin) {

        int bgAlpha = (int) (contentAlpha * 120);
        int headerAlpha = (int) (contentAlpha * 150);
        int outlineAlpha = (int) (contentAlpha * 100);
        int blurAlpha = (int) (contentAlpha * 80);
        int titleAlpha = (int) (contentAlpha * 255);
        int titleTextAlpha = (int) (contentAlpha * 155);

        int bgTopLeft = withAlpha(0x0d0f14, bgAlpha);
        int bgTopRight = withAlpha(0x101218, bgAlpha);
        int bgBottomLeft = withAlpha(0x08090c, bgAlpha);
        int bgBottomRight = withAlpha(0x0d0f14, bgAlpha);
        int headerTopLeft = withAlpha(0x14171f, headerAlpha);
        int headerTopRight = withAlpha(0x181b24, headerAlpha);
        int headerBottomLeft = withAlpha(0x10131a, headerAlpha);
        int headerBottomRight = withAlpha(0x14171f, headerAlpha);
        int outlineColor = withAlpha(0x252a36, outlineAlpha);
        int blurTint = withAlpha(0x060810, blurAlpha);

        Render2D.blur(x, y, width, height, BLUR_RADIUS, 6, blurTint);
        int[] bgColors = {bgTopLeft, bgTopRight, bgBottomRight, bgBottomLeft};
        Render2D.gradientRect(x, y, width, height, bgColors, 6);
        int[] headerColors = {headerTopLeft, headerTopRight, headerBottomRight, headerBottomLeft};
        Render2D.gradientRect(x, y, width, 22, headerColors, 6, 6, 0, 0);
        Render2D.outline(x, y, width, height, OUTLINE_THICKNESS, outlineColor, 6);
        Fonts.BOLD.drawCentered("Active Session", x + width / 2f - 15, y + 6, 8f, withAlpha(0xFFFFFF, titleAlpha));

        if (!activeAccountName.isEmpty()) {
            float faceX = x + 8;
            float faceY = y + 28;
            float faceSize = 24;

            Identifier skinTexture = SkinManager.getSkin(activeAccountName);
            int faceColor = withAlpha(0xFFFFFF, titleAlpha);

            drawPlayerFace(skinTexture, faceX, faceY, faceSize, faceColor);

            float textX = faceX + faceSize + 6;
            float nameY = faceY + 4;
            float dateY = nameY + 10;

            Fonts.TEST.draw(activeAccountName, textX, nameY, 6f, withAlpha(0xFFFFFF, titleAlpha));
            Fonts.TEST.draw(activeAccountDate, textX, dateY, 4.5f, withAlpha(0x808890, titleAlpha));
        } else {
            Fonts.REGULARNEW.drawCentered("No account selected", x + 50, y + 36, 5f, withAlpha(0x606878, titleTextAlpha));
        }
    }

    public void renderRightPanel(float x, float y, float width, float height, float contentAlpha,
                                 List<AccountEntry> accounts, float scrollOffset,
                                 float scaledMouseX, float scaledMouseY, float scale, int guiScale) {

        int bgAlpha = (int) (contentAlpha * 120);
        int headerAlpha = (int) (contentAlpha * 150);
        int outlineAlpha = (int) (contentAlpha * 100);
        int blurAlpha = (int) (contentAlpha * 80);
        int titleAlpha = (int) (contentAlpha * 255);
        int titleTextAlpha = (int) (contentAlpha * 155);

        int bgTopLeft = withAlpha(0x0d0f14, bgAlpha);
        int bgTopRight = withAlpha(0x101218, bgAlpha);
        int bgBottomLeft = withAlpha(0x08090c, bgAlpha);
        int bgBottomRight = withAlpha(0x0d0f14, bgAlpha);
        int headerTopLeft = withAlpha(0x14171f, headerAlpha);
        int headerTopRight = withAlpha(0x181b24, headerAlpha);
        int headerBottomLeft = withAlpha(0x10131a, headerAlpha);
        int headerBottomRight = withAlpha(0x14171f, headerAlpha);
        int outlineColor = withAlpha(0x252a36, outlineAlpha);
        int blurTint = withAlpha(0x060810, blurAlpha);

        Render2D.blur(x, y, width, height, BLUR_RADIUS, 6, blurTint);
        int[] bgColors = {bgTopLeft, bgTopRight, bgBottomRight, bgBottomLeft};
        Render2D.gradientRect(x, y, width, height, bgColors, 6);
        int[] headerColors = {headerTopLeft, headerTopRight, headerBottomRight, headerBottomLeft};
        Render2D.gradientRect(x, y, width, 22, headerColors, 6, 6, 0, 0);
        Render2D.outline(x, y, width, height, OUTLINE_THICKNESS, outlineColor, 6);
        Fonts.BOLD.draw("Accounts List", x + 8, y + 7, 8f, withAlpha(0xFFFFFF, titleAlpha));
        Render2D.blur(x, y, width, height, 0f, 0, ColorUtil.rgba(0, 0, 0, 1));

        float accountListX = x + 5;
        float accountListY = y + 28;
        float accountListWidth = width - 10;
        float accountListHeight = height - 31;

        float cardWidth = (accountListWidth - 5) / 2f;
        float cardHeight = 40;
        float cardGap = 5;

        float scissorScale = guiScale / scale;
        Scissor.enable(accountListX * scale, accountListY * scale, accountListWidth * scale, accountListHeight * scale, scissorScale);

        for (int i = 0; i < accounts.size(); i++) {
            AccountEntry account = accounts.get(i);

            int col = i % 2;
            int row = i / 2;

            float cardX = accountListX + col * (cardWidth + cardGap);
            float cardY = accountListY + row * (cardHeight + cardGap) - scrollOffset;

            if (cardY + cardHeight < accountListY - 10 || cardY > accountListY + accountListHeight + 10) {
                continue;
            }

            renderAccountCard(cardX, cardY, cardWidth, cardHeight, account, contentAlpha,
                    scaledMouseX, scaledMouseY, accountListY, accountListHeight);
        }

        Scissor.disable();

        if (accounts.isEmpty()) {
            Fonts.REGULARNEW.drawCentered("No accounts added", x + width / 2f, y + height / 2f + 2, 6f, withAlpha(0x606878, titleTextAlpha));
        }
    }

    private void renderAccountCard(float x, float y, float width, float height, AccountEntry account,
                                   float contentAlpha, float mouseX, float mouseY,
                                   float listY, float listHeight) {

        int titleAlpha = (int) (contentAlpha * 255);

        boolean cardHovered = isMouseOver(mouseX, mouseY, x, y, width, height)
                && mouseY >= listY && mouseY <= listY + listHeight;

        int cardAlpha = cardHovered ? (int) (contentAlpha * 160) : (int) (contentAlpha * 120);
        int cardTopLeft = withAlpha(0x12151c, cardAlpha);
        int cardTopRight = withAlpha(0x161a22, cardAlpha);
        int cardBottomLeft = withAlpha(0x0e1016, cardAlpha);
        int cardBottomRight = withAlpha(0x12151c, cardAlpha);
        int[] cardColors = {cardTopLeft, cardTopRight, cardBottomRight, cardBottomLeft};

        Render2D.gradientRect(x, y, width, height, cardColors, 4);
        Render2D.blur(x, y, 1, 1, 0f, 0, ColorUtil.rgba(0, 0, 0, 0));

        int cardOutlineColor = withAlpha(0x252a36, (int) (contentAlpha * 80));
        Render2D.outline(x, y, width, height, 0.5f, cardOutlineColor, 4);

        float faceX = x + 7;
        float faceY = y + 7;
        float faceSize = 25;

        Identifier skinTexture = SkinManager.getSkin(account.getName());
        drawPlayerFace(skinTexture, faceX, faceY, faceSize, withAlpha(0xFFFFFF, titleAlpha));

        float textX = faceX + faceSize + 5;
        float nameY = faceY + 2;
        float dateY = nameY + 9;

        String displayName = account.getName();
        float maxNameWidth = width - faceSize - 45;
        if (Fonts.TEST.getWidth(displayName, 7f) > maxNameWidth) {
            while (Fonts.TEST.getWidth(displayName + "...", 7f) > maxNameWidth && displayName.length() > 3) {
                displayName = displayName.substring(0, displayName.length() - 1);
            }
            displayName += "...";
        }

        Fonts.TEST.draw(displayName, textX, nameY, 7f, withAlpha(0xFFFFFF, titleAlpha));
        Fonts.TEST.draw(account.getDate(), textX, dateY, 6f, withAlpha(0x707888, titleAlpha));

        float buttonSize = 12;
        float buttonYPos = y + height - buttonSize - 5;
        float pinButtonX = x + width - buttonSize * 2 - 8;
        float deleteButtonX = x + width - buttonSize - 5;

        boolean pinHovered = isMouseOver(mouseX, mouseY, pinButtonX, buttonYPos, buttonSize, buttonSize)
                && mouseY >= listY && mouseY <= listY + listHeight;
        boolean deleteHovered = isMouseOver(mouseX, mouseY, deleteButtonX, buttonYPos, buttonSize, buttonSize)
                && mouseY >= listY && mouseY <= listY + listHeight;

        int pinBtnAlpha = pinHovered ? (int) (contentAlpha * 220) : (int) (contentAlpha * 160);
        int pinBtnColor;
        int pinOutlineColor;

        if (account.isPinned()) {
            pinBtnColor = withAlpha(0x4a3a10, pinBtnAlpha);
            pinOutlineColor = withAlpha(0xd4a017, (int) (contentAlpha * 180));
        } else {
            pinBtnColor = withAlpha(0x1a1d24, pinBtnAlpha);
            pinOutlineColor = withAlpha(0x353a46, (int) (contentAlpha * 100));
        }

        int[] pinBtnColors = {pinBtnColor, pinBtnColor, pinBtnColor, pinBtnColor};
        Render2D.gradientRect(pinButtonX, buttonYPos, buttonSize, buttonSize, pinBtnColors, 3);
        Render2D.outline(pinButtonX, buttonYPos, buttonSize, buttonSize, 0.5f, pinOutlineColor, 3);
        Render2D.blur(x, y, 1, 1, 0f, 0, ColorUtil.rgba(0, 0, 0, 0));

        int pinIconColor = account.isPinned() ? withAlpha(0xffd700, titleAlpha) : withAlpha(0xc0c8d4, titleAlpha);
        Fonts.MAINMENUSCREEN.drawCentered("c", pinButtonX + buttonSize / 2f, buttonYPos + 1.5f, 9f, pinIconColor);

        int delBtnAlpha = deleteHovered ? (int) (contentAlpha * 200) : (int) (contentAlpha * 140);
        int delBtnColor = deleteHovered ? withAlpha(0x5a2a2a, delBtnAlpha) : withAlpha(0x1a1d24, delBtnAlpha);
        int[] delBtnColors = {delBtnColor, delBtnColor, delBtnColor, delBtnColor};
        Render2D.gradientRect(deleteButtonX, buttonYPos, buttonSize, buttonSize, delBtnColors, 3);
        Render2D.outline(deleteButtonX, buttonYPos, buttonSize, buttonSize, 0.5f, withAlpha(0x353a46, (int) (contentAlpha * 100)), 3);
        Render2D.blur(x, y, 1, 1, 0f, 0, ColorUtil.rgba(0, 0, 0, 0));

        int delIconColor = deleteHovered ? withAlpha(0xff8080, titleAlpha) : withAlpha(0xc0c8d4, titleAlpha);
        Fonts.GUI_ICONS.drawCentered("O", deleteButtonX + buttonSize / 2f, buttonYPos + 0.5f, 11f, delIconColor);
    }

    public void drawPlayerFace(Identifier skin, float x, float y, float size, int color) {
        float u0 = 8f / 64f;
        float v0 = 8f / 64f;
        float u1 = 16f / 64f;
        float v1 = 16f / 64f;

        Render2D.texture(skin, x, y, size, size, u0, v0, u1, v1, color, 0, 3f);

        float hatScale = 1.12f;
        float hatSize = size * hatScale;
        float hatOffset = (hatSize - size) / 2f;

        float hatU0 = 40f / 64f;
        float hatV0 = 8f / 64f;
        float hatU1 = 48f / 64f;
        float hatV1 = 16f / 64f;

        Render2D.texture(skin, x - hatOffset, y - hatOffset, hatSize, hatSize, hatU0, hatV0, hatU1, hatV1, color, 0f, 3f);
    }

    public boolean isMouseOver(float mouseX, float mouseY, float x, float y, float width, float height) {
        return mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
    }

    public int withAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | (MathHelper.clamp(alpha, 0, 255) << 24);
    }
}