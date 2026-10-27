package rich.client.splash;

import java.io.*;
import java.nio.file.*;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Terminal-based splash: extracts aegis-splash.exe + art.txt from the jar (or
 * uses the ones next to the client), launches them in fullscreen WT with a
 * kiosk hook, and feeds progress through a file.
 */
public final class AegisTerminalSplash {
    private static final String RESOURCE_DIR = "/natives/";
    private static final String EXE_NAME = "aegis-splash.exe";
    private static final String ART_NAME = "art.txt";

    private static final AtomicReference<Path> workDir = new AtomicReference<>();
    private static final Path PROGRESS = Paths.get(System.getProperty("java.io.tmpdir"), "aegis-splash-progress.txt");

    private static volatile Process process;

    private AegisTerminalSplash() {}

    /** Extracts bundled resources to a temp dir (idempotent). Returns the dir or null. */
    private static synchronized Path ensureExtracted() {
        Path existing = workDir.get();
        if (existing != null && Files.exists(existing.resolve(EXE_NAME))) return existing;
        try {
            Path dir = Paths.get(System.getProperty("java.io.tmpdir"), "aegis-splash");
            Files.createDirectories(dir);
            extractResource(RESOURCE_DIR + EXE_NAME, dir.resolve(EXE_NAME));
            extractResource(RESOURCE_DIR + ART_NAME, dir.resolve(ART_NAME));
            workDir.set(dir);
            return dir;
        } catch (Exception e) {
            // fallback: look next to the client
            Path cwd = Paths.get("").toAbsolutePath();
            if (Files.exists(cwd.resolve(EXE_NAME))) { workDir.set(cwd); return cwd; }
            return null;
        }
    }

    private static void extractResource(String res, Path target) throws IOException {
        try (InputStream in = AegisTerminalSplash.class.getResourceAsStream(res)) {
            if (in == null) {
                // not in jar (dev mode) - already on disk
                if (Files.exists(target)) return;
                throw new IOException("resource not found: " + res);
            }
            // only overwrite if size differs (cheap versioning)
            if (Files.exists(target)) {
                try {
                    if (Files.size(target) == in.available()) return;
                } catch (IOException ignored) {}
            }
            Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    public static boolean available() {
        Path dir = ensureExtracted();
        return dir != null && Files.exists(dir.resolve(EXE_NAME));
    }

    private static Path exePath() { Path d = ensureExtracted(); return d == null ? null : d.resolve(EXE_NAME); }

    public static void initialize() {
        Path dir = ensureExtracted();
        if (dir == null) return;
        try {
            Files.writeString(PROGRESS, "0 Initializing...", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {}
        try {
            ProcessBuilder pb;
            if (isWindowsTerminalAvailable()) {
                pb = new ProcessBuilder("wt", "--title", "AEGIS NEO", "--", exePath().toString(), "--splash", PROGRESS.toString());
            } else {
                pb = new ProcessBuilder(exePath().toString(), "--splash", PROGRESS.toString());
            }
            pb.directory(dir.toFile());
            pb.inheritIO();
            process = pb.start();
        } catch (IOException e) {
            process = null;
        }
    }

    public static void updateProgress(int progress, String status) {
        if (process == null) return;
        int p = Math.max(0, Math.min(100, progress));
        String line = p + " " + (status == null ? "" : status);
        try {
            Files.writeString(PROGRESS, line, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {}
    }

    public static void completeAndWait() {
        if (process == null) return;
        updateProgress(100, "DONE");
        try { process.waitFor(); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
        process = null;
    }

    public static void showGoodbye() {
        Path dir = ensureExtracted();
        if (dir == null) return;
        try {
            ProcessBuilder pb;
            if (isWindowsTerminalAvailable()) {
                pb = new ProcessBuilder("wt", "--title", "AEGIS NEO", "--", exePath().toString(), "--goodbye");
            } else {
                pb = new ProcessBuilder(exePath().toString(), "--goodbye");
            }
            pb.directory(dir.toFile());
            pb.inheritIO();
            Process p = pb.start();
            p.waitFor();
        } catch (Exception ignored) {}
    }

    private static boolean isWindowsTerminalAvailable() {
        try {
            Process p = new ProcessBuilder("where", "wt").start();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
}
