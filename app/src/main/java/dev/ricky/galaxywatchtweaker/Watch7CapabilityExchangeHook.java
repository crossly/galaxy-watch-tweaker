package dev.ricky.galaxywatchtweaker;

import dev.ricky.galaxywatchtweaker.core.CompanionIdentityPolicy;
import dev.ricky.galaxywatchtweaker.settings.SpoofProfile;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

import java.lang.reflect.Constructor;
import java.util.LinkedHashMap;
import java.util.Map;

public final class Watch7CapabilityExchangeHook {
    private static final String TAG_PREFIX = "GalaxyWatchPluginHook: ";
    private static final String CAPABILITY_EXCHANGE_MESSAGE =
            "com.samsung.android.companionservice.capability.CapabilityExchangeMessage";
    private static final String CAPABILITY_EXCHANGE_SENDER =
            "com.samsung.android.companionservice.capability.CapabilityExchangeSender";
    private static final String FEATURE_VENDOR = "vender";
    private static final String FEATURE_MODEL_NUMBER = "modelNumber";
    private static final String FEATURE_CSC = "csc";
    private static volatile boolean installed;

    private Watch7CapabilityExchangeHook() {
    }

    public static void install(ClassLoader classLoader, SpoofProfile profile) {
        if (installed) {
            return;
        }
        installed = true;

        try {
            Constructor<?> constructor = findTwoArgumentConstructor(
                    XposedHelpers.findClass(CAPABILITY_EXCHANGE_MESSAGE, classLoader));
            XposedBridge.hookMethod(constructor, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    spoofFeatureExchangeData(param.thisObject, profile);
                }
            });
            hookCapabilitySender(classLoader, profile);
            log("hooked Watch7 capability feature_exchange data");
        } catch (Throwable t) {
            log("Watch7 capability feature_exchange hook unavailable: " + t);
        }
    }

    private static void hookCapabilitySender(ClassLoader classLoader, SpoofProfile profile) {
        XposedHelpers.findAndHookMethod(
                CAPABILITY_EXCHANGE_SENDER,
                classLoader,
                "setCapabilityMessage",
                XposedHelpers.findClass(CAPABILITY_EXCHANGE_MESSAGE, classLoader),
                int.class,
                new XC_MethodHook() {
                    @Override
                    protected void beforeHookedMethod(MethodHookParam param) {
                        spoofFeatureExchangeData(param.args[0], profile);
                    }
                });
    }

    private static Constructor<?> findTwoArgumentConstructor(Class<?> messageClass)
            throws NoSuchMethodException {
        for (Constructor<?> constructor : messageClass.getDeclaredConstructors()) {
            if (constructor.getParameterTypes().length == 2) {
                return constructor;
            }
        }
        throw new NoSuchMethodException(messageClass.getName() + "#<init>(*, *)");
    }

    private static void spoofFeatureExchangeData(Object message, SpoofProfile profile) {
        Object data = XposedHelpers.getObjectField(message, "data");
        if (!(data instanceof Map)) {
            log("skip feature_exchange spoof: data is unavailable");
            return;
        }

        Map<Object, Object> mutableData = new LinkedHashMap<>((Map<?, ?>) data);
        Object originalVendor = mutableData.get(FEATURE_VENDOR);
        String manufacturer = profile.manufacturer().toUpperCase();
        mutableData.put(FEATURE_VENDOR, manufacturer);
        mutableData.put(FEATURE_MODEL_NUMBER,
                CompanionIdentityPolicy.toWatchCompatibleModel(profile.model()));
        mutableData.put(FEATURE_CSC, profile.salesCode());
        XposedHelpers.setObjectField(message, "data", mutableData);
        log("spoofed Watch7 feature_exchange vendor " + originalVendor
                + " -> " + manufacturer);
    }

    private static void log(String message) {
        XposedBridge.log(TAG_PREFIX + message);
    }
}
