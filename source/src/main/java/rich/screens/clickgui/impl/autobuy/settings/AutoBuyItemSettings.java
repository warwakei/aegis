package rich.screens.clickgui.impl.autobuy.settings;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.item.Item;
import rich.util.config.impl.autobuyconfig.AutoBuyConfig;

@Getter
@Setter
public class AutoBuyItemSettings {
    private int buyBelow;
    private int minQuantity = 1;
    private boolean canHaveQuantity = false;
    private Item material;
    private String displayName;

    public AutoBuyItemSettings(int defaultPrice, Item material, String displayName) {
        this.buyBelow = defaultPrice;
        this.material = material;
        this.displayName = displayName;
        loadFromConfig();
    }

    public AutoBuyItemSettings(int defaultPrice, Item material, String displayName, boolean canHaveQuantity) {
        this.buyBelow = defaultPrice;
        this.material = material;
        this.displayName = displayName;
        this.canHaveQuantity = canHaveQuantity;
        loadFromConfig();
    }

    private void loadFromConfig() {
        AutoBuyConfig config = AutoBuyConfig.getInstance();
        if (config.hasItemConfig(displayName)) {
            AutoBuyConfig.ItemConfig itemConfig = config.getItemConfig(displayName);
            this.buyBelow = itemConfig.getBuyBelow();
            this.minQuantity = itemConfig.getMinQuantity();
        } else {
            config.loadItemSettings(displayName, buyBelow);
        }
    }

    public void saveToConfig() {
        AutoBuyConfig config = AutoBuyConfig.getInstance();
        config.setItemBuyBelow(displayName, buyBelow);
        config.setItemMinQuantity(displayName, minQuantity);
    }
}