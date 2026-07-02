package dev.ricky.galaxywatchtweaker.hook;

import android.content.res.Configuration;
import android.os.Build;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import dev.ricky.galaxywatchtweaker.settings.SpoofProfile;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public final class SamsungHealthMonitorHook {
    private static final String TAG = "GalaxyWatchTweaker/SHM: ";
    private static final String SHM_PACKAGE = "com.samsung.android.shealthmonitor";

    private SamsungHealthMonitorHook() {
    }

    public static void handleLoadPackage(
            XC_LoadPackage.LoadPackageParam lpparam,
            HookConfig config) {
        if (lpparam == null
                || config == null
                || !SHM_PACKAGE.equals(lpparam.packageName)
                || !config.isShmPatchEnabled()) {
            return;
        }

        SpoofProfile profile = config.spoofProfile();
        spoofBuildFields(profile);
        Locale.setDefault(Locale.US);
        hookSystemProperties(lpparam.classLoader, profile);
        hookTelephonyCountry(lpparam.classLoader, profile);
        hookResourcesConfiguration(lpparam.classLoader, profile);
        hookSamsungHealthMonitorCrashSite(lpparam.classLoader, profile);
        DeviceInfoSpoofHook.hook(profile);
        log("patched " + lpparam.packageName + " process=" + lpparam.processName
                + " profile=" + profile.model() + "/" + profile.salesCode());
    }

    private static void hookSamsungHealthMonitorCrashSite(ClassLoader classLoader, SpoofProfile profile) {
        try {
            XposedHelpers.findAndHookMethod(
                    "com.samsung.android.shealthmonitor.util.CSCUtil",
                    classLoader,
                    "getSalesCode",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setResult(profile.salesCode());
                        }
                    });
            log("hooked CSCUtil#getSalesCode");
        } catch (Throwable t) {
            log("CSCUtil#getSalesCode unavailable: " + t);
        }
        try {
            XposedHelpers.findAndHookMethod(
                    "com.samsung.android.shealthmonitor.util.CSCUtil",
                    classLoader,
                    "getCountryISOCode",
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setResult(profile.countryIso());
                        }
                    });
            log("hooked CSCUtil#getCountryISOCode");
        } catch (Throwable t) {
            log("CSCUtil#getCountryISOCode unavailable: " + t);
        }
    }

    private static void hookSystemProperties(ClassLoader classLoader, SpoofProfile profile) {
        hookSystemPropertyGet(classLoader, profile, "get", String.class);
        hookSystemPropertyGet(classLoader, profile, "get", String.class, String.class);
    }

    private static void hookSystemPropertyGet(
            ClassLoader classLoader,
            SpoofProfile profile,
            String methodName,
            Object... parameterTypes) {
        try {
            Object[] signature = new Object[parameterTypes.length + 1];
            System.arraycopy(parameterTypes, 0, signature, 0, parameterTypes.length);
            signature[signature.length - 1] = new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) {
                    if (param.args == null || param.args.length == 0) {
                        return;
                    }
                    String value = samsungProperties(profile).get(String.valueOf(param.args[0]));
                    if (value != null) {
                        param.setResult(value);
                    }
                }
            };
            XposedHelpers.findAndHookMethod(
                    "android.os.SystemProperties",
                    classLoader,
                    methodName,
                    signature);
            log("hooked android.os.SystemProperties#" + methodName);
        } catch (Throwable t) {
            log("SystemProperties#" + methodName + " unavailable: " + t);
        }
    }

    private static void hookTelephonyCountry(ClassLoader classLoader, SpoofProfile profile) {
        hookNoArgStringMethod(classLoader, "android.telephony.TelephonyManager",
                "getNetworkCountryIso", profile.countryIso().toLowerCase(Locale.US));
        hookNoArgStringMethod(classLoader, "android.telephony.TelephonyManager",
                "getSimCountryIso", profile.countryIso().toLowerCase(Locale.US));
        hookNoArgStringMethod(classLoader, "android.telephony.TelephonyManager",
                "getNetworkOperator", profile.operator());
        hookNoArgStringMethod(classLoader, "android.telephony.TelephonyManager",
                "getSimOperator", profile.operator());
        hookNoArgStringMethod(classLoader, "android.telephony.TelephonyManager",
                "getNetworkOperatorName", profile.operatorName());
        hookNoArgStringMethod(classLoader, "android.telephony.TelephonyManager",
                "getSimOperatorName", profile.operatorName());
    }

    private static void hookNoArgStringMethod(
            ClassLoader classLoader,
            String className,
            String methodName,
            String result) {
        try {
            XposedHelpers.findAndHookMethod(
                    className,
                    classLoader,
                    methodName,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            param.setResult(result);
                        }
                    });
            log("hooked " + className + "#" + methodName);
        } catch (Throwable t) {
            log(className + "#" + methodName + " unavailable: " + t);
        }
    }

    private static void hookResourcesConfiguration(ClassLoader classLoader, SpoofProfile profile) {
        try {
            XposedHelpers.findAndHookMethod(
                    "android.content.res.Resources",
                    classLoader,
                    "getConfiguration",
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) {
                            Object result = param.getResult();
                            if (result instanceof Configuration) {
                                Configuration configuration = (Configuration) result;
                                configuration.mcc = Integer.parseInt(profile.mcc());
                                configuration.mnc = Integer.parseInt(profile.mnc());
                                configuration.setLocale(Locale.US);
                            }
                        }
                    });
            log("hooked Resources#getConfiguration");
        } catch (Throwable t) {
            log("Resources#getConfiguration unavailable: " + t);
        }
    }

    private static void spoofBuildFields(SpoofProfile profile) {
        setStaticField(Build.class, "MANUFACTURER", profile.manufacturer());
        setStaticField(Build.class, "BRAND", profile.brand());
        setStaticField(Build.class, "MODEL", profile.model());
        setStaticField(Build.class, "DEVICE", profile.device());
        setStaticField(Build.class, "PRODUCT", profile.product());
        setStaticField(Build.class, "FINGERPRINT", profile.fingerprint());
    }

    private static void setStaticField(Class<?> type, String name, Object value) {
        try {
            Field field = type.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (Throwable t) {
            log("Build." + name + " spoof failed: " + t);
        }
    }

    private static Map<String, String> samsungProperties(SpoofProfile profile) {
        Map<String, String> values = new HashMap<>();
        values.put("ro.product.manufacturer", profile.manufacturer());
        values.put("ro.product.brand", profile.brand());
        values.put("ro.product.model", profile.model());
        values.put("ro.product.device", profile.device());
        values.put("ro.product.name", profile.product());
        values.put("ro.build.product", profile.device());
        values.put("ro.build.fingerprint", profile.fingerprint());
        values.put("ro.csc.sales_code", profile.salesCode());
        values.put("ro.csc.countryiso_code", profile.countryIso());
        values.put("persist.sys.selected_country_iso", profile.countryIso());
        values.put("ril.sales_code", profile.salesCode());
        values.put("ro.boot.sales_code", profile.salesCode());
        values.put("ro.boot.carrierid", profile.salesCode());
        return values;
    }

    private static void log(String message) {
        XposedBridge.log(TAG + message);
    }
}
