package dev.ricky.galaxywatchtweaker.core;

public final class WatchdogPolicy {
    public static final String WATCH_MANAGER_PACKAGE = "com.samsung.android.app.watchmanager";
    public static final String WATCH7_PLUGIN_PACKAGE = "com.samsung.wearable.watch7plugin";

    private static final long DEFAULT_STALE_TIMEOUT_MS = 90_000L;
    private static final long DEFAULT_RECOVERY_COOLDOWN_MS = 5 * 60_000L;

    private final long staleTimeoutMs;
    private final long recoveryCooldownMs;

    public WatchdogPolicy(long staleTimeoutMs, long recoveryCooldownMs) {
        this.staleTimeoutMs = staleTimeoutMs;
        this.recoveryCooldownMs = recoveryCooldownMs;
    }

    public static WatchdogPolicy defaults() {
        return new WatchdogPolicy(DEFAULT_STALE_TIMEOUT_MS, DEFAULT_RECOVERY_COOLDOWN_MS);
    }

    public static boolean isTargetPackage(String packageName) {
        return WATCH_MANAGER_PACKAGE.equals(packageName) || WATCH7_PLUGIN_PACKAGE.equals(packageName);
    }

    public RecoveryDecision evaluate(WatchdogState state, long nowElapsedMs) {
        if (!isTargetPackage(state.getPackageName())) {
            return RecoveryDecision.skip(RecoveryDecision.Reason.NOT_TARGET_PACKAGE);
        }
        if (!state.isEnabled()) {
            return RecoveryDecision.skip(RecoveryDecision.Reason.DISABLED);
        }
        if (!state.isBluetoothAvailable()) {
            return RecoveryDecision.skip(RecoveryDecision.Reason.BLUETOOTH_UNAVAILABLE);
        }
        if (!state.isConnectionStateKnown()) {
            return RecoveryDecision.skip(RecoveryDecision.Reason.CONNECTION_STATE_UNKNOWN);
        }
        if (state.isWearableConnected()) {
            return RecoveryDecision.skip(RecoveryDecision.Reason.ALREADY_CONNECTED);
        }
        if (state.isUserInteracting()) {
            return RecoveryDecision.skip(RecoveryDecision.Reason.USER_INTERACTING);
        }
        if (state.getLastRecoveryElapsedMs() > 0
                && nowElapsedMs - state.getLastRecoveryElapsedMs() < recoveryCooldownMs) {
            return RecoveryDecision.skip(RecoveryDecision.Reason.COOLDOWN_ACTIVE);
        }
        long disconnectedAge = state.getObservedDisconnectedAgeMs() > 0
                ? state.getObservedDisconnectedAgeMs()
                : nowElapsedMs - state.getDisconnectedSinceElapsedMs();
        if (disconnectedAge < staleTimeoutMs) {
            return RecoveryDecision.skip(RecoveryDecision.Reason.WAITING_FOR_STALE_TIMEOUT);
        }
        return RecoveryDecision.recover(RecoveryDecision.Reason.STALE_DISCONNECTED_PLUGIN);
    }
}
