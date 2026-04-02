package fun.aegis.display.screens.mainmenu.altscreen;

import com.mojang.blaze3d.systems.RenderSystem;
import fun.aegis.Aegis;
import fun.aegis.mixins.client.IMinecraftClient;
import fun.aegis.display.screens.mainmenu.altscreen.impl.Account;
import fun.aegis.display.screens.mainmenu.altscreen.impl.AccountRepository;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.geometry.Render2D;
import fun.aegis.utils.display.interfaces.QuickImports;
import fun.aegis.utils.display.scissor.ScissorAssist;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.common.animation.Animation;
import fun.aegis.common.animation.Direction;
import fun.aegis.common.animation.implement.InOutBack;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.SocialInteractionsManager;
import net.minecraft.client.session.ProfileKeys;
import net.minecraft.client.session.Session;
import net.minecraft.client.session.report.AbuseReportContext;
import net.minecraft.client.session.report.ReporterEnvironment;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;
import org.joml.Matrix4f;
import org.lwjgl.glfw.GLFW;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.minecraft.UserApiService;

import java.awt.Color;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

public class AltScreen implements QuickImports {
    private final AccountRepository accountRepository = Aegis.getInstance().getAccountRepository();
    private String currentAccount = "";
    private boolean typing = false;
    private String typedText = "";
    private int cursorPos = 0;
    private int selStart = -1;
    private int selEnd = -1;
    private long lastClick = 0;
    private float textXOffset = 0;
    private boolean dragging = false;
    private static final int MIN_LENGTH = 3;
    private static final int MAX_LENGTH = 16;
    private final Random rand = new Random();
    private float scroll = 0f;
    private float smoothedScroll = 0f;
    private float panelX;
    private float panelY;
    private final float panelWidth = 160;
    private final float panelHeight = 210;
    private final Map<String, Animation> accountAnimations = new HashMap<>();
    private final Map<String, Float> accountYPositions = new HashMap<>();
    private final Map<String, Animation> accountRemoveAnimations = new HashMap<>();
    private Animation emptyMessageAnimation = new InOutBack().setValue(1.0).setMs(300);
    private boolean wasEmpty = true;
    private long lastActionTime = 0;
    private static final long ACTION_DELAY = 250;

    public AltScreen(float x, float y) {
        this.panelX = x;
        this.panelY = y;
        this.currentAccount = accountRepository.currentAccount != null ? accountRepository.currentAccount : "";
        initializeAccountAnimations();
        emptyMessageAnimation.setDirection(Direction.FORWARDS);
        emptyMessageAnimation.reset();
        wasEmpty = accountRepository.accountList.isEmpty();
    }

    private void initializeAccountAnimations() {
        for (Account account : accountRepository.accountList) {
            if (!accountAnimations.containsKey(account.uuid)) {
                Animation anim = new InOutBack().setValue(1.0).setMs(300);
                anim.setDirection(Direction.FORWARDS);
                anim.reset();
                accountAnimations.put(account.uuid, anim);
            }
        }
    }

    public void updatePosition(float x, float y) {
        float deltaY = y - this.panelY;
        this.panelX = x;
        this.panelY = y;

        for (String key : accountYPositions.keySet()) {
            accountYPositions.put(key, accountYPositions.get(key) + deltaY);
        }
    }

    public void tick() {
        for (Account account : accountRepository.accountList) {
            float target = account.starred ? 1f : 0f;
            account.starAnim += (target - account.starAnim) * 0.2f;

            if (!accountAnimations.containsKey(account.uuid)) {
                Animation anim = new InOutBack().setValue(1.0).setMs(300);
                anim.setDirection(Direction.FORWARDS);
                anim.reset();
                accountAnimations.put(account.uuid, anim);
            }
        }

        accountAnimations.keySet().removeIf(uuid -> {
            boolean exists = accountRepository.accountList.stream().anyMatch(acc -> acc.uuid.equals(uuid));
            if (!exists) {
                accountYPositions.remove(uuid);
            }
            return !exists;
        });

        boolean isEmpty = accountRepository.accountList.isEmpty();
        if (isEmpty != wasEmpty) {
            wasEmpty = isEmpty;
            if (isEmpty) {
                emptyMessageAnimation.setDirection(Direction.FORWARDS);
            } else {
                emptyMessageAnimation.setDirection(Direction.BACKWARDS);
            }
            emptyMessageAnimation.reset();
        }
    }

