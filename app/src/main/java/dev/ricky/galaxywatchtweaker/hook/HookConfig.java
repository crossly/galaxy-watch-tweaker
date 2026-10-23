package dev.ricky.galaxywatchtweaker.hook;

import android.content.SharedPreferences;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import dev.ricky.galaxywatchtweaker.settings.CompanionProfilePolicy;
import dev.ricky.galaxywatchtweaker.settings.SpoofProfile;
import dev.ricky.galaxywatchtweaker.settings.TweakerPreferences;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import io.github.libxposed.api.XposedInterface;

public final class HookConfig {
    private static final String TAG = "GalaxyWatchTweakerConfig";
    private static final String MODULE_PACKAGE = "dev.ricky.galaxywatchtweaker";
    private static volatile XposedInterface defaultXposed;
    private final XposedInterface xposed;
    private Map<String, ?> snapshot;

    public HookConfig() { this(null); }
    public HookConfig(XposedInterface xposed) { this.xposed = xposed; }

    public static void setDefaultXposed(XposedInterface xposed) { defaultXposed = xposed; }

    public boolean isShmPatchEnabled() {
        return getBoolean(TweakerPreferences.KEY_SHM_PATCH_ENABLED, TweakerPreferences.DEFAULT_SHM_PATCH_ENABLED);
    }
    public boolean isCompanionIdentityEnabled() {
        return getBoolean(TweakerPreferences.KEY_COMPANION_IDENTITY_ENABLED, TweakerPreferences.DEFAULT_COMPANION_IDENTITY_ENABLED);
    }
    public boolean isCapabilityExchangeEnabled() {
        return getBoolean(TweakerPreferences.KEY_CAPABILITY_EXCHANGE_ENABLED, TweakerPreferences.DEFAULT_CAPABILITY_EXCHANGE_ENABLED);
    }
    public boolean isConnectionRecoveryEnabled() {
        return getBoolean(TweakerPreferences.KEY_CONNECTION_RECOVERY_ENABLED, TweakerPreferences.DEFAULT_CONNECTION_RECOVERY_ENABLED);
    }
    public boolean isWatchAliasOverrideEnabled() {
        return getBoolean(TweakerPreferences.KEY_WATCH_ALIAS_OVERRIDE_ENABLED, TweakerPreferences.DEFAULT_WATCH_ALIAS_OVERRIDE_ENABLED);
    }
    public String watchAliasName() {
        Object value = values().get(TweakerPreferences.KEY_WATCH_ALIAS_NAME);
        return value instanceof String ? ((String) value).trim() : TweakerPreferences.DEFAULT_WATCH_ALIAS_NAME;
    }

    public SpoofProfile spoofProfile() {
        return SpoofProfile.fromPreferences(values());
    }

    public SpoofProfile companionSpoofProfile() {
        Map<String, Object> profile = new HashMap<>();
        for (Map.Entry<String, String> entry : CompanionProfilePolicy.pinnedValues(values()).entrySet()) {
            profile.put(entry.getKey().substring(CompanionProfilePolicy.PREFIX.length()), entry.getValue());
        }
        return SpoofProfile.fromPreferences(profile);
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        Map<String, ?> values = readSnapshot();
        // A failed read must never silently replace a custom identity or re-enable a disabled hook.
        if (values == null) return false;
        Object value = values.get(key);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    private Map<String, ?> values() {
        Map<String, ?> values = readSnapshot();
        return values != null ? values : Collections.emptyMap();
    }

    private synchronized Map<String, ?> readSnapshot() {
        if (snapshot != null) return snapshot;
        try {
            XposedInterface current = xposed != null ? xposed : defaultXposed;
            Map<String, ?> all;
            if (current != null) {
                SharedPreferences preferences = current.getRemotePreferences(TweakerPreferences.REMOTE_GROUP);
                if (preferences == null) return null;
                all = preferences.getAll();
            } else {
                XSharedPreferences preferences = new XSharedPreferences(MODULE_PACKAGE, TweakerPreferences.FILE_NAME);
                preferences.reload();
                all = preferences.getAll();
                // An unreadable legacy file also returns an empty map; handled below.
            }
            if (all == null || all.isEmpty()) return null;
            // One read for all hooks/phases: never assemble an identity from separate preference reads.
            snapshot = Collections.unmodifiableMap(new HashMap<>(all));
            return snapshot;
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": configuration unavailable; hooks skipped until a later load phase");
            XposedBridge.log(t);
            return null;
        }
    }
}
