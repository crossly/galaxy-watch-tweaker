package dev.ricky.galaxywatchtweaker;

import java.util.Map;

import dev.ricky.galaxywatchtweaker.core.CompanionIdentityPolicy;
import dev.ricky.galaxywatchtweaker.settings.SpoofProfile;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public final class Watch7CapabilityExchangeHook {
    private static final String CAPABILITY_EXCHANGE_MESSAGE =
            "com.samsung.android.companionservice.capability.CapabilityExchangeMessage";
    private static final String CAPABILITY_EXCHANGE_SENDER =
            "com.samsung.android.companionservice.capability.CapabilityExchangeSender";
    private static boolean installed;

    private Watch7CapabilityExchangeHook() {
    }

    public static synchronized void install(ClassLoader classLoader, SpoofProfile profile) {
        if (installed) return;
        try {
            // Only rewrite outbound messages. Constructor hooks also changed inbound watch data.
            XposedHelpers.findAndHookMethod(CAPABILITY_EXCHANGE_SENDER, classLoader,
                    "setCapabilityMessage", XposedHelpers.findClass(CAPABILITY_EXCHANGE_MESSAGE, classLoader),
                    int.class, new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) {
                            try {
                                Object message = param.args[0];
                                if (message == null) return;
                                Object data = XposedHelpers.getObjectField(message, "data");
                                if (!(data instanceof Map)) return;
                                Map<Object, Object> updated = CompanionIdentityPolicy.outboundFeatureData(
                                        (Map<?, ?>) data, profile);
                                if (!updated.equals(data)) XposedHelpers.setObjectField(message, "data", updated);
                            } catch (Throwable t) {
                                log("outbound capability update skipped: " + t);
                            }
                        }
                    });
            installed = true;
            log("hooked outbound Watch7 capability identity");
        } catch (Throwable t) {
            log("Watch7 capability sender hook unavailable: " + t);
        }
    }

    private static void log(String message) {
        XposedBridge.log("GalaxyWatchTweaker: " + message);
    }
}