    public void render(DrawContext context, Color buttonColor, Color outlineColor, Color gradientColor, Color textColor, Color bgColor) {
        rectangle.render(ShapeProperties.create(context.getMatrices(), panelX + panelWidth / 2 - 22, panelY - 13.5f, 43, 12)
                .thickness(2).round(3)
                .outlineColor(outlineColor.getRGB())
                .color(buttonColor.getRGB(), buttonColor.getRGB(), gradientColor.getRGB(), gradientColor.getRGB()).build());
        Fonts.getSize(16, Fonts.Type.DEFAULT).drawCenteredString(context.getMatrices(), "Аккаунты", panelX + panelWidth / 2, panelY - 10, textColor.getRGB());

        rectangle.render(ShapeProperties.create(context.getMatrices(), panelX, panelY, panelWidth, panelHeight)
                .thickness(2).round(10)
                .outlineColor(outlineColor.getRGB())
                .color(buttonColor.getRGB(), buttonColor.getRGB(), gradientColor.getRGB(), gradientColor.getRGB()).build());

        renderTextField(context, buttonColor, outlineColor, gradientColor, textColor);
        renderAccountList(context, buttonColor, outlineColor, gradientColor, textColor);

        String displayAccount = currentAccount.isEmpty() ? "Не выбран" : currentAccount;
        String currentText = "Текущий аккаунт » " + displayAccount;
        float currentWidth = Fonts.getSize(15, Fonts.Type.SEMI).getStringWidth(currentText) + 20;
        float currentX = panelX + panelWidth / 2 - currentWidth / 2;
        rectangle.render(ShapeProperties.create(context.getMatrices(), currentX, panelY + panelHeight + 2, currentWidth, 12)
                .thickness(2).round(3)
                .outlineColor(outlineColor.getRGB())
                .color(buttonColor.getRGB(), buttonColor.getRGB(), gradientColor.getRGB(), gradientColor.getRGB()).build());
        Fonts.getSize(15, Fonts.Type.SEMI).drawCenteredString(context.getMatrices(), currentText, panelX + panelWidth / 2, panelY + panelHeight + 6, textColor.getRGB());
    }

