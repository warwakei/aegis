package aegis.updater;

import org.junit.Test;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class UpdaterVersionTest {
    private final UpdaterEntrypoint updater = new UpdaterEntrypoint();

    private int compare(String left, String right) throws Exception {
        Method method = UpdaterEntrypoint.class.getDeclaredMethod("compareVersions", String.class, String.class);
        method.setAccessible(true);
        return (int) method.invoke(updater, left, right);
    }

    @Test public void finalReleaseIsNewerThanItsPrerelease() throws Exception {
        assertTrue(compare("0.6.3", "0.6.3-rc.1") > 0);
        assertTrue(compare("0.6.3-rc.1", "0.6.3") < 0);
    }

    @Test public void numericPartsAndPrereleaseNumbersUseNumericOrder() throws Exception {
        assertTrue(compare("0.10.0", "0.9.9") > 0);
        assertTrue(compare("0.6.3-rc.10", "0.6.3-rc.2") > 0);
        assertEquals(0, compare("0.6.3+build.1", "0.6.3+build.2"));
    }

    @Test public void backupNamesAreNotTreatedAsInstalledModJars() throws Exception {
        Method method = UpdaterEntrypoint.class.getDeclaredMethod("extractVersionFromJarName", String.class);
        method.setAccessible(true);
        assertEquals("0.6.3", method.invoke(updater, "AegisNeo-0.6.3.jar"));
        assertNull(method.invoke(updater, "AegisNeo-0.6.3.jar.backup.jar"));
        assertNull(method.invoke(updater, "AegisNeo-0.6.3-sources.jar"));
    }
}
