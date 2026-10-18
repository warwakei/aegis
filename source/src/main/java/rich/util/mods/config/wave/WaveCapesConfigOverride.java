package rich.util.mods.config.wave;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class WaveCapesConfigOverride implements PreLaunchEntrypoint {

    private static final String CONFIG_CONTENT = """
            {
              "configVersion": 2,
              "windMode": "WAVES",
              "capeStyle": "SMOOTH",
              "capeMovement": "BASIC_SIMULATION_3D",
              "gravity": 15,
              "heightMultiplier": 5,
              "straveMultiplier": 5
            }
            """;

    @Override
    public void onPreLaunch() {
        Path configDir = FabricLoader.getInstance().getConfigDir();
        Path waveCapesConfig = configDir.resolve("waveycapes.json");
        
        try {
            Files.writeString(waveCapesConfig, CONFIG_CONTENT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}