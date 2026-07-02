package dev.ricky.galaxywatchtweaker.settings

data class TweakerSettings(
    val shmPatchEnabled: Boolean = TweakerPreferences.DEFAULT_SHM_PATCH_ENABLED,
    val companionIdentityEnabled: Boolean = TweakerPreferences.DEFAULT_COMPANION_IDENTITY_ENABLED,
    val capabilityExchangeEnabled: Boolean = TweakerPreferences.DEFAULT_CAPABILITY_EXCHANGE_ENABLED,
    val connectionRecoveryEnabled: Boolean = TweakerPreferences.DEFAULT_CONNECTION_RECOVERY_ENABLED,
    val spoofManufacturer: String = TweakerPreferences.DEFAULT_SPOOF_MANUFACTURER,
    val spoofBrand: String = TweakerPreferences.DEFAULT_SPOOF_BRAND,
    val spoofModel: String = TweakerPreferences.DEFAULT_SPOOF_MODEL,
    val spoofDevice: String = TweakerPreferences.DEFAULT_SPOOF_DEVICE,
    val spoofProduct: String = TweakerPreferences.DEFAULT_SPOOF_PRODUCT,
    val spoofFingerprint: String = TweakerPreferences.DEFAULT_SPOOF_FINGERPRINT,
    val spoofSalesCode: String = TweakerPreferences.DEFAULT_SPOOF_SALES_CODE,
    val spoofCountryIso: String = TweakerPreferences.DEFAULT_SPOOF_COUNTRY_ISO,
    val spoofMcc: String = TweakerPreferences.DEFAULT_SPOOF_MCC,
    val spoofMnc: String = TweakerPreferences.DEFAULT_SPOOF_MNC,
    val spoofOperator: String = TweakerPreferences.DEFAULT_SPOOF_OPERATOR,
    val spoofOperatorName: String = TweakerPreferences.DEFAULT_SPOOF_OPERATOR_NAME,
)
