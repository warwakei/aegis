package fun.aegis.features.impl.render;

import antidaunleak.api.annotation.Native;
import fun.aegis.events.packet.PacketEvent;
import fun.aegis.features.impl.combat.Aura;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.features.module.setting.implement.BooleanSetting;
import fun.aegis.utils.client.Instance;
import fun.aegis.utils.client.managers.event.EventHandler;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BetterMinecraft extends Module {

    public static BetterMinecraft getInstance() {
        return Instance.get(BetterMinecraft.class);
    }

    BooleanSetting betterButton = new BooleanSetting("Кастомные кнопки", "язаипалсяэтопаститьспасите")
            .setValue(true);
    BooleanSetting tabVanishButton = new BooleanSetting("Спектаторы в табе", "язаипалсяэтопаститьспасите")
            .setValue(true);

    public BetterMinecraft() {
        super("BetterMinecraft", "Better Minecraft", ModuleCategory.RENDER);
        setup(betterButton, tabVanishButton);
    }

}
