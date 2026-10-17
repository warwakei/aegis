package rich.util.config;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import rich.util.config.impl.ConfigSerializer;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class ConfigSystemTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private ConfigSystem system(ConfigSerializer serializer) throws Exception {
        ConfigSystem system = new ConfigSystem();
        Field serializerField = ConfigSystem.class.getDeclaredField("serializer");
        serializerField.setAccessible(true);
        serializerField.set(system, serializer);
        Field initialized = ConfigSystem.class.getDeclaredField("initialized");
        initialized.setAccessible(true);
        ((AtomicBoolean) initialized.get(system)).set(true);
        return system;
    }

    private boolean invoke(ConfigSystem system, String operation, Path path) throws Exception {
        Method method = ConfigSystem.class.getDeclaredMethod(operation, Path.class);
        method.setAccessible(true);
        return (boolean) method.invoke(system, path);
    }

    @Test public void manualSaveWaitsForAutosaveInsteadOfBeingDiscarded() throws Exception {
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        AtomicInteger saves = new AtomicInteger();
        ConfigSystem system = system(new ConfigSerializer() {
            @Override public String serialize() {
                int sequence = saves.incrementAndGet();
                if (sequence == 1) {
                    entered.countDown();
                    try { assertTrue(release.await(5, TimeUnit.SECONDS)); }
                    catch (InterruptedException e) { throw new AssertionError(e); }
                }
                return "save-" + sequence;
            }
        });
        Path file = temporary.getRoot().toPath().resolve("config.aegisconfig");
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> invoke(system, "saveToPath", file));
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            var second = executor.submit(() -> invoke(system, "saveToPath", file));
            try {
                assertThrows(TimeoutException.class, () -> second.get(150, TimeUnit.MILLISECONDS));
            } finally {
                release.countDown();
            }
            assertTrue(first.get(5, TimeUnit.SECONDS));
            assertTrue(second.get(5, TimeUnit.SECONDS));
        }
        assertEquals("save-2", Files.readString(file));
    }

    @Test public void failedWriteIsReportedToCallers() throws Exception {
        ConfigSystem system = system(new ConfigSerializer() {
            @Override public String serialize() { return "data"; }
        });
        Path target = temporary.newFolder("config.aegisconfig").toPath();
        Files.writeString(target.resolve("keep"), "data");
        assertFalse(invoke(system, "saveToPath", target));
        assertFalse(system.isSaving());
    }
}
