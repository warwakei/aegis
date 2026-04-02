package rich.modules.impl.render;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.CrossbowItem;
import net.minecraft.util.Hand;
import rich.events.api.EventHandler;
import rich.events.impl.HandOffsetEvent;
import rich.modules.module.ModuleStructure;
import rich.modules.module.category.ModuleCategory;
import rich.modules.module.setting.implement.SliderSettings;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ViewModel extends ModuleStructure {

    SliderSettings mainHandXSetting = new SliderSettings("Основная рука X", "Настройка значения X для основной руки")
            .setValue(0.0F).range(-1.0F, 1.0F);

    SliderSettings mainHandYSetting = new SliderSettings("Основная рука Y", "Настройка значения Y для основной руки")
            .setValue(0.0F).range(-1.0F, 1.0F);

    SliderSettings mainHandZSetting = new SliderSettings("Основная рука Z", "Настройка значения Z для основной руки")
            .setValue(0.0F).range(-2.5F, 2.5F);

    SliderSettings offHandXSetting = new SliderSettings("Второстепенная рука X", "Настройка значения X для второстепенной руки")
            .setValue(0.0F).range(-1.0F, 1.0F);

    SliderSettings offHandYSetting = new SliderSettings("Второстепенная рука Y", "Настройка значения Y для второстепенной руки")
            .setValue(0.0F).range(-1.0F, 1.0F);

    SliderSettings offHandZSetting = new SliderSettings("Второстепенная рука Z", "Настройка значения Z для второстепенной руки")
            .setValue(0.0F).range(-2.5F, 2.5F);

    public ViewModel() {
        super("ViewModel", "View Model", ModuleCategory.RENDER);
        settings(mainHandXSetting, mainHandYSetting, mainHandZSetting,
                offHandXSetting, offHandYSetting, offHandZSetting);
    }

    @EventHandler
    public void onHandOffset(HandOffsetEvent e) {
        Hand hand = e.getHand();
        if (hand.equals(Hand.MAIN_HAND) && e.getStack().getItem() instanceof CrossbowItem) return;

        MatrixStack matrix = e.getMatrices();

        if (hand.equals(Hand.MAIN_HAND)) {
            matrix.translate(mainHandXSetting.getValue(), mainHandYSetting.getValue(), mainHandZSetting.getValue());
        } else {
            matrix.translate(offHandXSetting.getValue(), offHandYSetting.getValue(), offHandZSetting.getValue());
        }
    }
}