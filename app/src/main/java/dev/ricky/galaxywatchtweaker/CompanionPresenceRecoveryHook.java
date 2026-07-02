package dev.ricky.galaxywatchtweaker;

import java.lang.reflect.Method;
import java.util.List;

import dev.ricky.galaxywatchtweaker.core.WatchdogPolicy;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class CompanionPresenceRecoveryHook {
    private static final String CDM_SERVICE =
            "com.android.server.companion.CompanionDeviceManagerService";
    private static final String TAG_PREFIX = "GalaxyWatchPluginHook: ";
    private static final long BOOT_RECOVERY_DELAY_MS = 45_000L;
    private static final long RETRY_RECOVERY_DELAY_MS = 120_000L;
    private static volatile boolean installed;

    private CompanionPresenceRecoveryHook() {
    }

    static void install(ClassLoader classLoader) {
        if (installed) {
            return;
        }
        installed = true;

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
            log("hooked " + CDM_SERVICE + "#onStart()");
        } catch (Throwable t) {
            log("CompanionDeviceManagerService#onStart hook unavailable: " + t);
        }
    }

    private static void scheduleCompanionPresenceRecovery(Object companionDeviceManagerService) {
        if (companionDeviceManagerService == null) {
            log("skip framework recovery: service is null");
            return;
        }

        Thread worker = new Thread(() -> {
            sleep(BOOT_RECOVERY_DELAY_MS);
            recoverSamsungWatchAssociations(companionDeviceManagerService);
            sleep(RETRY_RECOVERY_DELAY_MS);
            recoverSamsungWatchAssociations(companionDeviceManagerService);
        }, "GalaxyWatchPluginHook-CDM");
        worker.setDaemon(true);
        worker.start();
        log("scheduled companion presence recovery");
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
                    WatchdogPolicy.WATCH7_PLUGIN_PACKAGE);
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
                if (!WatchdogPolicy.WATCH7_PLUGIN_PACKAGE.equals(packageName)) {
                    continue;
                }

                int associationId = ((Integer) XposedHelpers.callMethod(association, "getId"));
                int userId = ((Integer) XposedHelpers.callMethod(association, "getUserId"));
                ensureObservedWhenPresent(associationStore, association);
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
