package rich.screens.clickgui.impl.autobuy.originalitems;

import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import rich.screens.clickgui.impl.autobuy.AutoBuyableItem;
import rich.screens.clickgui.impl.autobuy.items.customitem.CustomItem;
import rich.screens.clickgui.impl.autobuy.items.price.Defaultpricec;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PotionProvider {
    public static List<AutoBuyableItem> getPotions() {
        List<AutoBuyableItem> potions = new ArrayList<>();

        List<Text> hlopushkaLore = List.of(Text.literal("Хлопушка"));
        List<StatusEffectInstance> hlopushkaEffects = List.of(
                new StatusEffectInstance(StatusEffects.SLOWNESS, 200, 9),
                new StatusEffectInstance(StatusEffects.SPEED, 400, 4),
                new StatusEffectInstance(StatusEffects.BLINDNESS, 100, 9),
                new StatusEffectInstance(StatusEffects.GLOWING, 3600, 0)
        );
        potions.add(new CustomItem("[★] Хлопушка", null, Items.SPLASH_POTION, Defaultpricec.getPrice("Хлопушка"),
                new PotionContentsComponent(Optional.empty(), Optional.of(0xFF69B4), hlopushkaEffects, Optional.empty()), hlopushkaLore));

        List<Text> holyWaterLore = List.of(Text.literal("Святая вода"));
        List<StatusEffectInstance> holyWaterEffects = List.of(
                new StatusEffectInstance(StatusEffects.REGENERATION, 1200, 2),
                new StatusEffectInstance(StatusEffects.INVISIBILITY, 12000, 1),
                new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 0, 1)
        );
        potions.add(new CustomItem("[★] Святая вода", null, Items.SPLASH_POTION, Defaultpricec.getPrice("Святая вода"),
                new PotionContentsComponent(Optional.empty(), Optional.of(0xFFFFFF), holyWaterEffects, Optional.empty()), holyWaterLore));

        List<Text> gnevLore = List.of(Text.literal("Зелье Гнева"));
        List<StatusEffectInstance> gnevEffects = List.of(
                new StatusEffectInstance(StatusEffects.STRENGTH, 600, 4),
                new StatusEffectInstance(StatusEffects.SLOWNESS, 600, 3)
        );
        potions.add(new CustomItem("[★] Зелье Гнева", null, Items.SPLASH_POTION, Defaultpricec.getPrice("Зелье Гнева"),
                new PotionContentsComponent(Optional.empty(), Optional.of(0x993333), gnevEffects, Optional.empty()), gnevLore));

        List<Text> paladinLore = List.of(Text.literal("Зелье Палладина"));
        List<StatusEffectInstance> paladinEffects = List.of(
                new StatusEffectInstance(StatusEffects.RESISTANCE, 12000, 0),
                new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 12000, 0),
                new StatusEffectInstance(StatusEffects.INVISIBILITY, 18000, 0),
                new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 1200, 2)
        );
        potions.add(new CustomItem("[★] Зелье Палладина", null, Items.SPLASH_POTION, Defaultpricec.getPrice("Зелье Палладина"),
                new PotionContentsComponent(Optional.empty(), Optional.of(0x00FFFF), paladinEffects, Optional.empty()), paladinLore));

        List<Text> assassinLore = List.of(Text.literal("Зелье Ассасина"));
        List<StatusEffectInstance> assassinEffects = List.of(
                new StatusEffectInstance(StatusEffects.STRENGTH, 1200, 3),
                new StatusEffectInstance(StatusEffects.SPEED, 6000, 2),
                new StatusEffectInstance(StatusEffects.HASTE, 1200, 0),
                new StatusEffectInstance(StatusEffects.INSTANT_DAMAGE, 1, 1)
        );
        potions.add(new CustomItem("[★] Зелье Ассасина", null, Items.SPLASH_POTION, Defaultpricec.getPrice("Зелье Ассасина"),
                new PotionContentsComponent(Optional.empty(), Optional.of(0x333333), assassinEffects, Optional.empty()), assassinLore));

        List<Text> radiationLore = List.of(Text.literal("Зелье Радиации"));
        List<StatusEffectInstance> radiationEffects = List.of(
                new StatusEffectInstance(StatusEffects.POISON, 1200, 1),
                new StatusEffectInstance(StatusEffects.WITHER, 1200, 1),
                new StatusEffectInstance(StatusEffects.SLOWNESS, 1800, 2),
                new StatusEffectInstance(StatusEffects.HUNGER, 1200, 4),
                new StatusEffectInstance(StatusEffects.GLOWING, 2400, 0)
        );
        potions.add(new CustomItem("[★] Зелье Радиации", null, Items.SPLASH_POTION, Defaultpricec.getPrice("Зелье Радиации"),
                new PotionContentsComponent(Optional.empty(), Optional.of(0x32CD32), radiationEffects, Optional.empty()), radiationLore));

        List<Text> snotvornoyeLore = List.of(Text.literal("Снотворное"));
        List<StatusEffectInstance> snotvornoEffects = List.of(
                new StatusEffectInstance(StatusEffects.WEAKNESS, 1800, 1),
                new StatusEffectInstance(StatusEffects.MINING_FATIGUE, 200, 1),
                new StatusEffectInstance(StatusEffects.WITHER, 1800, 2),
                new StatusEffectInstance(StatusEffects.BLINDNESS, 200, 0)
        );
        potions.add(new CustomItem("[★] Снотворное", null, Items.SPLASH_POTION, Defaultpricec.getPrice("Снотворное"),
                new PotionContentsComponent(Optional.empty(), Optional.of(0x484848), snotvornoEffects, Optional.empty()), snotvornoyeLore));

        List<Text> mandarinovySokLore = List.of(Text.literal("Заряд витаминов и удачи"));
        List<StatusEffectInstance> mandarinovySokEffects = List.of(
                new StatusEffectInstance(StatusEffects.FIRE_RESISTANCE, 3600, 0),
                new StatusEffectInstance(StatusEffects.JUMP_BOOST, 3600, 1),
                new StatusEffectInstance(StatusEffects.LUCK, 3600, 0),
                new StatusEffectInstance(StatusEffects.HASTE, 3600, 1)
        );
        potions.add(new CustomItem("[🍹] Мандариновый сок", null, Items.POTION, Defaultpricec.getPrice("Мандариновый сок"),
                new PotionContentsComponent(Optional.empty(), Optional.of(0xD6CE43), mandarinovySokEffects, Optional.empty()), mandarinovySokLore));

        return potions;
    }
}