    private void renderTextField(DrawContext context, Color buttonColor, Color outlineColor, Color gradientColor, Color textColor) {
        rectangle.render(ShapeProperties.create(context.getMatrices(), panelX + 5, panelY + 185, panelWidth - 11, 20)
                .thickness(2).round(6).outlineColor(outlineColor.getRGB())
                .color(buttonColor.getRGB(), buttonColor.getRGB(), gradientColor.getRGB(), gradientColor.getRGB()).build());

        rectangle.render(ShapeProperties.create(context.getMatrices(), panelX + 5, panelY + 185, panelWidth - 11, 1)
                .thickness(2).round(5).outlineColor(outlineColor.getRGB())
                .color(new Color(buttonColor.getRed(), buttonColor.getGreen(), buttonColor.getBlue(), 5).getRGB(),
                        new Color(buttonColor.getRed(), buttonColor.getGreen(), buttonColor.getBlue(), textColor.getAlpha()).getRGB(),
                        new Color(gradientColor.getRed(), gradientColor.getGreen(), gradientColor.getBlue(), textColor.getAlpha()).getRGB(),
                        new Color(gradientColor.getRed(), gradientColor.getGreen(), gradientColor.getBlue(), 5).getRGB()).build());

        rectangle.render(ShapeProperties.create(context.getMatrices(), panelX + panelWidth - 25, panelY + 187.5f, 15, 15)
                .thickness(2).round(4).outlineColor(outlineColor.getRGB())
                .color(buttonColor.getRGB(), buttonColor.getRGB(), gradientColor.getRGB(), gradientColor.getRGB()).build());

        Fonts.getSize(24, Fonts.Type.ICONS).drawString(context.getMatrices(), "R", panelX + panelWidth - 24.5f, panelY + 192, textColor.getRGB());

        float textFieldX = panelX + 5;
        float textFieldY = panelY + 177;
        float textFieldWidth = panelWidth - 11;
        float textFieldHeight = 20;
        var font = Fonts.getSize(16, Fonts.Type.DEFAULT);
        long currentTime = System.currentTimeMillis();
        boolean blink = (currentTime % 1000 < 500);

        context.enableScissor((int) (textFieldX + 3), (int) textFieldY, (int) (textFieldX + textFieldWidth - 3), (int) (textFieldY + textFieldHeight) + 5);

        if (typing && hasSelection()) {
            int start = Math.min(selStart, selEnd);
            int end = Math.max(selStart, selEnd);
            float selXStart = textFieldX + 5 - textXOffset + font.getStringWidth(typedText.substring(0, start));
            float selWidth = font.getStringWidth(typedText.substring(start, end));
            Color selColor = new Color(85, 133, 232, textColor.getAlpha());
            rectangle.render(ShapeProperties.create(context.getMatrices(), selXStart, textFieldY + 13.5f, selWidth, textFieldHeight - 10).color(selColor.getRGB()).build());
        }

        if (!typedText.isEmpty() || typing) {
            font.drawString(context.getMatrices(), typedText, textFieldX + 5 - textXOffset, textFieldY + 16, textColor.getRGB());
        } else {
            font.drawString(context.getMatrices(), "Введите никнейм", textFieldX + 5, textFieldY + 16, textColor.getRGB());
        }

        if (typing && blink && !hasSelection()) {
            float cursorX = textFieldX + 5 - textXOffset + font.getStringWidth(typedText.substring(0, cursorPos));
            rectangle.render(ShapeProperties.create(context.getMatrices(), cursorX + 1, textFieldY + 15f, 0.5f, textFieldHeight - 13).color(textColor.getRGB()).build());
        }

        context.disableScissor();
    }

