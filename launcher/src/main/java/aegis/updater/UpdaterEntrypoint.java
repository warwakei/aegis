package aegis.updater;

import net.fabricmc.loader.api.FabricLoader;
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

    // Pattern to extract version from jar filename: AegisNeo-0.6.1.jar ->0.6.1
    private static final Pattern JAR_VERSION_PATTERN = Pattern.compile(
            Pattern.quote(MOD_NAME_PREFIX) + "(\\d+\\.\\d+\\.\\d+)" + Pattern.quote(MOD_NAME_SUFFIX)
    );

    @Override
    public void onPreLaunch() {
        try {
            // Step 1: Clean up old AegisNeo jars (keep only the latest version)
            cleanupOldAegisJars();

            // Step 2: Check if AegisNeo is missing and download if needed
            checkAndDownloadIfMissing();

            // Step 3: Check for updates against GitHub
            runUpdateCheck();
        } catch (Exception e) {
            LOGGER.error("[AegisUpdater] Failed during pre-launch", e);
        }
    }

    /**
     * Scans the mods folder for AegisNeo jars and removes all but the latest version.
     */
    private void cleanupOldAegisJars() {
        Path modsDir = FabricLoader.getInstance().getGameDir().resolve("mods");
        if (!Files.exists(modsDir)) {
            return;
        }

        // Find all AegisNeo jars and extract their versions
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

        // Sort by version descending (newest first)
        jars.sort((a, b) -> compareVersions(b.version, a.version));

        // Keep the first (newest), delete the rest
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
     * Extracts version number from jar filename.
     * e.g., "AegisNeo-0.6.1.jar" -> "0.6.1"
     */
    private String extractVersionFromJarName(String fileName) {
        Matcher matcher = JAR_VERSION_PATTERN.matcher(fileName);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Compares two version strings.
     * Returns positive if v1 > v2, negative if v1 < v2, 0 if equal.
     */
    private int compareVersions(String v1, String v2) {
        try {
            String[] parts1 = v1.split("[-.]");
            String[] parts2 = v2.split("[-.]");

            int len = Math.max(parts1.length, parts2.length);
            for (int i = 0; i < len; i++) {
                String p1 = (i < parts1.length) ? parts1[i] : "0";
                String p2 = (i < parts2.length) ? parts2[i] : "0";

                try {
                    int n1 = Integer.parseInt(p1);
                    int n2 = Integer.parseInt(p2);
                    if (n1 != n2) return Integer.compare(n1, n2);
                } catch (NumberFormatException e) {
                    int cmp = p1.compareTo(p2);
                    if (cmp != 0) return cmp;
                }
            }
            return 0;
        } catch (Exception e) {
            return 0;
        }
    }

    private record AegisJarInfo(Path path, String version) {}

    /**
     * If AegisNeo is not installed but the updater is, download the latest AegisNeo.
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
        // If AegisNeo is still not loaded, skip version check (we already tried to download it)
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
     * Gets the currently installed version of AegisNeo mod.
     */
    private String getCurrentAegisVersion() {
        return FabricLoader.getInstance().getModContainer(MOD_ID)
                .map(modContainer -> modContainer.getMetadata().getVersion().getFriendlyString())
                .orElse(null);
    }

    /**
     * Fetches the latest release version from GitHub.
     * Expected tag format: vX.X.X-stage (e.g., v0.6.1-beta)
     * Returns version without 'v' prefix (e.g.,0.6.1-beta)
     */
    private String fetchLatestReleaseVersion() {
        String url = "https://api.github.com/repos/" + GITHUB_REPO + "/releases/latest";
        try {
            HttpURLConnection conn = (HttpURLConnection) URI.create(url).toURL().openConnection();
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
     * Downloads the latest release jar from GitHub.
     * ВАЖНО: Скачиваем во временный файл, затем атомарно перемещаем.
     * Это предотвращает повреждение JAR если скачивание прервётся.
     */
    private Path downloadLatestRelease(String version) {
        String jarVersion = version.split("-")[0];
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
        // Скачиваем во временный файл чтобы не повредить основной JAR
        Path tempPath = modsDir.resolve(MOD_NAME_PREFIX + jarVersion + ".jar.tmp");

        try {
            LOGGER.info("[AegisUpdater] Downloading from: {}", downloadUrl);
            URL url = URI.create(downloadUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
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

            // Проверяем что файл скачался полностью (размер совпадает)
            long actualSize = Files.size(tempPath);
            if (contentLength > 0 && actualSize != contentLength) {
                LOGGER.error("[AegisUpdater] Download incomplete! Expected {} bytes, got {} bytes", contentLength, actualSize);
                Files.deleteIfExists(tempPath);
                return null;
            }

            // Теперь атомарно перемещаем временный файл в целевой
            // Сначала удаляем старые версии
            deleteOldAegisJars(modsDir);
            Files.move(tempPath, targetPath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);

            LOGGER.info("[AegisUpdater] Downloaded {} bytes to {}", actualSize, targetPath);
            return targetPath;
        } catch (Exception e) {
            LOGGER.error("[AegisUpdater] Failed to download update", e);
            // Удаляем временный файл при ошибке
            try {
                Files.deleteIfExists(tempPath);
            } catch (IOException ignored) {}
            // Не удаляем targetPath здесь — если он существовал, он должен остаться рабочим
            return null;
        }
    }

    private void deleteOldAegisJars(Path modsDir) throws IOException {
        try (var stream = Files.list(modsDir)) {
            stream.filter(p -> {
                String name = p.getFileName().toString();
                return name.startsWith(MOD_NAME_PREFIX) && name.endsWith(MOD_NAME_SUFFIX);
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
     * Compares two version strings.
     * Returns true if latestVersion > currentVersion.
     */
    private boolean isVersionNewer(String latestVersion, String currentVersion) {
        return compareVersions(latestVersion, currentVersion) > 0;
    }

    /**
     * Schedules a restart by creating a marker file.
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
