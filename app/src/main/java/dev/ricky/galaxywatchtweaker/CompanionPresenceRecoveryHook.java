package dev.ricky.galaxywatchtweaker;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import dev.ricky.galaxywatchtweaker.core.BluetoothReconnectController;
import dev.ricky.galaxywatchtweaker.core.TargetPackages;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class CompanionPresenceRecoveryHook {
    private static final String CDM_SERVICE =
            "com.android.server.companion.CompanionDeviceManagerService";
    private static final String DEVICE_PRESENCE_PROCESSOR =
            "com.android.server.companion.devicepresence.DevicePresenceProcessor";
    private static final Handler handler = new Handler(Looper.getMainLooper());
    private static final BluetoothReconnectController reconnect = new BluetoothReconnectController();
    private static final Map<Integer, Runnable> pendingBleRecovery = new HashMap<>();
    private static boolean serviceHookInstalled;
    private static boolean bleHookInstalled;
    private static boolean bleLostHookInstalled;
    private static boolean bootScheduled;

    private CompanionPresenceRecoveryHook() {
    }

    static synchronized void install(ClassLoader classLoader) {
        if (!serviceHookInstalled) {
            try {
                XposedHelpers.findAndHookMethod(CDM_SERVICE, classLoader, "onStart",
                        new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                if (!param.hasThrowable()) {
                                    handler.post(() -> scheduleBootRecovery(param.thisObject));
                                }
                            }
                        });
                serviceHookInstalled = true;
            } catch (Throwable t) {
                log("CDM service hook unavailable: " + t);
            }
        }
        if (!bleHookInstalled) {
            try {
                XposedHelpers.findAndHookMethod(DEVICE_PRESENCE_PROCESSOR, classLoader,
                        "onBleCompanionDeviceFound", int.class, int.class, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                if (param.hasThrowable()) return;
                                int id = (Integer) param.args[0];
                                int userId = (Integer) param.args[1];
                                handler.post(() -> scheduleBleFoundRecovery(param.thisObject, id, userId));
                            }
                        });
                bleHookInstalled = true;
            } catch (Throwable t) {
                log("BLE presence hook unavailable: " + t);
            }
        }
        if (!bleLostHookInstalled) {
            try {
                XposedHelpers.findAndHookMethod(DEVICE_PRESENCE_PROCESSOR, classLoader,
                        "onBleCompanionDeviceLost", int.class, int.class, new XC_MethodHook() {
                            @Override
                            protected void afterHookedMethod(MethodHookParam param) {
                                int id = (Integer) param.args[0];
                                handler.post(() -> {
                                    Runnable pending = pendingBleRecovery.remove(id);
                                    if (pending != null) handler.removeCallbacks(pending);
                                });
                            }
                        });
                bleLostHookInstalled = true;
            } catch (Throwable t) {
                log("BLE disappearance hook unavailable: " + t);
            }
        }
    }

    private static void scheduleBootRecovery(Object service) {
        if (service == null || bootScheduled) return;
        bootScheduled = true;
        handler.postDelayed(() -> recoverSamsungWatchAssociations(service), 45_000L);
        handler.postDelayed(() -> recoverSamsungWatchAssociations(service), 165_000L);
    }

    private static void scheduleBleFoundRecovery(Object processor, int id, int userId) {
        if (processor == null || pendingBleRecovery.containsKey(id)) return;
        Runnable task = () -> {
            pendingBleRecovery.remove(id);
            requestBluetoothConnection(processor, id, userId);
        };
        pendingBleRecovery.put(id, task);
        handler.postDelayed(task, 500L);
    }

    private static void requestBluetoothConnection(Object processor, int id, int userId) {
        try {
            Object store = XposedHelpers.getObjectField(processor, "mAssociationStore");
            // Revalidate after the delay: a user may have removed the association meanwhile.
            Object association = findActiveAssociation(store, id, userId);
            if (association == null) return;
            Object mac = XposedHelpers.callMethod(association, "getDeviceMacAddress");
            if (mac == null) return;
            Object context = XposedHelpers.getObjectField(processor, "mContext");
            Object manager = XposedHelpers.callMethod(context, "getSystemService", "bluetooth");
            if (manager == null) return;
            Object adapter = XposedHelpers.callMethod(manager, "getAdapter");
            if (adapter == null) return;
            Object device = XposedHelpers.callMethod(adapter, "getRemoteDevice", mac.toString());
            if (device == null) return;
            boolean accepted = reconnect.reconnect(id, new BluetoothReconnectController.Device() {
                public boolean isEnabled() {
                    return Boolean.TRUE.equals(XposedHelpers.callMethod(adapter, "isEnabled"));
                }
                public boolean isBonded() {
                    // BOND_BONDED = 12. Never initiate pairing for an unbonded device.
                    return Integer.valueOf(12).equals(XposedHelpers.callMethod(device, "getBondState"));
                }
                public boolean isConnected() {
                    return Boolean.TRUE.equals(XposedHelpers.callMethod(device, "isConnected"));
                }
                public boolean connect() {
                    return Boolean.TRUE.equals(XposedHelpers.callMethod(device, "connect"));
                }
            }, SystemClock.elapsedRealtime());
            if (accepted) {
                log("requested bonded watch reconnect, associationId=" + id
                        + "; waiting for Android transport events");
            }
            // Do not synthesize a CDM event, even if connect() accepted the asynchronous request.
        } catch (Throwable t) {
            log("watch reconnect skipped: " + t);
        }
    }

    private static Object findActiveAssociation(Object store, int id, int userId) {
        Object active = XposedHelpers.callMethod(store, "getActiveAssociationsByPackage",
                userId, TargetPackages.WATCH7_PLUGIN_PACKAGE);
        if (!(active instanceof List)) return null;
        for (Object association : (List<?>) active) {
            if (Integer.valueOf(id).equals(XposedHelpers.callMethod(association, "getId"))
                    && Integer.valueOf(userId).equals(XposedHelpers.callMethod(association, "getUserId"))
                    && TargetPackages.WATCH7_PLUGIN_PACKAGE.equals(
                            XposedHelpers.callMethod(association, "getPackageName"))) {
                return association;
            }
        }
        return null;
    }

    private static void recoverSamsungWatchAssociations(Object service) {
        try {
            Object store = XposedHelpers.getObjectField(service, "mAssociationStore");
            Object processor = XposedHelpers.getObjectField(service, "mDevicePresenceProcessor");
            if (store == null || processor == null) return;
            Object active = XposedHelpers.callMethod(store, "getActiveAssociationsByPackage",
                    0, TargetPackages.WATCH7_PLUGIN_PACKAGE);
            if (!(active instanceof List)) return;
            for (Object association : (List<?>) active) {
                if (!TargetPackages.WATCH7_PLUGIN_PACKAGE.equals(
                        XposedHelpers.callMethod(association, "getPackageName"))) continue;
                try {
                    int id = (Integer) XposedHelpers.callMethod(association, "getId");
                    int userId = (Integer) XposedHelpers.callMethod(association, "getUserId");
                    ensureObservedWhenPresent(store, association);
                    requestBluetoothConnection(processor, id, userId);
                } catch (Throwable t) {
                    log("association recovery skipped: " + t);
                }
            }
        } catch (Throwable t) {
            log("framework recovery unavailable: " + t);
        }
    }

    private static void ensureObservedWhenPresent(Object store, Object association) {
        try {
            if (Boolean.TRUE.equals(XposedHelpers.callMethod(association, "isNotifyOnDeviceNearby"))) return;
            Class<?> info = Class.forName("android.companion.AssociationInfo");
            Class<?> builderClass = Class.forName("android.companion.AssociationInfo$Builder");
            Object builder = builderClass.getConstructor(info).newInstance(association);
            XposedHelpers.callMethod(builder, "setNotifyOnDeviceNearby", true);
            XposedHelpers.callMethod(store, "updateAssociation", XposedHelpers.callMethod(builder, "build"));
        } catch (Throwable t) {
            log("unable to enable companion presence observation: " + t);
        }
    }

    private static void log(String message) {
        XposedBridge.log("GalaxyWatchTweaker: " + message);
    }
}
