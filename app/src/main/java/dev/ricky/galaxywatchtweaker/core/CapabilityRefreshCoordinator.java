package dev.ricky.galaxywatchtweaker.core;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Locale;
import java.util.Map;

/** One pair of refresh attempts per real connection, isolated by companion instance and watch. */
public final class CapabilityRefreshCoordinator {
    public interface Scheduler {
        void postDelayed(Runnable task, long delayMs);
        void remove(Runnable task);
    }

    public interface Exchange {
        void request(Object companion, String address);
    }

    private final Scheduler scheduler;
    private final Exchange exchange;
    private final Map<Object, Map<String, Session>> sessions = new IdentityHashMap<>();

    public CapabilityRefreshCoordinator(Scheduler scheduler, Exchange exchange) {
        this.scheduler = scheduler;
        this.exchange = exchange;
    }

    // All entry points, including scheduled callbacks, run on the same serial scheduler.
    public void onConnectionChanged(Object companion, String address, boolean connected) {
        if (companion == null || address == null || address.trim().isEmpty()) {
            return;
        }
        String normalized = address.trim().toUpperCase(Locale.ROOT);
        Map<String, Session> watches = sessions.get(companion);
        if (!connected) {
            if (watches != null) {
                Session previous = watches.remove(normalized);
                if (previous != null) previous.cancel();
                if (watches.isEmpty()) sessions.remove(companion);
            }
            return;
        }
        if (watches == null) {
            watches = new HashMap<>();
            sessions.put(companion, watches);
        }
        if (watches.containsKey(normalized)) return;
        Session session = new Session(companion, normalized);
        watches.put(normalized, session);
        scheduler.postDelayed(session.first, 15_000L);
        scheduler.postDelayed(session.retry, 75_000L);
    }

    public void cancel(Object companion) {
        Map<String, Session> watches = sessions.remove(companion);
        if (watches != null) {
            for (Session session : watches.values()) session.cancel();
        }
    }

    private final class Session {
        final Object companion;
        final String address;
        final Runnable first = this::request;
        final Runnable retry = this::request;

        Session(Object companion, String address) {
            this.companion = companion;
            this.address = address;
        }

        void request() {
            Map<String, Session> watches = sessions.get(companion);
            if (watches != null && watches.get(address) == this) {
                exchange.request(companion, address);
            }
        }

        void cancel() {
            scheduler.remove(first);
            scheduler.remove(retry);
        }
    }
}
