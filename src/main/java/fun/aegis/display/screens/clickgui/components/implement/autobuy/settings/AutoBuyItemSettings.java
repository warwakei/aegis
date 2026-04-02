package fun.aegis.display.screens.clickgui.components.implement.autobuy.settings;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.Item;
import net.minecraft.item.Items;

@Getter
@Setter
public class AutoBuyItemSettings {
    private int buyBelow;
    private int sellAbove;
    private int minQuantity;
    private final boolean canHaveQuantity;
    private final String itemName;

    public AutoBuyItemSettings(int defaultBuyBelow, Item material, String itemName) {
        this.itemName = itemName;
        this.buyBelow = defaultBuyBelow;
        this.sellAbove = (int) (defaultBuyBelow * 1.5);
        this.minQuantity = 1;
        this.canHaveQuantity = canItemStack(material);
    }

    private boolean canItemStack(Item material) {
        if (material == Items.NETHERITE_HELMET || material == Items.NETHERITE_CHESTPLATE ||
                material == Items.NETHERITE_LEGGINGS || material == Items.NETHERITE_BOOTS ||
                material == Items.NETHERITE_SWORD || material == Items.NETHERITE_PICKAXE ||
                material == Items.CROSSBOW || material == Items.TRIDENT || material == Items.MACE ||
                material == Items.ELYTRA || material == Items.TOTEM_OF_UNDYING) {
            return false;
        }
        return material.getMaxCount() > 1;
    }
}
