package aegis.updater;

import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.Version;
import net.fabricmc.loader.api.VersionParsingException;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdaterEntrypoint implements PreLaunchEntrypoint {

    private static final Logger LOGGER = LoggerFactory.getLogger("AegisUpdater");
    private static final String GITHUB_REPO = "warwakei/aegis";
    private static final String MOD_ID = "copyright";
    private static final String MOD_NAME_PREFIX = "AegisNeo-";
    private static final String MOD_NAME_SUFFIX = ".jar";

    // версию из имени джарника дёргаем
    private static final Pattern JAR_VERSION_PATTERN = Pattern.compile(
            Pattern.quote(MOD_NAME_PREFIX) + "(\\d+\\.\\d+\\.\\d+)" + Pattern.quote(MOD_NAME_SUFFIX)
    );

    @Override
    public void onPreLaunch() {
        try {
            // старые джарники чистим свежий оставляем
            cleanupOldAegisJars();

            // мода нет качаем заново
            checkAndDownloadIfMissing();

            // апдейты с гитхаба чекаем
            runUpdateCheck();
        } catch (Exception e) {
            LOGGER.error("[AegisUpdater] Failed during pre-launch", e);
        }
    }

    /**
     * старые джарники чистим свежий оставляем
     */
    private void cleanupOldAegisJars() {
        Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
        if (!Files.exists(modsDir)) {
            return;
        }

        // все джарники ищем версии смотрим
        List<AegisJarInfo> jars = new ArrayList<>();
        try (var stream = Files.list(modsDir)) {
            stream.filter(p -> {
                String name = p.getFileName().toString();
                return name.startsWith(MOD_NAME_PREFIX) && name.endsWith(MOD_NAME_SUFFIX);
            }).forEach(p -> {
                String version = extractVersionFromJarName(p.getFileName().toString());
                if (version != null) {
                    jars.add(new AegisJarInfo(p, version));
                }
            });
        } catch (IOException e) {
            LOGGER.warn("[AegisUpdater] Failed to scan mods directory for cleanup", e);
            return;
        }

        if (jars.isEmpty()) {
            return;
        }

        if (jars.size() == 1) {
            LOGGER.info("[AegisUpdater] Found single AegisNeo jar: {} — no cleanup needed", jars.get(0).path.getFileName());
            return;
        }

        // сортим свежие сверху кладём
        jars.sort((a, b) -> compareVersions(b.version, a.version));

        // свежий оставляем остальное трём
        AegisJarInfo latest = jars.get(0);
        LOGGER.info("[AegisUpdater] Found {} AegisNeo jars. Keeping: {} (v{})", jars.size(), latest.path.getFileName(), latest.version);

        for (int i = 1; i < jars.size(); i++) {
            AegisJarInfo oldJar = jars.get(i);
            try {
                Files.delete(oldJar.path);
                LOGGER.info("[AegisUpdater] Deleted old jar: {} (v{})", oldJar.path.getFileName(), oldJar.version);
            } catch (IOException e) {
                LOGGER.warn("[AegisUpdater] Could not delete {}: {}", oldJar.path.getFileName(), e.getMessage());
            }
        }
    }

    /**
     * версию из имени джарника тащим
     */
    private String extractVersionFromJarName(String fileName) {
        Matcher matcher = JAR_VERSION_PATTERN.matcher(fileName);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * версии между собой сравниваем
     */
    private int compareVersions(String v1, String v2) {
        try {
            return Version.parse(v1).compareTo(Version.parse(v2));
        } catch (VersionParsingException e) {
            LOGGER.warn("[AegisUpdater] Invalid version: {} or {}", v1, v2);
            return 0;
        }
    }

    private record AegisJarInfo(Path path, String version) {}

    /**
     * мода нет качаем тут
     */
    private void checkAndDownloadIfMissing() {
        boolean hasAegisNeo = FabricLoader.getInstance().isModLoaded(MOD_ID);

        if (!hasAegisNeo) {
            LOGGER.info("[AegisUpdater] AegisNeo is not installed. Downloading the latest version...");
            String latestVersion = fetchLatestReleaseVersion();
            if (latestVersion != null) {
                Path downloadedJar = downloadLatestRelease(latestVersion);
                if (downloadedJar != null) {
                    LOGGER.info("[AegisUpdater] AegisNeo downloaded successfully: {}", downloadedJar);
                    LOGGER.info("[AegisUpdater] ============================================");
                    LOGGER.info("[AegisUpdater] AegisNeo has been installed!");
                    LOGGER.info("[AegisUpdater] Please restart Minecraft to load AegisNeo.");
                    LOGGER.info("[AegisUpdater] ============================================");
                    scheduleRestart();
                } else {
                    LOGGER.error("[AegisUpdater] Failed to download AegisNeo. Please install it manually.");
                }
            } else {
                LOGGER.error("[AegisUpdater] Could not fetch latest version from GitHub.");
            }
        }
    }

    private void runUpdateCheck() {
        // мода нет проверку скипаем
        if (!FabricLoader.getInstance().isModLoaded(MOD_ID)) {
            return;
        }

        String currentVersion = getCurrentAegisVersion();
        String latestVersion = fetchLatestReleaseVersion();

        if (latestVersion == null) {
            LOGGER.info("[AegisUpdater] Could not fetch latest version from GitHub. Skipping update check.");
            return;
        }

        LOGGER.info("[AegisUpdater] Current version: {}, Latest version: {}", currentVersion, latestVersion);

        if (currentVersion == null || isVersionNewer(latestVersion, currentVersion)) {
            LOGGER.info("[AegisUpdater] Update available! Downloading AegisNeo-{}.jar...", latestVersion);
            Path downloadedJar = downloadLatestRelease(latestVersion);
            if (downloadedJar != null) {
                LOGGER.info("[AegisUpdater] Download complete: {}", downloadedJar);
                scheduleRestart();
            } else {
                LOGGER.error("[AegisUpdater] Failed to download update.");
            }
        } else if (currentVersion.equals(latestVersion)) {
            LOGGER.info("[AegisUpdater] AegisNeo is up to date.");
        } else {
            LOGGER.info("[AegisUpdater] You are running a newer version than the latest release.");
        }
    }

    /**
     * текущую версию мода смотрим
     */
    private String getCurrentAegisVersion() {
        return FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(modContainer -> modContainer.getMetadata().getVersion().getFriendlyString())
                .orElse(null);
    }

    /**
     * свежую версию с гитхаба тянем
     */
    private String fetchLatestReleaseVersion() {
        String url = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
            conn.setRequestProperty("Accept", "application/vnd.github.v3+json");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            if (conn.getResponseCode() != 200) {
                LOGGER.warn("[AegisUpdater] GitHub API returned {}", conn.getResponseCode());
                return null;
            }

            try (InputStream is = conn.getInputStream();
                 BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    sb.append(line);
                }
                String json = sb.toString();

                String tagName = extractJsonValue(json, "tag_name");
                if (tagName != null && tagName.startsWith("v")) {
                    return tagName.substring(1);
                }
                return tagName;
            }
        } catch (Exception e) {
            LOGGER.error("[AegisUpdater] Failed to fetch latest version", e);
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private String extractJsonValue(String json, String key) {
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        int colonIndex = json.indexOf(':', keyIndex + searchKey.length());
        if (colonIndex == -1) return null;
        int quoteStart = json.indexOf('"', colonIndex + 1);
        if (quoteStart == -1) return null;
        int quoteEnd = json.indexOf('"', quoteStart + 1);
        if (quoteEnd == -1) return null;
        return json.substring(quoteStart + 1, quoteEnd);
    }

    /**
     * апдейтер качает тут через темп
     */
    private Path downloadLatestRelease(String version) {
        if (version == null || !version.matches("\\d+\\.\\d+\\.\\d+(?:-[0-9A-Za-z.-]+)?(?:\\+[0-9A-Za-z.-]+)?")) {
            LOGGER.error("[AegisUpdater] Invalid release version: {}", version);
            return null;
        }
        String jarVersion = version.split("[-+]", 2)[0];
        String downloadUrl = "https://github.com/" + GITHUB_REPO + "/releases/download/v" + version + "/" + MOD_NAME_PREFIX + jarVersion + MOD_NAME_SUFFIX;

        Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
        if (!Files.exists(modsDir)) {
            try {
                Files.createDirectories(modsDir);
            } catch (IOException e) {
                LOGGER.error("[AegisUpdater] Failed to create mods directory", e);
                return null;
            }
        }

        Path targetPath = modsDir.resolve(MOD_NAME_PREFIX + jarVersion + MOD_NAME_SUFFIX);
        // качаем в темп чтоб не сломать
        Path tempPath = modsDir.resolve(MOD_NAME_PREFIX + jarVersion + ".jar.tmp");

        HttpURLConnection conn = null;
        try {
            LOGGER.info("[AegisUpdater] Downloading from: {}", downloadUrl);
            URL url = URI.create(downloadUrl).toURL();
            conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("User-Agent", "AegisUpdater/" + getUpdaterVersion());

            if (conn.getResponseCode() != 200) {
                LOGGER.error("[AegisUpdater] Download failed: HTTP {}", conn.getResponseCode());
                Files.deleteIfExists(tempPath);
                return null;
            }

            long contentLength = conn.getContentLengthLong();
            try (InputStream in = conn.getInputStream();
                 OutputStream out = Files.newOutputStream(tempPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

                byte[] buffer = new byte[8192];
                long totalRead = 0;
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    totalRead += read;
                    if (contentLength > 0) {
                        int progress = (int) ((totalRead * 100) / contentLength);
                        if (progress % 25 == 0 && totalRead > 0) {
                            LOGGER.info("[AegisUpdater] Download progress: {}%", progress);
                        }
                    }
                }
                out.flush();
            }

            // размер файла сверяем
            long actualSize = Files.size(tempPath);
            if (contentLength > 0 && actualSize != contentLength) {
                LOGGER.error("[AegisUpdater] Download incomplete! Expected {} bytes, got {} bytes", contentLength, actualSize);
                Files.deleteIfExists(tempPath);
                return null;
            }

            // Keep the working jars until the validated replacement is in place.
            UpdateInstaller.install(tempPath, targetPath);
            try {
                deleteOldAegisJars(modsDir, targetPath);
            } catch (IOException e) {
                LOGGER.warn("[AegisUpdater] Update installed, but old jars could not be scanned", e);
            }

            LOGGER.info("[AegisUpdater] Downloaded {} bytes to {}", actualSize, targetPath);
            return targetPath;
        } catch (Exception e) {
            LOGGER.error("[AegisUpdater] Failed to download update", e);
            // при ошибке темп чистим
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {}
            // рабочий джарник не трогаем
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void deleteOldAegisJars(Path modsDir, Path installedJar) throws IOException {
        try (var stream = Files.list(modsDir)) {
            stream.filter(p -> {
                String name = p.getFileName().toString();
                return !p.equals(installedJar) && JAR_VERSION_PATTERN.matcher(name).matches();
            })
            .sorted(Comparator.reverseOrder())
            .forEach(p -> {
                try {
                    Files.delete(p);
                    LOGGER.info("[AegisUpdater] Deleted old jar: {}", p.getFileName());
                } catch (IOException e) {
                    LOGGER.warn("[AegisUpdater] Could not delete {}: {}", p.getFileName(), e.getMessage());
                }
            });
        }
    }

    private String getUpdaterVersion() {
        return FabricLoader.getInstance()
                .getModContainer("aegis_updater")
                .map(mc -> mc.getMetadata().getVersion().getFriendlyString())
                .orElse("0.1.0");
    }

    /**
     * версия новее чекаем тут
     */
    private boolean isVersionNewer(String latestVersion, String currentVersion) {
        return compareVersions(latestVersion, currentVersion) > 0;
    }

    /**
     * рестарт через файлик планируем
     */
    private void scheduleRestart() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path restartFlag = gameDir.resolve(".aegis_needs_restart");
        try {
            Files.writeString(restartFlag, String.valueOf(System.currentTimeMillis()));
            LOGGER.info("[AegisUpdater] Update downloaded successfully. A restart is required.");
            LOGGER.info("[AegisUpdater] Please restart your Minecraft launcher to apply the update.");

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    attemptRestart(gameDir);
                } catch (Exception e) {
                    LOGGER.error("[AegisUpdater] Failed to execute restart hook", e);
                }
            }));
        } catch (IOException e) {
            LOGGER.error("[AegisUpdater] Failed to schedule restart", e);
        }
    }

    private void attemptRestart(Path gameDir) {
        Path restartFlag = gameDir.resolve(".aegis_needs_restart");
        if (!Files.exists(restartFlag)) return;

        try {
            Files.delete(restartFlag);
        } catch (IOException e) {
            LOGGER.warn("[AegisUpdater] Could not delete restart flag", e);
        }

        String launcherPath = System.getProperty("aegis.launcher.path");
        if (launcherPath != null && !launcherPath.isEmpty()) {
            try {
                LOGGER.info("[AegisUpdater] Restarting via launcher: {}", launcherPath);
                new ProcessBuilder(launcherPath).start();
                return;
            } catch (Exception e) {
                LOGGER.warn("[AegisUpdater] Failed to restart via launcher", e);
            }
        }

        LOGGER.info("[AegisUpdater] ============================================");
        LOGGER.info("[AegisUpdater] AegisNeo has been updated!");
        LOGGER.info("[AegisUpdater] Please restart your Minecraft launcher.");
        LOGGER.info("[AegisUpdater] ============================================");
    }
}
