package rich.util.render.font;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rich.Initialization;

public class FontInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("rich/FontInitializer");
    private static boolean registered = false;
    private static boolean initialized = false;

    public static void register() {
        if (registered) return;
        registered = true;

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (!initialized && client.getResourceManager() != null && client.getWindow() != null) {
                try {
                    FontRenderer fontRenderer = Initialization.getInstance().getManager().getRenderCore().getFontRenderer();
                    if (fontRenderer != null && !fontRenderer.isInitialized()) {
                        fontRenderer.initialize();
                        initialized = true;
                        LOGGER.info("Fonts initialized successfully");
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to initialize fonts", e);
                }
            }
        });
    }

    public static boolean isInitialized() {
        return initialized;
    }
}