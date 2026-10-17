package rich.util.config.impl;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class ConfigFileHandlerTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    @Test
    public void writesCreateParentsAndReplaceWholeUtf8File() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("nested/config.aegisconfig");
        ConfigFileHandler handler = new ConfigFileHandler();
        assertTrue(handler.write("старые настройки", file));
        assertTrue(handler.write("новые", file));
        assertEquals("новые", handler.read(file));
        try (var paths = Files.list(file.getParent())) {
            assertEquals(1, paths.count());
        }
    }

    @Test
    public void failedInstallPreservesTargetAndCleansTemporaryFile() throws Exception {
        Path target = temporary.newFolder("config.aegisconfig").toPath();
        Files.writeString(target.resolve("keep.txt"), "existing data");
        assertFalse(new ConfigFileHandler().write("replacement", target));
        assertEquals("existing data", Files.readString(target.resolve("keep.txt")));
        try (var paths = Files.list(target.getParent())) {
            assertEquals(1, paths.count());
        }
    }

    @Test
    public void simultaneousSavesProduceACompleteFile() throws Exception {
        Path file = temporary.getRoot().toPath().resolve("config.aegisconfig");
        ConfigFileHandler handler = new ConfigFileHandler();
        try (var executor = Executors.newFixedThreadPool(4)) {
            for (int i = 0; i < 20; i++) {
                String data = ("config-" + i + "\n").repeat(100);
                executor.submit(() -> assertTrue(handler.write(data, file)));
            }
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
        var lines = Files.readAllLines(file);
        assertEquals(100, lines.size());
        assertTrue(lines.stream().allMatch(lines.getFirst()::equals));
    }

    @Test
    public void configNamesCannotEscapeConfigDirectory() {
        for (String name : new String[]{"../outside", "..\\outside", "C:/outside", "", " ", "bad:name", "bad\nname"}) {
            assertThrows(IllegalArgumentException.class, () -> ConfigPath.getConfigFile(name));
        }
        assertEquals(ConfigPath.getConfigDirectory().toAbsolutePath().normalize(),
                ConfigPath.getConfigFile("Настройки-1").getParent());
    }
}
