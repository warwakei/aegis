package rich.netpanel.loggers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * кольцевой буфер логов держим
 */
public class LogBuffer {

    private static final AtomicLong ENTRY_IDS = new AtomicLong();
    private static final String SESSION_ID = java.util.UUID.randomUUID().toString();

    public record LogEntry(String id, long timestamp, String level, String message) {
        public LogEntry(String level, String message) {
            this(SESSION_ID + ":" + ENTRY_IDS.incrementAndGet(), System.currentTimeMillis(), level, message);
        }
    }

    private final LogEntry[] buffer;
    private final int capacity;
    private int index = 0;
    private int count = 0;
    private long revision = 0;

    public LogBuffer(int capacity) {
        if (capacity <= 0) throw new IllegalArgumentException("Capacity must be positive");
        this.capacity = capacity;
        this.buffer = new LogEntry[capacity];
    }

    public synchronized void add(String level, String message) {
        buffer[index] = new LogEntry(level, message);
        index = (index + 1) % capacity;
        if (count < capacity) count++;
        revision++;
    }

    public synchronized void add(String message) {
        add("INFO", message);
    }

    public synchronized List<LogEntry> getAll() {
        List<LogEntry> result = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            int idx = (index - count + i + capacity) % capacity;
            result.add(buffer[idx]);
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized List<LogEntry> getLatest(int n) {
        int actual = Math.max(0, Math.min(n, count));
        List<LogEntry> result = new ArrayList<>(actual);
        for (int i = 0; i < actual; i++) {
            int idx = (index - actual + i + capacity) % capacity;
            result.add(buffer[idx]);
        }
        return Collections.unmodifiableList(result);
    }

    public synchronized int size() {
        return count;
    }

    public synchronized long getRevision() {
        return revision;
    }

    public record Snapshot(long revision, List<LogEntry> entries) {}

    public synchronized Snapshot snapshotSince(long previousRevision) {
        int limit = previousRevision < 0 ? count : (int) Math.min(count, Math.max(0, revision - previousRevision));
        return new Snapshot(revision, getLatest(limit));
    }

    public synchronized void clear() {
        index = 0;
        count = 0;
        java.util.Arrays.fill(buffer, null);
        revision++;
    }
}