    private void renderAccountList(DrawContext context, Color buttonColor, Color outlineColor, Color gradientColor, Color textColor) {
        float accountSpacing = 25;
        MatrixStack matrix = context.getMatrices();
        Matrix4f positionMatrix = matrix.peek().getPositionMatrix();
        ScissorAssist scissorManager = Aegis.getInstance().getScissorManager();
        float listY = panelY + 5;
        float listHeight = panelHeight - 31;

        scissorManager.push(positionMatrix, panelX, listY, panelWidth, listHeight);
        smoothedScroll = MathHelper.lerp(0.1f, smoothedScroll, scroll);

        if (accountRepository.accountList.isEmpty()) {
            Fonts.getSize(16, Fonts.Type.DEFAULT).drawCenteredString(context.getMatrices(), "Пусто    ", panelX + panelWidth / 2 - 5, panelY + panelHeight / 2 - 10, textColor.getRGB());
            Fonts.getSize(36, Fonts.Type.ICONS).drawCenteredString(context.getMatrices(), "   W", panelX + panelWidth / 2 + 7, panelY + panelHeight / 2 - 16, textColor.getRGB());
        } else {
            for (int i = 0; i < accountRepository.accountList.size(); i++) {
                Account account = accountRepository.accountList.get(i);
                float targetY = panelY + 10 + i * accountSpacing - smoothedScroll;

                String key = account.uuid;
                accountYPositions.putIfAbsent(key, targetY);
                float currentY = accountYPositions.get(key);
                currentY = MathHelper.lerp(0.15f, currentY, targetY);
                accountYPositions.put(key, currentY);

                Animation anim = accountAnimations.get(account.uuid);
                Animation removeAnim = accountRemoveAnimations.get(account.uuid);
                if (anim == null) continue;

                float animProgress = anim.getOutput().floatValue();
                if (removeAnim != null) {
                    animProgress *= removeAnim.getOutput().floatValue();
                }

                float scale = 0.5f + animProgress * 0.5f;
                int alpha = (int) (textColor.getAlpha() * animProgress);

                if (currentY + 20 >= listY && currentY <= listY + listHeight) {
                    matrix.push();
                    float centerX = panelX + panelWidth / 2;
                    float centerY = currentY + 10;
                    matrix.translate(centerX, centerY, 0);
                    matrix.scale(scale, scale, 1);
                    matrix.translate(-centerX, -centerY, 0);

                    int clampedAlpha = Math.max(0, Math.min(255, alpha));
                    Color animButtonColor = new Color(buttonColor.getRed(), buttonColor.getGreen(), buttonColor.getBlue(), clampedAlpha);
                    Color animGradientColor = new Color(gradientColor.getRed(), gradientColor.getGreen(), gradientColor.getBlue(), clampedAlpha);
                    Color animOutlineColor = new Color(outlineColor.getRed(), outlineColor.getGreen(), outlineColor.getBlue(), clampedAlpha);
                    Color animTextColor = new Color(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), clampedAlpha);

                    rectangle.render(ShapeProperties.create(context.getMatrices(), panelX + 5, currentY, panelWidth - 11, 20)
                            .thickness(2).round(5).outlineColor(animOutlineColor.getRGB())
                            .color(animButtonColor.getRGB(), animButtonColor.getRGB(), animGradientColor.getRGB(), animGradientColor.getRGB()).build());

                    rectangle.render(ShapeProperties.create(context.getMatrices(), panelX + 5, currentY, panelWidth - 11, 1)
                            .thickness(2).round(5).outlineColor(animOutlineColor.getRGB())
                            .color(new Color(buttonColor.getRed(), buttonColor.getGreen(), buttonColor.getBlue(), Math.max(0, Math.min(255, (5 * clampedAlpha / 255)))).getRGB(),
                                    new Color(buttonColor.getRed(), buttonColor.getGreen(), buttonColor.getBlue(), clampedAlpha).getRGB(),
                                    new Color(gradientColor.getRed(), gradientColor.getGreen(), gradientColor.getBlue(), clampedAlpha).getRGB(),
                                    new Color(gradientColor.getRed(), gradientColor.getGreen(), gradientColor.getBlue(), Math.max(0, Math.min(255, (5 * clampedAlpha / 255)))).getRGB()).build());

                    Color starColor = interpolateColor(animTextColor, new Color(255, 255, 0, clampedAlpha), account.starAnim);
                    Color faceOutline = new Color(64, 64, 64, clampedAlpha);
                    rectangle.render(ShapeProperties.create(context.getMatrices(), panelX + 9.5f, currentY + 2.5, 16, 16)
                            .thickness(4).round(8).outlineColor(faceOutline.getRGB())
                            .color(animButtonColor.getRGB(), animButtonColor.getRGB(), animGradientColor.getRGB(), animGradientColor.getRGB()).build());

                    Fonts.getSize(25, Fonts.Type.ICONS).drawString(context.getMatrices(), "★", panelX + panelWidth - 23.5f, currentY + 4.5f, starColor.getRGB());
                    drawAccountFace(context, account, panelX + 10, currentY + 3, alpha);
                    Fonts.getSize(15, Fonts.Type.SEMI).drawString(context.getMatrices(), account.name, panelX + 28, currentY + 8.5f, animTextColor.getRGB());

                    matrix.pop();
                }
            }
        }

        scissorManager.pop();

