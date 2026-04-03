package aegis.updater;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.*;
import java.util.Comparator;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UpdaterEntrypoint implements ClientModInitializer {

    private static final Logger LOGGER = LoggerFactory.getLogger("AegisUpdater");
    private static final String GITHUB_REPO = "warwakei/aegis";
    private static final String MOD_ID = "copyright";
    private static final String MOD_NAME_PREFIX = "AegisNeo-";
    private static final String MOD_NAME_SUFFIX = ".jar";

    @Override
    public void onInitializeClient() {
        try {
            // Check if we need to download AegisNeo for the first time
            checkAndDownloadIfMissing();

            runUpdateCheck();
        } catch (Exception e) {
            LOGGER.error("[AegisUpdater] Failed to check for updates", e);
        }
    }

    /**
     * If AegisNeo is not installed but the updater is, download the latest AegisNeo.
     * This handles the case where user only has AegisUpdater01.jar without AegisNeo.
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
        Optional<ModContainer> container = FabricLoader.getInstance().getModContainer(MOD_ID);
        return container.map(modContainer -> modContainer.getMetadata().getVersion().getFriendlyString())
                        .orElse(null);
    }

    /**
     * Fetches the latest release version from GitHub.
     * Expected tag format: vX.X.X-stage (e.g., v0.5.6-beta)
     * Returns version without 'v' prefix (e.g., 0.5.6-beta)
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

                // Simple JSON parsing for "tag_name"
                String tagName = extractJsonValue(json, "tag_name");
                if (tagName != null && tagName.startsWith("v")) {
                    return tagName.substring(1); // Remove 'v' prefix
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
     * URL format: https://github.com/warwakei/aegis/releases/download/v0.5.6-beta/AegisNeo-0.5.6.jar
     */
    private Path downloadLatestRelease(String version) {
        // Extract version number without stage suffix for jar name
        // e.g., "0.5.6-beta" -> "0.5.6"
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

        // Delete old AegisNeo jars
        try {
            deleteOldAegisJars(modsDir);
        } catch (IOException e) {
            LOGGER.error("[AegisUpdater] Failed to delete old AegisNeo jars", e);
        }

        Path targetPath = modsDir.resolve(MOD_NAME_PREFIX + jarVersion + MOD_NAME_SUFFIX);

        try {
            LOGGER.info("[AegisUpdater] Downloading from: {}", downloadUrl);
            URL url = URI.create(downloadUrl).toURL();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setConnectTimeout(15000);
            conn.setReadTimeout(60000);
            conn.setRequestProperty("User-Agent", "AegisUpdater/" + getUpdaterVersion());

            if (conn.getResponseCode() != 200) {
                LOGGER.error("[AegisUpdater] Download failed: HTTP {}", conn.getResponseCode());
                return null;
            }

            long contentLength = conn.getContentLengthLong();
            try (InputStream in = conn.getInputStream();
                 OutputStream out = Files.newOutputStream(targetPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {

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
            }

            LOGGER.info("[AegisUpdater] Downloaded {} bytes to {}", contentLength, targetPath);
            return targetPath;
        } catch (Exception e) {
            LOGGER.error("[AegisUpdater] Failed to download update", e);
            // Clean up partial download
            try {
                Files.deleteIfExists(targetPath);
            } catch (IOException ignored) {}
            return null;
        }
    }

    private void deleteOldAegisJars(Path modsDir) throws IOException {
        try (var stream = Files.list(modsDir)) {
            stream.filter(p -> {
                String name = p.getFileName().toString();
                return name.startsWith(MOD_NAME_PREFIX) && name.endsWith(MOD_NAME_SUFFIX);
            })
            .sorted(Comparator.reverseOrder()) // Delete older ones first
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
        try {
            String[] latestParts = latestVersion.split("[-.]");
            String[] currentParts = currentVersion.split("[-.]");

            // Compare numeric parts
            int len = Math.max(latestParts.length, currentParts.length);
            for (int i = 0; i < len; i++) {
                String l = (i < latestParts.length) ? latestParts[i] : "0";
                String c = (i < currentParts.length) ? currentParts[i] : "0";

                // Try numeric comparison first
                try {
                    int lNum = Integer.parseInt(l);
                    int cNum = Integer.parseInt(c);
                    if (lNum > cNum) return true;
                    if (lNum < cNum) return false;
                } catch (NumberFormatException e) {
                    // String comparison for non-numeric parts (e.g., beta, alpha)
                    int cmp = l.compareTo(c);
                    if (cmp > 0) return true;
                    if (cmp < 0) return false;
                }
            }
            return false; // Equal
        } catch (Exception e) {
            LOGGER.warn("[AegisUpdater] Failed to compare versions", e);
            return false;
        }
    }

    /**
     * Schedules a restart by creating a marker file that the updater checks on next launch.
     * Since Fabric mods can't directly restart the game, we use a file-based approach:
     * The updater will detect this file on next launch and re-check for updates.
     *
     * For actual restart: we'll try to trigger it via Runtime shutdown hook.
     */
    private void scheduleRestart() {
        Path gameDir = FabricLoader.getInstance().getGameDir();
        Path restartFlag = gameDir.resolve(".aegis_needs_restart");
        try {
            Files.writeString(restartFlag, String.valueOf(System.currentTimeMillis()));
            LOGGER.info("[AegisUpdater] Update downloaded successfully. A restart is required.");
            LOGGER.info("[AegisUpdater] Please restart your Minecraft launcher to apply the update.");

            // Add shutdown hook to attempt restart
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

        // Try to restart via system property if provided by external launcher
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

        // Fallback: inform the user
        LOGGER.info("[AegisUpdater] ============================================");
        LOGGER.info("[AegisUpdater] AegisNeo has been updated!");
        LOGGER.info("[AegisUpdater] Please restart your Minecraft launcher.");
        LOGGER.info("[AegisUpdater] ============================================");
    }
}
