//package fun.aegis.display.hud;
//
//import dev.redstones.mediaplayerinfo.IMediaSession;
//import dev.redstones.mediaplayerinfo.MediaInfo;
//import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
//import fun.aegis.Aegis;
//import fun.aegis.features.impl.render.Hud;
//import fun.aegis.utils.client.Instance;
//import fun.aegis.utils.client.chat.StringHelper;
//import fun.aegis.utils.client.discord.Buffer;
//import fun.aegis.utils.client.managers.api.draggable.AbstractDraggable;
//import fun.aegis.utils.display.color.ColorAssist;
//import fun.aegis.utils.display.font.FontRenderer;
//import fun.aegis.utils.display.font.Fonts;
//import fun.aegis.utils.display.geometry.Render2D;
//import fun.aegis.utils.display.scissor.ScissorAssist;
//import fun.aegis.utils.display.shape.ShapeProperties;
//import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
//import fun.aegis.utils.math.calc.Calculate;
//import fun.aegis.utils.math.time.StopWatch;
//import net.minecraft.client.gui.DrawContext;
//import net.minecraft.client.util.math.MatrixStack;
//import net.minecraft.util.Identifier;
//import net.minecraft.util.math.MathHelper;
//
//import java.awt.*;
//import java.util.Arrays;
//import java.util.Comparator;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//
//public class MediaPlayer extends AbstractDraggable {
//
//    public static MediaPlayer getInstance() {
//        return Instance.getDraggable(MediaPlayer.class);
//    }
//
//    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
//    private MediaInfo mediaInfo = new MediaInfo("Название Трека", "Артист", new byte[0], 0, 0, false);
//    private final Identifier artwork = Identifier.of("textures/music.png");
//    private final StopWatch lastMedia = new StopWatch();
//    public IMediaSession session;
//    private float widthDuration;
//    private byte[] lastArtworkHash = new byte[0];
//
//    public MediaPlayer() {
//        super("Media Player", 10, 120, 100, 40, true);
//    }
//
//    @Override
//    public boolean visible() {
//        return !lastMedia.finished(2000) || PlayerInteractionHelper.isChat(mc.currentScreen) || (session != null && mediaInfo.getPlaying());
//    }
//
//    @Override
//    public void tick() {
//        if (Hud.getInstance().isState() && Hud.getInstance().interfaceSettings.isSelected("Media Player") && mc.player.age % 10 == 0) {
//            executorService.submit(() -> {
//                IMediaSession currentSession = session = MediaPlayerInfo.Instance.getMediaSessions().stream().max(Comparator.comparing(s -> s.getMedia().getPlaying())).orElse(null);
//                if (currentSession != null) {
//                    MediaInfo info = currentSession.getMedia();
//                    if (!info.getTitle().isEmpty() || !info.getArtist().isEmpty()) {
//                        if (!mediaInfo.getTitle().equals(info.getTitle()) ||
//                                !mediaInfo.getArtist().equals(info.getArtist()) ||
//                                !Arrays.equals(mediaInfo.getArtworkPng(), info.getArtworkPng()) ||
//                                mediaInfo.getPlaying() != info.getPlaying()) {
//                            if (!Arrays.equals(lastArtworkHash, info.getArtworkPng())) {
//                                Buffer.registerTexture(artwork, info.getArtworkPng());
//                                lastArtworkHash = info.getArtworkPng().clone();
//                            }
//                            mediaInfo = info;
//                            lastMedia.reset();
//                        }
//                    }
//                }
//            });
//        }
//    }
//
//    private boolean isButtonArea(double mouseX, double mouseY, float buttonX, float buttonSize, float buttonY, float buttonHeight) {
//        return mouseX >= buttonX && mouseX <= buttonX + buttonSize && mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
//    }
//
//    @Override
//    public boolean mouseClicked(double mouseX, double mouseY, int button) {
//        if (button == 0 && isHovered(mouseX, mouseY)) {
//            FontRenderer icon = Fonts.getSize(17, Fonts.Type.ICONS);
//            float playPauseX = (float) (getX() + (getWidth() + 32) / 2) + 4.5f;
//            float prevX = (float) (getX() + (getWidth() + 32) / 2) - 4;
//            float nextX = (float) (getX() + (getWidth() + 32) / 2) + 14f;
//            float y = getY() + 25.5f;
//            float buttonSize = icon.getStringWidth("I");
//            float buttonHeight = icon.getStringHeight("I");
//            if (session != null && isButtonArea(mouseX, mouseY, playPauseX, buttonSize, y, buttonHeight)) {
//                executorService.submit(() -> session.playPause());
//                return true;
//            } else if (session != null && isButtonArea(mouseX, mouseY, prevX, buttonSize, y, buttonHeight)) {
//                executorService.submit(() -> session.previous());
//                return true;
//            } else if (session != null && isButtonArea(mouseX, mouseY, nextX, buttonSize, y, buttonHeight)) {
//                executorService.submit(() -> session.next());
//                return true;
//            } else if (isCanDrag()) {
//                return super.mouseClicked(mouseX, mouseY, button);
//            }
//        }
//        return false;
//    }
//
//    @Override
//    public void drawDraggable(DrawContext context) {
//        MatrixStack matrix = context.getMatrices();
//        ScissorAssist scissor = Aegis.getInstance().getScissorManager();
//        FontRenderer big = Fonts.getSize(14, Fonts.Type.DEFAULT);
//        FontRenderer mini = Fonts.getSize(11, Fonts.Type.DEFAULT);
//        FontRenderer icon = Fonts.getSize(17, Fonts.Type.ICONS);
//
//        int sizeArtwork = 32;
//        int sizePausePlay = 4;
//        int maxDurationWidth = getWidth() - (sizeArtwork + 12) + 15;
//        int duration = (int) mediaInfo.getDuration();
//        int position = MathHelper.clamp((int) mediaInfo.getPosition(), 0, duration);
//
//        if (session != null && mediaInfo.getPlaying()) {
//            position = Math.min((int) (mediaInfo.getPosition() + lastMedia.elapsedTime() / 1000.0), duration);
//        }
//
//        String timeDuration = StringHelper.getDuration(duration);
//        String timePosition = StringHelper.getDuration(position);
//        widthDuration = MathHelper.clamp(Calculate.interpolateSmooth(1, widthDuration, duration > 0 ? (float) position / duration * maxDurationWidth : 0), 1, maxDurationWidth);
//
//
//        blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth() + 15, getHeight())
//                .round(5f).quality(12)
//                .color(new Color(0, 0, 0, 150).getRGB())
//                .build());
//
//        rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth() + 15, getHeight())
//                .round(5f)
//                .thickness(0.1f)
//                .outlineColor(new Color(18, 19, 20, 35).getRGB())
//                .color(
//                        new Color(18, 19, 20, 75).getRGB(),
//                        new Color(0, 2, 5, 75).getRGB(),
//                        new Color(0, 2, 5, 75).getRGB(),
//                        new Color(18, 19, 20, 75).getRGB())
//                .build());
//
//        scissor.push(matrix.peek().getPositionMatrix(), getX() + sizeArtwork + 8, getY(), getWidth() - sizeArtwork - 10 + 15, getHeight());
//        big.drawStringWithScroll(matrix, mediaInfo.getTitle(), getX() + sizeArtwork + 8, getY() + 7, 56, ColorAssist.getText());
//        mini.drawStringWithScroll(matrix, mediaInfo.getArtist(), getX() + sizeArtwork + 8, getY() + 15.5F, 56, ColorAssist.getText(0.75F));
//        scissor.pop();
//
//        Render2D.drawTexture(context, artwork, getX() + 4, getY() + 4, sizeArtwork, 3, sizeArtwork, sizeArtwork, sizeArtwork, ColorAssist.getRect(1));
//        mini.drawString(matrix, timePosition, getX() + 8 + sizeArtwork, getY() + 27, ColorAssist.getText());
//        mini.drawString(matrix, timeDuration, getX() + getWidth() - 4 - mini.getStringWidth(timeDuration) + 15, getY() + 27, ColorAssist.getText());
//
//        rectangle.render(ShapeProperties.create(matrix, getX() + 8 + sizeArtwork, getY() + getHeight() - 8f, maxDurationWidth, 3.5f).round(0.85F).color(ColorAssist.getRectDarker(0.75F)).build());
//        rectangle.render(ShapeProperties.create(matrix, getX() + 8 + sizeArtwork, getY() + getHeight() - 8f, widthDuration, 3.5f).softness(4).round(1).color(ColorAssist.roundClientColor(0.2F)).build());
//        rectangle.render(ShapeProperties.create(matrix, getX() + 8 + sizeArtwork, getY() + getHeight() - 8f, widthDuration, 3.5f).round(0.85F).color(ColorAssist.roundClientColor(1)).build());
//
//        icon.drawString(matrix, (mediaInfo.getPlaying() ? "I" : "H"), (float) (getX() + (getWidth() + sizeArtwork + 4 - sizePausePlay) / 2) + 4.5f, getY() + 25.5f, ColorAssist.rgba(255, 255, 255, 255));
//        icon.drawString(matrix, "O", (float) (getX() + (getWidth() + sizeArtwork + 4 - sizePausePlay) / 2) - 4, getY() + 25.5f, ColorAssist.rgba(255, 255, 255, 255));
//        icon.drawString(matrix, "P", (float) (getX() + (getWidth() + sizeArtwork + 4 - sizePausePlay) / 2) + 14f, getY() + 25.5f, ColorAssist.rgba(255, 255, 255, 255));
//    }
//}
