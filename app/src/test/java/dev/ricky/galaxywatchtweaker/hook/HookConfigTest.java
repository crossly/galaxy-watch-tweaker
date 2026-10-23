package dev.ricky.galaxywatchtweaker.hook;

import android.content.SharedPreferences;
import dev.ricky.galaxywatchtweaker.settings.TweakerPreferences;
import io.github.libxposed.api.XposedInterface;
import org.junit.Test;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import static org.junit.Assert.*;

public class HookConfigTest {
    private XposedInterface reader(AtomicReference<Map<String, Object>> values) {
        SharedPreferences preferences = (SharedPreferences) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{SharedPreferences.class},
                (proxy, method, args) -> { if (method.getName().equals("getAll")) return values.get();
                    throw new AssertionError("Only an atomic getAll read is permitted"); });
        return (XposedInterface) Proxy.newProxyInstance(getClass().getClassLoader(),
                new Class<?>[]{XposedInterface.class}, (proxy, method, args) -> preferences);
    }
    @Test public void allPhasesUseTheSameSnapshotInsteadOfMixingProfileRevisions() {
        Map<String, Object> original = new HashMap<>();
        original.put(TweakerPreferences.KEY_SPOOF_MODEL, "CUSTOM");
        original.put(TweakerPreferences.KEY_CONNECTION_RECOVERY_ENABLED, false);
        AtomicReference<Map<String, Object>> values = new AtomicReference<>(original);
        HookConfig config = new HookConfig(reader(values));
        assertFalse(config.isConnectionRecoveryEnabled());
        original.put(TweakerPreferences.KEY_SPOOF_MODEL, "MUTATED");
        values.set(Map.of(TweakerPreferences.KEY_SPOOF_SALES_CODE, "NEW"));
        assertEquals("CUSTOM", config.companionSpoofProfile().model());
        assertEquals("XAA", config.companionSpoofProfile().salesCode());
    }
    @Test public void pinnedCompanionIdentityIsSharedByBothCompanionDomains() {
        AtomicReference<Map<String, Object>> values = new AtomicReference<>(Map.of(
                "companion_spoof_model", "PINNED", TweakerPreferences.KEY_SPOOF_MODEL, "EDITED"));
        HookConfig config = new HookConfig(reader(values));
        assertEquals("PINNED", config.companionSpoofProfile().model());
        assertEquals("EDITED", config.spoofProfile().model());
    }
    @Test public void unreadablePreferencesSkipHooksAndCanBeRetriedAtTheNextLoadPhase() {
        AtomicReference<Map<String, Object>> values = new AtomicReference<>(null);
        HookConfig config = new HookConfig(reader(values));
        assertFalse(config.isCompanionIdentityEnabled());
        assertFalse(config.isCapabilityExchangeEnabled());
        assertFalse(config.isConnectionRecoveryEnabled());
        values.set(new HashMap<>());
        assertFalse(config.isCompanionIdentityEnabled());
        values.set(Map.of(TweakerPreferences.KEY_COMPANION_IDENTITY_ENABLED, true));
        assertTrue(config.isCompanionIdentityEnabled());
    }
}
