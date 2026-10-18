package rich.mixin;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * миксин чтоб меню почистить
 */
@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin {

    @Inject(method = "initWidgets", at = @At("TAIL"))
    private void onInitWidgets(CallbackInfo ci) {
        GameMenuScreen screen = (GameMenuScreen) (Object) this;
        
        // сносим кнопки по порядку
        // сносим с конца чтоб индексы целы
        var children = screen.children();
        
        // ищем мусорные кнопки сносим
        children.removeIf(element -> {
            if (element instanceof ButtonWidget button) {
                String message = button.getMessage().getString();
                
                // оставляем только нужные кнопки
                // костыль для ванилы по текстам
                return !message.contains("Вернуться") && 
                       !message.contains("Return") && 
                       !message.contains("Game") &&
                       !message.contains("Настройки") && 
                       !message.contains("Options") &&
                       !message.contains("Отключиться") && 
                       !message.contains("Disconnect") &&
                       !message.contains("Title") &&
                       !message.contains("Главное меню");
            }
            return false;
        });
    }
}
