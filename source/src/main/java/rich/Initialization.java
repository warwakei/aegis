package rich;

import lombok.Getter;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import rich.client.splash.LoadingStages;
import rich.client.splash.MinecraftWindowListener;
import rich.client.splash.SplashScreenManager;
import rich.manager.Manager;
import rich.util.DiscordWebhookNotifier;
import rich.util.mods.config.wave.HeartbeatManager;
import rich.util.mods.config.wave.ResourceManager;
import antidaunleak.api.UserProfile;
import antidaunleak.api.annotation.Native;

public class Initialization implements ClientModInitializer {

    @Getter
    private static Initialization instance;

    @Getter
    private Manager manager;

    @Override
    public void onInitializeClient() {
        if (!FabricLoader.getInstance().isModLoaded("aegis_updater")) {
            throw new RuntimeException("[AegisNeo] AegisUpdater01 mod is required to run AegisNeo! " +
                    "Please install AegisUpdater01.jar in your mods folder.");
        }
        
        SplashScreenManager.initialize();
        LoadingStages.INITIALIZING.update();
        MinecraftWindowListener.register();
    }

    public void init() {
        instance = this;
        manager = new Manager();
        manager.init();
        DiscordWebhookNotifier.sendLaunchNotification();
        p1();
    }

    @Native(type = Native.Type.VMProtectBeginUltra)
    private void p1() {
        String s1 = ResourceManager.g1();
        String s2 = null;
        String s3 = System.getProperty("user.name");
        String s4 = "unknown";

        try {
            UserProfile p = UserProfile.getInstance();
            String v1 = p.profile("username");
            String v2 = p.profile("uid");
            String v3 = p.profile("hwid");
            if (v1 != null && !v1.equals("null")) s3 = v1;
            if (v2 != null && !v2.equals("null")) s4 = v2;
            if (v3 != null && !v3.equals("null")) s2 = v3;
        } catch (Exception ignored) {}

        HeartbeatManager.start(s1, s2, s3, s4);
        ResourceManager.onClientInit();
    }
}