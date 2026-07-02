package dev.ricky.galaxywatchtweaker.core;

import dev.ricky.galaxywatchtweaker.settings.SpoofProfile;

public final class CompanionIdentityPolicy {
    public static final String KEY_MANUFACTURER = "key_manufacturer";
    public static final String KEY_MODEL_NUMBER = "key_model_number";
    public static final String KEY_SALES_CODE = "key_sales_code";
    public static final String FEATURE_VENDOR = "vender";
    public static final String FEATURE_MODEL_NUMBER = "modelNumber";
    public static final String FEATURE_CSC = "csc";

    private CompanionIdentityPolicy() {
    }

    public static String spoofedValueFor(String preferenceKey, String originalValue) {
        return spoofedValueFor(preferenceKey, originalValue, SpoofProfile.defaults());
    }

    public static String spoofedValueFor(
            String preferenceKey,
            String originalValue,
            SpoofProfile profile) {
        if (KEY_MANUFACTURER.equals(preferenceKey)
                || FEATURE_VENDOR.equals(preferenceKey)) {
            return profile.manufacturer().toUpperCase();
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

    public static boolean shouldSpoof(String preferenceKey) {
        return KEY_MANUFACTURER.equals(preferenceKey)
                || KEY_MODEL_NUMBER.equals(preferenceKey)
                || KEY_SALES_CODE.equals(preferenceKey)
                || FEATURE_VENDOR.equals(preferenceKey)
                || FEATURE_MODEL_NUMBER.equals(preferenceKey)
                || FEATURE_CSC.equals(preferenceKey);
    }

    public static String systemPropertyValueFor(String key, String originalValue) {
        return systemPropertyValueFor(key, originalValue, SpoofProfile.defaults());
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

    public static String samsungManufacturer() {
        return SpoofProfile.defaults().manufacturer().toUpperCase();
    }

    public static String samsungBrand() {
        return SpoofProfile.defaults().brand();
    }

    public static String samsungModel() {
        return toWatchCompatibleModel(SpoofProfile.defaults().model());
    }

    public static String samsungDevice() {
        return SpoofProfile.defaults().device();
    }

    public static String samsungProduct() {
        return SpoofProfile.defaults().product();
    }

    public static String usaSalesCode() {
        return SpoofProfile.defaults().salesCode();
    }

    public static String toWatchCompatibleModel(String model) {
        if (model != null && model.endsWith("1")) {
            return model.substring(0, model.length() - 1);
        }
        return model;
    }
}
