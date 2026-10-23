package dev.ricky.galaxywatchtweaker.settings;

import java.util.LinkedHashMap;
import java.util.Map;

public final class SpoofProfile {
    private final String manufacturer;
    private final String brand;
    private final String model;
    private final String device;
    private final String product;
    private final String fingerprint;
    private final String salesCode;
    private final String countryIso;
    private final String mcc;
    private final String mnc;
    private final String operator;
    private final String operatorName;

    public SpoofProfile(
            String manufacturer,
            String brand,
            String model,
            String device,
            String product,
            String fingerprint,
            String salesCode,
            String countryIso,
            String mcc,
            String mnc,
            String operator,
            String operatorName) {
        this.manufacturer = manufacturer;
        this.brand = brand;
        this.model = model;
        this.device = device;
        this.product = product;
        this.fingerprint = fingerprint;
        this.salesCode = salesCode;
        this.countryIso = countryIso;
        this.mcc = mcc;
        this.mnc = mnc;
        this.operator = operator;
        this.operatorName = operatorName;
    }

    public static SpoofProfile defaults() {
        return new SpoofProfile(
                TweakerPreferences.DEFAULT_SPOOF_MANUFACTURER,
                TweakerPreferences.DEFAULT_SPOOF_BRAND,
                TweakerPreferences.DEFAULT_SPOOF_MODEL,
                TweakerPreferences.DEFAULT_SPOOF_DEVICE,
                TweakerPreferences.DEFAULT_SPOOF_PRODUCT,
                TweakerPreferences.DEFAULT_SPOOF_FINGERPRINT,
                TweakerPreferences.DEFAULT_SPOOF_SALES_CODE,
                TweakerPreferences.DEFAULT_SPOOF_COUNTRY_ISO,
                TweakerPreferences.DEFAULT_SPOOF_MCC,
                TweakerPreferences.DEFAULT_SPOOF_MNC,
                TweakerPreferences.DEFAULT_SPOOF_OPERATOR,
                TweakerPreferences.DEFAULT_SPOOF_OPERATOR_NAME);
    }

    public String manufacturer() {
        return manufacturer;
    }

    public String brand() {
        return brand;
    }

    public String model() {
        return model;
    }

    public String device() {
        return device;
    }

    public String product() {
        return product;
    }

    public String fingerprint() {
        return fingerprint;
    }

    public String salesCode() {
        return salesCode;
    }

    public String countryIso() {
        return countryIso;
    }

    public String mcc() {
        return mcc;
    }

    public String mnc() {
        return mnc;
    }

    public String operator() {
        return operator;
    }

    public String operatorName() {
        return operatorName;
    }

    public Map<String, String> preferenceValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put(TweakerPreferences.KEY_SPOOF_MANUFACTURER, manufacturer);
        values.put(TweakerPreferences.KEY_SPOOF_BRAND, brand);
        values.put(TweakerPreferences.KEY_SPOOF_MODEL, model);
        values.put(TweakerPreferences.KEY_SPOOF_DEVICE, device);
        values.put(TweakerPreferences.KEY_SPOOF_PRODUCT, product);
        values.put(TweakerPreferences.KEY_SPOOF_FINGERPRINT, fingerprint);
        values.put(TweakerPreferences.KEY_SPOOF_SALES_CODE, salesCode);
        values.put(TweakerPreferences.KEY_SPOOF_COUNTRY_ISO, countryIso);
        values.put(TweakerPreferences.KEY_SPOOF_MCC, mcc);
        values.put(TweakerPreferences.KEY_SPOOF_MNC, mnc);
        values.put(TweakerPreferences.KEY_SPOOF_OPERATOR, operator);
        values.put(TweakerPreferences.KEY_SPOOF_OPERATOR_NAME, operatorName);
        return values;
    }

    public static SpoofProfile fromPreferences(Map<String, ?> values) {
        SpoofProfile defaults = defaults();
        return new SpoofProfile(
                value(values, TweakerPreferences.KEY_SPOOF_MANUFACTURER, defaults.manufacturer),
                value(values, TweakerPreferences.KEY_SPOOF_BRAND, defaults.brand),
                value(values, TweakerPreferences.KEY_SPOOF_MODEL, defaults.model),
                value(values, TweakerPreferences.KEY_SPOOF_DEVICE, defaults.device),
                value(values, TweakerPreferences.KEY_SPOOF_PRODUCT, defaults.product),
                value(values, TweakerPreferences.KEY_SPOOF_FINGERPRINT, defaults.fingerprint),
                value(values, TweakerPreferences.KEY_SPOOF_SALES_CODE, defaults.salesCode),
                value(values, TweakerPreferences.KEY_SPOOF_COUNTRY_ISO, defaults.countryIso),
                value(values, TweakerPreferences.KEY_SPOOF_MCC, defaults.mcc),
                value(values, TweakerPreferences.KEY_SPOOF_MNC, defaults.mnc),
                value(values, TweakerPreferences.KEY_SPOOF_OPERATOR, defaults.operator),
                value(values, TweakerPreferences.KEY_SPOOF_OPERATOR_NAME, defaults.operatorName));
    }

    private static String value(Map<String, ?> values, String key, String fallback) {
        Object value = values.get(key);
        return value instanceof String
                ? TweakerPreferences.nonBlankOrDefault((String) value, fallback) : fallback;
    }

    public Map<String, String> buildFields() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("MANUFACTURER", manufacturer);
        fields.put("BRAND", brand);
        fields.put("MODEL", model);
        fields.put("DEVICE", device);
        fields.put("PRODUCT", product);
        return fields;
    }
}
