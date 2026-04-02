package fun.aegis.display.hud;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import fun.aegis.features.impl.misc.FreeCam;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import fun.aegis.utils.client.managers.api.draggable.AbstractDraggable;
import fun.aegis.common.animation.Animation;
import fun.aegis.common.animation.Direction;
import fun.aegis.common.animation.implement.Decelerate;
import fun.aegis.utils.display.font.FontRenderer;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.shape.implement.LiquidGlass;
import fun.aegis.utils.display.geometry.Render2D;
import fun.aegis.utils.display.scissor.ScissorAssist;
import fun.aegis.utils.display.audio.AudioVisualizer;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import fun.aegis.utils.client.Instance;
import fun.aegis.Aegis;
import fun.aegis.features.impl.render.Hud;
import fun.aegis.utils.client.packet.network.Network;
import net.minecraft.entity.player.PlayerEntity;
import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DynamicIsland extends AbstractDraggable {
    private final Animation internetAnimation = new Decelerate().setMs(300).setValue(1);
    private final Animation mediaAnimation = new Decelerate().setMs(300).setValue(1);
    private final Animation pvpAnimation = new Decelerate().setMs(300).setValue(1);
    private final Animation barAnimation = new Decelerate().setMs(300).setValue(1);
    private final Animation moduleAnimation = new Decelerate().setMs(400).setValue(1);
    private final Animation moduleScaleAnimation = new Decelerate().setMs(400).setValue(1);
    private final Animation moduleFadeAnimation = new Decelerate().setMs(350).setValue(1);
    private final Animation freeCamAnimation = new Decelerate().setMs(300).setValue(1);
    private final Animation widthAnimation = new Decelerate().setMs(400).setValue(1);
    private final Animation heightAnimation = new Decelerate().setMs(400).setValue(1);
    private LiquidGlass liquidGlass = new LiquidGlass();
    
    private float currentWidth = 100f;
    private float targetWidth = 100f;
    
    private float[] targetBarHeights = new float[]{7f, 5.5f, 4f};
    private float[] currentBarHeights = new float[]{7f, 5.5f, 4f};
    private long lastUpdateTime = 0;

    private String currentModuleNotification = "";
    private String currentModuleNotificationClean = "";
    private long moduleNotificationTime = 0;
    private final long MODULE_NOTIFICATION_DURATION = 2000;
    
    // Для свапа предметов
    private ItemStack swapItemStack = null;
    private String swapItemName = "";
    private long swapNotificationTime = 0;
    private final long SWAP_NOTIFICATION_DURATION = 1500;
    private final Animation swapAnimation = new Decelerate().setMs(300).setValue(1);
    
    // Для AutoTotem Alert
    private String totemAlertReason = "";
    private long totemAlertTime = 0;
    private final long TOTEM_ALERT_DURATION = 1500;
    private final Animation totemAnimation = new Decelerate().setMs(300).setValue(1);
    
    // Для Elytra Swap
    private ItemStack elytraSwapStack = null;
    private String elytraSwapName = "";
    private long elytraSwapTime = 0;
    private final long ELYTRA_SWAP_DURATION = 1500;
    private final Animation elytraSwapAnimation = new Decelerate().setMs(300).setValue(1);
    
    private final Animation chatBackgroundAnimation = new Decelerate().setMs(200).setValue(0);
    
    private final AudioVisualizer audioVisualizer = new AudioVisualizer();
    
    private boolean showMusicControls = false;
    private final Animation musicControlsAnimation = new Decelerate().setMs(300).setValue(0);
    
    private static final Pattern PVP_TIMER_PATTERN = Pattern.compile("(\\d+)");

    private String trackName = null;
    private String artistsText = null;
    private float progress = 0.0f;
    private long currentTime = 0;
    private long totalTime = 1;
    private boolean isPlaying = false;
    private IMediaSession activeSession = null;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean polling = new AtomicBoolean(false);
    private volatile long lastPollMs = 0L;

    private final Identifier coverTextureLocation = Identifier.of("aegis", "music_cover_di");
    private NativeImageBackedTexture coverTexture = null;
    private int coverHash = 0;
    
    // Hover для расширенного режима музыки
    private boolean isHovered = false;
    private final Animation hoverAnimation = new Decelerate().setMs(300).setValue(0);
    private final Animation heightExpandAnimation = new Decelerate().setMs(300).setValue(0);
    private float currentHeight = 13f;
    private float targetHeight = 13f;
    
    // Новые игроки в радиусе
    private String newPlayerName = "";
    private long newPlayerTime = 0;
    private final long NEW_PLAYER_NOTIFICATION_DURATION = 2000;
    private final Animation newPlayerAnimation = new Decelerate().setMs(300).setValue(1);
    private final Set<String> knownPlayers = new HashSet<>();
    
    // Режим информации о сервере (при клике когда нет музыки)
    private boolean serverInfoMode = false;
    private final Animation serverInfoAnimation = new Decelerate().setMs(300).setValue(0);
    private long lastClickTime = 0;
    private int lastFps = 0;
    private float lastTps = 20.0f;
    private static final int PLAYER_DETECTION_RADIUS = 50;
    private static final int MAX_PLAYERS_FOR_NOTIFICATION = 4;

    public static DynamicIsland getInstance() {
        return Instance.getDraggable(DynamicIsland.class);
    }

    public DynamicIsland() {
        super("Dynamic Island", 0, 4, 100, 16, false);
    }

    @Override
    public void tick() {
        if (fullNullCheck()) return;

        audioVisualizer.update();

        checkNearbyPlayers();

        long now = System.currentTimeMillis();
        if (now - lastPollMs < 200L) {
            updateAnimationsAndWidth();
            return;
        }
        lastPollMs = now;

        if (!polling.compareAndSet(false, true)) {
            updateAnimationsAndWidth();
            return;
        }

        executor.execute(() -> {
            try {
                IMediaSession session = null;
                try {
                    session = MediaPlayerInfo.Instance.getMediaSessions().stream()
                            .max(Comparator.comparing(s -> s.getMedia().getPlaying()))
                            .orElse(null);
                } catch (UnsatisfiedLinkError | ExceptionInInitializerError e) {
                    polling.set(false);
                    return;
                }

                if (session != null) {
                    final IMediaSession finalSession = session;
                    MediaInfo info = session.getMedia();
                    if (info != null && !info.getTitle().isEmpty()) {
                        String newTrackName = info.getTitle();
                        String newArtistsText = (info.getArtist() != null && !info.getArtist().isEmpty())
                                ? info.getArtist()
                                : null;

                        long newCurrentTime = Math.max(0L, info.getPosition());
                        long newTotalTime = info.getDuration() > 0 ? info.getDuration() : 1;
                        boolean newPlaying = info.getPlaying();

                        byte[] newCover = info.getArtworkPng();
                        int newCoverHash = 0;
                        NativeImage decodedImage = null;

                        if (newCover != null && newCover.length > 0) {
                            try {
                                newCoverHash = Arrays.hashCode(newCover);
                                decodedImage = NativeImage.read(new ByteArrayInputStream(newCover));
                            } catch (Exception ignored) {
                                decodedImage = null;
                                newCoverHash = 0;
                            }
                        }

                        NativeImage finalDecodedImage = decodedImage;
                        int finalCoverHash = newCoverHash;
                        mc.execute(() -> {
                            activeSession = finalSession;
                            trackName = newTrackName;
                            artistsText = newArtistsText;
                            totalTime = newTotalTime;
                            currentTime = newCurrentTime;
                            progress = (float) newCurrentTime / (float) newTotalTime;
                            isPlaying = newPlaying;

                            if (newCover == null || newCover.length == 0) {
                                clearCoverTexture();
                                coverHash = 0;
                            } else {
                                if (finalDecodedImage != null) {
                                    if (finalCoverHash != coverHash) {
                                        updateCoverTexture(finalDecodedImage);
                                        coverHash = finalCoverHash;
                                    } else {
                                        try {
                                            finalDecodedImage.close();
                                        } catch (Exception ignored) {
                                        }
                                    }
                                } else {
                                    clearCoverTexture();
                                    coverHash = 0;
                                }
                            }
                        });
                    } else {
                        mc.execute(this::clearData);
                    }
                } else {
                    mc.execute(this::clearData);
                }
            } catch (Throwable e) {
                mc.execute(this::clearData);
            } finally {
                polling.set(false);
            }
        });

        updateAnimationsAndWidth();
    }

    private void updateAnimationsAndWidth() {
        int ping = 0;
        if (mc.player != null && mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null) {
            ping = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency();
        }

        boolean isChatOpen = mc.currentScreen instanceof ChatScreen;
        chatBackgroundAnimation.setDirection(isChatOpen ? Direction.FORWARDS : Direction.BACKWARDS);

        boolean isPvp = Network.isPvp();
        boolean mediaNull = (trackName == null || trackName.isEmpty());
        boolean hasActiveMusic = !mediaNull && !isPvp;
        
        boolean showControls = isChatOpen && !mediaNull && isHovered;
        musicControlsAnimation.setDirection(showControls ? Direction.FORWARDS : Direction.BACKWARDS);

        internetAnimation.setDirection(ping < 1000 ? Direction.FORWARDS : Direction.BACKWARDS);
        if (isPvp) {
            mediaAnimation.setDirection(Direction.BACKWARDS);
        } else {
            mediaAnimation.setDirection(mediaNull ? Direction.BACKWARDS : Direction.FORWARDS);
        }
        pvpAnimation.setDirection(isPvp ? Direction.FORWARDS : Direction.BACKWARDS);
        barAnimation.setDirection(hasActiveMusic ? Direction.FORWARDS : Direction.BACKWARDS);

        if (hasActiveMusic && System.currentTimeMillis() - lastUpdateTime > 100) {
            updateBarHeights();
            lastUpdateTime = System.currentTimeMillis();
        }

        for (int i = 0; i < 3; i++) {
            currentBarHeights[i] = lerp(currentBarHeights[i], targetBarHeights[i], 0.3f);
        }

        boolean showModuleNotification = !currentModuleNotification.isEmpty() &&
                System.currentTimeMillis() - moduleNotificationTime < MODULE_NOTIFICATION_DURATION;
        boolean showSwapNotification = swapItemStack != null && !swapItemName.isEmpty() &&
                System.currentTimeMillis() - swapNotificationTime < SWAP_NOTIFICATION_DURATION;
        boolean isTotemAlertActive = showTotemAlert();
        boolean isElytraSwapActive = showElytraSwap();
        
        // Проверяем FreeCam
        FreeCam freeCam = FreeCam.getInstance();
        boolean isFreeCamActive = freeCam != null && freeCam.isState() && freeCam.pos != null 
                && Hud.getInstance().dynamicIslandSettings.isSelected("FreeCam");
        freeCamAnimation.setDirection(isFreeCamActive ? Direction.FORWARDS : Direction.BACKWARDS);
        
        moduleAnimation.setDirection(showModuleNotification ? Direction.FORWARDS : Direction.BACKWARDS);
        moduleScaleAnimation.setDirection(showModuleNotification ? Direction.FORWARDS : Direction.BACKWARDS);
        moduleFadeAnimation.setDirection(showModuleNotification ? Direction.FORWARDS : Direction.BACKWARDS);
        swapAnimation.setDirection(showSwapNotification ? Direction.FORWARDS : Direction.BACKWARDS);
        totemAnimation.setDirection(isTotemAlertActive ? Direction.FORWARDS : Direction.BACKWARDS);
        elytraSwapAnimation.setDirection(isElytraSwapActive ? Direction.FORWARDS : Direction.BACKWARDS);
        
        // Анимации для новых игроков
        boolean isNewPlayerActive = showNewPlayerNotification();
        newPlayerAnimation.setDirection(isNewPlayerActive ? Direction.FORWARDS : Direction.BACKWARDS);
        
        // Hover анимация
        hoverAnimation.setDirection(isHovered ? Direction.FORWARDS : Direction.BACKWARDS);
        heightExpandAnimation.setDirection((isHovered && !mediaNull && !isPvp) || (serverInfoMode && mediaNull && !isPvp) ? Direction.FORWARDS : Direction.BACKWARDS);
        
        // Server info анимация
        serverInfoAnimation.setDirection(serverInfoMode && mediaNull && !isPvp ? Direction.FORWARDS : Direction.BACKWARDS);
        
        // Обновляем FPS и TPS
        lastFps = mc.getCurrentFps();
        
        // Обновляем целевую ширину
        FontRenderer font = Fonts.getSize(12, Fonts.Type.BOLD);
        float padding = 2f;
        if (isNewPlayerActive) {
            String playerText = "Игрок рядом: " + newPlayerName;
            targetWidth = 15 + font.getStringWidth(playerText) + padding * 2;
        } else if (isElytraSwapActive) {
            String elytraText = "Вы свапнули: " + elytraSwapName;
            targetWidth = 15 + 12 + font.getStringWidth(elytraText) + padding * 3;
        } else if (isTotemAlertActive) {
            String totemText = "Totem: " + totemAlertReason;
            targetWidth = 15 + font.getStringWidth(totemText) + padding * 2;
        } else if (showSwapNotification) {
            targetWidth = 15 + 12 + font.getStringWidth(swapItemName) + padding * 3;
        } else if (showModuleNotification) {
            targetWidth = 15 + font.getStringWidth(currentModuleNotificationClean) + padding * 2;
        } else if (isFreeCamActive) {
            BlockPos camPos = BlockPos.ofFloored(freeCam.pos);
            String coords = "XYZ: " + camPos.getX() + ", " + camPos.getY() + ", " + camPos.getZ();
            targetWidth = 15 + font.getStringWidth(coords) + padding * 2;
        } else if (isPvp) {
            String pvpText = "Вы в PvP";
            targetWidth = 15 + font.getStringWidth(pvpText) + padding * 2;
        } else if (!mediaNull) {
            String track = trackName != null ? trackName : "";
            String artist = artistsText != null ? artistsText : "";
            String fullTrack = track + (artist.isEmpty() ? "" : " - " + artist);
            float hoverExpand = (float) hoverAnimation.getOutput().floatValue();
            // При наведении добавляем место для прогресс бара
            float extraWidth = hoverExpand * 30f;
            targetWidth = 15 + font.getStringWidth(fullTrack) + padding * 2 + extraWidth;
        } else if (serverInfoMode) {
            // Расширенная ширина для режима информации о сервере
            targetWidth = 160f;
        } else {
            targetWidth = 15 + font.getStringWidth("Aegis") + padding * 2;
        }
        
        // Плавная интерполяция ширины
        currentWidth = lerp(currentWidth, targetWidth, 0.15f);
        
        // Плавная интерполяция высоты для hover режима и server info режима
        float hoverExpand = (float) heightExpandAnimation.getOutput().floatValue();
        float serverInfoExpand = (float) serverInfoAnimation.getOutput().floatValue();
        float maxExpand = Math.max(hoverExpand, serverInfoExpand);
        // Для server info режима нужно больше высоты (5 строк информации)
        float serverInfoHeight = serverInfoMode ? 58f : 12f;
        targetHeight = 13f + (maxExpand * serverInfoHeight);
        currentHeight = lerp(currentHeight, targetHeight, 0.15f);
    }

    public void showModuleNotification(String moduleName, boolean enabled) {
        if (!Hud.getInstance().dynamicIslandSettings.isSelected("Переключение модулей")) return;
        currentModuleNotification = moduleName + " " + (enabled ? "§aEnabled" : "§cDisabled");
        currentModuleNotificationClean = moduleName + " " + (enabled ? "Enabled" : "Disabled");
        moduleNotificationTime = System.currentTimeMillis();
        moduleAnimation.reset();
        moduleScaleAnimation.reset();
        moduleFadeAnimation.reset();
    }
    
    public void showSwapNotification(ItemStack stack, String itemName) {
        if (!Hud.getInstance().dynamicIslandSettings.isSelected("Авто свап")) return;
        swapItemStack = stack;
        swapItemName = itemName;
        swapNotificationTime = System.currentTimeMillis();
        swapAnimation.reset();
    }
    
    public void showTotemAlert(String reason) {
        if (!Hud.getInstance().dynamicIslandSettings.isSelected("Авто тотем")) return;
        totemAlertReason = reason;
        totemAlertTime = System.currentTimeMillis();
        totemAnimation.reset();
    }
    
    public void showElytraSwap(ItemStack stack, String itemName) {
        if (!Hud.getInstance().dynamicIslandSettings.isSelected("Элитра свап")) return;
        elytraSwapStack = stack;
        elytraSwapName = itemName;
        elytraSwapTime = System.currentTimeMillis();
        elytraSwapAnimation.reset();
    }
    
    private boolean showTotemAlert() {
        return !totemAlertReason.isEmpty() && System.currentTimeMillis() - totemAlertTime < TOTEM_ALERT_DURATION;
    }
    
    private boolean showElytraSwap() {
        return elytraSwapStack != null && !elytraSwapName.isEmpty() && System.currentTimeMillis() - elytraSwapTime < ELYTRA_SWAP_DURATION;
    }
    
    private boolean showNewPlayerNotification() {
        return !newPlayerName.isEmpty() && System.currentTimeMillis() - newPlayerTime < NEW_PLAYER_NOTIFICATION_DURATION;
    }
    
    private void checkNearbyPlayers() {
        if (mc.player == null || mc.world == null) return;
        if (!Hud.getInstance().dynamicIslandSettings.isSelected("Новый игрок")) return;
        
        // Считаем игроков в радиусе
        long playersInRadius = mc.world.getPlayers().stream()
                .filter(p -> p != mc.player)
                .filter(p -> p.squaredDistanceTo(mc.player) <= PLAYER_DETECTION_RADIUS * PLAYER_DETECTION_RADIUS)
                .count();
        
        // Если больше 4 игроков - не уведомляем
        if (playersInRadius > MAX_PLAYERS_FOR_NOTIFICATION) {
            return;
        }
        
        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == mc.player) continue;
            
            String playerName = player.getName().getString();
            double distance = player.squaredDistanceTo(mc.player);
            
            if (distance <= PLAYER_DETECTION_RADIUS * PLAYER_DETECTION_RADIUS) {
                if (!knownPlayers.contains(playerName)) {
                    knownPlayers.add(playerName);
                    newPlayerName = playerName;
                    newPlayerTime = System.currentTimeMillis();
                    newPlayerAnimation.reset();
                }
            } else {
                knownPlayers.remove(playerName);
            }
        }
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private void updateBarHeights() {
        for (int i = 0; i < 3; i++) {
            targetBarHeights[i] = 3 + (float) Math.random() * 5;
        }
    }

    private void clearData() {
        trackName = null;
        artistsText = null;
        progress = 0.0f;
        currentTime = 0;
        totalTime = 1;
        activeSession = null;
        clearCoverTexture();
    }

    private void clearCoverTexture() {
        try {
            TextureManager tm = mc.getTextureManager();
            if (tm != null) {
                tm.destroyTexture(coverTextureLocation);
            }
            if (coverTexture != null) {
                coverTexture.close();
                coverTexture = null;
            }
        } catch (Exception ignored) {
            coverTexture = null;
        }
        coverHash = 0;
    }

    private void updateCoverTexture(NativeImage nativeImage) {
        try {
            if (nativeImage != null) {
                TextureManager tm = mc.getTextureManager();
                if (tm != null) {
                    tm.destroyTexture(coverTextureLocation);
                }
                if (coverTexture != null) {
                    coverTexture.close();
                    coverTexture = null;
                }
                coverTexture = new NativeImageBackedTexture(nativeImage);
                if (tm != null) {
                    tm.registerTexture(coverTextureLocation, coverTexture);
                }
            }
        } catch (Exception e) {
            clearCoverTexture();
            try {
                nativeImage.close();
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (fullNullCheck()) return;

        MatrixStack matrix = context.getMatrices();
        ScissorAssist scissor = Aegis.getInstance().getScissorManager();

        String name = "Aegis";
        String track = trackName != null ? trackName : "";
        String artist = artistsText != null ? artistsText : "";
        String fullTrack = track + (artist.isEmpty() ? "" : " - " + artist);
        String pvp = "PVP";
        String pvpTimer = getPvpTimer();
        boolean isPvp = Network.isPvp();
        boolean mediaNull = (trackName == null || trackName.isEmpty());
        boolean showModuleNotification = !currentModuleNotification.isEmpty() &&
                System.currentTimeMillis() - moduleNotificationTime < MODULE_NOTIFICATION_DURATION;
        boolean showSwapNotification = swapItemStack != null && !swapItemName.isEmpty() &&
                System.currentTimeMillis() - swapNotificationTime < SWAP_NOTIFICATION_DURATION;
        boolean isTotemAlertActive = showTotemAlert();
        
        // Проверяем FreeCam
        FreeCam freeCam = FreeCam.getInstance();
        boolean isFreeCamActive = freeCam != null && freeCam.isState() && freeCam.pos != null;

        float padding = 2f;
        float round = 6f;

        FontRenderer font = Fonts.getSize(12, Fonts.Type.BOLD);

        float baseHeight = 13f;
        float width = currentWidth; // Используем анимированную ширину
        float height = currentHeight; // Используем анимированную высоту
        float x = mc.getWindow().getScaledWidth() / 2f - width / 2f;
        
        // Фиксированная позиция - всегда на одном месте, независимо от боссбаров
        float y = 4f;
        
        // Проверка hover
        double mouseX = mc.mouse.getX() * mc.getWindow().getScaledWidth() / mc.getWindow().getWidth();
        double mouseY = mc.mouse.getY() * mc.getWindow().getScaledHeight() / mc.getWindow().getHeight();
        isHovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
        
        scissor.push(matrix.peek().getPositionMatrix(), x - 6, y - 6, width + 12, height + 12);
        
        float chatAlpha = (float) chatBackgroundAnimation.getOutput().floatValue();
        if (chatAlpha > 0.01f) {
            rectangle.render(ShapeProperties.create(matrix, x, y, width, height)
                    .round(6)
                    .color(ColorAssist.getColor(0, 0, 0, (int)(255 * chatAlpha)))
                    .build());
            
            float shadeHeight = height * chatAlpha;
            rectangle.render(ShapeProperties.create(matrix, x, y, width, shadeHeight)
                    .round(6)
                    .color(ColorAssist.getColor(20, 20, 20, (int)(100 * chatAlpha)))
                    .build());
        } else if (Hud.getInstance().dynamicIslandSettings.isSelected("Amoled")) {
            // Amoled mode - pure black background with thin gray outline
            rectangle.render(ShapeProperties.create(matrix, x, y, width, height)
                    .round(6)
                    .color(ColorAssist.getColor(0, 0, 0, 255))
                    .build());
            
            // Thin gray outline
            rectangle.render(ShapeProperties.create(matrix, x, y, width, height)
                    .round(6)
                    .thickness(0.5f)
                    .color(ColorAssist.getColor(80, 80, 80, 100))
                    .build());
        } else {
            blur.render(ShapeProperties.create(matrix, x, y, width, height)
                    .round(6).softness(10F).thickness(0).color(ColorAssist.getRect(0.4F)).build());
        }
        
        // Новый игрок рядом - высший приоритет
        if (showNewPlayerNotification() && newPlayerAnimation.getOutput().floatValue() > 0.01f) {
            float alpha = (float) newPlayerAnimation.getOutput().floatValue();
            
            String playerText = "Игрок рядом: " + newPlayerName;
            Color textColor = new Color(255, 255, 255, (int) (255 * alpha));
            Color dotColor = new Color(255, 165, 0, (int) (255 * alpha)); // Оранжевый для игроков
            
            float dotSize = (baseHeight - padding * 2) / 1.5f;
            float dotX = x + padding + ((baseHeight - padding * 2) - dotSize) / 2f;
            float dotY = y + padding + ((baseHeight - padding * 2) - dotSize) / 2f;
            
            rectangle.render(ShapeProperties.create(matrix, dotX, dotY, dotSize, dotSize)
                    .round(dotSize / 2)
                    .color(ColorAssist.getColor(dotColor.getRed(), dotColor.getGreen(), dotColor.getBlue(), dotColor.getAlpha()))
                    .build());
            
            float textX = x + padding + (baseHeight - padding * 2) + padding * 0.5f;
            font.drawString(matrix, playerText, textX, y + padding + 3.5f, 
                    ColorAssist.getColor(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), textColor.getAlpha()));
        } else if (showElytraSwap() && elytraSwapAnimation.getOutput().floatValue() > 0.01f) {
            // Elytra Swap
            float alpha = (float) elytraSwapAnimation.getOutput().floatValue();
            
            String elytraText = "Вы свапнули: " + elytraSwapName;
            Color textColor = new Color(255, 255, 255, (int) (255 * alpha));
            Color dotColor = new Color(150, 100, 200, (int) (255 * alpha)); // Фиолетовый для элитр/брони
            
            // Рендерим цветной индикатор слева
            float dotSize = (height - padding * 2) / 1.5f;
            float dotX = x + padding + ((height - padding * 2) - dotSize) / 2f;
            float dotY = y + padding + ((height - padding * 2) - dotSize) / 2f;
            
            rectangle.render(ShapeProperties.create(matrix, dotX, dotY, dotSize, dotSize)
                    .round(dotSize / 2)
                    .color(ColorAssist.getColor(dotColor.getRed(), dotColor.getGreen(), dotColor.getBlue(), dotColor.getAlpha()))
                    .build());
            
            // Рендерим иконку предмета
            float itemSize = 10f;
            float itemX = x + padding + (height - padding * 2) + padding * 0.5f;
            float itemY = y + (height - itemSize) / 2f;
            
            matrix.push();
            matrix.translate(itemX, itemY, 0);
            float scale = itemSize / 16f;
            matrix.scale(scale, scale, 1);
            context.drawItem(elytraSwapStack, 0, 0);
            matrix.pop();
            
            // Рендерим текст
            float textX = itemX + itemSize + padding;
            font.drawString(matrix, elytraText, textX, y + padding + 3.5f, 
                    ColorAssist.getColor(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), textColor.getAlpha()));
        } else if (showTotemAlert() && totemAnimation.getOutput().floatValue() > 0.01f) {
            float alpha = (float) totemAnimation.getOutput().floatValue();
            
            String totemText = "Totem: " + totemAlertReason;
            Color textColor = new Color(255, 255, 255, (int) (255 * alpha));
            Color dotColor = new Color(255, 200, 50, (int) (255 * alpha)); // Золотой цвет для тотема
            
            // Рендерим цветной индикатор слева
            float dotSize = (height - padding * 2) / 1.5f;
            float dotX = x + padding + ((height - padding * 2) - dotSize) / 2f;
            float dotY = y + padding + ((height - padding * 2) - dotSize) / 2f;
            
            rectangle.render(ShapeProperties.create(matrix, dotX, dotY, dotSize, dotSize)
                    .round(dotSize / 2)
                    .color(ColorAssist.getColor(dotColor.getRed(), dotColor.getGreen(), dotColor.getBlue(), dotColor.getAlpha()))
                    .build());
            
            // Рендерим текст
            float textX = x + padding + (height - padding * 2) + padding * 0.5f;
            font.drawString(matrix, totemText, textX, y + padding + 3.5f, 
                    ColorAssist.getColor(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), textColor.getAlpha()));
        } else if (showSwapNotification && swapAnimation.getOutput().floatValue() > 0.01f) {
            float alpha = (float) swapAnimation.getOutput().floatValue();
            
            // Определяем цвет на основе типа предмета
            Color itemColor = getItemColor(swapItemStack);
            Color textColor = new Color(255, 255, 255, (int) (255 * alpha));
            Color dotColor = new Color(itemColor.getRed(), itemColor.getGreen(), itemColor.getBlue(), (int) (255 * alpha));
            
            // Рендерим цветной индикатор слева
            float dotSize = (height - padding * 2) / 1.5f;
            float dotX = x + padding + ((height - padding * 2) - dotSize) / 2f;
            float dotY = y + padding + ((height - padding * 2) - dotSize) / 2f;
            
            rectangle.render(ShapeProperties.create(matrix, dotX, dotY, dotSize, dotSize)
                    .round(dotSize / 2)
                    .color(ColorAssist.getColor(dotColor.getRed(), dotColor.getGreen(), dotColor.getBlue(), dotColor.getAlpha()))
                    .build());
            
            // Рендерим иконку предмета
            float itemSize = 10f;
            float itemX = x + padding + (height - padding * 2) + padding * 0.5f;
            float itemY = y + (height - itemSize) / 2f;
            
            matrix.push();
            matrix.translate(itemX, itemY, 0);
            float scale = itemSize / 16f;
            matrix.scale(scale, scale, 1);
            context.drawItem(swapItemStack, 0, 0);
            matrix.pop();
            
            // Рендерим текст
            float textX = itemX + itemSize + padding;
            font.drawString(matrix, swapItemName, textX, y + padding + 3.5f, 
                    ColorAssist.getColor(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), textColor.getAlpha()));
        } else if (showModuleNotification && moduleAnimation.getOutput().floatValue() > 0.01f) {
            float alpha = (float) moduleAnimation.getOutput().floatValue();
            float scaleAnim = (float) moduleScaleAnimation.getOutput().floatValue();
            float fadeAnim = (float) moduleFadeAnimation.getOutput().floatValue();
            
            // Плавное появление с масштабированием
            float scale = 0.8f + (scaleAnim * 0.2f); // От 0.8 до 1.0
            float finalAlpha = alpha * fadeAnim;
            
            Color textColor;
            Color dotColor;

            if (currentModuleNotification.contains("§a")) {
                textColor = new Color(255, 255, 255, (int) (255 * finalAlpha));
                dotColor = new Color(55, 255, 55, (int) (255 * finalAlpha));
            } else {
                textColor = new Color(255, 255, 255, (int) (255 * finalAlpha));
                dotColor = new Color(255, 55, 55, (int) (255 * finalAlpha));
            }

            // Применяем масштабирование
            matrix.push();
            float centerX = x + width / 2f;
            float centerY = y + height / 2f;
            matrix.translate(centerX, centerY, 0);
            matrix.scale(scale, scale, 1);
            matrix.translate(-centerX, -centerY, 0);

            // Draw status dot (circle) с пульсацией - уменьшен как кружок темы
            float dotSize = (height - padding * 2) / 1.5f;
            float dotX = x + padding + ((height - padding * 2) - dotSize) / 2f;
            float dotY = y + padding + ((height - padding * 2) - dotSize) / 2f;
            
            // Внешнее свечение для точки
            float glowSize = dotSize * (1.0f + (1.0f - scaleAnim) * 0.3f);
            float glowX = dotX - (glowSize - dotSize) / 2f;
            float glowY = dotY - (glowSize - dotSize) / 2f;
            rectangle.render(ShapeProperties.create(matrix, glowX, glowY, glowSize, glowSize)
                    .round(glowSize / 2)
                    .color(ColorAssist.getColor(dotColor.getRed(), dotColor.getGreen(), dotColor.getBlue(), (int)(dotColor.getAlpha() * 0.3f)))
                    .build());
            
            rectangle.render(ShapeProperties.create(matrix, dotX, dotY, dotSize, dotSize)
                    .round(dotSize / 2)
                    .color(ColorAssist.getColor(dotColor.getRed(), dotColor.getGreen(), dotColor.getBlue(), dotColor.getAlpha()))
                    .build());
            
            // Draw module text
            float textX = x + padding + (height - padding * 2) + padding * 0.5f;
            font.drawString(matrix, currentModuleNotificationClean, textX, y + padding + 3.5f, 
                    ColorAssist.getColor(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), textColor.getAlpha()));
            
            matrix.pop();
        } else if (isFreeCamActive && freeCamAnimation.getOutput().floatValue() > 0.01f) {
            // FreeCam координаты камеры
            float alpha = (float) freeCamAnimation.getOutput().floatValue();
            
            BlockPos camPos = BlockPos.ofFloored(freeCam.pos);
            String coords = "XYZ: " + camPos.getX() + ", " + camPos.getY() + ", " + camPos.getZ();
            Color textColor = new Color(255, 255, 255, (int) (255 * alpha));
            Color dotColor = new Color(100, 150, 255, (int) (255 * alpha)); // Синий для FreeCam
            
            // Рендерим цветной индикатор слева
            float dotSize = (height - padding * 2) / 1.5f;
            float dotX = x + padding + ((height - padding * 2) - dotSize) / 2f;
            float dotY = y + padding + ((height - padding * 2) - dotSize) / 2f;
            
            rectangle.render(ShapeProperties.create(matrix, dotX, dotY, dotSize, dotSize)
                    .round(dotSize / 2)
                    .color(ColorAssist.getColor(dotColor.getRed(), dotColor.getGreen(), dotColor.getBlue(), dotColor.getAlpha()))
                    .build());
            
            // Рендерим текст координат
            float textX = x + padding + (height - padding * 2) + padding * 0.5f;
            font.drawString(matrix, coords, textX, y + padding + 3.5f, 
                    ColorAssist.getColor(textColor.getRed(), textColor.getGreen(), textColor.getBlue(), textColor.getAlpha()));
        } else if (!mediaNull && !isPvp && mediaAnimation.getOutput().floatValue() > 0.01f) {
            float animationAlpha = (float) mediaAnimation.getOutput().floatValue();
            float hoverAlpha = (float) hoverAnimation.getOutput().floatValue();
            float expandAlpha = (float) heightExpandAnimation.getOutput().floatValue();
            
            // Размер обложки увеличивается при наведении
            float baseCoverSize = baseHeight - padding * 2;
            float expandedCoverSize = height - padding * 2;
            float coverSize = baseCoverSize + (expandedCoverSize - baseCoverSize) * expandAlpha;
            float coverY = y + padding;
            float coverX = x + padding;

            if (coverTexture != null) {
                try {
                    var tex = mc.getTextureManager().getTexture(coverTextureLocation);
                    if (tex != null) {
                        Render2D.drawTexture(context, coverTextureLocation, coverX, coverY, coverSize, 4f,
                                (int) coverSize, (int) coverSize, (int) coverSize,
                                ColorAssist.getColor(255, 255, 255, 255),
                                ColorAssist.multAlpha(ColorAssist.getText(), animationAlpha));
                    } else {
                        rectangle.render(ShapeProperties.create(matrix, coverX, coverY, coverSize, coverSize).round(4f).color(ColorAssist.multAlpha(ColorAssist.getColor(50, 50, 50, 140), animationAlpha)).build());
                    }
                } catch (Exception e) {
                    rectangle.render(ShapeProperties.create(matrix, coverX, coverY, coverSize, coverSize).round(4f).color(ColorAssist.multAlpha(ColorAssist.getColor(50, 50, 50, 140), animationAlpha)).build());
                }
            } else {
                rectangle.render(ShapeProperties.create(matrix, coverX, coverY, coverSize, coverSize).round(4f).color(ColorAssist.multAlpha(ColorAssist.getColor(50, 50, 50, 140), animationAlpha)).build());
            }

            float textX = x + padding + coverSize + padding;
            
            // При расширении показываем название и артиста отдельно + прогресс бар
            if (expandAlpha > 0.01f) {
                // Название трека
                FontRenderer titleFont = Fonts.getSize(11, Fonts.Type.BOLD);
                String displayTrack = track.length() > 25 ? track.substring(0, 22) + "..." : track;
                titleFont.drawString(matrix, displayTrack, textX, y + padding + 2f,
                        ColorAssist.multAlpha(ColorAssist.getText(), animationAlpha));
                
                // Артист (если есть)
                if (!artist.isEmpty()) {
                    FontRenderer artistFont = Fonts.getSize(9, Fonts.Type.DEFAULT);
                    String displayArtist = artist.length() > 30 ? artist.substring(0, 27) + "..." : artist;
                    artistFont.drawString(matrix, displayArtist, textX, y + padding + 12f,
                            ColorAssist.multAlpha(ColorAssist.getColor(180, 180, 180, 255), animationAlpha * expandAlpha));
                }
                
                // Прогресс бар
                float progressBarY = y + height - padding - 3f;
                float progressBarWidth = width - coverSize - padding * 4;
                float progressBarHeight = 2f;
                
                // Фон прогресс бара
                rectangle.render(ShapeProperties.create(matrix, textX, progressBarY, progressBarWidth, progressBarHeight)
                        .round(1f)
                        .color(ColorAssist.multAlpha(ColorAssist.getColor(60, 60, 60, 200), animationAlpha * expandAlpha))
                        .build());
                
                // Заполненная часть прогресс бара
                float filledWidth = progressBarWidth * progress;
                if (filledWidth > 0) {
                    rectangle.render(ShapeProperties.create(matrix, textX, progressBarY, filledWidth, progressBarHeight)
                            .round(1f)
                            .color(ColorAssist.multAlpha(ColorAssist.getClientColor(), animationAlpha * expandAlpha))
                            .build());
                }
                
                // Время (текущее / общее)
                FontRenderer timeSmallFont = Fonts.getSize(8, Fonts.Type.DEFAULT);
                String currentTimeStr = formatTime(currentTime);
                String totalTimeStr = formatTime(totalTime);
                String timeStr = currentTimeStr + " / " + totalTimeStr;
                float timeWidth = timeSmallFont.getStringWidth(timeStr);
                timeSmallFont.drawString(matrix, timeStr, textX + progressBarWidth - timeWidth, progressBarY - 8f,
                        ColorAssist.multAlpha(ColorAssist.getColor(150, 150, 150, 255), animationAlpha * expandAlpha));
                
                // Иконка play/pause
                String playIcon = isPlaying ? "▶" : "⏸";
                FontRenderer iconFont = Fonts.getSize(8, Fonts.Type.DEFAULT);
                iconFont.drawString(matrix, playIcon, textX, progressBarY - 8f,
                        ColorAssist.multAlpha(ColorAssist.getColor(150, 150, 150, 255), animationAlpha * expandAlpha));
            } else {
                // Обычный режим - одна строка
                font.drawString(matrix, fullTrack, textX, y + padding + 4f,
                        ColorAssist.multAlpha(ColorAssist.getText(), animationAlpha));
                
                float visualizerX = textX + font.getStringWidth(fullTrack) + padding * 2;
                float visualizerY = y + padding + 1f;
                float bandWidth = 1.5f;
                float bandSpacing = 1f;
                
                float[] bandHeights = audioVisualizer.getBandHeights();
                for (int i = 0; i < audioVisualizer.getBandCount(); i++) {
                    float bandX = visualizerX + (i * (bandWidth + bandSpacing));
                    float bandHeight = bandHeights[i];
                    rectangle.render(ShapeProperties.create(matrix, bandX, visualizerY + (10f - bandHeight), bandWidth, bandHeight)
                            .round(0.5f)
                            .color(ColorAssist.multAlpha(ColorAssist.getClientColor(), animationAlpha))
                            .build());
                }
                
                float controlsAlpha = (float) musicControlsAnimation.getOutput().floatValue();
                if (controlsAlpha > 0.01f) {
                    float buttonSize = 8f;
                    float buttonSpacing = 2f;
                    float controlsY = y + padding + 2f;
                    
                    float prevButtonX = x + padding;
                    float playButtonX = x + width / 2f - buttonSize / 2f;
                    float nextButtonX = x + width - padding - buttonSize;
                    
                    FontRenderer buttonFont = Fonts.getSize(7, Fonts.Type.BOLD);
                    
                    rectangle.render(ShapeProperties.create(matrix, prevButtonX, controlsY, buttonSize, buttonSize)
                            .round(2f)
                            .color(ColorAssist.multAlpha(ColorAssist.getClientColor(), controlsAlpha * 0.7f))
                            .build());
                    buttonFont.drawString(matrix, "◀", prevButtonX + 1.5f, controlsY + 1f,
                            ColorAssist.multAlpha(ColorAssist.getText(), controlsAlpha));
                    
                    rectangle.render(ShapeProperties.create(matrix, playButtonX, controlsY, buttonSize, buttonSize)
                            .round(2f)
                            .color(ColorAssist.multAlpha(ColorAssist.getClientColor(), controlsAlpha * 0.7f))
                            .build());
                    String playBtn = isPlaying ? "⏸" : "▶";
                    buttonFont.drawString(matrix, playBtn, playButtonX + 1f, controlsY + 1f,
                            ColorAssist.multAlpha(ColorAssist.getText(), controlsAlpha));
                    
                    rectangle.render(ShapeProperties.create(matrix, nextButtonX, controlsY, buttonSize, buttonSize)
                            .round(2f)
                            .color(ColorAssist.multAlpha(ColorAssist.getClientColor(), controlsAlpha * 0.7f))
                            .build());
                    buttonFont.drawString(matrix, "▶", nextButtonX + 1.5f, controlsY + 1f,
                            ColorAssist.multAlpha(ColorAssist.getText(), controlsAlpha));
                }
            }

        } else if (!isPvp && mediaAnimation.getOutput().floatValue() < 0.99f) {
            float defaultAlpha = 1f - (float) mediaAnimation.getOutput().floatValue();
            float serverInfoAlpha = (float) serverInfoAnimation.getOutput().floatValue();
            
            // Если активен режим server info - показываем информацию о сервере
            if (serverInfoMode && serverInfoAlpha > 0.01f) {
                // Получаем информацию
                int ping = 0;
                if (mc.player != null && mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null) {
                    ping = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency();
                }
                
                int playersOnline = mc.getNetworkHandler() != null ? mc.getNetworkHandler().getPlayerList().size() : 0;
                String serverIp = mc.getCurrentServerEntry() != null ? mc.getCurrentServerEntry().address : "Singleplayer";
                String biome = mc.world != null && mc.player != null ? 
                        mc.world.getBiome(mc.player.getBlockPos()).getKey().map(k -> k.getValue().getPath()).orElse("Unknown") : "Unknown";
                BlockPos pos = mc.player != null ? mc.player.getBlockPos() : BlockPos.ORIGIN;
                boolean connected = mc.getNetworkHandler() != null && mc.getNetworkHandler().getConnection().isOpen();
                
                FontRenderer infoFont = Fonts.getSize(10, Fonts.Type.DEFAULT);
                FontRenderer labelFont = Fonts.getSize(9, Fonts.Type.BOLD);
                float lineHeight = 11f;
                float startY = y + padding + 2f;
                float textStartX = x + padding + 4f;
                
                // Заголовок
                labelFont.drawString(matrix, "Server Info", textStartX, startY, 
                        ColorAssist.multAlpha(ColorAssist.getClientColor(), serverInfoAlpha));
                startY += lineHeight + 2f;
                
                // FPS / TPS
                String fpsText = "FPS: " + lastFps;
                infoFont.drawString(matrix, fpsText, textStartX, startY, 
                        ColorAssist.multAlpha(ColorAssist.getText(), serverInfoAlpha));
                startY += lineHeight;
                
                // Пинг с цветовой индикацией
                Color pingColor;
                if (ping < 50) {
                    pingColor = new Color(100, 255, 100); // Зелёный
                } else if (ping < 150) {
                    pingColor = new Color(255, 255, 100); // Жёлтый
                } else {
                    pingColor = new Color(255, 100, 100); // Красный
                }
                String pingText = "Ping: " + ping + "ms";
                infoFont.drawString(matrix, pingText, textStartX, startY, 
                        ColorAssist.multAlpha(ColorAssist.getColor(pingColor.getRed(), pingColor.getGreen(), pingColor.getBlue(), 255), serverInfoAlpha));
                startY += lineHeight;
                
                // Игроки онлайн
                String playersText = "Players: " + playersOnline;
                infoFont.drawString(matrix, playersText, textStartX, startY, 
                        ColorAssist.multAlpha(ColorAssist.getText(), serverInfoAlpha));
                startY += lineHeight;
                
                // Координаты
                String coordsText = "XYZ: " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ();
                infoFont.drawString(matrix, coordsText, textStartX, startY, 
                        ColorAssist.multAlpha(ColorAssist.getText(), serverInfoAlpha));
                startY += lineHeight;
                
                // Сервер IP и статус
                Color statusColor = connected ? new Color(100, 255, 100) : new Color(255, 100, 100);
                String statusText = (connected ? "● " : "○ ") + (serverIp.length() > 20 ? serverIp.substring(0, 17) + "..." : serverIp);
                infoFont.drawString(matrix, statusText, textStartX, startY, 
                        ColorAssist.multAlpha(ColorAssist.getColor(statusColor.getRed(), statusColor.getGreen(), statusColor.getBlue(), 255), serverInfoAlpha));
                
            } else {
                // Обычный режим - показываем Aegis
                float circleSize = (baseHeight - padding * 2) / 1.5f;
                float circleX = x + padding + ((baseHeight - padding * 2) - circleSize) / 2f;
                float circleY = y + padding + ((baseHeight - padding * 2) - circleSize) / 2f;
                rectangle.render(ShapeProperties.create(matrix, circleX, circleY, circleSize, circleSize).round(4f).color(ColorAssist.multAlpha(ColorAssist.getClientColor(), defaultAlpha)).build());


                font.drawString(matrix, name, x + baseHeight, y + padding + 4f, ColorAssist.getText());
            }
        } else if (isPvp && pvpAnimation.getOutput().floatValue() > 0.01f) {
            float pvpAlpha = (float) pvpAnimation.getOutput().floatValue();
            
            // Красный индикатор слева
            float indicatorSize = baseHeight - padding * 2;
            float indicatorX = x + padding;
            float indicatorY = y + padding;
            rectangle.render(ShapeProperties.create(matrix, indicatorX, indicatorY, indicatorSize, indicatorSize)
                    .round(4f)
                    .color(ColorAssist.multAlpha(ColorAssist.RED, pvpAlpha))
                    .build());
            
            // Таймер поверх красного индикатора (белый текст по центру)
            FontRenderer timerFont = Fonts.getSize(9, Fonts.Type.BOLD);
            float timerWidth = timerFont.getStringWidth(pvpTimer);
            float timerX = indicatorX + (indicatorSize - timerWidth) / 2f;
            float timerY = indicatorY + (indicatorSize - timerFont.getStringHeight(pvpTimer)) / 2f;
            timerFont.drawString(matrix, pvpTimer, timerX, timerY, 
                    ColorAssist.multAlpha(ColorAssist.getColor(255, 255, 255), pvpAlpha));

            // Текст "Вы в PvP" справа от индикатора
            String pvpText = "Вы в PvP";
            font.drawString(matrix, pvpText, x + padding + indicatorSize + padding, y + padding + 4.5f, 
                    ColorAssist.multAlpha(ColorAssist.getText(), pvpAlpha));
        }

        scissor.pop();

        String currentTime = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        FontRenderer timeFont = Fonts.getSize(15, Fonts.Type.DEFAULT);
        // Увеличен отступ после времени (было padding * 3f, стало padding * 5f)
        timeFont.drawString(matrix, currentTime, x - (timeFont.getStringWidth(currentTime) + (padding * 5f)), y + padding + 2.5f, ColorAssist.getText());

        int ping = 0;
        if (mc.player != null && mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null) {
            ping = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency();
        }

        float baseBarY = y + padding + (Fonts.getSize(7, Fonts.Type.ICONS).getStringHeight("P") / 2f) - 4 + 1;
        float[] barYs = new float[3];

        for (int i = 0; i < 3; i++) {
            barYs[i] = baseBarY + (10 - currentBarHeights[i]) / 2f;
        }

        boolean hasActiveMusic = !mediaNull && !isPvp;
        float internetAlpha = (float) internetAnimation.getOutput().floatValue();
        float barAlpha = (float) barAnimation.getOutput().floatValue();
        
        // Увеличен отступ перед антенкой (было padding * 3f, стало padding * 5f)
        // Антенка теперь тоньше (2.5F вместо 3.5F) и повёрнута в другую сторону (от большой к маленькой)
        float antennaOffset = padding * 5f;
        float barWidth = 2.5F;
        float barSpacing = 3.5f;

        if (hasActiveMusic && barAlpha > 0.01f && internetAlpha > 0.01f) {
            float combinedAlpha = internetAlpha * barAlpha;
            // Начинается с маленькой, заканчивается большой
            rectangle.render(ShapeProperties.create(matrix, x + width + antennaOffset, barYs[0], barWidth, currentBarHeights[0]).round(1f).color(ColorAssist.multAlpha(ColorAssist.getText(), combinedAlpha)).build());
            rectangle.render(ShapeProperties.create(matrix, x + width + antennaOffset + barSpacing, barYs[1], barWidth, currentBarHeights[1]).round(1f).color(ColorAssist.multAlpha(ColorAssist.getText(), combinedAlpha)).build());
            rectangle.render(ShapeProperties.create(matrix, x + width + antennaOffset + barSpacing * 2, barYs[2], barWidth, currentBarHeights[2]).round(1f).color(ColorAssist.multAlpha(ColorAssist.getText(), combinedAlpha)).build());
        } else if (internetAlpha > 0.01f) {
            // Начинается с маленькой (4), заканчивается большой (7) - уменьшено для стиля
            rectangle.render(ShapeProperties.create(matrix, x + width + antennaOffset, baseBarY + 3, barWidth, 4F).round(1f).color(ColorAssist.multAlpha(ColorAssist.getText(), internetAlpha)).build());
            rectangle.render(ShapeProperties.create(matrix, x + width + antennaOffset + barSpacing, baseBarY + 1.5f, barWidth, 5.5F).round(1f).color(ColorAssist.multAlpha(ColorAssist.getText(), internetAlpha)).build());
            rectangle.render(ShapeProperties.create(matrix, x + width + antennaOffset + barSpacing * 2, baseBarY, barWidth, 7F).round(1f).color(ColorAssist.multAlpha(ColorAssist.getText(), internetAlpha)).build());
        }

        if (internetAlpha < 0.99f) {
            float combinedAlpha = internetAlpha * barAlpha;
            rectangle.render(ShapeProperties.create(matrix, x + width + antennaOffset, barYs[0], barWidth, currentBarHeights[0]).round(1f).color(ColorAssist.multAlpha(ColorAssist.getText(), combinedAlpha)).build());
            rectangle.render(ShapeProperties.create(matrix, x + width + antennaOffset + barSpacing, barYs[1], barWidth, currentBarHeights[1]).round(1f).color(ColorAssist.multAlpha(ColorAssist.getText(), combinedAlpha)).build());
            rectangle.render(ShapeProperties.create(matrix, x + width + antennaOffset + barSpacing * 2, barYs[2], barWidth, currentBarHeights[2]).round(1f).color(ColorAssist.multAlpha(ColorAssist.getText(), combinedAlpha)).build());
        }
    }

    private String getPvpTimer() {
        if (mc.inGameHud == null || mc.inGameHud.getBossBarHud() == null) {
            return "30";
        }

        for (ClientBossBar bossBar : mc.inGameHud.getBossBarHud().bossBars.values()) {
            String name = bossBar.getName().getString().toLowerCase();
            if (name.contains("pvp") || name.contains("пвп")) {
                Matcher matcher = PVP_TIMER_PATTERN.matcher(bossBar.getName().getString());
                if (matcher.find()) {
                    return matcher.group(1);
                }
            }
        }

        return "30";
    }

    private boolean fullNullCheck() {
        return mc.player == null || mc.world == null;
    }
    
    private String formatTime(long ms) {
        long seconds = ms / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
    
    private Color getItemColor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return new Color(150, 150, 150); // Серый по умолчанию
        }
        
        var item = stack.getItem();
        
        // Тотемы - золотой/жёлтый
        if (item == Items.TOTEM_OF_UNDYING) {
            return new Color(255, 200, 50);
        }
        // Золотые яблоки - золотой
        if (item == Items.GOLDEN_APPLE || item == Items.ENCHANTED_GOLDEN_APPLE) {
            return new Color(255, 215, 0);
        }
        // Головы игроков - бежевый/телесный
        if (item == Items.PLAYER_HEAD) {
            return new Color(200, 160, 120);
        }
        // Щит - синий
        if (item == Items.SHIELD) {
            return new Color(100, 150, 255);
        }
        // Элитры - серо-фиолетовый
        if (item == Items.ELYTRA) {
            return new Color(150, 100, 200);
        }
        // Жемчуг эндера - бирюзовый
        if (item == Items.ENDER_PEARL) {
            return new Color(50, 200, 180);
        }
        // Зелья - розовый/фиолетовый
        if (item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION) {
            return new Color(200, 100, 200);
        }
        // Фейерверки - красный
        if (item == Items.FIREWORK_ROCKET) {
            return new Color(255, 100, 100);
        }
        // Лук/арбалет - коричневый
        if (item == Items.BOW || item == Items.CROSSBOW) {
            return new Color(150, 100, 50);
        }
        // Мечи - в зависимости от материала
        if (item == Items.NETHERITE_SWORD) {
            return new Color(80, 70, 80);
        }
        if (item == Items.DIAMOND_SWORD) {
            return new Color(100, 220, 255);
        }
        if (item == Items.IRON_SWORD) {
            return new Color(200, 200, 200);
        }
        if (item == Items.GOLDEN_SWORD) {
            return new Color(255, 215, 0);
        }
        
        // По умолчанию - цвет клиента
        return new Color(ColorAssist.getClientColor());
    }
    
    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) {
            float width = currentWidth;
            float height = currentHeight;
            float x = mc.getWindow().getScaledWidth() / 2f - width / 2f;
            float y = 4f;
            
            boolean hovered = mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + height;
            
            if (hovered) {
                boolean mediaNull = (trackName == null || trackName.isEmpty());
                boolean isPvp = Network.isPvp();
                
                if (mediaNull && !isPvp) {
                    // Переключаем режим server info
                    serverInfoMode = !serverInfoMode;
                    return true;
                } else if (!mediaNull && !isPvp && activeSession != null) {
                    // Клик по музыке - play/pause
                    try {
                        if (isPlaying) {
                            activeSession.pause();
                        } else {
                            activeSession.play();
                        }
                    } catch (Exception ignored) {}
                    return true;
                }
            } else if (serverInfoMode) {
                // Закрываем server info если кликнули вне
                serverInfoMode = false;
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    public void shutdown() {
        try {
            if (executor != null && !executor.isShutdown()) {
                executor.shutdown();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            }
        } catch (Exception ignored) {}
        
        clearCoverTexture();
        clearData();
    }
}
