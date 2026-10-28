package rich.util.render.shader;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

public class ShaderCompilationTracker {
    private static final AtomicInteger totalShaders = new AtomicInteger(0);
    private static final AtomicInteger compiledShaders = new AtomicInteger(0);
    private static final AtomicReference<String> currentShader = new AtomicReference<>("Инициализация...");
    private static final AtomicReference<CompilationCallback> callback = new AtomicReference<>();

    private static final AtomicLong startTime = new AtomicLong(0);
    private static final AtomicLong shaderStartTime = new AtomicLong(0);
    private static final AtomicLong totalCompilationTime = new AtomicLong(0);

    public static final int TOTAL_PIPELINES = 11;

    static {
        totalShaders.set(TOTAL_PIPELINES);
    }

    public interface CompilationCallback {
        void onProgress(int compiled, int total, String currentShader, long elapsedMs);
    }

    public static void setCallback(CompilationCallback cb) {
        callback.set(cb);
    }

    public static void beginTracking() {
        startTime.set(System.currentTimeMillis());
        compiledShaders.set(0);
        totalCompilationTime.set(0);
    }

    public static void beginTracking(int total) {
        totalShaders.set(Math.max(1, total));
        beginTracking();
    }

    public static void startCompilation(String shaderName) {
        currentShader.set(shaderName);
        shaderStartTime.set(System.currentTimeMillis());
        notifyProgress();
    }

    public static void completeShader() {
        long elapsed = System.currentTimeMillis() - shaderStartTime.get();
        totalCompilationTime.addAndGet(elapsed);
        compiledShaders.incrementAndGet();
        notifyProgress();
    }

    public static void reset() {
        compiledShaders.set(0);
        currentShader.set("Инициализация...");
        startTime.set(0);
        shaderStartTime.set(0);
        totalCompilationTime.set(0);
    }

    public static int getProgress() {
        int compiled = compiledShaders.get();
        int total = totalShaders.get();
        return total > 0 ? (compiled * 100) / total : 0;
    }

    public static String getCurrentShader() {
        return currentShader.get();
    }

    public static long getTotalElapsedTime() {
        long start = startTime.get();
        return start == 0 ? 0 : System.currentTimeMillis() - start;
    }

    public static long getShaderCompilationTime() {
        return totalCompilationTime.get();
    }

    private static void notifyProgress() {
        CompilationCallback cb = callback.get();
        if (cb != null) {
            long elapsed = getTotalElapsedTime();
            cb.onProgress(compiledShaders.get(), totalShaders.get(), currentShader.get(), elapsed);
        }
    }
}
