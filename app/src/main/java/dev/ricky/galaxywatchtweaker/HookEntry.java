package dev.ricky.galaxywatchtweaker;

import android.content.Context;

import java.util.Map;
import java.util.Locale;
import java.util.Objects;

import dev.ricky.galaxywatchtweaker.hook.HookConfig;
import dev.ricky.galaxywatchtweaker.hook.SamsungHealthMonitorHook;
import dev.ricky.galaxywatchtweaker.core.CompanionIdentityPolicy;
import dev.ricky.galaxywatchtweaker.core.TargetPackages;
import dev.ricky.galaxywatchtweaker.settings.SpoofProfile;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class HookEntry implements IXposedHookLoadPackage {
    private static final String ANDROID_PACKAGE = "android";
    private static final String SYSTEM_SERVER_PROCESS = "system";
    private static final String GOOGLE_WEAR_PACKAGE = "com.google.android.wearable.app.cn";
    private static final String CAPABILITY_DATA_SETTER =
            "com.samsung.android.basicdata.capability.CapabilityDataSetter";
    private static final String TAG_PREFIX = "GalaxyWatchTweaker: ";
    private static volatile boolean companionIdentityHookInstalled;
    private static final java.util.Set<Integer> googleWearGetArities =
            java.util.concurrent.ConcurrentHashMap.newKeySet();

    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) {
        HookConfig config = new HookConfig();
        handleLoadPackage(lpparam, config);
    }

    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam, HookConfig config) {
        if (lpparam == null || config == null) {
            return;
        }
        if (ANDROID_PACKAGE.equals(lpparam.packageName)
                && SYSTEM_SERVER_PROCESS.equals(lpparam.processName)) {
            if (config.isConnectionRecoveryEnabled()) {
                CompanionPresenceRecoveryHook.install(lpparam.classLoader);
            }
            return;
        }

        if (SamsungHealthMonitorHook.SHM_PACKAGE.equals(lpparam.packageName)) {
            SamsungHealthMonitorHook.handleLoadPackage(lpparam, config);
            return;
        }

        if (!TargetPackages.isTargetPackage(lpparam.packageName)) {
            if (GOOGLE_WEAR_PACKAGE.equals(lpparam.packageName)) {
                if (config.isCompanionIdentityEnabled()) {
                    hookGoogleWearCompanionIdentity(lpparam.classLoader, config.companionSpoofProfile());
                }
            }
            return;
        }

        log("loaded for " + lpparam.packageName);
        if (TargetPackages.WATCH7_PLUGIN_PACKAGE.equals(lpparam.packageName)) {
            SpoofProfile profile = config.companionSpoofProfile();
            if (config.isCompanionIdentityEnabled()) {
                hookCompanionIdentityWrites(lpparam.classLoader, profile);
                Watch7CompanionIdentityHook.install(lpparam.classLoader, profile);
            }
            if (config.isCapabilityExchangeEnabled()) {
                Watch7CapabilityExchangeHook.install(lpparam.classLoader, profile);
                Watch7CapabilityRefreshHook.install(lpparam.classLoader);
            }
            if (config.isWatchAliasOverrideEnabled()) {
                WatchDisplayNameHook.install(lpparam.classLoader, config.watchAliasName());
            }
            return;
        }

        log("Samsung process hook is passive for " + lpparam.packageName);
    }

    private static synchronized void hookGoogleWearCompanionIdentity(ClassLoader classLoader, SpoofProfile profile) {
        if (googleWearGetArities.size() == 2) return;
        spoofBuildIdentity(profile);
        for (int arity = 1; arity <= 2; arity++) {
            if (googleWearGetArities.contains(arity)) continue;
            XC_MethodHook callback = new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.hasThrowable()) return;
                    String key = (String) param.args[0];
                    String original = (String) param.getResult();
                    String spoofed = CompanionIdentityPolicy.systemPropertyValueFor(key, original, profile);
                    if (!Objects.equals(spoofed, original)) param.setResult(spoofed);
                }
            };
            try {
                Object[] signature = arity == 1
                        ? new Object[]{String.class, callback}
                        : new Object[]{String.class, String.class, callback};
                XposedHelpers.findAndHookMethod("android.os.SystemProperties", classLoader, "get", signature);
                googleWearGetArities.add(arity);
            } catch (Throwable t) {
                log("Google Wear SystemProperties#get overload unavailable: " + t);
            }
        }
    }

    private static void spoofBuildIdentity(SpoofProfile profile) {
        for (Map.Entry<String, String> buildField : profile.buildFields().entrySet()) {
            String value = buildField.getValue();
            if ("MANUFACTURER".equals(buildField.getKey())) {
                value = value.toUpperCase(Locale.ROOT);
            } else if ("MODEL".equals(buildField.getKey())) {
                value = CompanionIdentityPolicy.toWatchCompatibleModel(value);
            }
            setBuildField(buildField.getKey(), value);
        }
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
            companionIdentityHookInstalled = true;
            log("hooked " + CAPABILITY_DATA_SETTER + "#setPreference(String)");
        } catch (Throwable t) {
            log("CapabilityDataSetter#setPreference hook unavailable: " + t);
        }
    }

    private static void log(String message) {
        XposedBridge.log(TAG_PREFIX + message);
    }
}
