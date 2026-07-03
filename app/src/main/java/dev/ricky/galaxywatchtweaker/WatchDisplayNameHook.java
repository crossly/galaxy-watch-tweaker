package dev.ricky.galaxywatchtweaker;

import android.content.Context;

import dev.ricky.galaxywatchtweaker.core.WatchAliasPolicy;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;

public final class WatchDisplayNameHook {
    private static final String TAG_PREFIX = "GalaxyWatchPluginHook: ";
    private static final String SHARED_COMMON_UTILS =
            "com.samsung.android.plugin.sharedlib.utils.SharedCommonUtils";
    private static volatile boolean installed;

    private WatchDisplayNameHook() {
    }

    public static void install(ClassLoader classLoader, String aliasName) {
        if (installed) {
            return;
        }
        installed = true;

        try {
            XposedHelpers.findAndHookMethod(
                    SHARED_COMMON_UTILS,
                    classLoader,
                    "getAliasName",
                    String.class,
                    aliasHook(aliasName));
            XposedHelpers.findAndHookMethod(
                    SHARED_COMMON_UTILS,
                    classLoader,
                    "getAliasName",
                    String.class,
                    Context.class,
                    aliasHook(aliasName));
            log("hooked Watch display alias getter");
        } catch (Throwable t) {
            log("Watch display alias hook unavailable: " + t);
        }
    }

    private static XC_MethodHook aliasHook(String aliasName) {
        return new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) {
                String originalAlias = (String) param.getResult();
                String alias = WatchAliasPolicy.aliasFor(true, aliasName, originalAlias);
                if (alias != originalAlias && !alias.equals(originalAlias)) {
                    param.setResult(alias);
                }
            }
        };
    }

    private static void log(String message) {
        XposedBridge.log(TAG_PREFIX + message);
    }
}
