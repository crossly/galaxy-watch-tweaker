package dev.ricky.galaxywatchtweaker.core;

public final class TargetPackages {
    public static final String WATCH_MANAGER_PACKAGE = "com.samsung.android.app.watchmanager";
    public static final String WATCH7_PLUGIN_PACKAGE = "com.samsung.wearable.watch7plugin";

    private TargetPackages() {
    }

    public static boolean isTargetPackage(String packageName) {
        return WATCH_MANAGER_PACKAGE.equals(packageName) || WATCH7_PLUGIN_PACKAGE.equals(packageName);
    }
}
