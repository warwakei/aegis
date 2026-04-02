package fun.aegis.common.discord.callbacks;

import com.sun.jna.Callback;
import fun.aegis.common.discord.utils.DiscordUser;

public interface ReadyCallback extends Callback {
    void apply(DiscordUser var1);
}
