package rich.screens.clickgui.impl.autobuy.items.customitem;

import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.google.common.collect.ImmutableMultimap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.settings.AutoBuyItemSettings;
import rich.util.config.impl.autobuyconfig.AutoBuyConfig;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class CustomItem implements AutoBuyableItem {
    private final String displayName;
    private final NbtCompound nbt;
    private final Item material;
    private final int price;
    private final PotionContentsComponent potionContents;
    private final List<Text> loreTexts;
    private final AutoBuyItemSettings settings;
    private final boolean hasGlint;
    private boolean enabled;

    public CustomItem(String displayName, NbtCompound nbt, Item material, int price, PotionContentsComponent potionContents, List<Text> loreTexts) {
        this(displayName, nbt, material, price, potionContents, loreTexts, shouldHaveGlint(material, displayName), false);
    }

    public CustomItem(String displayName, NbtCompound nbt, Item material, int price, PotionContentsComponent potionContents, List<Text> loreTexts, boolean hasGlint) {
        this(displayName, nbt, material, price, potionContents, loreTexts, hasGlint, false);
    }

    public CustomItem(String displayName, NbtCompound nbt, Item material, int price, PotionContentsComponent potionContents, List<Text> loreTexts, boolean hasGlint, boolean canHaveQuantity) {
        this.displayName = displayName;
        this.nbt = nbt;
        this.material = material;
        this.price = price;
        this.potionContents = potionContents;
        this.loreTexts = loreTexts;
        this.hasGlint = hasGlint;
        this.settings = new AutoBuyItemSettings(price, material, displayName, canHaveQuantity);
        AutoBuyConfig config = AutoBuyConfig.getInstance();
        if (config.hasItemConfig(displayName)) {
            this.enabled = config.isItemEnabled(displayName);
        } else {
            this.enabled = true;
            config.loadItemSettings(displayName, price);
        }
    }

    public CustomItem(String displayName, NbtCompound nbt, Item material, int price) {
        this(displayName, nbt, material, price, null, null);
    }

    public CustomItem(String displayName, NbtCompound nbt, Item material, int price, boolean canHaveQuantity) {
        this(displayName, nbt, material, price, null, null, shouldHaveGlint(material, displayName), canHaveQuantity);
    }

    public CustomItem(String displayName, NbtCompound nbt, Item material, int price, PotionContentsComponent potionContents, List<Text> loreTexts, int minQuantity) {
        this(displayName, nbt, material, price, potionContents, loreTexts, shouldHaveGlint(material, displayName), true);
    }

    private static boolean shouldHaveGlint(Item material, String displayName) {
        if (material == Items.TOTEM_OF_UNDYING || material == Items.ELYTRA) {
            return false;
        }
        if (material == Items.NETHERITE_HELMET ||
                material == Items.NETHERITE_CHESTPLATE ||
                material == Items.NETHERITE_LEGGINGS ||
                material == Items.NETHERITE_BOOTS ||
                material == Items.NETHERITE_SWORD ||
                material == Items.NETHERITE_PICKAXE ||
                material == Items.NETHERITE_AXE ||
                material == Items.NETHERITE_SHOVEL ||
                material == Items.NETHERITE_HOE ||
                material == Items.DIAMOND_HELMET ||
                material == Items.DIAMOND_CHESTPLATE ||
                material == Items.DIAMOND_LEGGINGS ||
                material == Items.DIAMOND_BOOTS ||
                material == Items.DIAMOND_SWORD ||
                material == Items.DIAMOND_PICKAXE ||
                material == Items.DIAMOND_AXE ||
                material == Items.DIAMOND_SHOVEL ||
                material == Items.DIAMOND_HOE ||
                material == Items.IRON_HELMET ||
                material == Items.IRON_CHESTPLATE ||
                material == Items.IRON_LEGGINGS ||
                material == Items.IRON_BOOTS ||
                material == Items.IRON_SWORD ||
                material == Items.IRON_PICKAXE ||
                material == Items.IRON_AXE ||
                material == Items.IRON_SHOVEL ||
                material == Items.IRON_HOE ||
                material == Items.GOLDEN_HELMET ||
                material == Items.GOLDEN_CHESTPLATE ||
                material == Items.GOLDEN_LEGGINGS ||
                material == Items.GOLDEN_BOOTS ||
                material == Items.GOLDEN_SWORD ||
                material == Items.GOLDEN_PICKAXE ||
                material == Items.GOLDEN_AXE ||
                material == Items.GOLDEN_SHOVEL ||
                material == Items.GOLDEN_HOE ||
                material == Items.BOW ||
                material == Items.CROSSBOW ||
                material == Items.TRIDENT ||
                material == Items.MACE ||
                material == Items.SHIELD ||
                material == Items.FISHING_ROD) {
            return true;
        }
        if (displayName != null && displayName.contains("[★]")) {
            if (material == Items.POTION || material == Items.SPLASH_POTION || material == Items.LINGERING_POTION) {
                return true;
            }
        }
        return false;
    }

    public String getDisplayName() {
        return displayName;
    }

    public ItemStack createItemStack() {
        ItemStack stack = new ItemStack(material);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(displayName));
        if (isPotion(material)) {
            if (potionContents != null) {
                stack.set(DataComponentTypes.POTION_CONTENTS, potionContents);
            } else {
                int color = getPotionColorByName(displayName);
                stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(
                        Optional.empty(),
                        Optional.of(color),
                        List.<StatusEffectInstance>of(),
                        Optional.empty()
                ));
            }
        }
        if (loreTexts != null && !loreTexts.isEmpty()) {
            stack.set(DataComponentTypes.LORE, new LoreComponent(loreTexts));
        }
        if (hasGlint) {
            stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        if (nbt != null) {
            NbtCompound nbtCopy = nbt.copy();
            if (material == Items.PLAYER_HEAD && nbtCopy.getCompound("SkullOwner").isPresent()) {
                NbtCompound skullOwner = nbtCopy.getCompound("SkullOwner").get();
                Optional<int[]> idArray = skullOwner.getIntArray("Id");
                UUID id;
                if (idArray.isPresent()) {
                    int[] arr = idArray.get();
                    id = uuidFromIntArray(arr);
                } else {
                    Optional<String> idString = skullOwner.getString("Id");
                    id = idString.map(UUID::fromString).orElse(UUID.randomUUID());
                }
                ImmutableMultimap.Builder<String, Property> builder = ImmutableMultimap.builder();
                Optional<NbtCompound> propertiesOpt = skullOwner.getCompound("Properties");
                if (propertiesOpt.isPresent()) {
                    NbtCompound properties = propertiesOpt.get();
                    Optional<NbtList> texturesOpt = properties.getList("textures");
                    if (texturesOpt.isPresent()) {
                        NbtList textures = texturesOpt.get();
                        if (!textures.isEmpty()) {
                            Optional<NbtCompound> textureNbtOpt = textures.getCompound(0);
                            if (textureNbtOpt.isPresent()) {
                                Optional<String> valueOpt = textureNbtOpt.get().getString("Value");
                                if (valueOpt.isPresent()) {
                                    builder.put("textures", new Property("textures", valueOpt.get()));
                                }
                            }
                        }
                    }
                }
                PropertyMap propertyMap = new PropertyMap(builder.build());
                GameProfile profile = new GameProfile(id, "", propertyMap);
                stack.set(DataComponentTypes.PROFILE, ProfileComponent.ofStatic(profile));
                nbtCopy.remove("SkullOwner");
            }
            if (!nbtCopy.isEmpty()) {
                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbtCopy));
            }
        }
        return stack;
    }

    private boolean isPotion(Item item) {
        return item == Items.POTION || item == Items.SPLASH_POTION || item == Items.LINGERING_POTION;
    }

    private int getPotionColorByName(String name) {
        return switch (name) {
            case "Зелье отрыжки" -> 0xFF5D00;
            case "Зелье серной кислоты" -> 0x00C200;
            case "Зелье вспышки" -> 0xFFFFFF;
            case "Зелье мочи Флеша" -> 0x5CF7FF;
            case "Зелье победителя" -> 0x00FF00;
            case "Зелье агента" -> 0xFFFB00;
            case "Зелье медика" -> 0xFF00DE;
            case "Зелье киллера" -> 0xFF0000;
            case "[★] Хлопушка" -> 0xFF69B4;
            case "[★] Святая вода" -> 0xFFFFFF;
            case "[★] Зелье Гнева" -> 0x993333;
            case "[★] Зелье Палладина" -> 0x00FFFF;
            case "[★] Зелье Ассасина" -> 0x333333;
            case "[★] Зелье Радиации" -> 0x32CD32;
            case "[★] Снотворное" -> 0x484848;
            case "[🍹] Мандариновый сок" -> 0xD6CE43;
            default -> 0x385DC6;
        };
    }

    private static UUID uuidFromIntArray(int[] arr) {
        if (arr.length != 4) {
            return UUID.randomUUID();
        }
        long mostSig = ((long) arr[0] << 32) | (arr[1] & 0xFFFFFFFFL);
        long leastSig = ((long) arr[2] << 32) | (arr[3] & 0xFFFFFFFFL);
        return new UUID(mostSig, leastSig);
    }

    public int getPrice() {
        return price;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public AutoBuyItemSettings getSettings() {
        return settings;
    }
}