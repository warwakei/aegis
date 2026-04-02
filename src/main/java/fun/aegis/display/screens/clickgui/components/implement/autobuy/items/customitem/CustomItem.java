package fun.aegis.display.screens.clickgui.components.implement.autobuy.items.customitem;

import fun.aegis.display.screens.clickgui.components.implement.autobuy.items.AutoBuyableItem;
import fun.aegis.display.screens.clickgui.components.implement.autobuy.settings.AutoBuyItemSettings;
import fun.aegis.display.screens.clickgui.components.implement.autobuy.settings.AutoBuySettingsManager;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.Text;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
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
    private boolean enabled;

    public CustomItem(String displayName, NbtCompound nbt, Item material, int price, PotionContentsComponent potionContents, List<Text> loreTexts) {
        this.displayName = displayName;
        this.nbt = nbt;
        this.material = material;
        this.price = price;
        this.potionContents = potionContents;
        this.loreTexts = loreTexts;
        this.enabled = true;
        this.settings = new AutoBuyItemSettings(price, material, displayName);
        AutoBuySettingsManager.getInstance().loadSettings(displayName, this.settings);
    }

    public CustomItem(String displayName, NbtCompound nbt, Item material, int price) {
        this(displayName, nbt, material, price, null, null);
    }

    public String getDisplayName() {
        return displayName;
    }

    public ItemStack createItemStack() {
        ItemStack stack = new ItemStack(material);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(displayName));
        if (material == Items.POTION) {
            int color = switch (displayName) {
                case "Зелье отрыжки" -> 0xFF5D00;
                case "Зелье серной кислоты" -> 0x00C200;
                case "Зелье вспышки" -> 0xFFFFFF;
                case "Зелье мочи Флеша" -> 0x5CF7FF;
                case "Зелье победителя" -> 0x00FF00;
                case "Зелье агента" -> 0xFFFB00;
                case "Зелье медика" -> 0xFF00DE;
                case "Зелье киллера" -> 0xFF0000;
                default -> 0x385DC6;
            };
            stack.set(DataComponentTypes.POTION_CONTENTS, new PotionContentsComponent(Optional.empty(), Optional.of(color), List.<StatusEffectInstance>of(), Optional.empty()));
        } else if (potionContents != null) {
            stack.set(DataComponentTypes.POTION_CONTENTS, potionContents);
        }
        if (loreTexts != null) {
            stack.set(DataComponentTypes.LORE, new LoreComponent(loreTexts));
        }
        if (material == Items.TOTEM_OF_UNDYING) {
            stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        if (nbt != null) {
            NbtCompound nbtCopy = nbt.copy();
            if (material == Items.PLAYER_HEAD && nbtCopy.contains("SkullOwner", NbtElement.COMPOUND_TYPE)) {
                NbtCompound skullOwner = nbtCopy.getCompound("SkullOwner");
                UUID id = skullOwner.getUuid("Id");
                GameProfile profile = new GameProfile(id, "");
                if (skullOwner.contains("Properties", NbtElement.COMPOUND_TYPE)) {
                    NbtCompound properties = skullOwner.getCompound("Properties");
                    if (properties.contains("textures", NbtElement.LIST_TYPE)) {
                        NbtList textures = properties.getList("textures", NbtElement.COMPOUND_TYPE);
                        if (!textures.isEmpty()) {
                            NbtCompound textureNbt = textures.getCompound(0);
                            String value = textureNbt.getString("Value");
                            profile.getProperties().put("textures", new Property("textures", value));
                        }
                    }
                }
                stack.set(DataComponentTypes.PROFILE, new ProfileComponent(profile));
                nbtCopy.remove("SkullOwner");
            }
            if (!nbtCopy.isEmpty()) {
                stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbtCopy));
            }
        }
        return stack;
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
