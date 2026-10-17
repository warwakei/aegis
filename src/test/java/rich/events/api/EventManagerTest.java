package rich.events.api;

import org.junit.After;
import org.junit.Test;
import rich.events.api.events.Event;
import rich.events.api.events.EventStoppable;
import rich.events.api.types.Priority;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;

public class EventManagerTest {
    private record Tick() implements Event {}
    private static final AtomicInteger STATIC_CALLS = new AtomicInteger();

    @After
    public void cleanup() {
        EventManager.cleanMap(false);
        STATIC_CALLS.set(0);
    }

    private static class Listener {
        final AtomicInteger calls = new AtomicInteger();
        @EventHandler private void tick(Tick tick) { calls.incrementAndGet(); }
    }

    private static class StaticListener {
        @EventHandler private static void tick(Tick tick) { STATIC_CALLS.incrementAndGet(); }
        @EventHandler private void invalid(String value) { fail("Invalid handler must be ignored"); }
    }

    @Test
    public void duplicateRegistrationAndUnregisterWork() {
        Listener listener = new Listener();
        EventManager.register(listener);
        EventManager.register(listener);
        EventManager.callEvent(new Tick());
        assertEquals(1, listener.calls.get());
        EventManager.unregister(listener);
        EventManager.callEvent(new Tick());
        assertEquals(1, listener.calls.get());
    }

    @Test
    public void staticHandlersAreRegisteredWithoutIllegalAccessArgument() {
        EventManager.register(new StaticListener());
        EventManager.callEvent(new Tick());
        assertEquals(1, STATIC_CALLS.get());
    }

    @Test
    public void networkDispatchAndRegistrationsDoNotLoseListeners() throws Exception {
        List<Listener> listeners = new ArrayList<>();
        for (int i = 0; i < 400; i++) listeners.add(new Listener());
        try (var executor = Executors.newFixedThreadPool(5)) {
            for (Listener listener : listeners) executor.submit(() -> EventManager.register(listener));
            for (int i = 0; i < 100; i++) executor.submit(() -> EventManager.callEvent(new Tick()));
            executor.shutdown();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
        int[] before = listeners.stream().mapToInt(l -> l.calls.get()).toArray();
        EventManager.callEvent(new Tick());
        for (int i = 0; i < listeners.size(); i++) assertEquals(before[i] + 1, listeners.get(i).calls.get());
    }

    @Test
    public void stoppableEventsRespectPriority() {
        class Stop extends EventStoppable {}
        List<String> order = new ArrayList<>();
        Object listener = new Object() {
            @EventHandler(Priority.LOW) void last(Stop event) { order.add("low"); }
            @EventHandler(Priority.HIGHEST) void first(Stop event) { order.add("high"); event.stop(); }
        };
        EventManager.register(listener);
        EventManager.callEvent(new Stop());
        assertEquals(List.of("high"), order);
    }
}
