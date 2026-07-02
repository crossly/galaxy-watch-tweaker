package dev.ricky.galaxywatchtweaker.core;

public final class WatchdogState {
    private final String packageName;
    private final boolean enabled;
    private final boolean bluetoothAvailable;
    private final boolean pluginProcessAlive;
    private final boolean userInteracting;
    private final boolean connectionStateKnown;
    private final boolean wearableConnected;
    private final long disconnectedSinceElapsedMs;
    private final long lastRecoveryElapsedMs;
    private final long observedDisconnectedAgeMs;

    public WatchdogState(
            String packageName,
            boolean enabled,
            boolean bluetoothAvailable,
            boolean pluginProcessAlive,
            boolean userInteracting,
            boolean wearableConnected,
            long disconnectedSinceElapsedMs,
            long lastRecoveryElapsedMs,
            long observedDisconnectedAgeMs
    ) {
        this(
                packageName,
                enabled,
                bluetoothAvailable,
                pluginProcessAlive,
                userInteracting,
                true,
                wearableConnected,
                disconnectedSinceElapsedMs,
                lastRecoveryElapsedMs,
                observedDisconnectedAgeMs
        );
    }

    public WatchdogState(
            String packageName,
            boolean enabled,
            boolean bluetoothAvailable,
            boolean pluginProcessAlive,
            boolean userInteracting,
            boolean connectionStateKnown,
            boolean wearableConnected,
            long disconnectedSinceElapsedMs,
            long lastRecoveryElapsedMs,
            long observedDisconnectedAgeMs
    ) {
        this.packageName = packageName;
        this.enabled = enabled;
        this.bluetoothAvailable = bluetoothAvailable;
        this.pluginProcessAlive = pluginProcessAlive;
        this.userInteracting = userInteracting;
        this.connectionStateKnown = connectionStateKnown;
        this.wearableConnected = wearableConnected;
        this.disconnectedSinceElapsedMs = disconnectedSinceElapsedMs;
        this.lastRecoveryElapsedMs = lastRecoveryElapsedMs;
        this.observedDisconnectedAgeMs = observedDisconnectedAgeMs;
    }

    public String getPackageName() {
        return packageName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public boolean isBluetoothAvailable() {
        return bluetoothAvailable;
    }

    public boolean isPluginProcessAlive() {
        return pluginProcessAlive;
    }

    public boolean isUserInteracting() {
        return userInteracting;
    }

    public boolean isConnectionStateKnown() {
        return connectionStateKnown;
    }

    public boolean isWearableConnected() {
        return wearableConnected;
    }

    public long getDisconnectedSinceElapsedMs() {
        return disconnectedSinceElapsedMs;
    }

    public long getLastRecoveryElapsedMs() {
        return lastRecoveryElapsedMs;
    }

    public long getObservedDisconnectedAgeMs() {
        return observedDisconnectedAgeMs;
    }
}
