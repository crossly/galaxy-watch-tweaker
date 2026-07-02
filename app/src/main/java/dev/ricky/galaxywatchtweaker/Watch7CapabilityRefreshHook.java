package dev.ricky.galaxywatchtweaker;

import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class Watch7CapabilityRefreshHook {
    private static final String TAG_PREFIX = "GalaxyWatchPluginHook: ";
    private static final String CAPABILITY_COMPANION =
            "com.samsung.android.companionservice.service.CapabilityCompanion";
    private static final String WATCH =
            "com.google.android.libraries.wear.companion.watch.Watch";
    private static final String CONNECTION_STATE =
            "com.google.android.libraries.wear.companion.watch.ConnectionState";
    private static final long FIRST_REFRESH_DELAY_MS = 15_000L;
    private static final long SECOND_REFRESH_DELAY_MS = 75_000L;
    private static final Set<String> scheduledBtAddresses =
            Collections.synchronizedSet(new HashSet<>());
    private static volatile boolean installed;

    private Watch7CapabilityRefreshHook() {
    }

    public static void install(ClassLoader classLoader) {
        if (installed) {
            return;
        }
        installed = true;

        boolean hooked = false;
        try {
            hookCapabilityInit(classLoader);
            hooked = true;
        } catch (Throwable t) {
            log("Watch7 capability init refresh hook unavailable: " + t);
        }

        try {
            hookConnectionState(classLoader);
            hooked = true;
        } catch (Throwable t) {
            log("Watch7 capability connection refresh hook unavailable: " + t);
        }

        if (hooked) {
            log("hooked Watch7 capability refresh");
        }
    }

    private static void hookCapabilityInit(ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod(
                CAPABILITY_COMPANION,
                classLoader,
                "init",
                XposedHelpers.findClass("androidx.lifecycle.LifecycleOwner", classLoader),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        scheduleKnownWatchRefreshes(param.thisObject);
                    }
                });
    }

    private static void hookConnectionState(ClassLoader classLoader) {
        XposedHelpers.findAndHookMethod(
                CAPABILITY_COMPANION,
                classLoader,
                "onConnectionStateChanged",
                XposedHelpers.findClass(WATCH, classLoader),
                XposedHelpers.findClass(CONNECTION_STATE, classLoader),
                new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) {
                        if (!isConnectedState(param.args[1])) {
                            return;
                        }

                        Object btAddr = XposedHelpers.getObjectField(
                                param.thisObject,
                                "mLastestConnectedBtAddr");
                        scheduleRefresh(param.thisObject, String.valueOf(btAddr));
                    }
                });
    }

    private static void scheduleKnownWatchRefreshes(Object capabilityCompanion) {
        try {
            Object pairedWatch = XposedHelpers.callMethod(capabilityCompanion, "getPairedWatch");
            if (!(pairedWatch instanceof Map)) {
                log("skip WCS refresh: paired watch map unavailable");
                return;
            }

            for (Object btAddr : ((Map<?, ?>) pairedWatch).keySet()) {
                scheduleRefresh(capabilityCompanion, String.valueOf(btAddr));
            }
        } catch (Throwable t) {
            log("skip WCS refresh from init: " + t);
        }
    }

    private static boolean isConnectedState(Object connectionState) {
        try {
            return Boolean.TRUE.equals(XposedHelpers.callMethod(connectionState, "isConnected"));
        } catch (Throwable t) {
            log("skip WCS refresh: connection state unavailable: " + t);
            return false;
        }
    }

    private static void scheduleRefresh(Object capabilityCompanion, String btAddr) {
        if (capabilityCompanion == null || btAddr == null || btAddr.trim().isEmpty()) {
            log("skip WCS refresh: btAddr unavailable");
            return;
        }

        String normalizedBtAddr = btAddr.trim().toUpperCase();
        if (!scheduledBtAddresses.add(normalizedBtAddr)) {
            log("skip WCS refresh: already scheduled for " + normalizedBtAddr);
            return;
        }

        Thread worker = new Thread(() -> {
            sleep(FIRST_REFRESH_DELAY_MS);
            requestCapabilityExchange(capabilityCompanion, normalizedBtAddr, "first");
            sleep(SECOND_REFRESH_DELAY_MS - FIRST_REFRESH_DELAY_MS);
            requestCapabilityExchange(capabilityCompanion, normalizedBtAddr, "retry");
        }, "GalaxyWatchPluginHook-WCS");
        worker.setDaemon(true);
        worker.start();
        log("scheduled WCS capability refresh for " + normalizedBtAddr);
    }

    private static void requestCapabilityExchange(
            Object capabilityCompanion,
            String btAddr,
            String attempt) {
        try {
            XposedHelpers.callMethod(capabilityCompanion, "requestCapabilityExchange", btAddr);
            log("requested WCS capability refresh " + attempt + " for " + btAddr);
        } catch (Throwable t) {
            log("WCS capability refresh " + attempt + " failed for " + btAddr + ": " + t);
        }
    }

    private static void sleep(long delayMs) {
        try {
            Thread.sleep(delayMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void log(String message) {
        XposedBridge.log(TAG_PREFIX + message);
    }
}
