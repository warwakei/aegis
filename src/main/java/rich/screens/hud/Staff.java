package rich.screens.hud;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.util.Identifier;
import rich.client.draggables.AbstractHudElement;
import rich.util.ColorUtil;
import rich.util.animations.Direction;
import rich.util.render.Render2D;
import rich.util.render.shader.Scissor;
import rich.util.render.font.Fonts;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;

public class Staff extends AbstractHudElement {

    private static final Pattern NAME_PATTERN = Pattern.compile("^\\w{3,16}$");
    private static final Pattern DIGIT_ONLY_PATTERN = Pattern.compile("^\\d{1,4}$");
    private static final Identifier STEVE_SKIN = Identifier.of("rich", "textures/entity/player/wide/steve.png");

    private static class StaffInfo {
        String name;
        GameProfile profile;
        Identifier skin;

        StaffInfo(String name, GameProfile profile, Identifier skin) {
            this.name = name;
            this.profile = profile;
            this.skin = skin;
        }
    }

    private Map<String, StaffInfo> staffMap = new LinkedHashMap<>();
    private Map<String, Float> staffAnimations = new LinkedHashMap<>();
    private Set<String> activeStaffIds = new HashSet<>();
    private Identifier cachedRandomSkin = null;
    private boolean skinCached = false;

    private float animatedWidth = 80;
    private float animatedHeight = 23;
    private long lastUpdateTime = System.currentTimeMillis();

    private static final float ANIMATION_SPEED = 8.0f;
    private static final float FACE_SIZE = 8f;
    private static final float CIRCLE_SIZE = 5f;

    public Staff() {
        super("Staff", 300, 150, 80, 23, true);
        stopAnimation();
    }

    @Override
    public boolean visible() {
        return !scaleAnimation.isFinished(Direction.BACKWARDS);
    }

