package fun.aegis.features.impl.misc;

import antidaunleak.api.UserProfile;
import fun.aegis.Aegis;
import fun.aegis.common.discord.DiscordManager;
import fun.aegis.events.chat.ChatEvent;
import fun.aegis.features.module.Module;
import fun.aegis.features.module.ModuleCategory;
import fun.aegis.utils.client.chat.ChatMessage;
import fun.aegis.utils.client.managers.event.EventHandler;
import fun.aegis.utils.client.managers.file.FileRepository;
import fun.aegis.utils.client.managers.file.exception.FileLoadException;
import fun.aegis.utils.client.managers.file.impl.PrefixFile;
import fun.aegis.utils.math.calc.Calculate;
import fun.aegis.utils.math.time.StopWatch;
import lombok.experimental.NonFinal;
import org.apache.logging.log4j.core.appender.rolling.action.IfAll;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.system.MathUtil;

public class SelfDestruct extends Module {
    public static boolean unhooked;

    public SelfDestruct() {
        super("SelfDestruct", "Self Destruct", ModuleCategory.MISC);
    }
    @NonFinal
    StopWatch timer = new StopWatch();

    @Override
    public void activate() {
        unhooked = true;

        Aegis.getInstance().getDiscordManager().stopRPC();

        for (Module module : Aegis.getInstance().getModuleProvider().getModules()) {
            if (module != this && module.isState()) {
                module.setState(false);
            }
        }

        ChatMessage.brandmessage("Для возвращения чита впишите в чат ваш username в чите");
        ChatMessage.brandmessage("Сообщение удалится через пол секунды");
        if (timer.every(500)) {
            mc.inGameHud.getChatHud().clear(true);
        }

        for (Module module : Aegis.getInstance().getModuleProvider().getModules()) {
            module.setKey(GLFW.GLFW_KEY_UNKNOWN);
        }

        Aegis.getInstance().getCommandDispatcher().prefix = "" + Calculate.getRandom(0, 9999999);

        super.activate();
    }

    @EventHandler
    public void onChat(ChatEvent event) {
        String msg = event.getMessage().trim();
        if (msg.equalsIgnoreCase(UserProfile.getInstance().profile("username"))) {
            unhooked = false;
            Aegis.getInstance().getDiscordManager().setRunning(true);
            state = false;
            Aegis.getInstance().getCommandDispatcher().prefix = ".";
            ChatMessage.brandmessage("Unhook reset to FALSE");
            event.setCancelled(true);
        }
    }

    @Override
    public void deactivate() {
        unhooked = false;
        super.deactivate();
    }
}
