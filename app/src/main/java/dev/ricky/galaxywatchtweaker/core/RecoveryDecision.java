package dev.ricky.galaxywatchtweaker.core;

public final class RecoveryDecision {
    public enum Reason {
        NOT_TARGET_PACKAGE,
        DISABLED,
        BLUETOOTH_UNAVAILABLE,
        CONNECTION_STATE_UNKNOWN,
        ALREADY_CONNECTED,
        USER_INTERACTING,
        COOLDOWN_ACTIVE,
        WAITING_FOR_STALE_TIMEOUT,
        STALE_DISCONNECTED_PLUGIN
    }

    private final boolean shouldRecover;
    private final Reason reason;

    private RecoveryDecision(boolean shouldRecover, Reason reason) {
        this.shouldRecover = shouldRecover;
        this.reason = reason;
    }

    public static RecoveryDecision recover(Reason reason) {
        return new RecoveryDecision(true, reason);
    }

    public static RecoveryDecision skip(Reason reason) {
        return new RecoveryDecision(false, reason);
    }

    public boolean shouldRecover() {
        return shouldRecover;
    }

    public Reason getReason() {
        return reason;
    }
}
