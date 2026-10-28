package rich.client.splash;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.minecraft.client.MinecraftClient;

public class MinecraftWindowListener {
    private static boolean initialized = false;

    public static void register() {
        if (initialized) return;
        initialized = true;

        ClientLifecycleEvents.CLIENT_STARTED.register(client -> {
            SplashScreenManager.complete();
        });
    }
}