        if (accountRepository.accountList.size() > 7) {
            renderScrollbar(context, listY, listHeight, accountSpacing, textColor.getAlpha());
        }
    }

    private void renderScrollbar(DrawContext context, float listY, float listHeight, float accountSpacing, int alpha) {
        float contentHeight = accountRepository.accountList.size() * accountSpacing;
        float maxScroll = Math.max(0, contentHeight - listHeight);
        scroll = MathHelper.clamp(scroll, 0, maxScroll);

        float scrollbarWidth = 2;
        float scrollbarX = panelX + panelWidth - scrollbarWidth - 2.5f;
        float scrollbarY = listY + 1;
        float scrollbarHeight = listHeight - 1;

        Color bgScrollColor = new Color(30, 30, 30, (int) (100 * (alpha / 255.0)));
        rectangle.render(ShapeProperties.create(context.getMatrices(), scrollbarX, scrollbarY, scrollbarWidth, scrollbarHeight)
                .round(1f).color(bgScrollColor.getRGB()).build());

        float handleHeight = Math.max(20, listHeight * (listHeight / (contentHeight + listHeight)));
        float scrollRatio = smoothedScroll / maxScroll;
        float handleY = scrollbarY + (scrollbarHeight - handleHeight) * scrollRatio;

        Color handleColor = new Color(100, 100, 100, (int) (150 * (alpha / 255.0)));
        rectangle.render(ShapeProperties.create(context.getMatrices(), scrollbarX, handleY, scrollbarWidth, handleHeight)
                .round(1f).color(handleColor.getRGB()).build());
    }

    private void drawAccountFace(DrawContext context, Account account, float x, float y, int alpha) {
        GameProfile profile = new GameProfile(UUID.fromString(account.uuid), account.name);
        Identifier skinTexture = MinecraftClient.getInstance().getSkinProvider().getSkinTextures(profile).texture();
        if (skinTexture == null) {
            skinTexture = Identifier.of("minecraft", "textures/entity/steve.png");
        }

        MatrixStack matrices = context.getMatrices();
        matrices.push();
        RenderSystem.enableBlend();
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha / 255.0f);
        Render2D.drawTexture(context, skinTexture, x, y, 15, 7, 8, 8, 64, ColorAssist.getRect(1), -1);
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
        RenderSystem.disableBlend();
        matrices.pop();
    }

    private Color interpolateColor(Color start, Color end, float t) {
        int r = (int) (start.getRed() + (end.getRed() - start.getRed()) * t);
        int g = (int) (start.getGreen() + (end.getGreen() - start.getGreen()) * t);
        int b = (int) (start.getBlue() + (end.getBlue() -start.getBlue()) * t);
        int a = (int) (start.getAlpha() + (end.getAlpha() - start.getAlpha()) * t);
        return new Color(r, g, b, a);
    }

    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        float textFieldX = panelX + 5;
        float textFieldY = panelY + 177;

        if (button == 0 && isInBounds(mouseX, mouseY, textFieldX, textFieldY + 8, panelWidth - 30, 20)) {
            handleTextFieldClick(mouseX);
            return true;
        } else {
            typing = false;
            clearSelection();
        }

        if (button == 0 && isInBounds(mouseX, mouseY, panelX + panelWidth - 25, panelY + 187.5f, 15, 15)) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastActionTime >= ACTION_DELAY) {
                lastActionTime = currentTime;
                addRandomAccount();
            }
            return true;
        }

        return handleAccountListClick(mouseX, mouseY, button);
    }

    private void handleTextFieldClick(double mouseX) {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastClick < 250) {
            selStart = 0;
            selEnd = typedText.length();
        } else {
            typing = true;
            cursorPos = getCursorIndexAt(mouseX);
            selStart = cursorPos;
            selEnd = cursorPos;
            lastClick = currentTime;
        }
        dragging = true;
    }

    private void addRandomAccount() {
        String username = generateRandomUsername();

        if (accountExists(username)) {
            return;
        }

        String offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + username).getBytes(StandardCharsets.UTF_8)).toString();
        Account newAccount = new Account(username, false, false, null, offlineUuid, "0");
        accountRepository.accountList.add(newAccount);
        accountRepository.accountList.sort((a1, a2) -> Boolean.compare(a2.starred, a1.starred));

        Animation anim = new InOutBack().setValue(1.0).setMs(300);
        anim.setDirection(Direction.FORWARDS);
        anim.reset();
        accountAnimations.put(offlineUuid, anim);

        typedText = "";
        cursorPos = 0;
        clearSelection();
        saveAccounts();
    }

    private boolean accountExists(String username) {
        return accountRepository.accountList.stream()
                .anyMatch(account -> account.name.equalsIgnoreCase(username));
    }

    private String generateRandomUsername() {
        char[] vowels = {'a', 'e', 'i', 'o', 'u'};
        char[] consonants = {'b', 'c', 'd', 'f', 'g', 'h', 'j', 'k', 'l', 'm', 'n', 'p', 'r', 's', 't', 'v', 'w', 'x', 'y', 'z'};
        StringBuilder username = new StringBuilder();
        int length = 6 + rand.nextInt(5);
        boolean startWithVowel = rand.nextBoolean();

        for (int i = 0; i < length; i++) {
            if (i % 2 == 0) {
                username.append(startWithVowel ? vowels[rand.nextInt(vowels.length)] : consonants[rand.nextInt(consonants.length)]);
            } else {
                username.append(startWithVowel ? consonants[rand.nextInt(consonants.length)] : vowels[rand.nextInt(vowels.length)]);
            }
        }

        if (rand.nextInt(100) < 30) {
            username.append(rand.nextInt(100));
        }

        String result = username.substring(0, 1).toUpperCase() + username.substring(1);
        String finalResult = result;
        if (accountRepository.accountList.stream().anyMatch(account -> account.name.equals(finalResult))) {
            result += System.currentTimeMillis() % 1000;
        }
        return result;
    }

    private boolean handleAccountListClick(double mouseX, double mouseY, int button) {
        float accountSpacing = 25;
        float listY = panelY + 5;
        float listHeight = panelHeight - 31;

        for (int i = 0; i < accountRepository.accountList.size(); i++) {
            Account account = accountRepository.accountList.get(i);
            float accY = accountYPositions.getOrDefault(account.uuid, panelY + 10 + i * accountSpacing - smoothedScroll);

            if (accY + 20 < listY || accY > listY + listHeight) continue;

            if (button == 0 && isInBounds(mouseX, mouseY, panelX + panelWidth - 25, accY + 6.5f, 15, 15)) {
                account.starred = !account.starred;
                accountRepository.accountList.sort((a1, a2) -> Boolean.compare(a2.starred, a1.starred));
                saveAccounts();
                return true;
            }

            if (button == 0 && isInBounds(mouseX, mouseY, panelX + 5, accY, panelWidth - 11, 20)) {
                currentAccount = account.name;
                accountRepository.currentAccount = account.name;
                setSession(account);
                saveAccounts();
                return true;
            }

            if (button == 1 && isInBounds(mouseX, mouseY, panelX + 5, accY, panelWidth - 11, 20)) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastActionTime >= ACTION_DELAY) {
                    lastActionTime = currentTime;
                    Account accountToRemove = accountRepository.accountList.get(i);

                    if (accountToRemove.name.equals(currentAccount)) {
                        accountRepository.currentAccount = "";
                        currentAccount = "";
                    }

                    Animation removeAnim = new InOutBack().setValue(1.0).setMs(250);
                    removeAnim.setDirection(Direction.BACKWARDS);
                    removeAnim.reset();
                    accountRemoveAnimations.put(accountToRemove.uuid, removeAnim);

                    new Thread(() -> {
                        try {
                            Thread.sleep(250);
                            accountRepository.accountList.remove(accountToRemove);
                            accountYPositions.remove(accountToRemove.uuid);
                            accountAnimations.remove(accountToRemove.uuid);
                            accountRemoveAnimations.remove(accountToRemove.uuid);
                            saveAccounts();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                }
                return true;
            }
        }
        return false;
    }

    private void setSession(Account account) {
        Session newSession = new Session(account.name, UUID.fromString(account.uuid), "0", Optional.empty(), Optional.empty(), Session.AccountType.MOJANG);
        IMinecraftClient mca = (IMinecraftClient) MinecraftClient.getInstance();
        mca.setSessionT(newSession);
        MinecraftClient.getInstance().getGameProfile().getProperties().clear();
        UserApiService apiService = UserApiService.OFFLINE;
        mca.setUserApiService(apiService);
        mca.setSocialInteractionsManagerT(new SocialInteractionsManager(MinecraftClient.getInstance(), apiService));
        mca.setProfileKeys(ProfileKeys.create(apiService, newSession, MinecraftClient.getInstance().runDirectory.toPath()));
        mca.setAbuseReportContextT(AbuseReportContext.create(ReporterEnvironment.ofIntegratedServer(), apiService));

        accountRepository.currentAccount = account.name;
        currentAccount = account.name;
    }

    private void saveAccounts() {
        try {
            Aegis.getInstance().getFileController().saveFiles();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean mouseScrolled(double mouseX, double mouseY, double vertical) {
        if (accountRepository.accountList.size() > 7) {
            float listY = panelY + 5;
            float listHeight = panelHeight - 31;

            if (isInBounds(mouseX, mouseY, panelX, listY, panelWidth, listHeight)) {
                float contentHeight = accountRepository.accountList.size() * 25f;
                float maxScroll = Math.max(0, contentHeight - listHeight);
                scroll -= vertical * 20;
                scroll = MathHelper.clamp(scroll, 0, maxScroll);
                return true;
            }
        }
        return false;
    }

    public boolean mouseDragged(double mouseX, double mouseY, int button) {
        if (dragging && button == 0) {
            cursorPos = getCursorIndexAt(mouseX);
            selEnd = cursorPos;
            return true;
        }
        return false;
    }

    public boolean mouseReleased() {
        dragging = false;
        return false;
    }

    public boolean charTyped(char chr) {
        if (typing && typedText.length() < MAX_LENGTH) {
            deleteSelectedText();
            typedText = typedText.substring(0, cursorPos) + chr + typedText.substring(cursorPos);
            cursorPos++;
            clearSelection();
            updateTextXOffset();
            return true;
        }
        return false;
    }

    public boolean keyPressed(int keyCode) {
        if (typing) {
            if (keyCode == GLFW.GLFW_KEY_LEFT_CONTROL || keyCode == GLFW.GLFW_KEY_RIGHT_CONTROL) {
                return false;
            }
            if (MinecraftClient.getInstance().currentScreen != null && MinecraftClient.getInstance().currentScreen.hasControlDown()) {
                switch (keyCode) {
                    case GLFW.GLFW_KEY_A:
                        selStart = 0;
                        selEnd = typedText.length();
                        return true;
                    case GLFW.GLFW_KEY_C:
                        if (hasSelection()) {
                            GLFW.glfwSetClipboardString(MinecraftClient.getInstance().getWindow().getHandle(), getSelectedText());
                        }
                        return true;
                    case GLFW.GLFW_KEY_V:
                        String clipboard = GLFW.glfwGetClipboardString(MinecraftClient.getInstance().getWindow().getHandle());
                        if (clipboard != null) {
                            deleteSelectedText();
                            typedText = typedText.substring(0, cursorPos) + clipboard + typedText.substring(cursorPos);
                            cursorPos += clipboard.length();
                            clearSelection();
                            updateTextXOffset();
                        }
                        return true;
                }
            }
            switch (keyCode) {
                case GLFW.GLFW_KEY_BACKSPACE:
                    if (hasSelection()) {
                        deleteSelectedText();
                    } else if (cursorPos > 0) {
                        typedText = typedText.substring(0, cursorPos - 1) + typedText.substring(cursorPos);
                        cursorPos--;
                    }
                    updateTextXOffset();
                    return true;
                case GLFW.GLFW_KEY_LEFT:
                    if (cursorPos > 0) cursorPos--;
                    updateSelectionAfterMove();
                    updateTextXOffset();
                    return true;
                case GLFW.GLFW_KEY_RIGHT:
                    if (cursorPos < typedText.length()) cursorPos++;
                    updateSelectionAfterMove();
                    updateTextXOffset();
                    return true;
                case GLFW.GLFW_KEY_ENTER:
                    if (typedText.length() >= MIN_LENGTH && typedText.length() <= MAX_LENGTH) {
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - lastActionTime >= ACTION_DELAY) {
                            if (accountExists(typedText)) {
                                typedText = "";
                                cursorPos = 0;
                                typing = false;
                                clearSelection();
                                return true;
                            }

                            lastActionTime = currentTime;
                            String offlineUuid = UUID.nameUUIDFromBytes(("OfflinePlayer:" + typedText).getBytes(StandardCharsets.UTF_8)).toString();
                            Account newAccount = new Account(typedText, false, false, null, offlineUuid, "0");
                            accountRepository.accountList.add(newAccount);
                            accountRepository.accountList.sort((a1, a2) -> Boolean.compare(a2.starred, a1.starred));

                            Animation anim = new InOutBack().setValue(1.0).setMs(300);
                            anim.setDirection(Direction.FORWARDS);
                            anim.reset();
                            accountAnimations.put(offlineUuid, anim);

                            typedText = "";
                            cursorPos = 0;
                            typing = false;
                            clearSelection();
                            saveAccounts();
                        }
                    }
                    return true;
            }
        }
        return false;
    }

    public void reset() {
        typing = false;
        clearSelection();
    }

    private boolean isInBounds(double mx, double my, float x, float y, float w, float h) {
        return mx >= x && mx <= x + w && my >= y && my <= y + h;
    }

    private boolean hasSelection() {
        return selStart != selEnd;
    }

    private String getSelectedText() {
        int start = Math.min(selStart, selEnd);
        int end = Math.max(selStart, selEnd);
        return typedText.substring(start, end);
    }

    private void deleteSelectedText() {
        if (hasSelection()) {
            int start = Math.min(selStart, selEnd);
            int end = Math.max(selStart, selEnd);
            typedText = typedText.substring(0, start) + typedText.substring(end);
            cursorPos = start;
            clearSelection();
        }
    }

    private void clearSelection() {
        selStart = cursorPos;
        selEnd = cursorPos;
    }

    private void updateSelectionAfterMove() {
        if (MinecraftClient.getInstance().currentScreen != null && MinecraftClient.getInstance().currentScreen.hasShiftDown()) {
            selEnd = cursorPos;
        } else {
            clearSelection();
        }
    }

    private int getCursorIndexAt(double mouseX) {
        float textFieldX = panelX + 5;
        var font = Fonts.getSize(16, Fonts.Type.DEFAULT);
        float relX = (float) mouseX - textFieldX - 3 + textXOffset;
        int pos = 0;
        for (; pos < typedText.length(); pos++) {
            if (font.getStringWidth(typedText.substring(0, pos + 1)) > relX) {
                break;
            }
        }
        return pos;
    }

    private void updateTextXOffset() {
        float textFieldWidth = panelWidth - 11;
        var font = Fonts.getSize(16, Fonts.Type.DEFAULT);
        float cursorX = font.getStringWidth(typedText.substring(0, cursorPos));
        if (cursorX < textXOffset) {
            textXOffset = cursorX;
        } else if (cursorX > textXOffset + textFieldWidth - 10) {
            textXOffset = cursorX - (textFieldWidth - 10);
        }
    }
}
