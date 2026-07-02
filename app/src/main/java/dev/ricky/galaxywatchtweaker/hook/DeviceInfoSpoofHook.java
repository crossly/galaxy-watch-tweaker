package dev.ricky.galaxywatchtweaker.hook;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Constructor;

import dev.ricky.galaxywatchtweaker.settings.SpoofProfile;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

final class DeviceInfoSpoofHook {
    private static final String TAG = "GalaxyWatchTweaker/DeviceInfo: ";
    private static volatile boolean installed;

    private DeviceInfoSpoofHook() {
    }

    static void hook(SpoofProfile profile) {
        if (installed) {
            return;
        }
        installed = true;
        hookJsonPut(profile);
        hookJsonStringConstructor(profile);
    }

    static Object spoofValue(String key, Object value, SpoofProfile profile) {
        if (key == null) {
            return value;
        }
        String stringValue = value != null ? String.valueOf(value) : "";
        switch (key) {
            case "device_name":
            case "model_number":
            case "model":
                if (isChineseGalaxyWatchUltra(stringValue)) {
                    return "SM-L705U";
                }
                if (!isHonorPhoneValue(stringValue)) {
                    return value;
                }
                return profile.model();
            case "vendor":
            case "manufacturer":
                return profile.manufacturer();
            case "csc":
            case "sales_code":
                return profile.salesCode();
            case "country":
            case "country_code":
            case "countryiso_code":
            case "sim_iso_country":
                return profile.countryIso();
            case "mcc":
                return profile.mcc();
            case "mnc":
                return profile.mnc();
            case "device_platform_version":
            case "platform_version":
                return "15";
            case "sdk_version":
                return "35";
            default:
                return value;
        }
    }

    static boolean spoofKnownDeviceInfo(JSONObject jsonObject, SpoofProfile profile) {
        if (jsonObject == null) {
            return false;
        }
        boolean changed = false;
        String[] keys = {
                "device_name",
                "model_number",
                "model",
                "vendor",
                "manufacturer",
                "csc",
                "sales_code",
                "country",
                "country_code",
                "countryiso_code",
                "sim_iso_country",
                "mcc",
                "mnc",
                "device_platform_version",
                "platform_version",
                "sdk_version"
        };
        for (String key : keys) {
            try {
                if (jsonObject.has(key)) {
                    Object original = jsonObject.opt(key);
                    Object spoofed = spoofValue(key, original, profile);
                    jsonObject.put(key, spoofed);
                    if (spoofed != original && !String.valueOf(spoofed).equals(String.valueOf(original))) {
                        changed = true;
                    }
                }
            } catch (JSONException e) {
                log("failed to spoof " + key + ": " + e);
            } catch (Throwable t) {
                log("failed to inspect " + key + ": " + t);
            }
        }
        try {
            JSONObject deviceInfo = jsonObject.optJSONObject("device_info");
            if (deviceInfo != null && spoofKnownDeviceInfo(deviceInfo, profile)) {
                changed = true;
            }
        } catch (Throwable t) {
            log("failed to inspect nested device_info: " + t);
        }
        return changed;
    }

    private static boolean isHonorPhoneValue(String value) {
        return value.startsWith("BKQ")
                || value.startsWith("HNBKQ")
                || value.toUpperCase().contains("HONOR");
    }

    private static boolean isChineseGalaxyWatchUltra(String value) {
        return value.startsWith("SM-L7050");
    }

    private static void hookJsonPut(SpoofProfile profile) {
        try {
            XposedHelpers.findAndHookMethod(
                    JSONObject.class,
                    "put",
                    String.class,
                    Object.class,
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            if (param.args == null || param.args.length < 2) {
                                return;
                            }
                            String key = String.valueOf(param.args[0]);
                            Object original = param.args[1];
                            Object spoofed = spoofValue(key, original, profile);
                            if (spoofed != original && !String.valueOf(spoofed).equals(String.valueOf(original))) {
                                param.args[1] = spoofed;
                                log("JSONObject.put " + key + "=" + original + " -> " + spoofed);
                            }
                        }
                    });
            log("hooked JSONObject#put device fields");
        } catch (Throwable t) {
            log("JSONObject#put hook unavailable: " + t);
        }
    }

    private static void hookJsonStringConstructor(SpoofProfile profile) {
        try {
            Constructor<JSONObject> constructor = JSONObject.class.getConstructor(String.class);
            XposedBridge.hookMethod(constructor, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.args == null
                            || param.args.length != 1
                            || !(param.args[0] instanceof String)
                            || !(param.thisObject instanceof JSONObject)) {
                        return;
                    }
                    JSONObject jsonObject = (JSONObject) param.thisObject;
                    if (spoofKnownDeviceInfo(jsonObject, profile)) {
                        log("JSONObject(String) spoofed known device fields");
                    }
                }
            });
            log("hooked JSONObject constructors for inbound device fields");
        } catch (Throwable t) {
            log("JSONObject constructor hook unavailable: " + t);
        }
    }

    private static void log(String message) {
        XposedBridge.log(TAG + message);
    }
}
