package fun.aegis.display.hud;

import com.mojang.authlib.GameProfile;
import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.Team;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import fun.aegis.utils.client.managers.api.draggable.AbstractDraggable;
import fun.aegis.common.animation.Animation;
import fun.aegis.common.repository.staff.StaffRepository;
import fun.aegis.common.animation.Direction;
import fun.aegis.common.animation.implement.Decelerate;
import fun.aegis.utils.display.font.FontRenderer;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.glow.GlowEffect;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.display.geometry.Render2D;
import fun.aegis.features.impl.render.Hud;
import fun.aegis.utils.client.packet.network.Network;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.entity.LivingEntity;
import java.util.*;
import java.awt.*;
import java.util.List;
import java.util.regex.Pattern;

public class StaffList extends AbstractDraggable {
    public static StaffList getInstance() {
        return Instance.getDraggable(StaffList.class);
    }

    public final Map<PlayerListEntry, Animation> list = new HashMap<>();
    private final Set<String> notifiedPlayers = new HashSet<>();
    private final Pattern namePattern = Pattern.compile("^\\w{3,16}$");
    private long lastColorChange = 0;
    private int currentColorIndex = 0;
    private static final Map<String, String> CHAR_TO_NAME = new HashMap<>();
    private static final Map<String, Integer> PREFIX_COLORS = new HashMap<>();

    static {
        CHAR_TO_NAME.put("ꔀ", "player");
        CHAR_TO_NAME.put("ꔄ", "hero");
        CHAR_TO_NAME.put("ꔈ", "titan");
        CHAR_TO_NAME.put("ꔒ", "avenger");
        CHAR_TO_NAME.put("ꔖ", "overlord");
        CHAR_TO_NAME.put("ꔠ", "magister");
        CHAR_TO_NAME.put("ꔤ", "imperator");
        CHAR_TO_NAME.put("ꔨ", "dragon");
        CHAR_TO_NAME.put("ꔲ", "bull");
        CHAR_TO_NAME.put("ꕒ", "rabbit");
        CHAR_TO_NAME.put("ꔶ", "tiger");
        CHAR_TO_NAME.put("ꕄ", "dracula");
        CHAR_TO_NAME.put("ꕖ", "bunny");
        CHAR_TO_NAME.put("ꕀ", "hydra");
        CHAR_TO_NAME.put("ꕈ", "cobra");
        CHAR_TO_NAME.put("ꔁ", "media");
        CHAR_TO_NAME.put("ꔅ", "yt");
        CHAR_TO_NAME.put("ꕠ", "d.helper");
        CHAR_TO_NAME.put("ꔉ", "helper");
        CHAR_TO_NAME.put("ꔓ", "ml.moder");
        CHAR_TO_NAME.put("ꔗ", "moder");
        CHAR_TO_NAME.put("ꔡ", "moder+");
        CHAR_TO_NAME.put("ꔥ", "st.moder");
        CHAR_TO_NAME.put("ꔩ", "gl.moder");
        CHAR_TO_NAME.put("ꔳ", "ml.admin");
        CHAR_TO_NAME.put("ꔷ", "admin");

        PREFIX_COLORS.put("media", new Color(255, 0, 0, 255).getRGB());
        PREFIX_COLORS.put("yt", new Color(255, 0, 0, 255).getRGB());
        PREFIX_COLORS.put("d.helper", new Color(255, 255, 0, 255).getRGB());
        PREFIX_COLORS.put("helper", new Color(255, 255, 0, 255).getRGB());
        PREFIX_COLORS.put("ml.moder", new Color(0, 255, 255, 255).getRGB());
        PREFIX_COLORS.put("moder", new Color(0, 0, 255, 255).getRGB());
        PREFIX_COLORS.put("moder+", new Color(0, 0, 255, 255).getRGB());
        PREFIX_COLORS.put("st.moder", new Color(128, 0, 128, 255).getRGB());
        PREFIX_COLORS.put("gl.moder", new Color(128, 0, 128, 255).getRGB());
        PREFIX_COLORS.put("ml.admin", new Color(0, 255, 255, 255).getRGB());
        PREFIX_COLORS.put("admin", new Color(255, 0, 0, 255).getRGB());
        PREFIX_COLORS.put("Vanish", new Color(255, 0, 0, 255).getRGB());
    }

