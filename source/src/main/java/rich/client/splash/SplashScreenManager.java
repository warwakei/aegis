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

    private static volatile boolean useTerminalSplash = false;

    public static void initialize() {
        if (instance.get() != null) {
            return;
        }

        if (AegisTerminalSplash.available()) {
            useTerminalSplash = true;
            AegisTerminalSplash.initialize();
            keepMinecraftWindowHidden = true;
            startWindowVisibilityGuard();
            if (!requirementsChecked) {
                boolean allowed = checkSystemRequirementsBlockingTerminal();
                requirementsChecked = true;
                if (!allowed) {
                    AegisTerminalSplash.completeAndWait();
                    System.exit(1);
                }
            }
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

    private static boolean checkSystemRequirementsBlockingTerminal() {
        updateProgress(1, "Checking system requirements...");
        try {
            List<SystemRequirementsChecker.RequirementResult> results = SystemRequirementsChecker.checkSystemRequirements();
            List<SystemRequirementsChecker.RequirementResult> failed = results.stream().filter(r -> !r.isPassed()).toList();
            if (!failed.isEmpty()) {
                for (SystemRequirementsChecker.RequirementResult r : failed) updateProgress(1, "WARN: " + r.getMessage());
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            }
        } catch (Throwable ignored) {}
        updateProgress(3, "System requirements confirmed.");
        return true;
    }

    private static boolean checkSystemRequirementsBlocking() {
        updateProgress(1, "Checking system requirements...");

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
            updateProgress(3, "System requirements confirmed.");
        } else {
            updateProgress(3, "Waiting for a system requirements decision...");
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
        if (useTerminalSplash) {
            AegisTerminalSplash.updateProgress(progress, status);
            return;
        }
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
        if (useTerminalSplash) {
            AegisTerminalSplash.completeAndWait();
            keepMinecraftWindowHidden = false;
            showMinecraftWindow();
            // fullscreen + focus
            try {
                MinecraftClient client = MinecraftClient.getInstance();
                if (client != null && client.getWindow() != null) {
                    Object window = client.getWindow();
                    // try to enable fullscreen via reflection (method name varies by version)
                    for (java.lang.reflect.Method m : window.getClass().getDeclaredMethods()) {
                        String name = m.getName().toLowerCase();
                        if (name.contains("fullscreen")) {
                            m.setAccessible(true);
                            if (m.getParameterCount() == 1 && m.getParameterTypes()[0] == boolean.class) {
                                m.invoke(window, true);
                            } else if (m.getParameterCount() == 0) {
                                m.invoke(window);
                            }
                            break;
                        }
                    }
                    long handle = client.getWindow().getHandle();
                    if (handle != 0L) {
                        org.lwjgl.glfw.GLFW.glfwMaximizeWindow(handle);
                        org.lwjgl.glfw.GLFW.glfwFocusWindow(handle);
                    }
                }
            } catch (Throwable ignored) {}
            return;
        }
        SplashScreen splash = instance.get();
        if (splash != null) {
            splash.updateProgress(100, "Done!");
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
     * добивалка на выход с точками
     * отдельно от старта без проверок
     */
    public static SplashScreen showFinishingScreen() {
        if (useTerminalSplash) {
            AegisTerminalSplash.showGoodbye();
            return null;
        }
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
