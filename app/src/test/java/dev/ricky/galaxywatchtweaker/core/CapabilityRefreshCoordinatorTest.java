package dev.ricky.galaxywatchtweaker.core;

import org.junit.Test;
import java.util.ArrayList;
import java.util.List;
import static org.junit.Assert.*;

public class CapabilityRefreshCoordinatorTest {
    static final class Scheduler implements CapabilityRefreshCoordinator.Scheduler {
        final List<Runnable> tasks = new ArrayList<>();
        final List<Long> delays = new ArrayList<>();
        public void postDelayed(Runnable task, long delay) { tasks.add(task); delays.add(delay); }
        public void remove(Runnable task) { tasks.remove(task); }
        void runAll() { for (Runnable task : new ArrayList<>(tasks)) { tasks.remove(task); task.run(); } }
    }
    final Scheduler scheduler = new Scheduler();
    final List<String> exchanges = new ArrayList<>();
    final CapabilityRefreshCoordinator coordinator = new CapabilityRefreshCoordinator(
            scheduler, (owner, address) -> exchanges.add(address));
    final Object owner = new Object();

    @Test public void repeatedConnectedEventsDoNotScheduleUnboundedRefreshes() {
        coordinator.onConnectionChanged(owner, "aa:bb", true);
        coordinator.onConnectionChanged(owner, "AA:BB", true);
        assertEquals(2, scheduler.tasks.size());
        assertEquals(List.of(15_000L, 75_000L), scheduler.delays);
        scheduler.runAll();
        coordinator.onConnectionChanged(owner, "aa:bb", true);
        assertTrue(scheduler.tasks.isEmpty());
        assertEquals(List.of("AA:BB", "AA:BB"), exchanges);
    }
    @Test public void reconnectSchedulesAgainAfterBothRefreshesCompleted() {
        coordinator.onConnectionChanged(owner, "aa", true);
        scheduler.runAll();
        coordinator.onConnectionChanged(owner, "aa", false);
        coordinator.onConnectionChanged(owner, "aa", true);
        scheduler.runAll();
        assertEquals(4, exchanges.size());
    }
    @Test public void disconnectCancelsPendingAttemptsAndOldCallbacksCannotCrossSessions() {
        coordinator.onConnectionChanged(owner, "aa", true);
        List<Runnable> stale = new ArrayList<>(scheduler.tasks);
        coordinator.onConnectionChanged(owner, "aa", false);
        assertTrue(scheduler.tasks.isEmpty());
        coordinator.onConnectionChanged(owner, "aa", true);
        for (Runnable task : stale) task.run();
        assertTrue(exchanges.isEmpty());
        scheduler.runAll();
        assertEquals(2, exchanges.size());
    }
    @Test public void watchesAndCompanionInstancesAreIndependent() {
        Object other = new Object();
        coordinator.onConnectionChanged(owner, "aa", true);
        coordinator.onConnectionChanged(owner, "bb", true);
        coordinator.onConnectionChanged(other, "aa", true);
        coordinator.onConnectionChanged(owner, "aa", false);
        scheduler.runAll();
        assertEquals(4, exchanges.size());
        assertEquals(2, exchanges.stream().filter("AA"::equals).count());
        assertEquals(2, exchanges.stream().filter("BB"::equals).count());
    }
    @Test public void unknownDisconnectCancelsAllWatchesForThatCompanionOnly() {
        coordinator.onConnectionChanged(owner, "aa", true);
        coordinator.onConnectionChanged(new Object(), "bb", true);
        coordinator.cancel(owner);
        scheduler.runAll();
        assertEquals(List.of("BB", "BB"), exchanges);
    }
}
