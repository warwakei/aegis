package rich.client.splash;

import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class SplashScreenManager {
    private static final AtomicReference<SplashScreen> instance = new AtomicReference<>();
    private static final AtomicReference<SplashScreen> finishingInstance = new AtomicReference<>();
    private static volatile boolean requirementsChecked = false;
    private static volatile boolean keepMinecraftWindowHidden = true;
    private static volatile boolean visibilityGuardStarted = false;

    public static void initialize() {
        if (instance.get() != null) {
            return;
        }

        SplashScreen splash = new SplashScreen();
        instance.set(splash);

        keepMinecraftWindowHidden = true;
        startWindowVisibilityGuard();

        if (!requirementsChecked) {
            boolean allowed = checkSystemRequirementsBlocking();
            requirementsChecked = true;
            if (!allowed) {
                close();
                System.exit(1);
            }
        }
    }

    private static boolean checkSystemRequirementsBlocking() {
        updateProgress(1, "Проверка системных требований...");

        List<SystemRequirementsChecker.RequirementResult> results =
                SystemRequirementsChecker.checkSystemRequirements();

        List<SystemRequirementsChecker.RequirementResult> failed = results.stream()
                .filter(r -> !r.isPassed())
                .toList();

        boolean passed = failed.isEmpty();
        if (!passed) {
            SplashScreen splash = instance.get();
            if (splash != null) {
                passed = splash.showRequirements(failed);
            }
        }

        if (passed) {
            updateProgress(3, "Системные требования подтверждены.");
        } else {
            updateProgress(3, "Ожидание решения по системным требованиям...");
        }

        return passed;
    }

    private static void startWindowVisibilityGuard() {
        if (visibilityGuardStarted) {
            return;
        }
        visibilityGuardStarted = true;

        Thread t = new Thread(() -> {
            while (keepMinecraftWindowHidden) {
                try {
                    MinecraftClient client = MinecraftClient.getInstance();
                    if (client != null && client.getWindow() != null) {
                        long handle = client.getWindow().getHandle();
                        if (handle != 0L) {
                            GLFW.glfwHideWindow(handle);
                        }
                    }
                } catch (Throwable ignored) {
                }

                try {
                    Thread.sleep(16L);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            visibilityGuardStarted = false;
        }, "aegis-window-visibility-guard");
        t.setDaemon(true);
        t.start();
    }

    private static void showMinecraftWindow() {
        try {
            MinecraftClient client = MinecraftClient.getInstance();
            if (client != null && client.getWindow() != null) {
                long handle = client.getWindow().getHandle();
                if (handle != 0L) {
                    GLFW.glfwShowWindow(handle);
                    GLFW.glfwMaximizeWindow(handle);
                    GLFW.glfwFocusWindow(handle);
                }
            }
        } catch (Throwable ignored) {
        }
    }

    public static void updateProgress(int progress, String status) {
        SplashScreen splash = instance.get();
        if (splash != null) {
            splash.updateProgress(progress, status);
        }
    }

    public static void setProgress(int progress) {
        SplashScreen splash = instance.get();
        if (splash != null) {
            splash.setProgress(progress);
        }
    }

    public static void setStatus(String status) {
        SplashScreen splash = instance.get();
        if (splash != null) {
            splash.setStatus(status);
        }
    }

    public static void complete() {
        SplashScreen splash = instance.get();
        if (splash != null) {
            splash.updateProgress(100, "Готово!");
            new Thread(() -> {
                splash.showReadyAndWaitForLaunch();
                keepMinecraftWindowHidden = false;
                showMinecraftWindow();
                splash.close();
                instance.set(null);
            }, "aegis-splash-close").start();
        }
    }

    /**
     * Shows a "Finishing..." screen (with animated dots) intended for client shutdown.
     * Independent from the startup splash instance and does not run requirement checks.
     */
    public static SplashScreen showFinishingScreen() {
        SplashScreen existing = finishingInstance.get();
        if (existing != null) {
            return existing;
        }

        SplashScreen splash = new SplashScreen();
        splash.showFinishing();
        finishingInstance.set(splash);
        return splash;
    }

    public static void closeFinishingScreen() {
        SplashScreen splash = finishingInstance.getAndSet(null);
        if (splash != null) {
            splash.close();
        }
    }

    public static void close() {
        keepMinecraftWindowHidden = false;
        SplashScreen splash = instance.get();
        if (splash != null) {
            splash.close();
            instance.set(null);
        }
    }

    public static boolean isVisible() {
        return instance.get() != null;
    }
}
