package dev.ricky.galaxywatchtweaker.core;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WatchdogPolicyTest {
    private static final String PLUGIN = "com.samsung.wearable.watch7plugin";
    private static final String MANAGER = "com.samsung.android.app.watchmanager";

    @Test
    public void recognizesOnlySamsungWearableTargets() {
        assertTrue(WatchdogPolicy.isTargetPackage(PLUGIN));
        assertTrue(WatchdogPolicy.isTargetPackage(MANAGER));
        assertFalse(WatchdogPolicy.isTargetPackage("com.example.other"));
    }

    @Test
    public void waitsUntilDisconnectedStateIsStale() {
        WatchdogPolicy policy = WatchdogPolicy.defaults();
        WatchdogState state = new WatchdogState(
                PLUGIN,
                true,
                true,
                true,
                false,
                false,
                1_000L,
                0L,
                60_000L
        );

        RecoveryDecision decision = policy.evaluate(state, 100_000L);

        assertFalse(decision.shouldRecover());
        assertEquals(RecoveryDecision.Reason.WAITING_FOR_STALE_TIMEOUT, decision.getReason());
    }

    @Test
    public void recoversStalePluginWhenCooldownAllows() {
        WatchdogPolicy policy = WatchdogPolicy.defaults();
        WatchdogState state = new WatchdogState(
                PLUGIN,
                true,
                true,
                true,
                false,
                false,
                1_000L,
                0L,
                120_000L
        );

        RecoveryDecision decision = policy.evaluate(state, 130_000L);

        assertTrue(decision.shouldRecover());
        assertEquals(RecoveryDecision.Reason.STALE_DISCONNECTED_PLUGIN, decision.getReason());
    }

    @Test
    public void blocksRecoveryDuringCooldown() {
        WatchdogPolicy policy = WatchdogPolicy.defaults();
        WatchdogState state = new WatchdogState(
                PLUGIN,
                true,
                true,
                true,
                false,
                false,
                1_000L,
                120_000L,
                120_000L
        );

        RecoveryDecision decision = policy.evaluate(state, 130_000L);

        assertFalse(decision.shouldRecover());
        assertEquals(RecoveryDecision.Reason.COOLDOWN_ACTIVE, decision.getReason());
    }

    @Test
    public void refusesRecoveryWhenUserIsActivelyUsingWearableApp() {
        WatchdogPolicy policy = WatchdogPolicy.defaults();
        WatchdogState state = new WatchdogState(
                PLUGIN,
                true,
                true,
                true,
                true,
                false,
                1_000L,
                0L,
                120_000L
        );

        RecoveryDecision decision = policy.evaluate(state, 130_000L);

        assertFalse(decision.shouldRecover());
        assertEquals(RecoveryDecision.Reason.USER_INTERACTING, decision.getReason());
    }

    @Test
    public void refusesRecoveryWhenConnectionStateIsUnknown() {
        WatchdogPolicy policy = WatchdogPolicy.defaults();
        WatchdogState state = new WatchdogState(
                PLUGIN,
                true,
                true,
                true,
                false,
                false,
                false,
                1_000L,
                0L,
                120_000L
        );

        RecoveryDecision decision = policy.evaluate(state, 130_000L);

        assertFalse(decision.shouldRecover());
        assertEquals(RecoveryDecision.Reason.CONNECTION_STATE_UNKNOWN, decision.getReason());
    }
}
