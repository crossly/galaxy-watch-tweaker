package dev.ricky.galaxywatchtweaker.settings

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import dev.ricky.galaxywatchtweaker.GalaxyWatchTweakerApplication
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TweakerSettingsRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = openPrivatePreferences()
    private val mutableSettings = MutableStateFlow(preferences.readSettings())

    init {
        syncRemotePreferences()
    }

    val settings: StateFlow<TweakerSettings> = mutableSettings.asStateFlow()

    fun setShmPatchEnabled(enabled: Boolean) = update {
        putBoolean(TweakerPreferences.KEY_SHM_PATCH_ENABLED, enabled)
    }

    fun setCompanionIdentityEnabled(enabled: Boolean) = update {
        putBoolean(TweakerPreferences.KEY_COMPANION_IDENTITY_ENABLED, enabled)
    }

    fun setCapabilityExchangeEnabled(enabled: Boolean) = update {
        putBoolean(TweakerPreferences.KEY_CAPABILITY_EXCHANGE_ENABLED, enabled)
    }

    fun setConnectionRecoveryEnabled(enabled: Boolean) = update {
        putBoolean(TweakerPreferences.KEY_CONNECTION_RECOVERY_ENABLED, enabled)
    }

    fun setSpoofValue(key: String, value: String) = update {
        putString(key, value)
    }

    fun restoreDefaultProfile() = update {
        putDefaultProfile()
    }

    fun packageVersions(): PackageVersions {
        return PackageVersions(
            phoneShm = versionName("com.samsung.android.shealthmonitor"),
            watchShm = null,
            watchManager = versionName("com.samsung.android.app.watchmanager"),
            watchPlugin = versionName("com.samsung.wearable.watch7plugin"),
            googleWear = versionName("com.google.android.wearable.app.cn"),
        )
    }

    private fun update(block: SharedPreferences.Editor.() -> Unit) {
        preferences.edit().apply(block).commit()
        updateRemotePreferences(block)
        mutableSettings.value = preferences.readSettings()
    }

    private fun syncRemotePreferences() {
        val settings = preferences.readSettings()
        updateRemotePreferences {
            putBoolean(TweakerPreferences.KEY_SHM_PATCH_ENABLED, settings.shmPatchEnabled)
            putBoolean(TweakerPreferences.KEY_COMPANION_IDENTITY_ENABLED, settings.companionIdentityEnabled)
            putBoolean(TweakerPreferences.KEY_CAPABILITY_EXCHANGE_ENABLED, settings.capabilityExchangeEnabled)
            putBoolean(TweakerPreferences.KEY_CONNECTION_RECOVERY_ENABLED, settings.connectionRecoveryEnabled)
            putString(TweakerPreferences.KEY_SPOOF_MANUFACTURER, settings.spoofManufacturer)
            putString(TweakerPreferences.KEY_SPOOF_BRAND, settings.spoofBrand)
            putString(TweakerPreferences.KEY_SPOOF_MODEL, settings.spoofModel)
            putString(TweakerPreferences.KEY_SPOOF_DEVICE, settings.spoofDevice)
            putString(TweakerPreferences.KEY_SPOOF_PRODUCT, settings.spoofProduct)
            putString(TweakerPreferences.KEY_SPOOF_FINGERPRINT, settings.spoofFingerprint)
            putString(TweakerPreferences.KEY_SPOOF_SALES_CODE, settings.spoofSalesCode)
            putString(TweakerPreferences.KEY_SPOOF_COUNTRY_ISO, settings.spoofCountryIso)
            putString(TweakerPreferences.KEY_SPOOF_MCC, settings.spoofMcc)
            putString(TweakerPreferences.KEY_SPOOF_MNC, settings.spoofMnc)
            putString(TweakerPreferences.KEY_SPOOF_OPERATOR, settings.spoofOperator)
            putString(TweakerPreferences.KEY_SPOOF_OPERATOR_NAME, settings.spoofOperatorName)
        }
    }

    private fun SharedPreferences.Editor.putDefaultProfile() {
        putString(TweakerPreferences.KEY_SPOOF_MANUFACTURER, TweakerPreferences.DEFAULT_SPOOF_MANUFACTURER)
        putString(TweakerPreferences.KEY_SPOOF_BRAND, TweakerPreferences.DEFAULT_SPOOF_BRAND)
        putString(TweakerPreferences.KEY_SPOOF_MODEL, TweakerPreferences.DEFAULT_SPOOF_MODEL)
        putString(TweakerPreferences.KEY_SPOOF_DEVICE, TweakerPreferences.DEFAULT_SPOOF_DEVICE)
        putString(TweakerPreferences.KEY_SPOOF_PRODUCT, TweakerPreferences.DEFAULT_SPOOF_PRODUCT)
        putString(TweakerPreferences.KEY_SPOOF_FINGERPRINT, TweakerPreferences.DEFAULT_SPOOF_FINGERPRINT)
        putString(TweakerPreferences.KEY_SPOOF_SALES_CODE, TweakerPreferences.DEFAULT_SPOOF_SALES_CODE)
        putString(TweakerPreferences.KEY_SPOOF_COUNTRY_ISO, TweakerPreferences.DEFAULT_SPOOF_COUNTRY_ISO)
        putString(TweakerPreferences.KEY_SPOOF_MCC, TweakerPreferences.DEFAULT_SPOOF_MCC)
        putString(TweakerPreferences.KEY_SPOOF_MNC, TweakerPreferences.DEFAULT_SPOOF_MNC)
        putString(TweakerPreferences.KEY_SPOOF_OPERATOR, TweakerPreferences.DEFAULT_SPOOF_OPERATOR)
        putString(TweakerPreferences.KEY_SPOOF_OPERATOR_NAME, TweakerPreferences.DEFAULT_SPOOF_OPERATOR_NAME)
    }

    private fun updateRemotePreferences(block: SharedPreferences.Editor.() -> Unit) {
        runCatching {
            GalaxyWatchTweakerApplication
                .getRemotePreferences(TweakerPreferences.REMOTE_GROUP)
                ?.edit()
                ?.apply(block)
                ?.commit()
        }
    }

    private fun SharedPreferences.readSettings(): TweakerSettings {
        return TweakerSettings(
            shmPatchEnabled = getBoolean(
                TweakerPreferences.KEY_SHM_PATCH_ENABLED,
                TweakerPreferences.DEFAULT_SHM_PATCH_ENABLED,
            ),
            companionIdentityEnabled = getBoolean(
                TweakerPreferences.KEY_COMPANION_IDENTITY_ENABLED,
                TweakerPreferences.DEFAULT_COMPANION_IDENTITY_ENABLED,
            ),
            capabilityExchangeEnabled = getBoolean(
                TweakerPreferences.KEY_CAPABILITY_EXCHANGE_ENABLED,
                TweakerPreferences.DEFAULT_CAPABILITY_EXCHANGE_ENABLED,
            ),
            connectionRecoveryEnabled = getBoolean(
                TweakerPreferences.KEY_CONNECTION_RECOVERY_ENABLED,
                TweakerPreferences.DEFAULT_CONNECTION_RECOVERY_ENABLED,
            ),
            spoofManufacturer = profileValue(TweakerPreferences.KEY_SPOOF_MANUFACTURER, TweakerPreferences.DEFAULT_SPOOF_MANUFACTURER),
            spoofBrand = profileValue(TweakerPreferences.KEY_SPOOF_BRAND, TweakerPreferences.DEFAULT_SPOOF_BRAND),
            spoofModel = profileValue(TweakerPreferences.KEY_SPOOF_MODEL, TweakerPreferences.DEFAULT_SPOOF_MODEL),
            spoofDevice = profileValue(TweakerPreferences.KEY_SPOOF_DEVICE, TweakerPreferences.DEFAULT_SPOOF_DEVICE),
            spoofProduct = profileValue(TweakerPreferences.KEY_SPOOF_PRODUCT, TweakerPreferences.DEFAULT_SPOOF_PRODUCT),
            spoofFingerprint = profileValue(TweakerPreferences.KEY_SPOOF_FINGERPRINT, TweakerPreferences.DEFAULT_SPOOF_FINGERPRINT),
            spoofSalesCode = profileValue(TweakerPreferences.KEY_SPOOF_SALES_CODE, TweakerPreferences.DEFAULT_SPOOF_SALES_CODE),
            spoofCountryIso = profileValue(TweakerPreferences.KEY_SPOOF_COUNTRY_ISO, TweakerPreferences.DEFAULT_SPOOF_COUNTRY_ISO),
            spoofMcc = profileValue(TweakerPreferences.KEY_SPOOF_MCC, TweakerPreferences.DEFAULT_SPOOF_MCC),
            spoofMnc = profileValue(TweakerPreferences.KEY_SPOOF_MNC, TweakerPreferences.DEFAULT_SPOOF_MNC),
            spoofOperator = profileValue(TweakerPreferences.KEY_SPOOF_OPERATOR, TweakerPreferences.DEFAULT_SPOOF_OPERATOR),
            spoofOperatorName = profileValue(TweakerPreferences.KEY_SPOOF_OPERATOR_NAME, TweakerPreferences.DEFAULT_SPOOF_OPERATOR_NAME),
        )
    }

    private fun SharedPreferences.profileValue(key: String, defaultValue: String): String {
        return TweakerPreferences.nonBlankOrDefault(getString(key, defaultValue), defaultValue)
    }

    private fun versionName(packageName: String): String? {
        return try {
            appContext.packageManager.getPackageInfo(packageName, 0).versionName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun openPrivatePreferences(): SharedPreferences {
        return appContext.getSharedPreferences(TweakerPreferences.FILE_NAME, Context.MODE_PRIVATE)
    }
}
