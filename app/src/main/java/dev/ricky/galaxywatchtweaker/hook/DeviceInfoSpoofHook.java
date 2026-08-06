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
    private static final String KEY_DEVICE_NAME = "device_name";
    private static final String KEY_MODEL_NUMBER = "model_number";
    private static final String KEY_MODEL = "model";
    private static final String KEY_VENDOR = "vendor";
    private static final String KEY_MANUFACTURER = "manufacturer";
    private static final String KEY_CSC = "csc";
    private static final String KEY_SALES_CODE = "sales_code";
    private static final String KEY_COUNTRY = "country";
    private static final String KEY_COUNTRY_CODE = "country_code";
    private static final String KEY_COUNTRYISO_CODE = "countryiso_code";
    private static final String KEY_SIM_ISO_COUNTRY = "sim_iso_country";
    private static final String KEY_MCC = "mcc";
    private static final String KEY_MNC = "mnc";
    private static final String KEY_DEVICE_PLATFORM_VERSION = "device_platform_version";
    private static final String KEY_PLATFORM_VERSION = "platform_version";
    private static final String KEY_SDK_VERSION = "sdk_version";
    private static final String[] SPOOF_KEYS = {
            KEY_DEVICE_NAME,
            KEY_MODEL_NUMBER,
            KEY_MODEL,
            KEY_VENDOR,
            KEY_MANUFACTURER,
            KEY_CSC,
            KEY_SALES_CODE,
            KEY_COUNTRY,
            KEY_COUNTRY_CODE,
            KEY_COUNTRYISO_CODE,
            KEY_SIM_ISO_COUNTRY,
            KEY_MCC,
            KEY_MNC,
            KEY_DEVICE_PLATFORM_VERSION,
            KEY_PLATFORM_VERSION,
            KEY_SDK_VERSION
    };
    private static volatile boolean installed;

    private DeviceInfoSpoofHook() {
    }

    static void hook(SpoofProfile profile) {
        if (installed) {
            return;
        }
        hookJsonPut(profile);
        hookJsonStringConstructor(profile);
        installed = true;
    }

    static Object spoofValue(String key, Object value, SpoofProfile profile) {
        if (key == null) {
            return value;
        }
        String stringValue = value != null ? String.valueOf(value) : "";
        switch (key) {
            case KEY_DEVICE_NAME:
            case KEY_MODEL_NUMBER:
            case KEY_MODEL:
                if (isChineseGalaxyWatchUltra(stringValue)) {
                    return "SM-L705U";
                }
                if (!isHonorPhoneValue(stringValue)) {
                    return value;
                }
                return profile.model();
            case KEY_VENDOR:
            case KEY_MANUFACTURER:
                return profile.manufacturer();
            case KEY_CSC:
            case KEY_SALES_CODE:
                return profile.salesCode();
            case KEY_COUNTRY:
            case KEY_COUNTRY_CODE:
            case KEY_COUNTRYISO_CODE:
            case KEY_SIM_ISO_COUNTRY:
                return profile.countryIso();
            case KEY_MCC:
                return profile.mcc();
            case KEY_MNC:
                return profile.mnc();
            case KEY_DEVICE_PLATFORM_VERSION:
            case KEY_PLATFORM_VERSION:
                return "15";
            case KEY_SDK_VERSION:
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
        for (String key : SPOOF_KEYS) {
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
