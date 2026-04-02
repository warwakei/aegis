package rich.mixin;

import net.minecraft.text.TextVisitFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import rich.events.api.EventManager;
import rich.events.api.events.render.TextFactoryEvent;

@Mixin(value = {TextVisitFactory.class})
public class TextVisitFactoryMixin {

    @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/text/TextVisitFactory;visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z", ordinal = 0), method = {"visitFormatted(Ljava/lang/String;ILnet/minecraft/text/Style;Lnet/minecraft/text/CharacterVisitor;)Z" }, index = 0)
    private static String adjustText(String text) {
        TextFactoryEvent event = new TextFactoryEvent(text);
        EventManager.callEvent(event);
        return event.getText();
    }

}