    public StaffList() {
        super("Список стафа", 115, 40, 80, 23, true);
    }

    @Override
    public boolean visible() {
        return !list.isEmpty() || PlayerInteractionHelper.isChat(mc.currentScreen);
    }

    @Override
    public void tick() {
        if (mc.world == null || mc.player == null || mc.getNetworkHandler() == null) {
            list.clear();
            return;
        }

        Collection<PlayerListEntry> playerList = mc.getNetworkHandler().getPlayerList();
        Scoreboard scoreboard = mc.world.getScoreboard();
        Set<String> addedNames = new HashSet<>();

        if (list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastColorChange >= 1000) {
                currentColorIndex = (currentColorIndex + 1) % PREFIX_COLORS.size();
                lastColorChange = System.currentTimeMillis();
            }
            return;
        }

        for (PlayerListEntry entry : playerList) {
            String name = entry.getProfile().getName();
            if (addedNames.contains(name) || list.containsKey(entry)) {
                continue;
            }
            String display = entry.getDisplayName() != null ? entry.getDisplayName().getString() : name;
        }

        for (StaffRepository.Staff staff : StaffRepository.getStaff()) {
            String staffName = staff.getName();
            if (addedNames.contains(staffName) || list.keySet().stream().anyMatch(e -> e.getProfile().getName().equals(staffName))) {
                continue;
            }
            playerList.stream()
                    .filter(p -> p.getProfile().getName().equalsIgnoreCase(staffName))
                    .findFirst()
                    .ifPresent(entry -> {
                        list.put(entry, new Decelerate().setMs(150).setValue(1));
                        addedNames.add(staffName);
                    });
        }

        List<Team> teams = new ArrayList<>(scoreboard.getTeams());
        teams.sort(Comparator.comparing(Team::getName));
        Collection<PlayerListEntry> online = mc.getNetworkHandler().getPlayerList();

