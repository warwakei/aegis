package rich.util.config.impl.autosaver;

import rich.util.config.impl.consolelogger.Logger;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 *  © 2026 Copyright Rich Client 2.0
 *        All Rights Reserved ®
 */

public class ConfigAutoSaver {

    private static final long SAVE_INTERVAL_MS = 90_000;
    private static final long INITIAL_DELAY_MS = 90_000;

    private final ScheduledExecutorService executor;
    private final Runnable saveTask;
    private final AtomicBoolean running;
    private final AtomicLong lastSaveTime;
    private ScheduledFuture<?> scheduledTask;

    public ConfigAutoSaver(Runnable saveTask) {
        this.saveTask = saveTask;
        this.running = new AtomicBoolean(false);
        this.lastSaveTime = new AtomicLong(0);
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "Rich-ConfigAutoSaver");
            thread.setDaemon(true);
            thread.setPriority(Thread.MIN_PRIORITY);
            return thread;
        });
    }

    public void start() {
        if (running.compareAndSet(false, true)) {
            scheduledTask = executor.scheduleAtFixedRate(
                    this::executeSave,
                    INITIAL_DELAY_MS,
                    SAVE_INTERVAL_MS,
                    TimeUnit.MILLISECONDS
            );
            Logger.info("AutoConfiguration: AutoSaver started (interval: 90s)");
        }
    }

    private void executeSave() {
        if (!running.get()) {
            return;
        }
        try {
            saveTask.run();
            lastSaveTime.set(System.currentTimeMillis());
        } catch (Exception e) {
            Logger.error("AutoConfiguration: AutoSave failed! " + e.getMessage());
        }
    }

    public void stop() {
        running.set(false);
        if (scheduledTask != null) {
            scheduledTask.cancel(false);
        }
    }

    public void shutdown() {
        stop();
        executor.shutdown();
        try {
            if (!executor.awaitTermination(3, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    public long getLastSaveTime() {
        return lastSaveTime.get();
    }

    public boolean isRunning() {
        return running.get();
    }
}