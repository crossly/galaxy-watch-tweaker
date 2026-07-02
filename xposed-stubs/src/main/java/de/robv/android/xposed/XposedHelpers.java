package de.robv.android.xposed;

public final class XposedHelpers {
    private XposedHelpers() {
    }

    public static Class<?> findClass(String className, ClassLoader classLoader) {
        throw new UnsupportedOperationException("Stub only");
    }

    public static XC_MethodHook.Unhook findAndHookMethod(
            String className,
            ClassLoader classLoader,
            String methodName,
            Object... parameterTypesAndCallback) {
        throw new UnsupportedOperationException("Stub only");
    }

    public static XC_MethodHook.Unhook findAndHookMethod(
            Class<?> clazz,
            String methodName,
            Object... parameterTypesAndCallback) {
        throw new UnsupportedOperationException("Stub only");
    }

    public static Object callMethod(Object obj, String methodName, Object... args) {
        throw new UnsupportedOperationException("Stub only");
    }

    public static Object callStaticMethod(Class<?> clazz, String methodName, Object... args) {
        throw new UnsupportedOperationException("Stub only");
    }

    public static boolean getBooleanField(Object obj, String fieldName) {
        throw new UnsupportedOperationException("Stub only");
    }

    public static Object getObjectField(Object obj, String fieldName) {
        throw new UnsupportedOperationException("Stub only");
    }

    public static void setBooleanField(Object obj, String fieldName, boolean value) {
        throw new UnsupportedOperationException("Stub only");
    }

    public static void setObjectField(Object obj, String fieldName, Object value) {
        throw new UnsupportedOperationException("Stub only");
    }

    public static void setStaticObjectField(Class<?> clazz, String fieldName, Object value) {
        throw new UnsupportedOperationException("Stub only");
    }
}