        for (Team team : teams) {
            Collection<String> members = team.getPlayerList();
            if (members.size() != 1) {
                continue;
            }
            String name = members.iterator().next();
            if (!namePattern.matcher(name).matches() || addedNames.contains(name)) {
                continue;
            }
            boolean present = online.stream().anyMatch(e -> e.getProfile() != null && name.equals(e.getProfile().getName()));
            if (present) {
                continue;
            }
            if (list.keySet().stream().anyMatch(e -> e.getProfile().getName().equals(name))) {
                continue;
            }
            String teamPrefix = team.getPrefix().getString();
            String prefix = CHAR_TO_NAME.entrySet().stream()
                    .filter(e -> teamPrefix.contains(e.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse("");
            MutableText displayName = Text.empty();
            if (Network.isReallyWorld()) {
                displayName.append(Text.literal(name).formatted(Formatting.GRAY))
                        .append(Text.literal(" [").formatted(Formatting.GRAY))
                        .append(Text.literal(prefix.isEmpty() ? "V" : prefix).formatted(Formatting.RESET))
                        .append(Text.literal("]").formatted(Formatting.GRAY));
            } else {
                displayName.append(Text.literal("[").formatted(Formatting.GRAY))
                        .append(Text.literal(prefix.isEmpty() ? "V" : prefix).formatted(Formatting.RESET))
                        .append(Text.literal("] ").formatted(Formatting.GRAY))
                        .append(Text.literal(name).formatted(Formatting.GRAY));
            }
            GameProfile fakeProfile = new GameProfile(UUID.randomUUID(), name);
            PlayerListEntry fake = new PlayerListEntry(fakeProfile, mc.isInSingleplayer());
            fake.setDisplayName(displayName);
            fake.setListOrder(Integer.MIN_VALUE);
            list.put(fake, new Decelerate().setMs(150).setValue(1));
            addedNames.add(name);
            if (Hud.getInstance().notificationSettings.isSelected("Staff Join") && !notifiedPlayers.contains(name)) {
                Notifications.getInstance().addList(Text.literal(name + " - Зашел на сервер!"), 5000);
                notifiedPlayers.add(name);
            }
        }

        list.entrySet().removeIf(entry -> {
            String name = entry.getKey().getProfile().getName();
            boolean isFromRepo = StaffRepository.isStaff(name);
            boolean inPlayerList = playerList.stream().anyMatch(p -> p.getProfile().getName().equals(name));
            boolean inTeam = scoreboard.getTeams().stream().flatMap(t -> t.getPlayerList().stream()).anyMatch(name::equals);
            boolean shouldRemove = false;
            if (isFromRepo) {
                if (!inPlayerList) {
                    shouldRemove = true;
                }
            } else {
                if (inPlayerList || !inTeam) {
                    shouldRemove = true;
                }
            }
            if (shouldRemove) {
                entry.getValue().setDirection(Direction.BACKWARDS);
            }
            if (entry.getValue().isFinished(Direction.BACKWARDS)) {
                notifiedPlayers.remove(name);
                if (!inPlayerList && Hud.getInstance().notificationSettings.isSelected("Staff Leave")) {
                    Notifications.getInstance().addList(Text.literal(name + " - Вышел с сервера!"), 5000);
                }
                return true;
            }
            return false;
        });
    }

    @Override
    public void drawDraggable(DrawContext context) {
        MatrixStack matrix = context.getMatrices();
        FontRenderer font = Fonts.getSize(13, Fonts.Type.DEFAULT);
        FontRenderer fontPlayer = Fonts.getSize(13, Fonts.Type.DEFAULT);
        FontRenderer icon = Fonts.getSize(19, Fonts.Type.ICONCATACLYSMREG);
        FontRenderer items = Fonts.getSize(12, Fonts.Type.ICONCATACLYSMREG);
        long activeStaff = list.entrySet().stream().filter(e -> !e.getValue().isFinished(Direction.BACKWARDS)).count();
        String staffCountText = String.valueOf(activeStaff);
        float textWidth = items.getStringWidth(staffCountText);
        float boxWidth = textWidth + 6;

        blur.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
                .round(4).softness(10F).thickness(0).color(ColorAssist.getRect(0.4F)).build());

        // Remove rectangle border
        // rectangle.render(ShapeProperties.create(matrix, getX(), getY(), getWidth(), getHeight())
        //         .round(4)
        //         .thickness(0.1f)
        //         .color(
        //                 new Color(18, 19, 20, 35).getRGB(),
        //                 new Color(0, 2, 5, 35).getRGB(),
        //                 new Color(0, 2, 5, 35).getRGB(),
        //                 new Color(18, 19, 20, 35).getRGB())
        //         .build());

        // Перемещаем иконку вправo
        float iconX = getX() + getWidth() - 18;
        icon.drawString(matrix, "G", iconX, getY() + 5f, new Color(225, 225, 255, 255).getRGB());
        font.drawString(matrix, getName(), getX() + 4, getY() + 6.5F, ColorAssist.getText());
        float centerX = getX() + getWidth() / 2.0F;
        int offset = 23;
        int maxWidth = 80;
        Collection<PlayerListEntry> playerList = Objects.requireNonNull(mc.player).networkHandler.getPlayerList();

        for (Map.Entry<PlayerListEntry, Animation> staff : list.entrySet()) {
            PlayerListEntry player = staff.getKey();
            if (player == null) {
                continue;
            }
            String name = player.getProfile().getName();
            float centerY = getY() + offset;
            float animation = staff.getValue().getOutput().floatValue();
            boolean isVisible = playerList.stream().anyMatch(p -> p.getProfile().getName().equals(name));
            PlayerListEntry renderEntry = isVisible ?
                    playerList.stream().filter(p -> p.getProfile().getName().equals(name)).findFirst().orElse(player) :
                    player;
            String displayName = renderEntry.getDisplayName() != null ? renderEntry.getDisplayName().getString() : name;
            final String prefix = CHAR_TO_NAME.entrySet().stream()
                    .filter(e -> displayName.contains(e.getKey()))
                    .map(Map.Entry::getValue)
                    .findFirst()
                    .orElse("Vanish");
            int prefixColor = PREFIX_COLORS.getOrDefault(prefix, new Color(255, 0, 0, 255).getRGB());
            Identifier skinTexture = renderEntry.getSkinTextures().texture();
            int textColor = ColorAssist.getText();
            int textAlpha = 255;
            int colorWithAlpha = ColorAssist.rgba((textColor >> 16) & 255, (textColor >> 8) & 255, textColor & 255, textAlpha);
            float prefixWidth = fontPlayer.getStringWidth(prefix);
            float prefixBoxWidth = prefixWidth + 6;
            Calculate.scale(matrix, centerX, centerY, 1, animation, () -> {
                Render2D.drawTexture(context, skinTexture, getX() + 4.5f, centerY - 1.5f, 8, 3.5f, 8, 8, 64, ColorAssist.getRect(1));
                fontPlayer.drawString(matrix, name, getX() + 19, centerY + 1, colorWithAlpha);
                fontPlayer.drawString(matrix, prefix, getX() + getWidth() - prefixWidth - 8, centerY + 1, prefixColor);
            });
            float width = fontPlayer.getStringWidth(name) + 25 + 10;
            maxWidth = (int) Math.max(width, maxWidth);
            offset += (int) (11 * animation);
        }

        if (list.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            float centerY = getY() + offset;
            String name = "Example Staff";
            final String prefix = "Vanish";
            int textColor = ColorAssist.getText();
            int textAlpha = 255;
            int colorWithAlpha = ColorAssist.rgba((textColor >> 16) & 255, (textColor >> 8) & 255, textColor & 255, textAlpha);
            int prefixColor = PREFIX_COLORS.getOrDefault(prefix, new Color(225, 225, 255, 255).getRGB());
            float prefixWidth = fontPlayer.getStringWidth(prefix);
            float prefixBoxWidth = prefixWidth + 6;
            Calculate.scale(matrix, centerX, centerY, 1, 1, () -> {
                EntityRenderer<? super LivingEntity, ?> baseRenderer = mc.getEntityRenderDispatcher().getRenderer(mc.player);
                if (baseRenderer instanceof LivingEntityRenderer<?, ?, ?>) {
                    LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?> renderer = (LivingEntityRenderer<LivingEntity, LivingEntityRenderState, ?>) baseRenderer;
                    LivingEntityRenderState state = renderer.getAndUpdateRenderState(mc.player, tickCounter.getTickDelta(false));
                    Identifier textureLocation = renderer.getTexture(state);
                    Render2D.drawTexture(context, textureLocation, getX() + 4.5f, centerY - 1.5f, 8, 3, 8, 8, 64, ColorAssist.getRect(1), ColorAssist.multRed(-1, 1));
                }
                fontPlayer.drawString(matrix, name, getX() + 19, centerY + 1, colorWithAlpha);
                fontPlayer.drawString(matrix, prefix, getX() + getWidth() - prefixWidth - 8, centerY + 1, prefixColor);
            });
            int width = (int) fontPlayer.getStringWidth(name) + 25 + 10;
            maxWidth = Math.max(width, maxWidth);
            offset += 11;
        }

        setWidth(maxWidth + 20);
        setHeight(offset);
    }
}
