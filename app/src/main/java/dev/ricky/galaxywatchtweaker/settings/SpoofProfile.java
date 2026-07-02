package dev.ricky.galaxywatchtweaker.settings;

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
}
