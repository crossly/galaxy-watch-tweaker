package dev.ricky.galaxywatchtweaker.core;

import java.util.HashMap;
import java.util.Map;

/** Requests a transport connection only; Android owns all connection-state notifications. */
public final class BluetoothReconnectController {
    public interface Device {
        boolean isEnabled();
        boolean isBonded();
        boolean isConnected();
        boolean connect();
    }

    private static final long COOLDOWN_MS = 60_000L;
    private final Map<Integer, Long> lastRequests = new HashMap<>();

    public synchronized boolean reconnect(int associationId, Device device, long elapsedRealtime) {
        if (!device.isEnabled() || !device.isBonded() || device.isConnected()) {
            return false;
        }
        Long last = lastRequests.get(associationId);
        if (last != null && elapsedRealtime - last < COOLDOWN_MS) {
            return false;
        }
        // A rejected/throwing request must also be throttled, without claiming it connected.
        lastRequests.put(associationId, elapsedRealtime);
        return device.connect();
    }
}
