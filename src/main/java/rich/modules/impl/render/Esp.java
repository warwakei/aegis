package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Vector4d;
import rich.events.api.EventHandler;
import rich.events.impl.DrawEvent;
import rich.events.impl.TickEvent;
import rich.events.impl.WorldLoadEvent;
import rich.events.impl.WorldRenderEvent;
import rich.modules.impl.combat.AntiBot;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.BooleanSetting;
import rich.modules.module.setting.implement.ColorSetting;
import rich.modules.module.setting.implement.MultiSelectSetting;
import rich.modules.module.setting.implement.SelectSetting;
import rich.modules.module.setting.implement.SliderSettings;
import rich.util.ColorUtil;
import rich.util.Instance;
import rich.util.math.Projection;
import rich.util.modules.esp.RwPrefix;
import rich.util.network.Network;
import rich.util.render.Render2D;
import rich.util.render.font.Fonts;
import rich.util.render.item.ItemRender;
import rich.util.render.Render3D;
import rich.util.repository.friend.FriendUtils;
import rich.util.string.PlayerInteractionHelper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class Esp extends ModuleStructure {

    public static Esp getInstance() {
        return Instance.get(Esp.class);
    }

    Identifier TEXTURE = Identifier.of("rich", "textures/features/esp/container.png");

    List<PlayerEntity> players = new ArrayList<>();

    public MultiSelectSetting entityType = new MultiSelectSetting("Тип сущности", "Сущности, которые будут отображаться")
            .value("Player", "Item").selected("Player", "Item");

    MultiSelectSetting playerSetting = new MultiSelectSetting("Настройки игрока", "Настройки для игроков")
            .value("Box", "Armor", "NameTags", "Hand Items").selected("Box", "Armor", "NameTags", "Hand Items")
            .visible(() -> entityType.isSelected("Player"));

    public SelectSetting boxType = new SelectSetting("Тип", "Тип")
            .value("Corner", "3D Box").selected("Corner")
            .visible(() -> playerSetting.isSelected("Box"));

    public ColorSetting boxColor = new ColorSetting("Цвет бокса", "Цвет для отображения бокса")
            .value(0xFFFFAA00)
            .visible(() -> playerSetting.isSelected("Box"));

    public ColorSetting friendColor = new ColorSetting("Цвет друга", "Цвет для отображения друзей")
            .value(0xFF00FF00)
            .visible(() -> playerSetting.isSelected("Box"));

    public BooleanSetting flatBoxOutline = new BooleanSetting("Контур", "Контур для плоских боксов")
            .visible(() -> playerSetting.isSelected("Box") && boxType.isSelected("Corner"));

    public SliderSettings boxAlpha = new SliderSettings("Прозрачность", "Прозрачность бокса")
            .setValue(1.0F).range(0.1F, 1.0F).visible(() -> boxType.isSelected("3D Box"));

    private static final float DISTANCE = 128.0f;
    private static final int GRAY_COLOR = 0xFF888888;
    private static final int WHITE_COLOR = 0xFFFFFFFF;

    public Esp() {
        super("Esp", "Esp", ModuleCategory.RENDER);
        settings(entityType, playerSetting, boxType, boxColor, friendColor, flatBoxOutline, boxAlpha);
    }

    @EventHandler
    public void onWorldLoad(WorldLoadEvent e) {
        players.clear();
    }

    @EventHandler
    public void onTick(TickEvent e) {
        players.clear();
        if (mc.world != null) {
            mc.world.getPlayers().stream()
                    .filter(player -> player != mc.player)
                    .filter(player -> player.getCustomName() == null || !player.getCustomName().getString().startsWith("Ghost_"))
                    .forEach(players::add);
        }
    }

    @EventHandler
    public void onWorldRender(WorldRenderEvent e) {
        if (!entityType.isSelected("Player")) return;
        float tickDelta = e.getPartialTicks();

        for (PlayerEntity player : players) {
            if (player == null || player == mc.player) continue;
            if (player.getCustomName() != null && player.getCustomName().getString().startsWith("Ghost_")) continue;

            double interpX = MathHelper.lerp(tickDelta, player.lastX, player.getX());
            double interpY = MathHelper.lerp(tickDelta, player.lastY, player.getY());
            double interpZ = MathHelper.lerp(tickDelta, player.lastZ, player.getZ());
            Vec3d interpCenter = new Vec3d(interpX, interpY, interpZ);

            float distance = (float) mc.gameRenderer.getCamera().getCameraPos().distanceTo(interpCenter);
            if (distance < 1) continue;

            boolean friend = FriendUtils.isFriend(player);
            int baseColor = friend ? getFriendColor() : getClientColor();
            int alpha = (int) (boxAlpha.getValue() * 255);
            int fillColor = (baseColor & 0x00FFFFFF) | (alpha << 24);
            int outlineColor = baseColor | 0xFF000000;

            if (boxType.isSelected("3D Box") && playerSetting.isSelected("Box")) {
                Box interpBox = player.getDimensions(player.getPose()).getBoxAt(interpX, interpY, interpZ);
                Render3D.drawBox(interpBox, fillColor, 2, true, true, false);
                Render3D.drawBox(interpBox, outlineColor, 2, true, false, false);
            }
        }
    }

    @EventHandler
    public void onDraw(DrawEvent e) {
        DrawContext context = e.getDrawContext();
        float tickDelta = e.getPartialTicks();
        float size = 5.5f;

        if (entityType.isSelected("Player")) {
            for (PlayerEntity player : players) {
                if (player == null || player == mc.player) continue;
                if (player.getCustomName() != null && player.getCustomName().getString().startsWith("Ghost_")) continue;

                Vector4d vec4d = Projection.getVector4D(player, tickDelta);
                float distance = (float) mc.gameRenderer.getCamera().getCameraPos().distanceTo(player.getBoundingBox().getCenter());
                boolean friend = FriendUtils.isFriend(player);

                if (distance < 1) continue;
                if (Projection.cantSee(vec4d)) continue;

                if (playerSetting.isSelected("Box") && !boxType.isSelected("3D Box")) {
                    drawBox(friend, vec4d, player);
                }
                if (playerSetting.isSelected("Armor")) {
                    drawArmor(context, player, vec4d);
                }
                if (playerSetting.isSelected("Hand Items")) {
                    drawHands(context, player, vec4d, size);
                }

                drawPlayerName(context, player, friend, Projection.centerX(vec4d), vec4d.y - 2, size);
            }
        }

        List<Entity> entities = PlayerInteractionHelper.streamEntities()
                .sorted(Comparator.comparing(ent -> ent instanceof ItemEntity item && item.getStack().getName().getContent().toString().equals("empty")))
                .toList();

        for (Entity entity : entities) {
            if (entity instanceof ItemEntity item && entityType.isSelected("Item")) {
                Vector4d vec4d = Projection.getVector4D(entity, tickDelta);
                ItemStack stack = item.getStack();
                ContainerComponent compoundTag = stack.get(DataComponentTypes.CONTAINER);
                List<ItemStack> list = compoundTag != null ? compoundTag.stream().toList() : List.of();

                if (Projection.cantSee(vec4d)) continue;

                String text = item.getStack().getName().getString();

                if (!list.isEmpty()) {
                    drawShulkerBox(context, stack, list, vec4d);
                } else {
                    drawText(context, text, Projection.centerX(vec4d), vec4d.y, size);
                }
            }
        }
    }

    private void drawPlayerName(DrawContext context, PlayerEntity player, boolean friend, double centerX, double startY, float size) {
        StringBuilder extraInfo = new StringBuilder();
        if (friend) extraInfo.append("[Friend] ");
        if (AntiBot.getInstance() != null && AntiBot.getInstance().isState() && AntiBot.getInstance().isBot(player)) {
            extraInfo.append("[BOT] ");
        }

        String displayName;
        if (playerSetting.isSelected("NameTags")) {
            displayName = player.getDisplayName().getString();
        } else {
            displayName = player.getName().getString();
        }

        RwPrefix.ParsedName parsed = RwPrefix.parseDisplayName(displayName);

        String sphere = "";
        ItemStack offHand = player.getStackInHand(Hand.OFF_HAND);
        if (offHand.getItem().equals(Items.PLAYER_HEAD) || offHand.getItem().equals(Items.TOTEM_OF_UNDYING)) {
            sphere = getSphere(offHand);
        }

        String prefixPart = "";
        if (!parsed.prefix.isEmpty()) {
            prefixPart = parsed.prefix + " ";
        }
        String namePart = parsed.name;
        String clanPart = !parsed.clan.isEmpty() ? " " + parsed.clan : "";
        String spherePart = sphere;
        String extraPart = extraInfo.toString();

        float extraWidth = extraPart.isEmpty() ? 0 : Fonts.TEST.getWidth(extraPart, size);
        float prefixWidth = prefixPart.isEmpty() ? 0 : Fonts.TEST.getWidth(prefixPart, size);
        float nameWidth = Fonts.TEST.getWidth(namePart, size);
        float clanWidth = clanPart.isEmpty() ? 0 : Fonts.TEST.getWidth(clanPart, size);
        float sphereWidth = spherePart.isEmpty() ? 0 : Fonts.TEST.getWidth(spherePart, size);

        float totalWidth = extraWidth + prefixWidth + nameWidth + clanWidth + sphereWidth;
        float height = Fonts.TEST.getHeight(size);

        float posX = (float) centerX - totalWidth / 2;
        float posY = (float) startY - height;

        Render2D.rect(posX - 4, posY - 1.25f, totalWidth + 8, height + 2, 0x80000000, 2f);

        float drawX = posX;

        if (!extraPart.isEmpty()) {
            Fonts.TEST.draw(extraPart, drawX, posY, size, friend ? getFriendColor() : 0xFFFF5555);
            drawX += extraWidth;
        }

        if (!prefixPart.isEmpty()) {
            Fonts.TEST.draw(prefixPart, drawX, posY, size, GRAY_COLOR);
            drawX += prefixWidth;
        }

        Fonts.TEST.draw(namePart, drawX, posY, size, WHITE_COLOR);
        drawX += nameWidth;

        if (!clanPart.isEmpty()) {
            Fonts.TEST.draw(clanPart, drawX, posY, size, GRAY_COLOR);
            drawX += clanWidth;
        }

        if (!spherePart.isEmpty()) {
            Fonts.TEST.draw(spherePart, drawX, posY, size, GRAY_COLOR);
        }
    }

    private void drawBox(boolean friend, Vector4d vec, PlayerEntity player) {
        int client = friend ? getFriendColor() : getClientColor();
        int black = 0x80000000;

        float posX = (float) vec.x;
        float posY = (float) vec.y;
        float endPosX = (float) vec.z;
        float endPosY = (float) vec.w;
        float size = (endPosX - posX) / 3;

        if (boxType.isSelected("Corner")) {
            Render2D.rect(posX - 0.5F, posY - 0.5F, size, 0.5F, client);
            Render2D.rect(posX - 0.5F, posY, 0.5F, size + 0.5F, client);
            Render2D.rect(posX - 0.5F, endPosY - size - 0.5F, 0.5F, size, client);
            Render2D.rect(posX - 0.5F, endPosY - 0.5F, size, 0.5F, client);
            Render2D.rect(endPosX - size + 1, posY - 0.5F, size, 0.5F, client);
            Render2D.rect(endPosX + 0.5F, posY, 0.5F, size + 0.5F, client);
            Render2D.rect(endPosX + 0.5F, endPosY - size - 0.5F, 0.5F, size, client);
            Render2D.rect(endPosX - size + 1, endPosY - 0.5F, size, 0.5F, client);

            if (flatBoxOutline.isValue()) {
                Render2D.rect(posX - 1F, posY - 1, size + 1, 1.5F, black);
                Render2D.rect(posX - 1F, posY + 0.5F, 1.5F, size + 0.5F, black);
                Render2D.rect(posX - 1F, endPosY - size - 1, 1.5F, size, black);
                Render2D.rect(posX - 1F, endPosY - 1, size + 1, 1.5F, black);
                Render2D.rect(endPosX - size + 0.5F, posY - 1, size + 1, 1.5F, black);
                Render2D.rect(endPosX, posY + 0.5F, 1.5F, size + 0.5F, black);
                Render2D.rect(endPosX, endPosY - size - 1, 1.5F, size, black);
                Render2D.rect(endPosX - size + 0.5F, endPosY - 1, size + 1, 1.5F, black);
            }
        }
    }

    private void drawArmor(DrawContext context, PlayerEntity player, Vector4d vec) {
        List<ItemStack> items = new ArrayList<>();
        for (EquipmentSlot slot : EquipmentSlot.VALUES) {
            ItemStack stack = player.getEquippedStack(slot);
            if (!stack.isEmpty()) {
                items.add(stack);
            }
        }

        float posX = (float) (Projection.centerX(vec) - items.size() * 4.5f);
        float posY = (float) (vec.y - 20);
        float offset = 0;

        for (ItemStack stack : items) {
            ItemRender.drawItemWithContext(context, stack, posX + offset, posY, 0.5F, 1.0F);
            offset += 11;
        }
    }

    private void drawHands(DrawContext context, PlayerEntity player, Vector4d vec, float size) {
        double posY = vec.w;

        ItemStack mainHand = player.getStackInHand(Hand.MAIN_HAND);
        ItemStack offHand = player.getStackInHand(Hand.OFF_HAND);

        for (ItemStack stack : new ItemStack[]{mainHand, offHand}) {
            if (stack.isEmpty()) continue;
            String text = stack.getName().getString();
            posY += Fonts.TEST.getHeight(size) / 2.0 + 6;
            drawText(context, text, Projection.centerX(vec), posY, size);
        }
    }

    private void drawShulkerBox(DrawContext context, ItemStack itemStack, List<ItemStack> stacks, Vector4d vec) {
        int width = 176;
        int height = 67;
        int color = ((BlockItem) itemStack.getItem()).getBlock().getDefaultMapColor().color | 0xFF000000;

        float scale = 0.5F;
        float scaledWidth = width * scale;
        float scaledHeight = height * scale;

        float drawX = (float) Projection.centerX(vec) - scaledWidth / 2;
        float drawY = (float) vec.y - scaledHeight - 2;

        Render2D.texture(TEXTURE, drawX, drawY, scaledWidth, scaledHeight, color);
        Render2D.blur(drawX, drawY, 1, 1, 0f, 0, ColorUtil.rgba(0, 0, 0, 0));

        float itemScale = scale;
        float itemStartX = drawX + 7 * scale;
        float itemStartY = drawY + 6 * scale;
        float itemSize = 18 * scale;

        int col = 0;
        int row = 0;
        for (ItemStack stack : stacks) {
            float itemX = itemStartX + col * itemSize;
            float itemY = itemStartY + row * itemSize;
            ItemRender.drawItemWithContext(context, stack, itemX, itemY, itemScale, 1F);
            col++;
            if (col >= 9) {
                row++;
                col = 0;
            }
        }
    }

    private void drawText(DrawContext context, String text, double startX, double startY, float size) {
        String cleanText = RwPrefix.stripFormatting(text);
        float width = Fonts.TEST.getWidth(cleanText, size);
        float height = Fonts.TEST.getHeight(size);
        float posX = (float) (startX - width / 2);
        float posY = (float) startY - height;

        Render2D.rect(posX - 4, posY - 1, width + 8, height + 2, 0x80000000, 2f);
        Fonts.TEST.draw(cleanText, posX, posY, size, 0xFFFFFFFF);
    }

    private String getSphere(ItemStack stack) {
        var component = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (Network.isFunTime() && component != null) {
            NbtCompound compound = component.copyNbt();
            int tslevel = compound.getInt("tslevel").orElse(0);
            if (tslevel != 0) {
                String donItem = compound.getString("don-item").orElse("");
                return " [" + donItem.replace("sphere-", "").toUpperCase() + "]";
            }
        }
        return "";
    }

    private float getHealth(PlayerEntity player) {
        return player.getHealth() + player.getAbsorptionAmount();
    }

    private String getHealthString(float hp) {
        return String.format("%.1f", hp).replace(",", ".").replace(".0", "");
    }

    private int getFriendColor() {
        return friendColor.getColorNoAlpha();
    }

    private int getClientColor() {
        return boxColor.getColorNoAlpha();
    }
}