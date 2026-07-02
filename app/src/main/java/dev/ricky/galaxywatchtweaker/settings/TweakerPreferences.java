package dev.ricky.galaxywatchtweaker.settings;

public final class TweakerPreferences {
    public static final String FILE_NAME = "galaxy_watch_tweaker_settings";
    public static final String REMOTE_GROUP = FILE_NAME;

    public static final String KEY_SHM_PATCH_ENABLED = "shm_patch_enabled";
    public static final String KEY_COMPANION_IDENTITY_ENABLED = "companion_identity_enabled";
    public static final String KEY_CAPABILITY_EXCHANGE_ENABLED = "capability_exchange_enabled";
    public static final String KEY_CONNECTION_RECOVERY_ENABLED = "connection_recovery_enabled";
    public static final String KEY_SPOOF_MANUFACTURER = "spoof_manufacturer";
    public static final String KEY_SPOOF_BRAND = "spoof_brand";
    public static final String KEY_SPOOF_MODEL = "spoof_model";
    public static final String KEY_SPOOF_DEVICE = "spoof_device";
    public static final String KEY_SPOOF_PRODUCT = "spoof_product";
    public static final String KEY_SPOOF_FINGERPRINT = "spoof_fingerprint";
    public static final String KEY_SPOOF_SALES_CODE = "spoof_sales_code";
    public static final String KEY_SPOOF_COUNTRY_ISO = "spoof_country_iso";
    public static final String KEY_SPOOF_MCC = "spoof_mcc";
    public static final String KEY_SPOOF_MNC = "spoof_mnc";
    public static final String KEY_SPOOF_OPERATOR = "spoof_operator";
    public static final String KEY_SPOOF_OPERATOR_NAME = "spoof_operator_name";

    public static final boolean DEFAULT_SHM_PATCH_ENABLED = true;
    public static final boolean DEFAULT_COMPANION_IDENTITY_ENABLED = true;
    public static final boolean DEFAULT_CAPABILITY_EXCHANGE_ENABLED = true;
    public static final boolean DEFAULT_CONNECTION_RECOVERY_ENABLED = true;
    public static final String DEFAULT_SPOOF_MANUFACTURER = "samsung";
    public static final String DEFAULT_SPOOF_BRAND = "samsung";
    public static final String DEFAULT_SPOOF_MODEL = "SM-S938U1";
    public static final String DEFAULT_SPOOF_DEVICE = "pa3q";
    public static final String DEFAULT_SPOOF_PRODUCT = "pa3qxxx";
    public static final String DEFAULT_SPOOF_FINGERPRINT =
            "samsung/pa3qxxx/pa3q:15/AP3A.240905.015.A2/S938U1UEU1AYB3:user/release-keys";
    public static final String DEFAULT_SPOOF_SALES_CODE = "XAA";
    public static final String DEFAULT_SPOOF_COUNTRY_ISO = "US";
    public static final String DEFAULT_SPOOF_MCC = "310";
    public static final String DEFAULT_SPOOF_MNC = "260";
    public static final String DEFAULT_SPOOF_OPERATOR = "310260";
    public static final String DEFAULT_SPOOF_OPERATOR_NAME = "T-Mobile";

    private TweakerPreferences() {
    }

    public static String nonBlankOrDefault(String value, String defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        return value.trim();
    }
}
