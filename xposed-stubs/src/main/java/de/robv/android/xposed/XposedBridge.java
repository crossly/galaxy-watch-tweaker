package de.robv.android.xposed;

import java.lang.reflect.Member;

public final class XposedBridge {
    private XposedBridge() {
    }

    public static XC_MethodHook.Unhook hookMethod(Member hookMethod, XC_MethodHook callback) {
        throw new UnsupportedOperationException("Stub only");
    }

    public static void log(String text) {
        throw new UnsupportedOperationException("Stub only");
    }

    public static void log(Throwable throwable) {
        throw new UnsupportedOperationException("Stub only");
    }
}
