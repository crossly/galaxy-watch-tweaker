package dev.ricky.galaxywatchtweaker;

import android.content.Context;

import dev.ricky.galaxywatchtweaker.hook.HookConfig;
import dev.ricky.galaxywatchtweaker.hook.SamsungHealthMonitorHook;
import dev.ricky.galaxywatchtweaker.core.CompanionIdentityPolicy;
import dev.ricky.galaxywatchtweaker.core.WatchdogPolicy;
import dev.ricky.galaxywatchtweaker.settings.SpoofProfile;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class HookEntry implements IXposedHookLoadPackage {
    private static final String ANDROID_PACKAGE = "android";
    private static final String GOOGLE_WEAR_PACKAGE = "com.google.android.wearable.app.cn";
    private static final String SHM_PACKAGE = "com.samsung.android.shealthmonitor";
    private static final String CAPABILITY_DATA_SETTER =
            "com.samsung.android.basicdata.capability.CapabilityDataSetter";
    private static final String TAG_PREFIX = "GalaxyWatchPluginHook: ";
    private static volatile boolean companionIdentityHookInstalled;
    private static volatile boolean googleWearIdentityHookInstalled;

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        HookConfig config = new HookConfig();
        handleLoadPackage(lpparam, config);
    }

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam, HookConfig config) {
        if (lpparam == null || config == null) {
            return;
        }
        if (ANDROID_PACKAGE.equals(lpparam.packageName)) {
            if (config.isConnectionRecoveryEnabled()) {
                CompanionPresenceRecoveryHook.install(lpparam.classLoader);
            }
            return;
        }

        if (SHM_PACKAGE.equals(lpparam.packageName)) {
            SamsungHealthMonitorHook.handleLoadPackage(lpparam, config);
            return;
        }

        if (!WatchdogPolicy.isTargetPackage(lpparam.packageName)) {
            if (GOOGLE_WEAR_PACKAGE.equals(lpparam.packageName)) {
                if (config.isCompanionIdentityEnabled()) {
                    hookGoogleWearCompanionIdentity(lpparam.classLoader, config.spoofProfile());
                }
            }
            return;
        }

        log("loaded for " + lpparam.packageName);
        if (WatchdogPolicy.WATCH7_PLUGIN_PACKAGE.equals(lpparam.packageName)) {
            SpoofProfile profile = config.spoofProfile();
            if (config.isCompanionIdentityEnabled()) {
                hookCompanionIdentityWrites(lpparam.classLoader, profile);
                Watch7CompanionIdentityHook.install(lpparam.classLoader, profile);
            }
            if (config.isCapabilityExchangeEnabled()) {
                Watch7CapabilityExchangeHook.install(lpparam.classLoader, profile);
                Watch7CapabilityRefreshHook.install(lpparam.classLoader);
            }
            return;
        }

        log("Samsung process hook is passive for " + lpparam.packageName);
    }

    private static void hookGoogleWearCompanionIdentity(ClassLoader classLoader, SpoofProfile profile) {
        if (googleWearIdentityHookInstalled) {
            return;
        }
        googleWearIdentityHookInstalled = true;

        spoofBuildIdentity(profile);
        try {
            XposedHelpers.findAndHookMethod(
                    "android.os.SystemProperties",
                    classLoader,
                    "get",
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            String key = (String) param.args[0];
                            String originalValue = (String) param.getResult();
                            String spoofedValue = CompanionIdentityPolicy.systemPropertyValueFor(
                                    key,
                                    originalValue,
                                    profile);
                            if (!spoofedValue.equals(originalValue)) {
                                param.setResult(spoofedValue);
                                log("spoofed Google Wear system property " + key + "=" + spoofedValue);
                            }
                        }
                    });
            log("hooked Google Wear companion identity");
        } catch (Throwable t) {
            log("Google Wear companion identity hook unavailable: " + t);
        }
    }

    private static void spoofBuildIdentity(SpoofProfile profile) {
        setBuildField("MANUFACTURER", profile.manufacturer().toUpperCase());
        setBuildField("BRAND", profile.brand());
        setBuildField("MODEL", CompanionIdentityPolicy.toWatchCompatibleModel(profile.model()));
        setBuildField("DEVICE", profile.device());
        setBuildField("PRODUCT", profile.product());
    }

    private static void setBuildField(String fieldName, String value) {
        try {
            XposedHelpers.setStaticObjectField(android.os.Build.class, fieldName, value);
            log("spoofed Google Wear Build." + fieldName + "=" + value);
        } catch (Throwable t) {
            log("unable to spoof Google Wear Build." + fieldName + ": " + t);
        }
    }

    private static void hookCompanionIdentityWrites(ClassLoader classLoader, SpoofProfile profile) {
        if (companionIdentityHookInstalled) {
            return;
        }
        companionIdentityHookInstalled = true;

        try {
            XposedHelpers.findAndHookMethod(
                    CAPABILITY_DATA_SETTER,
                    classLoader,
                    "setPreference",
                    Context.class,
                    String.class,
                    String.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            String preferenceKey = String.valueOf(param.args[1]);
                            if (!CompanionIdentityPolicy.shouldSpoof(preferenceKey)) {
                                return;
                            }

                            String originalValue = (String) param.args[2];
                            String spoofedValue = CompanionIdentityPolicy.spoofedValueFor(
                                    preferenceKey,
                                    originalValue,
                                    profile);
                            param.args[2] = spoofedValue;
                            log("spoofed companion identity preference "
                                    + preferenceKey
                                    + "="
                                    + spoofedValue);
                        }
                    });
            log("hooked " + CAPABILITY_DATA_SETTER + "#setPreference(String)");
        } catch (Throwable t) {
            log("CapabilityDataSetter#setPreference hook unavailable: " + t);
        }
    }

    private static void log(String message) {
        XposedBridge.log(TAG_PREFIX + message);
    }
}