    private Identifier getSkinFromEntry(PlayerListEntry entry) {
        try {
            SkinTextures skinTextures = entry.getSkinTextures();
            if (skinTextures != null && skinTextures.body() != null) {
                Identifier texturePath = skinTextures.body().texturePath();
                if (texturePath != null) {
                    return texturePath;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Identifier findRandomOnlineSkin() {
        if (mc.player == null || mc.player.networkHandler == null) {
            return STEVE_SKIN;
        }

        List<PlayerListEntry> players = new ArrayList<>(mc.player.networkHandler.getPlayerList());
        Collections.shuffle(players);

        for (PlayerListEntry entry : players) {
            Identifier skin = getSkinFromEntry(entry);
            if (skin != null) {
                return skin;
            }
        }

        return STEVE_SKIN;
    }

    private Identifier getCachedSkin() {
        if (!skinCached || cachedRandomSkin == null) {
            if (mc.player != null && mc.player.networkHandler != null) {
                Identifier found = findRandomOnlineSkin();
                if (found != null && !found.equals(STEVE_SKIN)) {
                    cachedRandomSkin = found;
                    skinCached = true;
                } else if (cachedRandomSkin == null) {
                    cachedRandomSkin = STEVE_SKIN;
                }
            } else {
                cachedRandomSkin = STEVE_SKIN;
            }
        }
        return cachedRandomSkin;
    }

    @Override
    public void tick() {
        if (mc.player == null || mc.world == null) {
            staffMap.clear();
            activeStaffIds.clear();
            cachedRandomSkin = null;
            skinCached = false;
            stopAnimation();
            return;
        }

        String myName = mc.player.getName().getString();
        activeStaffIds.clear();

        Scoreboard scoreboard = mc.world.getScoreboard();
        List<Team> teams = new ArrayList<>(scoreboard.getTeams());
        teams.sort(Comparator.comparing(Team::getName));

        Collection<PlayerListEntry> online = mc.player.networkHandler.getPlayerList();
        Set<String> onlineNames = new HashSet<>();
        for (PlayerListEntry entry : online) {
            if (entry.getProfile() != null && entry.getProfile().name() != null) {
                onlineNames.add(entry.getProfile().name());
            }
        }

        for (Team team : teams) {
            Collection<String> members = team.getPlayerList();
            if (members.size() != 1) continue;
            String name = members.iterator().next();
            if (!NAME_PATTERN.matcher(name).matches()) continue;
            if (name.equals(myName)) continue;
            if (DIGIT_ONLY_PATTERN.matcher(name).matches()) continue;

            boolean isOnline = onlineNames.contains(name);

            if (!isOnline) {
                activeStaffIds.add(name);

                if (!staffMap.containsKey(name)) {
                    GameProfile fakeProfile = new GameProfile(UUID.randomUUID(), name);
                    Identifier randomSkin = findRandomOnlineSkin();
                    staffMap.put(name, new StaffInfo(name, fakeProfile, randomSkin));
                }

                if (!staffAnimations.containsKey(name)) {
                    staffAnimations.put(name, 0f);
                }
            }
        }

        boolean hasActiveStaff = !activeStaffIds.isEmpty() || !staffAnimations.isEmpty();
        boolean inChat = isChat(mc.currentScreen);

        if (hasActiveStaff || inChat) {
            startAnimation();
        } else {
            stopAnimation();
        }
    }

    private float lerp(float current, float target, float deltaTime) {
        float factor = (float) (1.0 - Math.pow(0.001, deltaTime * ANIMATION_SPEED));
        return current + (target - current) * factor;
    }

    @Override
    public void drawDraggable(DrawContext context, int alpha) {
        if (alpha <= 0) return;

        float alphaFactor = alpha / 255.0f;

        long currentTime = System.currentTimeMillis();
        float deltaTime = (currentTime - lastUpdateTime) / 1000.0f;
        lastUpdateTime = currentTime;
        deltaTime = Math.min(deltaTime, 0.1f);

        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Float> entry : staffAnimations.entrySet()) {
            String id = entry.getKey();
            float currentAnim = entry.getValue();
            float targetAnim = activeStaffIds.contains(id) ? 1f : 0f;
            float newAnim = lerp(currentAnim, targetAnim, deltaTime);

            if (Math.abs(newAnim - targetAnim) < 0.01f) {
                newAnim = targetAnim;
            }

            if (newAnim <= 0.01f && targetAnim == 0f) {
                toRemove.add(id);
            } else {
                staffAnimations.put(id, newAnim);
            }
        }
        for (String id : toRemove) {
            staffAnimations.remove(id);
            staffMap.remove(id);
        }

        float x = getX();
        float y = getY();

        boolean hasAnimatingStaff = !staffAnimations.isEmpty();
        boolean showExample = !hasAnimatingStaff && isChat(mc.currentScreen);

        int offset = 23;
        float targetWidth = 80;

        if (showExample) {
            offset += 11;
            String name = "ExampleStaff";
            float nameWidth = Fonts.BOLD.getWidth(name, 6);
            targetWidth = Math.max(nameWidth + 55, targetWidth);
        } else if (hasAnimatingStaff) {
            for (Map.Entry<String, Float> entry : staffAnimations.entrySet()) {
                String id = entry.getKey();
                float animation = entry.getValue();
                if (animation <= 0) continue;

                StaffInfo info = staffMap.get(id);
                if (info == null) continue;

                offset += (int) (animation * 11);

                float nameWidth = Fonts.BOLD.getWidth(info.name, 6);
                targetWidth = Math.max(nameWidth + 55, targetWidth);
            }
        }

        float targetHeight = offset + 2;

        animatedWidth = lerp(animatedWidth, targetWidth, deltaTime);
        animatedHeight = lerp(animatedHeight, targetHeight, deltaTime);

        if (Math.abs(animatedWidth - targetWidth) < 0.3f) {
            animatedWidth = targetWidth;
        }
        if (Math.abs(animatedHeight - targetHeight) < 0.3f) {
            animatedHeight = targetHeight;
        }

        setWidth((int) Math.ceil(animatedWidth));
        setHeight((int) Math.ceil(animatedHeight));

        float contentHeight = animatedHeight;
        int bgAlpha = (int) (255 * alphaFactor);

        if (contentHeight > 0) {
            Render2D.gradientRect(x, y, getWidth(), contentHeight,
                    new int[]{
                            new Color(52, 52, 52, bgAlpha).getRGB(),
                            new Color(32, 32, 32, bgAlpha).getRGB(),
                            new Color(52, 52, 52, bgAlpha).getRGB(),
                            new Color(32, 32, 32, bgAlpha).getRGB()
                    },
                    5);
            Render2D.outline(x, y, getWidth(), contentHeight, 0.35f, new Color(90, 90, 90, bgAlpha).getRGB(), 5);
        }

        Scissor.enable(x, y, getWidth(), contentHeight, 2);

        Render2D.gradientRect(x + getWidth() - 18.5f, y + 5, 14, 12,
                new int[]{
                        new Color(52, 52, 52, bgAlpha).getRGB(),
                        new Color(52, 52, 52, bgAlpha).getRGB(),
                        new Color(52, 52, 52, bgAlpha).getRGB(),
                        new Color(52, 52, 52, bgAlpha).getRGB()
                },
                3);

        Fonts.ICONS.draw("E", x + getWidth() - 15.5f, y + 7.5f, 8, new Color(165, 165, 165, bgAlpha).getRGB());
        Fonts.BOLD.draw("Staff", x + 8, y + 6.5f, 6, new Color(255, 255, 255, bgAlpha).getRGB());

        int moduleOffset = 23;

        Identifier exampleSkin = getCachedSkin();

        if (showExample) {
            String name = "ExampleStaff";

            float faceX = x + 8;
            float faceY = y + moduleOffset - 2f;

            drawFace(exampleSkin, faceX, faceY, bgAlpha);

            float nameX = x + 8 + FACE_SIZE + 4;
            Fonts.BOLD.draw(name, nameX, y + moduleOffset - 1.5f, 6, new Color(255, 255, 255, bgAlpha).getRGB());

            float circleX = x + getWidth() - 14f;
            float circleY = y + moduleOffset - 0.5f;

            drawStatusCircle(circleX, circleY, bgAlpha);

        } else if (hasAnimatingStaff) {
            for (Map.Entry<String, Float> entry : staffAnimations.entrySet()) {
                String id = entry.getKey();
                float animation = entry.getValue();
                if (animation <= 0) continue;

                StaffInfo info = staffMap.get(id);
                if (info == null) continue;

                Identifier skinToUse = info.skin != null ? info.skin : STEVE_SKIN;

                int textAlpha = (int) (255 * animation * alphaFactor);

                float faceX = x + 8;
                float faceY = y + moduleOffset - 2f;

                drawFace(skinToUse, faceX, faceY, textAlpha);

                float nameX = faceX + FACE_SIZE + 4;
                Fonts.BOLD.draw(info.name, nameX, y + moduleOffset - 1.5f, 6, new Color(255, 255, 255, textAlpha).getRGB());

                float circleX = x + getWidth() - 14f;
                float circleY = y + moduleOffset - 0.5f;

                drawStatusCircle(circleX, circleY, textAlpha);

                moduleOffset += (int) (animation * 11);
            }
        }

        Scissor.disable();
    }

    private void drawFace(Identifier skin, float faceX, float faceY, int alpha) {
        int color = new Color(255, 255, 255, alpha).getRGB();

        Render2D.texture(skin, faceX, faceY, FACE_SIZE, FACE_SIZE,
                8f / 64f, 8f / 64f, 16f / 64f, 16f / 64f, color, 0, 2f);

        float hatScale = 1.15f;
        float hatSize = FACE_SIZE * hatScale;
        float hatOffset = (hatSize - FACE_SIZE) / 2f;

        Render2D.texture(skin, faceX - hatOffset, faceY - hatOffset, hatSize, hatSize,
                40f / 64f, 8f / 64f, 48f / 64f, 16f / 64f, color, 0, 2f);

        Render2D.blur(faceX, faceY, 1, 1, 0f, 0, ColorUtil.rgba(0, 0, 0, 0));
    }

    private void drawStatusCircle(float circleX, float circleY, int alpha) {
        Render2D.gradientRect(circleX - 3, circleY - 2, 11, 9,
                new int[]{
                        new Color(52, 52, 52, alpha).getRGB(),
                        new Color(52, 52, 52, alpha).getRGB(),
                        new Color(52, 52, 52, alpha).getRGB(),
                        new Color(52, 52, 52, alpha).getRGB()
                },
                3);

        Render2D.outline(circleX - 3, circleY - 2, 11, 9, 0.35f, new Color(90, 90, 90, alpha).getRGB(), 3);
        Render2D.rect(circleX, circleY, CIRCLE_SIZE, CIRCLE_SIZE, new Color(255, 80, 80, alpha).getRGB(), CIRCLE_SIZE / 2f);
    }
}