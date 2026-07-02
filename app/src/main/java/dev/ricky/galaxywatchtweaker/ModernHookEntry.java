package dev.ricky.galaxywatchtweaker;

import android.util.Log;

import java.util.HashSet;
import java.util.Set;

import dev.ricky.galaxywatchtweaker.hook.HookConfig;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import io.github.libxposed.api.XposedModule;

public final class ModernHookEntry extends XposedModule {
    private static final String TAG = "GalaxyWatchTweaker";
    private final HookConfig config = new HookConfig(this);
    private final HookEntry legacyEntry = new HookEntry();
    private final Set<String> dispatchedTargets = new HashSet<>();
    private String currentProcessName = "";

    @Override
    public void onModuleLoaded(ModuleLoadedParam param) {
        HookConfig.setDefaultXposed(this);
        currentProcessName = param.getProcessName();
        log(Log.INFO, TAG, "loaded process=" + currentProcessName);
    }

    @Override
    public void onSystemServerStarting(SystemServerStartingParam param) {
        XC_LoadPackage.LoadPackageParam legacyParam = new XC_LoadPackage.LoadPackageParam();
        legacyParam.packageName = "android";
        legacyParam.processName = "android";
        legacyParam.classLoader = param.getClassLoader();
        dispatch(legacyParam);
    }

    @Override
    public void onPackageLoaded(PackageLoadedParam param) {
        XC_LoadPackage.LoadPackageParam legacyParam = new XC_LoadPackage.LoadPackageParam();
        legacyParam.packageName = param.getPackageName();
        legacyParam.processName = currentProcessName;
        legacyParam.classLoader = param.getDefaultClassLoader();
        dispatch(legacyParam);
    }

    @Override
    public void onPackageReady(PackageReadyParam param) {
        XC_LoadPackage.LoadPackageParam legacyParam = new XC_LoadPackage.LoadPackageParam();
        legacyParam.packageName = param.getPackageName();
        legacyParam.processName = currentProcessName;
        legacyParam.classLoader = param.getClassLoader();
        dispatch(legacyParam);
    }

    private void dispatch(XC_LoadPackage.LoadPackageParam legacyParam) {
        String targetKey = legacyParam.packageName + "/" + legacyParam.processName;
        if (!dispatchedTargets.add(targetKey)) {
            return;
        }

        legacyEntry.handleLoadPackage(legacyParam, config);
        log(Log.INFO, TAG, "evaluated " + legacyParam.packageName
                + " process=" + legacyParam.processName);
    }
}
