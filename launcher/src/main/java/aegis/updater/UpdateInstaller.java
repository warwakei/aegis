package aegis.updater;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarFile;

final class UpdateInstaller {
    private UpdateInstaller() {}

    static void validate(Path path) throws IOException {
        try (JarFile jar = new JarFile(path.toFile())) {
            var metadata = jar.getJarEntry("fabric.mod.json");
            if (metadata == null || jar.getJarEntry("rich/Initialization.class") == null) {
                throw new IOException("Downloaded file is not an AegisNeo mod");
            }
            try (var reader = new InputStreamReader(jar.getInputStream(metadata), StandardCharsets.UTF_8)) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                if (!json.has("id") || !"copyright".equals(json.get("id").getAsString())) {
                    throw new IOException("Downloaded mod has an unexpected id");
                }
            } catch (RuntimeException e) {
                throw new IOException("Invalid mod metadata", e);
            }
        }
    }

    static void install(Path downloaded, Path target) throws IOException {
        validate(downloaded);
        try {
            Files.move(downloaded, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(downloaded, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
