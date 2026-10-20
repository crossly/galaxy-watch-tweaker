package dev.ricky.galaxywatchtweaker;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import dev.ricky.galaxywatchtweaker.core.TargetPackages;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class CompanionPresenceRecoveryHook {
    private static final String CDM_SERVICE =
            "com.android.server.companion.CompanionDeviceManagerService";
    private static final String DEVICE_PRESENCE_PROCESSOR =
            "com.android.server.companion.devicepresence.DevicePresenceProcessor";
    private static final String TAG_PREFIX = "GalaxyWatchTweaker: ";
    private static final long BOOT_RECOVERY_DELAY_MS = 45_000L;
    private static final long RETRY_RECOVERY_DELAY_MS = 120_000L;
    private static final long BLE_FOUND_RECOVERY_DELAY_MS = 500L;
    private static final long BLE_RECOVERY_COOLDOWN_MS = 30_000L;
    private static final long BLUETOOTH_CONNECT_COOLDOWN_MS = 60_000L;
    private static volatile boolean installed;
    private static final Set<Integer> pendingBleRecovery = ConcurrentHashMap.newKeySet();
    private static final java.util.Map<Integer, Long> lastBleRecovery =
            new ConcurrentHashMap<>();
    private static final java.util.Map<Integer, Long> lastBluetoothConnectRequest =
            new ConcurrentHashMap<>();

    private CompanionPresenceRecoveryHook() {
    }

    static void install(ClassLoader classLoader) {
        if (installed) {
            return;
        }

        boolean anyHookInstalled = false;
        try {
            XposedHelpers.findAndHookMethod(
                    CDM_SERVICE,
                    classLoader,
                    "onStart",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            scheduleCompanionPresenceRecovery(param.thisObject);
                        }
                    });
            anyHookInstalled = true;
            log("hooked " + CDM_SERVICE + "#onStart()");
        } catch (Throwable t) {
            log("CompanionDeviceManagerService#onStart hook unavailable: " + t);
        }

        try {
            XposedHelpers.findAndHookMethod(
                    DEVICE_PRESENCE_PROCESSOR,
                    classLoader,
                    "onBleCompanionDeviceFound",
                    int.class,
                    int.class,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            scheduleBleFoundRecovery(
                                    param.thisObject,
                                    (Integer) param.args[0],
                                    (Integer) param.args[1]);
                        }
                    });
            anyHookInstalled = true;
            log("hooked " + DEVICE_PRESENCE_PROCESSOR + "#onBleCompanionDeviceFound(int,int)");
        } catch (Throwable t) {
            log("DevicePresenceProcessor#onBleCompanionDeviceFound hook unavailable: " + t);
        }

        installed = anyHookInstalled;
    }

    private static void scheduleCompanionPresenceRecovery(Object companionDeviceManagerService) {
        if (companionDeviceManagerService == null) {
            log("skip framework recovery: service is null");
            return;
        }

        Thread worker = new Thread(() -> {
            HookUtils.sleep(BOOT_RECOVERY_DELAY_MS);
            recoverSamsungWatchAssociations(companionDeviceManagerService);
            HookUtils.sleep(RETRY_RECOVERY_DELAY_MS);
            recoverSamsungWatchAssociations(companionDeviceManagerService);
        }, "GalaxyWatchTweaker-CDM");
        worker.setDaemon(true);
        worker.start();
        log("scheduled companion presence recovery");
    }

    private static void scheduleBleFoundRecovery(
            Object presenceProcessor, int associationId, int userId) {
        if (presenceProcessor == null) {
            return;
        }
        try {
            if (!isSamsungWatchAssociation(presenceProcessor, associationId)) {
                return;
            }
            long now = System.currentTimeMillis();
            Long last = lastBleRecovery.get(associationId);
            if (last != null && now - last < BLE_RECOVERY_COOLDOWN_MS) {
                return;
            }
            if (!pendingBleRecovery.add(associationId)) {
                return;
            }
            lastBleRecovery.put(associationId, now);

            Thread worker = new Thread(() -> {
                try {
                    HookUtils.sleep(BLE_FOUND_RECOVERY_DELAY_MS);
                    requestBluetoothConnection(presenceProcessor, associationId);
                    XposedHelpers.callMethod(
                            presenceProcessor,
                            "onBluetoothCompanionDeviceConnected",
                            associationId,
                            userId);
                    log("watch reappeared: re-requested companion BT connected event, associationId="
                            + associationId
                            + ", userId="
                            + userId);
                } catch (Throwable t) {
                    log("BLE reappear recovery failed: " + t);
                } finally {
                    pendingBleRecovery.remove(associationId);
                }
            }, "GalaxyWatchTweaker-CDM-BLE");
            worker.setDaemon(true);
            worker.start();
        } catch (Throwable t) {
            pendingBleRecovery.remove(associationId);
            log("BLE reappear recovery scheduling failed: " + t);
        }
    }

    private static void requestBluetoothConnection(Object presenceProcessor, int associationId) {
        try {
            long now = System.currentTimeMillis();
            Long last = lastBluetoothConnectRequest.get(associationId);
            if (last != null && now - last < BLUETOOTH_CONNECT_COOLDOWN_MS) {
                return;
            }
            lastBluetoothConnectRequest.put(associationId, now);

            Object associationStore = XposedHelpers.getObjectField(
                    presenceProcessor, "mAssociationStore");
            if (associationStore == null) {
                return;
            }
            Object association = XposedHelpers.callMethod(
                    associationStore, "getAssociationById", associationId);
            if (association == null) {
                return;
            }
            Object macObject = XposedHelpers.callMethod(association, "getDeviceMacAddress");
            if (macObject == null) {
                return;
            }
            String macAddress = macObject.toString();

            Object context = XposedHelpers.getObjectField(presenceProcessor, "mContext");
            if (context == null) {
                return;
            }
            Object bluetoothManager = XposedHelpers.callMethod(
                    context, "getSystemService", "bluetooth");
            if (bluetoothManager == null) {
                return;
            }
            Object adapter = XposedHelpers.callMethod(bluetoothManager, "getAdapter");
            if (adapter == null) {
                return;
            }
            if (!Boolean.TRUE.equals(XposedHelpers.callMethod(adapter, "isEnabled"))) {
                return;
            }
            Object device = XposedHelpers.callMethod(adapter, "getRemoteDevice", macAddress);
            if (device == null) {
                return;
            }
            if (Boolean.TRUE.equals(XposedHelpers.callMethod(device, "isConnected"))) {
                return;
            }
            Object result = XposedHelpers.callMethod(device, "connect");
            log("requested real BT reconnect for " + macAddress + " result=" + result);
        } catch (Throwable t) {
            log("real BT reconnect request failed: " + t);
        }
    }

    private static boolean isSamsungWatchAssociation(Object presenceProcessor, int associationId) {
        try {
            Object associationStore = XposedHelpers.getObjectField(
                    presenceProcessor, "mAssociationStore");
            if (associationStore == null) {
                return false;
            }
            Object association = XposedHelpers.callMethod(
                    associationStore, "getAssociationById", associationId);
            if (association == null) {
                return false;
            }
            String packageName = String.valueOf(
                    XposedHelpers.callMethod(association, "getPackageName"));
            return TargetPackages.WATCH7_PLUGIN_PACKAGE.equals(packageName);
        } catch (Throwable t) {
            return false;
        }
    }

    private static void recoverSamsungWatchAssociations(Object service) {
        try {
            Object associationStore = XposedHelpers.getObjectField(service, "mAssociationStore");
            Object devicePresenceProcessor =
                    XposedHelpers.getObjectField(service, "mDevicePresenceProcessor");
            if (associationStore == null || devicePresenceProcessor == null) {
                log("skip framework recovery: CDM internals unavailable");
                return;
            }

            Object associationsObject = XposedHelpers.callMethod(
                    associationStore,
                    "getActiveAssociationsByPackage",
                    0,
                    TargetPackages.WATCH7_PLUGIN_PACKAGE);
            if (!(associationsObject instanceof List)) {
                log("skip framework recovery: association list unavailable");
                return;
            }

            Method connectedMethod = devicePresenceProcessor.getClass().getMethod(
                    "onBluetoothCompanionDeviceConnected",
                    int.class,
                    int.class);
            for (Object association : (List<?>) associationsObject) {
                String packageName = String.valueOf(
                        XposedHelpers.callMethod(association, "getPackageName"));
                if (!TargetPackages.WATCH7_PLUGIN_PACKAGE.equals(packageName)) {
                    continue;
                }

                int associationId = ((Integer) XposedHelpers.callMethod(association, "getId"));
                int userId = ((Integer) XposedHelpers.callMethod(association, "getUserId"));
                ensureObservedWhenPresent(associationStore, association);
                requestBluetoothConnection(devicePresenceProcessor, associationId);
                connectedMethod.invoke(devicePresenceProcessor, associationId, userId);
                log("requested companion BT connected event, associationId="
                        + associationId
                        + ", userId="
                        + userId);
            }
        } catch (Throwable t) {
            log("framework recovery failed: " + t);
        }
    }

    private static void ensureObservedWhenPresent(Object associationStore, Object association) {
        try {
            Boolean isObserved = (Boolean) XposedHelpers.callMethod(
                    association,
                    "isNotifyOnDeviceNearby");
            if (Boolean.TRUE.equals(isObserved)) {
                return;
            }

            Class<?> associationInfoClass = Class.forName("android.companion.AssociationInfo");
            Class<?> builderClass = Class.forName("android.companion.AssociationInfo$Builder");
            Object builder = builderClass.getConstructor(associationInfoClass).newInstance(association);
            XposedHelpers.callMethod(builder, "setNotifyOnDeviceNearby", true);
            Object updatedAssociation = XposedHelpers.callMethod(builder, "build");
            XposedHelpers.callMethod(associationStore, "updateAssociation", updatedAssociation);
            log("enabled companion presence observation for Watch7 association");
        } catch (Throwable t) {
            log("unable to enable companion presence observation: " + t);
        }
    }

    private static void log(String message) {
        XposedBridge.log(TAG_PREFIX + message);
    }
}
