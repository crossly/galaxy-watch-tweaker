package dev.ricky.galaxywatchtweaker;

import java.lang.reflect.Method;
import java.util.Locale;

import dev.ricky.galaxywatchtweaker.core.CompanionIdentityModel;
import dev.ricky.galaxywatchtweaker.core.CompanionIdentityPolicy;
import dev.ricky.galaxywatchtweaker.settings.SpoofProfile;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public final class Watch7CompanionIdentityHook {
    private static final String WEAR_COMPANION_IDENTITY_MODEL =
            "com.google.android.gms.internal.wear_companion.wt";
    private static boolean installed;

    private Watch7CompanionIdentityHook() {
    }

    public static synchronized void install(ClassLoader classLoader, SpoofProfile profile) {
        if (installed) return;
        try {
            Class<?> type = XposedHelpers.findClass(WEAR_COMPANION_IDENTITY_MODEL, classLoader);
            CompanionIdentityModel model = new CompanionIdentityModel(type);
            Method method = CompanionIdentityModel.findSingleArgumentMethod(type, "e");
            XposedBridge.hookMethod(method, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) {
                    if (param.hasThrowable()) return;
                    try {
                        model.apply(param.thisObject,
                                CompanionIdentityPolicy.toWatchCompatibleModel(profile.model()),
                                profile.manufacturer().toUpperCase(Locale.ROOT));
                    } catch (Throwable t) {
                        log("identity update skipped: " + t);
                    }
                }
            });
            installed = true;
            log("hooked validated Watch7 companion identity model");
        } catch (Throwable t) {
            log("Watch7 companion identity contract unsupported: " + t);
        }
    }

    private static void log(String message) {
        XposedBridge.log("GalaxyWatchTweaker: " + message);
    }
}
