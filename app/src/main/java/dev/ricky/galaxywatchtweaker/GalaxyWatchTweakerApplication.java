package dev.ricky.galaxywatchtweaker;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.lang.reflect.Method;

import dev.ricky.galaxywatchtweaker.settings.SpoofProfile;
import dev.ricky.galaxywatchtweaker.settings.TweakerPreferences;

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
        SharedPreferences localPreferences = getSharedPreferences(
                TweakerPreferences.FILE_NAME,
                Context.MODE_PRIVATE);
        SharedPreferences remotePreferences = getRemotePreferences(TweakerPreferences.REMOTE_GROUP);
        if (remotePreferences == null) {
            return;
        }
        SpoofProfile profile = SpoofProfile.defaults();
        remotePreferences.edit()
                .putBoolean(TweakerPreferences.KEY_SHM_PATCH_ENABLED, localPreferences.getBoolean(
                        TweakerPreferences.KEY_SHM_PATCH_ENABLED,
                        TweakerPreferences.DEFAULT_SHM_PATCH_ENABLED))
                .putBoolean(TweakerPreferences.KEY_COMPANION_IDENTITY_ENABLED, localPreferences.getBoolean(
                        TweakerPreferences.KEY_COMPANION_IDENTITY_ENABLED,
                        TweakerPreferences.DEFAULT_COMPANION_IDENTITY_ENABLED))
                .putBoolean(TweakerPreferences.KEY_CAPABILITY_EXCHANGE_ENABLED, localPreferences.getBoolean(
                        TweakerPreferences.KEY_CAPABILITY_EXCHANGE_ENABLED,
                        TweakerPreferences.DEFAULT_CAPABILITY_EXCHANGE_ENABLED))
                .putBoolean(TweakerPreferences.KEY_CONNECTION_RECOVERY_ENABLED, localPreferences.getBoolean(
                        TweakerPreferences.KEY_CONNECTION_RECOVERY_ENABLED,
                        TweakerPreferences.DEFAULT_CONNECTION_RECOVERY_ENABLED))
                .putString(TweakerPreferences.KEY_SPOOF_MANUFACTURER, localPreferences.getString(
                        TweakerPreferences.KEY_SPOOF_MANUFACTURER, profile.manufacturer()))
                .putString(TweakerPreferences.KEY_SPOOF_BRAND, localPreferences.getString(
                        TweakerPreferences.KEY_SPOOF_BRAND, profile.brand()))
                .putString(TweakerPreferences.KEY_SPOOF_MODEL, localPreferences.getString(
                        TweakerPreferences.KEY_SPOOF_MODEL, profile.model()))
                .putString(TweakerPreferences.KEY_SPOOF_DEVICE, localPreferences.getString(
                        TweakerPreferences.KEY_SPOOF_DEVICE, profile.device()))
                .putString(TweakerPreferences.KEY_SPOOF_PRODUCT, localPreferences.getString(
                        TweakerPreferences.KEY_SPOOF_PRODUCT, profile.product()))
                .putString(TweakerPreferences.KEY_SPOOF_FINGERPRINT, localPreferences.getString(
                        TweakerPreferences.KEY_SPOOF_FINGERPRINT, profile.fingerprint()))
                .putString(TweakerPreferences.KEY_SPOOF_SALES_CODE, localPreferences.getString(
                        TweakerPreferences.KEY_SPOOF_SALES_CODE, profile.salesCode()))
                .putString(TweakerPreferences.KEY_SPOOF_COUNTRY_ISO, localPreferences.getString(
                        TweakerPreferences.KEY_SPOOF_COUNTRY_ISO, profile.countryIso()))
                .putString(TweakerPreferences.KEY_SPOOF_MCC, localPreferences.getString(
                        TweakerPreferences.KEY_SPOOF_MCC, profile.mcc()))
                .putString(TweakerPreferences.KEY_SPOOF_MNC, localPreferences.getString(
                        TweakerPreferences.KEY_SPOOF_MNC, profile.mnc()))
                .putString(TweakerPreferences.KEY_SPOOF_OPERATOR, localPreferences.getString(
                        TweakerPreferences.KEY_SPOOF_OPERATOR, profile.operator()))
                .putString(TweakerPreferences.KEY_SPOOF_OPERATOR_NAME, localPreferences.getString(
                        TweakerPreferences.KEY_SPOOF_OPERATOR_NAME, profile.operatorName()))
                .commit();
    }
}
