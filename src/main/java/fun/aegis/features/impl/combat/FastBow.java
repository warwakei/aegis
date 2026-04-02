package fun.aegis.features.impl.combat;

import fun.aegis.events.player.TickEvent;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.SliderSettings;
import fun.aegis.utils.client.managers.event.EventHandler;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.item.BowItem;
import net.minecraft.item.CrossbowItem;
import net.minecraft.item.Items;

@FieldDefaults(level = AccessLevel.PRIVATE)
public class FastBow extends Module {

    final SliderSettings minCharge = new SliderSettings("Мин. натяжение", "Минимальное время натяжения в тиках (3 = минимум для выстрела)")
            .setValue(3F).range(3F, 20F);

    int chargeTime = 0;
    boolean wasUsing = false;

    public FastBow() {
        super("FastBow", "Fast Bow", ModuleCategory.COMBAT);
        setup(minCharge);
    }

    @Override
    public void activate() {
        chargeTime = 0;
        wasUsing = false;
    }

    @Override
    public void deactivate() {
        chargeTime = 0;
        wasUsing = false;
    }

    @EventHandler
    public void onTick(TickEvent e) {
        if (mc.player == null || mc.world == null) return;

        boolean isUsingBow = mc.player.isUsingItem() && 
                (mc.player.getActiveItem().getItem() instanceof BowItem);

        if (isUsingBow) {
            chargeTime++;
            
            // Если достигли минимального натяжения - отпускаем и стреляем
            if (chargeTime >= (int) minCharge.getValue()) {
                mc.player.stopUsingItem();
                chargeTime = 0;
            }
            wasUsing = true;
        } else {
            // Если только что отпустили лук и зажата ПКМ - начинаем заново
            if (wasUsing && mc.options.useKey.isPressed()) {
                // Автоматически начнёт использовать лук снова
                chargeTime = 0;
            }
            wasUsing = false;
            chargeTime = 0;
        }
    }
}
