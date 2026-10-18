package aegis.updater;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.Assert.*;

public class UpdateInstallerTest {
    @Rule public TemporaryFolder temporary = new TemporaryFolder();

    private Path jar(String name, String metadata) throws Exception {
        Path path = temporary.getRoot().toPath().resolve(name);
        try (var out = new JarOutputStream(Files.newOutputStream(path))) {
            out.putNextEntry(new JarEntry("fabric.mod.json"));
            out.write(metadata.getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new JarEntry("rich/Initialization.class"));
            out.write(new byte[]{1, 2, 3});
            out.closeEntry();
        }
        return path;
    }

    @Test public void invalidDownloadNeverReplacesWorkingJar() throws Exception {
        Path working = jar("working.jar", "{\"id\":\"copyright\"}");
        byte[] before = Files.readAllBytes(working);
        Path downloaded = temporary.newFile("download.jar.tmp").toPath();
        Files.writeString(downloaded, "<html>upstream error</html>");
        assertThrows(IOException.class, () -> UpdateInstaller.install(downloaded, working));
        assertArrayEquals(before, Files.readAllBytes(working));
    }

    @Test public void wrongModAndBrokenMetadataAreRejected() throws Exception {
        Path wrong = jar("wrong.jar", "{\"id\":\"another_mod\"}");
        assertThrows(IOException.class, () -> UpdateInstaller.validate(wrong));
        Path malformed = jar("bad.jar", "not json");
        assertThrows(IOException.class, () -> UpdateInstaller.validate(malformed));
    }

    @Test public void validDownloadIsInstalledBeforeOldJarIsTouched() throws Exception {
        Path old = jar("AegisNeo-0.6.2.jar", "{\"id\":\"copyright\"}");
        Path downloaded = jar("download.jar.tmp", "{\"id\":\"copyright\",\"version\":\"0.6.3\"}");
        Path target = old.resolveSibling("AegisNeo-0.6.3.jar");
        UpdateInstaller.install(downloaded, target);
        assertTrue(Files.exists(old));
        assertTrue(Files.exists(target));
        assertFalse(Files.exists(downloaded));
        UpdateInstaller.validate(target);
    }

    @Test public void failedMoveLeavesOldVersionAvailable() throws Exception {
        Path old = jar("AegisNeo-0.6.2.jar", "{\"id\":\"copyright\"}");
        Path downloaded = jar("download.jar.tmp", "{\"id\":\"copyright\"}");
        Path invalidTarget = temporary.newFolder("target.jar").toPath();
        Files.writeString(invalidTarget.resolve("keep.txt"), "data");
        assertThrows(IOException.class, () -> UpdateInstaller.install(downloaded, invalidTarget));
        assertTrue(Files.exists(old));
        assertEquals("data", Files.readString(invalidTarget.resolve("keep.txt")));
    }
}
