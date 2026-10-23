package dev.ricky.galaxywatchtweaker;

import android.os.Handler;
import android.os.Looper;

import java.util.Map;

import dev.ricky.galaxywatchtweaker.core.CapabilityRefreshCoordinator;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public final class Watch7CapabilityRefreshHook {
    private static final String CAPABILITY_COMPANION =
            "com.samsung.android.companionservice.service.CapabilityCompanion";
    private static final String WATCH =
            "com.google.android.libraries.wear.companion.watch.Watch";
    private static final String CONNECTION_STATE =
            "com.google.android.libraries.wear.companion.watch.ConnectionState";
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final CapabilityRefreshCoordinator refresh = new CapabilityRefreshCoordinator(
            new CapabilityRefreshCoordinator.Scheduler() {
                public void postDelayed(Runnable task, long delayMs) { handler.postDelayed(task, delayMs); }
                public void remove(Runnable task) { handler.removeCallbacks(task); }
            }, Watch7CapabilityRefreshHook::requestCapabilityExchange);
    private static boolean connectionHookInstalled;
    private static boolean initHookInstalled;

    private Watch7CapabilityRefreshHook() {
    }

    public static synchronized void install(ClassLoader classLoader) {
        if (!connectionHookInstalled) {
            try {
                XposedHelpers.findAndHookMethod(CAPABILITY_COMPANION, classLoader,
                        "onConnectionStateChanged", XposedHelpers.findClass(WATCH, classLoader),
                        XposedHelpers.findClass(CONNECTION_STATE, classLoader), new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                if (param.hasThrowable()) return;
                                try {
                                    boolean connected = isConnectedState(param.args[1]);
                                    String address = addressFor(param.thisObject, param.args[0], connected);
                                    handler.post(() -> {
                                        if (address != null) {
                                            refresh.onConnectionChanged(param.thisObject, address, connected);
                                        } else if (!connected) {
                                            // Unknown watch: cancel rather than send to a stale last-connected address.
                                            refresh.cancel(param.thisObject);
                                        }
                                    });
                                } catch (Throwable t) {
                                    handler.post(() -> refresh.cancel(param.thisObject));
                                    log("connection refresh skipped: " + t);
                                }
                            }
                        });
                connectionHookInstalled = true;
            } catch (Throwable t) {
                log("connection refresh hook unavailable: " + t);
            }
        }
        if (!initHookInstalled) {
            try {
                XposedHelpers.findAndHookMethod(CAPABILITY_COMPANION, classLoader, "init",
                        XposedHelpers.findClass("androidx.lifecycle.LifecycleOwner", classLoader),
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                if (!param.hasThrowable()) {
                                    handler.post(() -> scheduleConnectedWatchRefreshes(param.thisObject));
                                }
                            }
                        });
                initHookInstalled = true;
            } catch (Throwable t) {
                log("init refresh hook unavailable: " + t);
            }
        }
    }

    private static String addressFor(Object companion, Object watch, boolean connected) {
        Object paired = XposedHelpers.callMethod(companion, "getPairedWatch");
        if (!(paired instanceof Map)) return null;
        Map<?, ?> watches = (Map<?, ?>) paired;
        for (Map.Entry<?, ?> entry : watches.entrySet()) {
            if (watch != null && entry.getValue() == watch && entry.getKey() instanceof String) {
                return (String) entry.getKey();
            }
        }
        // Older plugins expose only mLastestConnectedBtAddr. Use it only on a connected event
        // with exactly one paired watch; never use it to attribute another watch's disconnect.
        if (connected && watches.size() == 1) {
            Object latest = XposedHelpers.getObjectField(companion, "mLastestConnectedBtAddr");
            if (latest instanceof String && watches.containsKey(latest)) return (String) latest;
        }
        return null;
    }

    private static void scheduleConnectedWatchRefreshes(Object companion) {
        try {
            Object paired = XposedHelpers.callMethod(companion, "getPairedWatch");
            if (!(paired instanceof Map)) return;
            for (Map.Entry<?, ?> entry : ((Map<?, ?>) paired).entrySet()) {
                try {
                    if (entry.getKey() instanceof String && entry.getValue() != null
                            && isConnectedState(XposedHelpers.callMethod(entry.getValue(), "getConnectionState"))) {
                        refresh.onConnectionChanged(companion, (String) entry.getKey(), true);
                    }
                } catch (Throwable t) {
                    // Unsupported watch API: wait for the real connection-state callback.
                    log("initial watch state unavailable; waiting for connection callback");
                }
            }
        } catch (Throwable t) {
            log("init refresh skipped: " + t);
        }
    }

    private static boolean isConnectedState(Object state) {
        return state != null && Boolean.TRUE.equals(XposedHelpers.callMethod(state, "isConnected"));
    }

    private static void requestCapabilityExchange(Object companion, String address) {
        try {
            XposedHelpers.callMethod(companion, "requestCapabilityExchange", address);
            log("requested WCS capability refresh for connected watch");
        } catch (Throwable t) {
            log("WCS capability refresh failed: " + t);
        }
    }

    private static void log(String message) {
        XposedBridge.log("GalaxyWatchTweaker: " + message);
    }
}
