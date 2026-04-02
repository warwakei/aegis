package fun.aegis.common.logger.implement;

import antidaunleak.api.annotation.Native;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import fun.aegis.common.logger.Logger;
import fun.aegis.utils.display.interfaces.QuickImports;

import java.util.Arrays;

public class MinecraftLogger implements Logger, QuickImports {
    @Override
    public void log(Object message) {

    }

    @Override

    public void minecraftLog(Text... components) {
        if (mc.player != null) {
            MutableText component = Text.literal("");
            Arrays.asList(components).forEach(component::append);
            mc.inGameHud.getChatHud().addMessage(component);
        }
    }
}
