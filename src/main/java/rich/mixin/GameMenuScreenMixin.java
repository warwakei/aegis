package rich.mixin;

import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Миксин для удаления лишних кнопок из меню паузы (ESC)
 * Оставляет только: "Вернуться к игре", "Настройки" и "Отключиться"
 * 
 * Порядок кнопок в GameMenuScreen (по initWidgets):
 * 1. Вернуться к игре (широкая)
 * 2. Достижения
 * 3. Статистика
 * 4. Настройки
 * 5. Обратная связь / Ссылки сервера / Жалобы
 * 6. Отключиться (широкая)
 */
@Mixin(GameMenuScreen.class)
public class GameMenuScreenMixin {

    @Inject(method = "initWidgets", at = @At("TAIL"))
    private void onInitWidgets(CallbackInfo ci) {
        GameMenuScreen screen = (GameMenuScreen) (Object) this;
        
        // Удаляем кнопки по их порядку в списке children()
        // Нужно удалять в обратном порядке чтобы индексы не сбились
        var children = screen.children();
        
        // Находим и удаляем ненужные кнопки
        children.removeIf(element -> {
            if (element instanceof ButtonWidget button) {
                String message = button.getMessage().getString();
                
                // Удаляем всё кроме "Вернуться к игре", "Настройки" и "Отключиться"
                // Эти кнопки имеют специфичные тексты в Minecraft
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
