package dev.ricky.galaxywatchtweaker.hook;

import android.content.SharedPreferences;

import dev.ricky.galaxywatchtweaker.settings.SpoofProfile;
import dev.ricky.galaxywatchtweaker.settings.TweakerPreferences;
import de.robv.android.xposed.XposedBridge;
import io.github.libxposed.api.XposedInterface;

public final class HookConfig {
    private static final String TAG = "GalaxyWatchTweakerConfig";
    private static volatile XposedInterface defaultXposed;
    private final XposedInterface xposed;

    public HookConfig() {
        this(null);
    }

    public HookConfig(XposedInterface xposed) {
        this.xposed = xposed;
    }

    public static void setDefaultXposed(XposedInterface xposed) {
        defaultXposed = xposed;
    }

    public boolean isShmPatchEnabled() {
        return getBoolean(
                TweakerPreferences.KEY_SHM_PATCH_ENABLED,
                TweakerPreferences.DEFAULT_SHM_PATCH_ENABLED);
    }

    public boolean isCompanionIdentityEnabled() {
        return getBoolean(
                TweakerPreferences.KEY_COMPANION_IDENTITY_ENABLED,
                TweakerPreferences.DEFAULT_COMPANION_IDENTITY_ENABLED);
    }

    public boolean isCapabilityExchangeEnabled() {
        return getBoolean(
                TweakerPreferences.KEY_CAPABILITY_EXCHANGE_ENABLED,
                TweakerPreferences.DEFAULT_CAPABILITY_EXCHANGE_ENABLED);
    }

    public boolean isConnectionRecoveryEnabled() {
        return getBoolean(
                TweakerPreferences.KEY_CONNECTION_RECOVERY_ENABLED,
                TweakerPreferences.DEFAULT_CONNECTION_RECOVERY_ENABLED);
    }

    public boolean isWatchAliasOverrideEnabled() {
        return getBoolean(
                TweakerPreferences.KEY_WATCH_ALIAS_OVERRIDE_ENABLED,
                TweakerPreferences.DEFAULT_WATCH_ALIAS_OVERRIDE_ENABLED);
    }

    public String watchAliasName() {
        SharedPreferences preferences = getRemotePreferences();
        if (preferences == null) {
            return TweakerPreferences.DEFAULT_WATCH_ALIAS_NAME;
        }
        return getString(
                preferences,
                TweakerPreferences.KEY_WATCH_ALIAS_NAME,
                TweakerPreferences.DEFAULT_WATCH_ALIAS_NAME);
    }

    public SpoofProfile spoofProfile() {
        SharedPreferences preferences = getRemotePreferences();
        if (preferences == null) {
            return SpoofProfile.defaults();
        }
        return new SpoofProfile(
                getString(preferences, TweakerPreferences.KEY_SPOOF_MANUFACTURER,
                        TweakerPreferences.DEFAULT_SPOOF_MANUFACTURER),
                getString(preferences, TweakerPreferences.KEY_SPOOF_BRAND,
                        TweakerPreferences.DEFAULT_SPOOF_BRAND),
                getString(preferences, TweakerPreferences.KEY_SPOOF_MODEL,
                        TweakerPreferences.DEFAULT_SPOOF_MODEL),
                getString(preferences, TweakerPreferences.KEY_SPOOF_DEVICE,
                        TweakerPreferences.DEFAULT_SPOOF_DEVICE),
                getString(preferences, TweakerPreferences.KEY_SPOOF_PRODUCT,
                        TweakerPreferences.DEFAULT_SPOOF_PRODUCT),
                getString(preferences, TweakerPreferences.KEY_SPOOF_FINGERPRINT,
                        TweakerPreferences.DEFAULT_SPOOF_FINGERPRINT),
                getString(preferences, TweakerPreferences.KEY_SPOOF_SALES_CODE,
                        TweakerPreferences.DEFAULT_SPOOF_SALES_CODE),
                getString(preferences, TweakerPreferences.KEY_SPOOF_COUNTRY_ISO,
                        TweakerPreferences.DEFAULT_SPOOF_COUNTRY_ISO),
                getString(preferences, TweakerPreferences.KEY_SPOOF_MCC,
                        TweakerPreferences.DEFAULT_SPOOF_MCC),
                getString(preferences, TweakerPreferences.KEY_SPOOF_MNC,
                        TweakerPreferences.DEFAULT_SPOOF_MNC),
                getString(preferences, TweakerPreferences.KEY_SPOOF_OPERATOR,
                        TweakerPreferences.DEFAULT_SPOOF_OPERATOR),
                getString(preferences, TweakerPreferences.KEY_SPOOF_OPERATOR_NAME,
                        TweakerPreferences.DEFAULT_SPOOF_OPERATOR_NAME));
    }

    private boolean getBoolean(String key, boolean defaultValue) {
        SharedPreferences preferences = getRemotePreferences();
        if (preferences == null) {
            return defaultValue;
        }
        try {
            return preferences.getBoolean(key, defaultValue);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to read " + key);
            XposedBridge.log(t);
            return defaultValue;
        }
    }

    private String getString(SharedPreferences preferences, String key, String defaultValue) {
        try {
            return TweakerPreferences.nonBlankOrDefault(preferences.getString(key, defaultValue), defaultValue);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to read " + key);
            XposedBridge.log(t);
            return defaultValue;
        }
    }

    private SharedPreferences getRemotePreferences() {
        XposedInterface currentXposed = xposed != null ? xposed : defaultXposed;
        if (currentXposed == null) {
            return null;
        }
        try {
            return currentXposed.getRemotePreferences(TweakerPreferences.REMOTE_GROUP);
        } catch (Throwable t) {
            XposedBridge.log(TAG + ": failed to open remote preferences");
            XposedBridge.log(t);
            return null;
        }
    }
}
