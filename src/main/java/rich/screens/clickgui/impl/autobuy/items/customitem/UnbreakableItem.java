package rich.screens.clickgui.impl.autobuy.items.customitem;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.settings.AutoBuyItemSettings;
import rich.util.config.impl.autobuyconfig.AutoBuyConfig;

import java.util.List;
import java.util.Optional;

public class UnbreakableItem implements AutoBuyableItem {
    private final String displayName;
    private final Item material;
    private final int price;
    private final List<Text> loreTexts;
    private final AutoBuyItemSettings settings;
    private boolean enabled;

    public UnbreakableItem(String displayName, Item material, int price, List<Text> loreTexts) {
        this.displayName = displayName;
        this.material = material;
        this.price = price;
        this.loreTexts = loreTexts;
        this.settings = new AutoBuyItemSettings(price, material, displayName);
        AutoBuyConfig config = AutoBuyConfig.getInstance();
        if (config.hasItemConfig(displayName)) {
            this.enabled = config.isItemEnabled(displayName);
        } else {
            this.enabled = true;
            config.loadItemSettings(displayName, price);
        }
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public ItemStack createItemStack() {
        ItemStack stack = new ItemStack(material);
        stack.set(DataComponentTypes.CUSTOM_NAME, Text.literal(displayName).formatted(Formatting.LIGHT_PURPLE));
        NbtCompound nbt = new NbtCompound();
        nbt.putInt("HideFlags", 127);
        nbt.putBoolean("Unbreakable", true);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world != null) {
            try {
                RegistryWrapper.WrapperLookup registryLookup = client.world.getRegistryManager();
                RegistryWrapper<Enchantment> enchantmentRegistry = registryLookup.getOrThrow(RegistryKeys.ENCHANTMENT);
                ItemEnchantmentsComponent.Builder builder = new ItemEnchantmentsComponent.Builder(
                        stack.getOrDefault(DataComponentTypes.ENCHANTMENTS, ItemEnchantmentsComponent.DEFAULT));
                Optional<RegistryEntry.Reference<Enchantment>> vanishingOpt =
                        enchantmentRegistry.getOptional(Enchantments.VANISHING_CURSE);
                if (vanishingOpt.isPresent()) {
                    builder.add(vanishingOpt.get(), 1);
                }
                stack.set(DataComponentTypes.ENCHANTMENTS, builder.build());
            } catch (Exception ignored) {}
        }
        if (loreTexts != null && !loreTexts.isEmpty()) {
            stack.set(DataComponentTypes.LORE, new LoreComponent(loreTexts));
        }
        stack.set(DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE, true);
        return stack;
    }

    @Override
    public int getPrice() {
        return price;
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