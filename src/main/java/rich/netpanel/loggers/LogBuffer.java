package rich.netpanel.loggers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Ring buffer for log entries. Automatically overwrites oldest entries when full.
 * Thread-safe.
 */
public class LogBuffer {

    public record LogEntry(long timestamp, String level, String message) {
        public LogEntry(String level, String message) {
            this(System.currentTimeMillis(), level, message);
        }
    }

    private final LogEntry[] buffer;
    private final int capacity;
    private int index = 0;
    private int count = 0;

    public LogBuffer(int capacity) {
        this.capacity = capacity;
        this.buffer = new LogEntry[capacity];
    }

    public synchronized void add(String level, String message) {
        buffer[index] = new LogEntry(level, message);
        index = (index + 1) % capacity;
        if (count < capacity) count++;
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
        int actual = Math.min(n, count);
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

    public synchronized void clear() {
        index = 0;
        count = 0;
    }
}
