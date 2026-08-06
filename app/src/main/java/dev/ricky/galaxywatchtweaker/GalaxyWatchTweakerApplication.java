package dev.ricky.galaxywatchtweaker;

import android.app.Application;
import android.content.SharedPreferences;
import android.util.Log;

import java.lang.reflect.Method;

import dev.ricky.galaxywatchtweaker.settings.TweakerSettingsRepository;

public final class GalaxyWatchTweakerApplication extends Application {
    private static final String TAG = "GalaxyWatchTweakerApp";
    private static volatile Object xposedService;

    @Override
    public void onCreate() {
        super.onCreate();
        registerXposedServiceListener();
    }

    public static SharedPreferences getRemotePreferences(String group) {
        Object service = xposedService;
        if (service == null) {
            return null;
        }

        try {
            Method method = service.getClass().getMethod("getRemotePreferences", String.class);
            Object preferences = method.invoke(service, group);
            if (preferences instanceof SharedPreferences) {
                return (SharedPreferences) preferences;
            }
        } catch (Throwable t) {
            Log.w(TAG, "remote preferences unavailable", t);
        }
        return null;
    }

    private void registerXposedServiceListener() {
        try {
            Class<?> helperClass = Class.forName("io.github.libxposed.service.XposedServiceHelper");
            Class<?> listenerClass = Class.forName(
                    "io.github.libxposed.service.XposedServiceHelper$OnServiceListener");
            Object listener = java.lang.reflect.Proxy.newProxyInstance(
                    listenerClass.getClassLoader(),
                    new Class<?>[]{listenerClass},
                    (proxy, method, args) -> {
                        if ("onServiceBind".equals(method.getName()) && args != null && args.length == 1) {
                            xposedService = args[0];
                            syncRemotePreferences();
                        } else if ("onServiceDied".equals(method.getName())) {
                            xposedService = null;
                        }
                        return null;
                    });
            helperClass.getMethod("registerListener", listenerClass).invoke(null, listener);
        } catch (Throwable t) {
            Log.w(TAG, "xposed service listener unavailable", t);
        }
    }

    private void syncRemotePreferences() {
        new TweakerSettingsRepository(this).syncRemotePreferences();
    }
}
