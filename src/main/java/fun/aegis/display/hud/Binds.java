package fun.aegis.display.hud;

import fun.aegis.utils.interactions.interact.PlayerInteractionHelper;
import fun.aegis.utils.interactions.inv.InventoryTask;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import fun.aegis.utils.client.managers.api.draggable.AbstractDraggable;
import fun.aegis.features.impl.misc.ServerHelper;
import fun.aegis.features.impl.render.Hud;
import fun.aegis.common.animation.Animation;
import fun.aegis.common.animation.Direction;
import fun.aegis.common.animation.implement.Decelerate;
import fun.aegis.utils.display.font.FontRenderer;
import fun.aegis.utils.display.font.Fonts;
import fun.aegis.utils.display.shape.ShapeProperties;
import fun.aegis.utils.display.color.ColorAssist;
import fun.aegis.utils.display.glow.GlowEffect;
import fun.aegis.utils.client.chat.StringHelper;
import net.minecraft.client.render.DiffuseLighting;
import fun.aegis.events.container.SetScreenEvent;
import com.mojang.blaze3d.systems.RenderSystem;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class Binds extends AbstractDraggable {
    private final ServerHelper serverHelper;
    private final List<BindInfo> binds = new ArrayList<>();
    private final Map<Item, Long> cooldownStartTimes = new HashMap<>();
    private final Map<Item, Integer> itemCounts = new HashMap<>();
    private final Set<Item> activeCooldowns = new HashSet<>();
    private static final int BINDS_PER_ROW = 5;
    private float width;
    private float height;
    private long lastBindChange = 0;
    private java.lang.String currentRandomKey1 = "None";
    private java.lang.String currentRandomKey2 = "None";
    private java.lang.String currentRandomKey3 = "None";
    private int currentItemIndex1 = 0;
    private int currentItemIndex2 = 1;
    private int currentItemIndex3 = 2;
    private static final Item[] EXAMPLE_ITEMS = {Items.ENDER_EYE, Items.SUGAR, Items.DRIED_KELP, Items.NETHERITE_SCRAP};
    private final Animation animation = new Decelerate().setMs(300).setValue(1);

    private static class BindInfo {
        ServerHelper.KeyBind keyBind;
        java.lang.String displayName;
        int color;
        java.lang.String searchName;

        BindInfo(ServerHelper.KeyBind keyBind, java.lang.String displayName, int color, java.lang.String searchName) {
            this.keyBind = keyBind;
            this.displayName = displayName;
            this.color = color;
            this.searchName = searchName;
        }
    }

    public Binds() {
        super("Бинды", 10, 180, 150, 40, true);
        this.serverHelper = ServerHelper.getInstance();
        initializeBinds();
    }

    private void initializeBinds() {
        for (ServerHelper.KeyBind keyBind : serverHelper.getKeyBindings()) {
            java.lang.String name = keyBind.setting().getName();
            java.lang.String searchName = getSearchNameForBind(name);
            int color = getColorForBind(name);
            binds.add(new BindInfo(keyBind, name, color, searchName));
        }
    }

    private java.lang.String getSearchNameForBind(java.lang.String name) {
        return switch (name) {
            case "Зелье отрыжки" -> "отрыжки";
            case "Зелье серной кислоты" -> "серная";
            case "Зелье вспышки" -> "вспышка";
            case "Зелье мочи Флеша" -> "моча флеша";
            case "Зелье победителя" -> "победителя";
            case "Зелье агента" -> "агента";
            case "Зелье медика" -> "медика";
            case "Зелье киллера" -> "киллера";
            default -> null;
        };
    }

    private int getColorForBind(java.lang.String name) {
        return switch (name) {
            case "Зелье отрыжки" -> 0xFF5D00;
            case "Зелье серной кислоты" -> 0x00C200;
            case "Зелье вспышки" -> 0xFFFFFF;
            case "Зелье мочи Флеша" -> 0x5CF7FF;
            case "Зелье победителя" -> 0x00FF00;
            case "Зелье агента" -> 0xFFFB00;
            case "Зелье медика" -> 0xFF00DE;
            case "Зелье киллера" -> 0xFF0000;
            default -> -1;
        };
    }

    private ItemStack createColoredPotion(Item item, int color) {
        ItemStack stack = new ItemStack(item);
        if (color != -1 && item == Items.SPLASH_POTION) {
            stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.of(color), List.of(), Optional.empty()));
        }
        return stack;
    }

    private void renderItemStack(DrawContext context, ItemStack stack, float x, float y, float scale) {
        MatrixStack matrices = context.getMatrices();
        matrices.push();
        matrices.translate(x, y, 100);
        matrices.scale(scale, scale, 1);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        DiffuseLighting.enableGuiDepthLighting();

        context.drawItem(stack, 0, 0);

        DiffuseLighting.disableGuiDepthLighting();
        matrices.pop();
    }

    private int countItemInInventory(Item targetItem, java.lang.String searchName) {
        if (PlayerInteractionHelper.nullCheck()) return 0;

        int count = 0;
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (itemStack.isEmpty()) continue;

            if (searchName != null && targetItem == Items.SPLASH_POTION) {
                if (itemStack.getItem() == targetItem &&
                        InventoryTask.getCleanName(itemStack.getName()).contains(searchName.toLowerCase())) {
                    count += itemStack.getCount();
                }
            } else {
                if (itemStack.getItem() == targetItem) {
                    count += itemStack.getCount();
                }
            }
        }
        return count;
    }

    private float getCooldownProgress(Item item) {
        if (PlayerInteractionHelper.nullCheck()) return 0;
        ItemStack stack = new ItemStack(item);
        return mc.player.getItemCooldownManager().getCooldownProgress(stack, mc.getRenderTickCounter().getTickDelta(false));
    }

    private Color getProgressColor(float progress) {
        if (progress <= 0.5f) {
            float t = progress * 2f;
            int r = (int) (255 * (1 - t) + 255 * t);
            int g = (int) (0 * (1 - t) + 165 * t);
            int b = 0;
            return new Color(r, g, b);
        } else {
            float t = (progress - 0.5f) * 2f;
            int r = (int) (255 * (1 - t) + 0 * t);
            int g = (int) (165 * (1 - t) + 255 * t);
            int b = 0;
            return new Color(r, g, b);
        }
    }

    @Override
    public boolean visible() {
        return Hud.getInstance().interfaceSettings.isSelected("Бинды") && Hud.getInstance().state && !animation.isFinished(Direction.BACKWARDS);
    }

    @Override
    public void tick() {
        if (PlayerInteractionHelper.nullCheck()) {
            animation.setDirection(Direction.BACKWARDS);
            return;
        }
        List<BindInfo> activeBinds = binds.stream().filter(this::isBindActive).toList();
        if (!activeBinds.isEmpty()) {
            animation.setDirection(Direction.FORWARDS);
        } else if (PlayerInteractionHelper.isChat(mc.currentScreen)) {
            animation.setDirection(Direction.FORWARDS);
        } else {
            animation.setDirection(Direction.BACKWARDS);
        }

        itemCounts.clear();
        for (BindInfo bind : binds) {
            int count = countItemInInventory(bind.keyBind.item(), bind.searchName);
            itemCounts.put(bind.keyBind.item(), count);
        }

        activeCooldowns.clear();
        long currentTime = System.currentTimeMillis();
        for (int i = 0; i < mc.player.getInventory().size(); i++) {
            ItemStack itemStack = mc.player.getInventory().getStack(i);
            if (!itemStack.isEmpty() && mc.player.getItemCooldownManager().isCoolingDown(itemStack)) {
                Item item = itemStack.getItem();
                if (!cooldownStartTimes.containsKey(item)) {
                    cooldownStartTimes.put(item, currentTime);
                }
                activeCooldowns.add(item);
            }
        }
        cooldownStartTimes.keySet().removeIf(item -> !activeCooldowns.contains(item));
        if (activeBinds.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            if (currentTime - lastBindChange >= 1000) {
                List<java.lang.String> availableKeys = List.of("A", "B", "C", "D", "E");
                currentRandomKey1 = availableKeys.get(new Random().nextInt(availableKeys.size()));
                currentRandomKey2 = availableKeys.get(new Random().nextInt(availableKeys.size()));
                currentRandomKey3 = availableKeys.get(new Random().nextInt(availableKeys.size()));
                currentItemIndex1 = (currentItemIndex1 + 1) % EXAMPLE_ITEMS.length;
                currentItemIndex2 = (currentItemIndex2 + 1) % EXAMPLE_ITEMS.length;
                currentItemIndex3 = (currentItemIndex3 + 1) % EXAMPLE_ITEMS.length;
                lastBindChange = currentTime;
            }
        }
    }

    private boolean isBindActive(BindInfo bind) {
        if (PlayerInteractionHelper.nullCheck() || !bind.keyBind.setting().isVisible() || bind.keyBind.setting().getKey() == -1 || bind.keyBind.setting().getKey() == 0) {
            return false;
        }
        return true;
    }

    @Override
    public void setScreen(SetScreenEvent e) {
        super.setScreen(e);
        if (PlayerInteractionHelper.nullCheck()) {
            animation.setDirection(Direction.BACKWARDS);
            return;
        }
        if (PlayerInteractionHelper.isChat(e.getScreen())) {
            animation.setDirection(Direction.FORWARDS);
        } else {
            List<BindInfo> activeBinds = binds.stream().filter(this::isBindActive).toList();
            if (!activeBinds.isEmpty()) {
                animation.setDirection(Direction.FORWARDS);
            } else {
                animation.setDirection(Direction.BACKWARDS);
            }
        }
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (!Hud.getInstance().interfaceSettings.isSelected("Бинды") || !Hud.getInstance().state || animation.isFinished(Direction.BACKWARDS)) return;
        MatrixStack matrix = context.getMatrices();
        float animationValue = animation.getOutput().floatValue();
        if (animationValue <= 0) return;
        List<BindInfo> activeBinds = binds.stream().filter(this::isBindActive).collect(Collectors.toList());
        float padding = 3;
        float iconSize = 16;
        float spacing = 3;
        float rowHeight = iconSize + 2 * padding;
        float posX = getX();
        float posY = getY();
        int totalBinds = activeBinds.size();
        int rowCount = (int) Math.ceil((double) totalBinds / BINDS_PER_ROW);
        List<Float> rowWidths = new ArrayList<>();
        float firstRowWidth = 0;
        FontRenderer font = Fonts.getSize(13, Fonts.Type.DEFAULT);
        if (activeBinds.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            rowCount = 1;
            java.lang.String name = "Example Bind";
            java.lang.String[] keys = {currentRandomKey1, currentRandomKey2, currentRandomKey3};
            float textWidth = font.getStringWidth(Arrays.stream(keys).max((a, b) -> Float.compare(font.getStringWidth(a), font.getStringWidth(b))).orElse(""));
            float bindWidth = iconSize + padding + textWidth + padding + 4;
            float totalWidth = bindWidth * 3 + spacing * 2;
            rowWidths.add(totalWidth);
            firstRowWidth = totalWidth;
        } else {
            for (int row = 0; row < rowCount; row++) {
                float currentX = padding;
                int bindsInThisRow = Math.min(BINDS_PER_ROW, totalBinds - row * BINDS_PER_ROW);
                for (int i = 0; i < bindsInThisRow; i++) {
                    int bindIndex = row * BINDS_PER_ROW + i;
                    BindInfo bind = activeBinds.get(bindIndex);
                    java.lang.String keyName = StringHelper.getBindName(bind.keyBind.setting().getKey());
                    float textWidth = font.getStringWidth(keyName);
                    float bindWidth = iconSize + padding + textWidth + padding + 4;
                    currentX += bindWidth + spacing;
                }
                if (bindsInThisRow > 0) {
                    currentX -= spacing;
                }
                rowWidths.add(currentX);
                if (row == 0) {
                    firstRowWidth = currentX;
                }
            }
        }
        width = firstRowWidth + padding;
        height = rowHeight * rowCount;
        if (activeBinds.isEmpty() && PlayerInteractionHelper.isChat(mc.currentScreen)) {
            java.lang.String name = "Example Bind";
            java.lang.String[] keys = {currentRandomKey1, currentRandomKey2, currentRandomKey3};
            ItemStack[] stacks = {new ItemStack(EXAMPLE_ITEMS[currentItemIndex1]), new ItemStack(EXAMPLE_ITEMS[currentItemIndex2]), new ItemStack(EXAMPLE_ITEMS[currentItemIndex3])};
            float offsetX = padding;
            float currentY = posY;
            for (int i = 0; i < 3; i++) {
                float textWidth = Fonts.getSize(11, Fonts.Type.DEFAULT).getStringWidth(keys[i]) + 1.5f;
                float backgroundWidth = iconSize + padding + textWidth + padding + 4;

                blur.render(ShapeProperties.create(matrix, posX + offsetX - 1, currentY, backgroundWidth, rowHeight - 6)
                        .round(3).quality(12)
                        .color(new Color(0, 0, 0, 150).getRGB())
                        .build());

                rectangle.render(ShapeProperties.create(matrix, posX + offsetX - 1, currentY, backgroundWidth, rowHeight - 6)
                        .round(3)
                        .thickness(0.1f)
                        .outlineColor(new Color(33, 33, 33, 255).getRGB())
                        .color(
                                new Color(18, 19, 20, 35).getRGB(),
                                new Color(18, 19, 20, 35).getRGB(),
                                new Color(0, 2, 5, 35).getRGB(),
                                new Color(0, 2, 5, 35).getRGB())
                        .build());

                rectangle.render(ShapeProperties.create(matrix, posX + offsetX - 1, currentY, 16, rowHeight - 6)
                        .round(0, 0, 3, 3)
                        .outlineColor(new Color(33, 33, 33, 255).getRGB())
                        .color(
                                new Color(18, 19, 20, 35).getRGB(),
                                new Color(18, 19, 20, 35).getRGB(),
                                new Color(0, 2, 5, 35).getRGB(),
                                new Color(0, 2, 5, 35).getRGB())
                        .build());

                renderItemStack(context, stacks[i], posX + offsetX + 2, currentY + padding + 0.5f, 0.5f);

                float textX = posX + offsetX + iconSize + padding;
                float textY = currentY + padding + (iconSize - Fonts.getSize(11, Fonts.Type.DEFAULT).getStringHeight(keys[i])) / 2;
                int textAlpha = (int) Math.min(255, Math.max(0, 255 * animationValue));
                Fonts.getSize(11, Fonts.Type.DEFAULT).drawString(matrix, keys[i], textX + 1f, textY + 3f, new Color(225, 225, 255, textAlpha).getRGB());

                Fonts.getSize(10, Fonts.Type.DEFAULT).drawString(matrix, "64", posX + offsetX + 7, currentY + rowHeight - 12, new Color(255, 255, 255, textAlpha).getRGB());

                offsetX += backgroundWidth + spacing;
            }
        } else {
            for (int row = 0; row < rowCount; row++) {
                float rowWidth = rowWidths.get(row);
                float offsetX = row == 0 ? padding : padding + (firstRowWidth - rowWidth) / 2;
                float currentY = posY + row * rowHeight;
                int bindsInThisRow = Math.min(BINDS_PER_ROW, totalBinds - row * BINDS_PER_ROW);
                for (int i = 0; i < bindsInThisRow; i++) {
                    int bindIndex = row * BINDS_PER_ROW + i;
                    BindInfo bind = activeBinds.get(bindIndex);
                    ItemStack stack = createColoredPotion(bind.keyBind.item(), bind.color);
                    java.lang.String keyName = StringHelper.getBindName(bind.keyBind.setting().getKey());
                    float textWidth = Fonts.getSize(11, Fonts.Type.DEFAULT).getStringWidth(keyName) + 1.5f;
                    float backgroundWidth = iconSize + padding + textWidth + padding + 4;

                    int itemCount = itemCounts.getOrDefault(bind.keyBind.item(), 0);
                    boolean isEmpty = itemCount == 0;

                    float cooldownProgress = getCooldownProgress(bind.keyBind.item());
                    float totalHeight = rowHeight - 6;
                    float cooldownHeight = totalHeight * cooldownProgress;
                    float cooldownStartY = currentY + totalHeight - cooldownHeight;

                    blur.render(ShapeProperties.create(matrix, posX + offsetX - 1, currentY, backgroundWidth, rowHeight - 6)
                            .round(3).quality(12)
                            .color(new Color(0, 0, 0, 150).getRGB())
                            .build());

                    if (isEmpty) {
                        rectangle.render(ShapeProperties.create(matrix, posX + offsetX - 1, currentY, backgroundWidth, rowHeight - 6)
                                .round(3)
                                .outlineColor(new Color(33, 33, 33, 255).getRGB())
                                .color(
                                        new Color(250, 10, 10, 35).getRGB(),
                                        new Color(250, 10, 10, 35).getRGB(),
                                        new Color(250, 5, 5, 35).getRGB(),
                                        new Color(250, 5, 5, 35).getRGB())
                                .build());

                        rectangle.render(ShapeProperties.create(matrix, posX + offsetX - 1, currentY, 16, rowHeight - 6)
                                .round(0, 0, 3, 3)
                                .outlineColor(new Color(33, 33, 33, 255).getRGB())
                                .color(
                                        new Color(80, 10, 10, 65).getRGB(),
                                        new Color(20, 5, 5, 65).getRGB(),
                                        new Color(20, 5, 5, 65).getRGB(),
                                        new Color(20, 5, 5, 65).getRGB())
                                .build());
                    } else {
                        rectangle.render(ShapeProperties.create(matrix, posX + offsetX - 1, currentY, backgroundWidth, rowHeight - 6)
                                .round(3)
                                .outlineColor(new Color(33, 33, 33, 255).getRGB())
                                .color(
                                        new Color(18, 19, 20, 75).getRGB(),
                                        new Color(18, 19, 20, 75).getRGB(),
                                        new Color(0, 2, 5, 75).getRGB(),
                                        new Color(0, 2, 5, 75).getRGB())
                                .build());

                        if (cooldownProgress > 0) {
                            Color progressColor = getProgressColor(cooldownProgress);
                            Color progressColorBright = new Color(
                                    Math.min(255, (int)(progressColor.getRed() * 1.3)),
                                    Math.min(255, (int)(progressColor.getGreen() * 1.3)),
                                    Math.min(255, (int)(progressColor.getBlue() * 1.3)),
                                    180
                            );
                            Color progressColorDark = new Color(
                                    (int)(progressColor.getRed() * 0.6),
                                    (int)(progressColor.getGreen() * 0.6),
                                    (int)(progressColor.getBlue() * 0.6),
                                    180
                            );

//                            rectangle.render(ShapeProperties.create(matrix, posX + offsetX + 15, cooldownStartY, 15.75f, cooldownHeight)
//                                    .round(cooldownProgress >= 0.85f ? 3 : 0, 3, 0, 0)
//                                    .color(
//                                            progressColorBright.getRGB(),
//                                            progressColorBright.getRGB(),
//                                            progressColorDark.getRGB(),
//                                            progressColorDark.getRGB())
//                                    .build());
                        }

                        rectangle.render(ShapeProperties.create(matrix, posX + offsetX - 1, currentY, 16, rowHeight - 6)
                                .round(0, 0, 3, 3)
                                .outlineColor(new Color(33, 33, 33, 255).getRGB())
                                .color(
                                        new Color(18, 19, 20, 75).getRGB(),
                                        new Color(18, 19, 20, 75).getRGB(),
                                        new Color(0, 2, 5, 75).getRGB(),
                                        new Color(0, 2, 5, 75).getRGB())
                                .build());
                    }

                    renderItemStack(context, stack, posX + offsetX + 2, currentY + padding + 0.5f, 0.5f);

                    float textX = posX + offsetX + iconSize + padding;
                    float textY = currentY + padding + (iconSize - Fonts.getSize(11, Fonts.Type.DEFAULT).getStringHeight(keyName)) / 2;
                    int textAlpha = (int) Math.min(255, Math.max(0, 255 * animationValue));
                    Fonts.getSize(13, Fonts.Type.DEFAULT).drawString(matrix, keyName, textX + 1f, textY + 3f, new Color(225, 225, 255, textAlpha).getRGB());

                    Fonts.getSize(10, Fonts.Type.DEFAULT).drawString(matrix, String.valueOf(itemCount), posX + offsetX + 7, currentY + rowHeight - 12, new Color(255, 255, 255, textAlpha).getRGB());

                    offsetX += backgroundWidth + spacing;
                }
            }
        }
        setWidth((int) width);
        setHeight((int) height);
    }
}
