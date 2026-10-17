package rich.mixin;

import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * миксин чтоб настройки почистить
 */
@Mixin(OptionsScreen.class)
public class OptionsScreenMixin {

    @Inject(method = "init", at = @At("TAIL"))
    private void onInit(CallbackInfo ci) {
        OptionsScreen screen = (OptionsScreen) (Object) this;
        
        // сносим лишние кнопки настроек
        screen.children().removeIf(element -> {
            if (element instanceof ButtonWidget button) {
                String message = button.getMessage().getString();
                
                // чекаем кнопки по словам
                boolean isCredits = message.contains("Credits") || message.contains("Attribution") ||
                                   message.contains("Титры") || message.contains("Атрибуции");
                
                boolean isAccessibility = message.contains("Accessibility") || 
                                         message.contains("Специальные возможности") ||
                                         message.contains("Доступность");
                
                boolean isAppearance = message.contains("Skin") || message.contains("Внешний вид") ||
                                      message.contains("Appearance") || message.contains("Customization");
                
                return isCredits || isAccessibility || isAppearance;
            }
            return false;
        });
    }
}
