package dev.ricky.galaxywatchtweaker.core;

import dev.ricky.galaxywatchtweaker.settings.SpoofProfile;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public final class CompanionIdentityPolicy {
    public static final String KEY_MANUFACTURER = "key_manufacturer";
    public static final String KEY_MODEL_NUMBER = "key_model_number";
    public static final String KEY_SALES_CODE = "key_sales_code";
    public static final String FEATURE_VENDOR = "vender";
    public static final String FEATURE_MODEL_NUMBER = "modelNumber";
    public static final String FEATURE_CSC = "csc";
    private static final String WATCH_COMPATIBLE_MODEL = "SM-S938U";

    private CompanionIdentityPolicy() {
    }

    public static String spoofedValueFor(
            String preferenceKey,
            String originalValue,
            SpoofProfile profile) {
        if (KEY_MANUFACTURER.equals(preferenceKey)
                || FEATURE_VENDOR.equals(preferenceKey)) {
            return profile.manufacturer().toUpperCase(Locale.ROOT);
        }
        if (KEY_MODEL_NUMBER.equals(preferenceKey)
                || FEATURE_MODEL_NUMBER.equals(preferenceKey)) {
            return toWatchCompatibleModel(profile.model());
        }
        if (KEY_SALES_CODE.equals(preferenceKey)
                || FEATURE_CSC.equals(preferenceKey)) {
            return profile.salesCode();
        }
        return originalValue;
    }

    public static Map<Object, Object> outboundFeatureData(Map<?, ?> data, SpoofProfile profile) {
        Map<Object, Object> updated = new LinkedHashMap<>(data);
        for (String key : new String[]{FEATURE_VENDOR, FEATURE_MODEL_NUMBER, FEATURE_CSC}) {
            if (data.containsKey(key)) updated.put(key, spoofedValueFor(key, null, profile));
        }
        return updated;
    }

    public static boolean shouldSpoof(String preferenceKey) {
        return KEY_MANUFACTURER.equals(preferenceKey)
                || KEY_MODEL_NUMBER.equals(preferenceKey)
                || KEY_SALES_CODE.equals(preferenceKey)
                || FEATURE_VENDOR.equals(preferenceKey)
                || FEATURE_MODEL_NUMBER.equals(preferenceKey)
                || FEATURE_CSC.equals(preferenceKey);
    }

    public static String systemPropertyValueFor(
            String key,
            String originalValue,
            SpoofProfile profile) {
        if ("ro.product.manufacturer".equals(key)
                || "ro.product.vendor.manufacturer".equals(key)
                || "ro.product.odm.manufacturer".equals(key)
                || "ro.product.system.manufacturer".equals(key)) {
            return profile.manufacturer();
        }
        if ("ro.product.brand".equals(key)
                || "ro.product.vendor.brand".equals(key)
                || "ro.product.odm.brand".equals(key)
                || "ro.product.system.brand".equals(key)) {
            return profile.brand();
        }
        if ("ro.product.model".equals(key)
                || "ro.product.vendor.model".equals(key)
                || "ro.product.odm.model".equals(key)
                || "ro.product.system.model".equals(key)) {
            return toWatchCompatibleModel(profile.model());
        }
        if ("ro.product.device".equals(key)
                || "ro.product.vendor.device".equals(key)
                || "ro.product.odm.device".equals(key)
                || "ro.product.system.device".equals(key)) {
            return profile.device();
        }
        if ("ro.product.name".equals(key)
                || "ro.product.vendor.name".equals(key)
                || "ro.product.odm.name".equals(key)
                || "ro.product.system.name".equals(key)) {
            return profile.product();
        }
        if ("ro.csc.sales_code".equals(key)
                || "ril.sales_code".equals(key)
                || "persist.sys.omc.sales_code".equals(key)) {
            return profile.salesCode();
        }
        return originalValue;
    }

    public static String toWatchCompatibleModel(String model) {
        if ("SM-S938U1".equals(model)) {
            return WATCH_COMPATIBLE_MODEL;
        }
        return model;
    }
}
