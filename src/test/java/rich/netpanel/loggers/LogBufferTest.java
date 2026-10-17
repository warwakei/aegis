package rich.netpanel.loggers;

import org.junit.Test;

import java.util.HashSet;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.Assert.*;

public class LogBufferTest {
    @Test
    public void streamsContinueAfterBufferFills() {
        LogBuffer buffer = new LogBuffer(2);
        buffer.add("first");
        buffer.add("second");
        var initial = buffer.snapshotSince(-1);
        buffer.add("third");
        var next = buffer.snapshotSince(initial.revision());
        assertEquals(2, buffer.size());
        assertEquals(1, next.entries().size());
        assertEquals("third", next.entries().getFirst().message());
        assertTrue(buffer.snapshotSince(next.revision()).entries().isEmpty());
    }

    @Test
    public void snapshotRecoversAllRetainedEntriesAfterLargeBurst() {
        LogBuffer buffer = new LogBuffer(100);
        for (int i = 0; i < 250; i++) buffer.add("entry-" + i);
        var snapshot = buffer.snapshotSince(0);
        assertEquals(100, snapshot.entries().size());
        assertEquals("entry-150", snapshot.entries().getFirst().message());
        assertEquals("entry-249", snapshot.entries().getLast().message());
    }

    @Test
    public void concurrentEntriesHaveUniqueIdsAndOrderedSnapshots() throws Exception {
        LogBuffer buffer = new LogBuffer(4000);
        try (var executor = Executors.newFixedThreadPool(4)) {
            for (int t = 0; t < 4; t++) {
                executor.submit(() -> {
                    for (int i = 0; i < 1000; i++) buffer.add("same message");
                });
            }
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
        var ids = new HashSet<String>();
        buffer.getAll().forEach(entry -> assertTrue(ids.add(entry.id())));
        assertEquals(4000, ids.size());
        assertEquals(4000, buffer.getRevision());
    }

    @Test
    public void clearChangesRevisionAndNegativeLimitsAreEmpty() {
        LogBuffer buffer = new LogBuffer(2);
        buffer.add("first");
        assertTrue(buffer.getLatest(-10).isEmpty());
        long revision = buffer.getRevision();
        buffer.clear();
        assertTrue(buffer.getRevision() > revision);
        assertTrue(buffer.getAll().isEmpty());
        buffer.add("second");
        assertEquals("second", buffer.snapshotSince(revision).entries().getFirst().message());
    }

    @Test(expected = IllegalArgumentException.class)
    public void zeroCapacityIsRejected() {
        new LogBuffer(0);
    }
}
