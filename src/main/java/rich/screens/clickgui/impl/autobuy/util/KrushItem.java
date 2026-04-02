package rich.screens.clickgui.impl.autobuy.util;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.settings.AutoBuyItemSettings;
import rich.util.config.impl.autobuyconfig.AutoBuyConfig;

public class KrushItem implements AutoBuyableItem {
    private final String displayName;
    private final Item material;
    private final ItemStack displayStack;
    private final int defaultPrice;
    private final AutoBuyItemSettings settings;
    private final boolean isKrushItem;
    private boolean enabled;

    public KrushItem(String displayName, Item material, ItemStack displayStack, int defaultPrice) {
        this(displayName, material, displayStack, defaultPrice, true, false);
    }

    public KrushItem(String displayName, Item material, ItemStack displayStack, int defaultPrice, boolean canHaveQuantity) {
        this(displayName, material, displayStack, defaultPrice, canHaveQuantity, false);
    }

    public KrushItem(String displayName, Item material, ItemStack displayStack, int defaultPrice, boolean canHaveQuantity, boolean isKrushItem) {
        this.displayName = displayName;
        this.material = material;
        this.displayStack = displayStack;
        this.defaultPrice = defaultPrice;
        this.isKrushItem = isKrushItem;
        this.settings = new AutoBuyItemSettings(defaultPrice, material, displayName, canHaveQuantity);
        AutoBuyConfig config = AutoBuyConfig.getInstance();
        if (config.hasItemConfig(displayName)) {
            this.enabled = config.isItemEnabled(displayName);
        } else {
            this.enabled = true;
            config.loadItemSettings(displayName, defaultPrice);
        }
    }

    private boolean shouldHaveGlint() {
        if (!isKrushItem) {
            return false;
        }
        return material == Items.TOTEM_OF_UNDYING ||
                material == Items.NETHERITE_HELMET ||
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
                material == Items.ELYTRA ||
                material == Items.SHIELD ||
                material == Items.FISHING_ROD;
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public ItemStack createItemStack() {
        ItemStack copy = displayStack.copy();
        if (shouldHaveGlint()) {
            copy.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        }
        return copy;
    }

    @Override
    public int getPrice() {
        return settings.getBuyBelow();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public AutoBuyItemSettings getSettings() {
        return settings;
    }
}