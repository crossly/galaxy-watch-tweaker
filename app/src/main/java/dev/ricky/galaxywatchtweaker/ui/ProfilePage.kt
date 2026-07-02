package dev.ricky.galaxywatchtweaker.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import dev.ricky.galaxywatchtweaker.R
import dev.ricky.galaxywatchtweaker.settings.TweakerPreferences
import dev.ricky.galaxywatchtweaker.settings.TweakerSettings

internal fun LazyListScope.profilePage(
    settings: TweakerSettings,
    onSpoofValueChanged: (String, String) -> Unit,
    onRestoreDefaultProfile: () -> Unit,
) {
    item {
        SettingsGroup(title = stringResource(R.string.section_spoof_profile)) {
            ProfileTextField(stringResource(R.string.field_manufacturer), TweakerPreferences.KEY_SPOOF_MANUFACTURER, settings.spoofManufacturer, onSpoofValueChanged)
            ProfileTextField(stringResource(R.string.field_brand), TweakerPreferences.KEY_SPOOF_BRAND, settings.spoofBrand, onSpoofValueChanged)
            ProfileTextField(stringResource(R.string.field_model), TweakerPreferences.KEY_SPOOF_MODEL, settings.spoofModel, onSpoofValueChanged)
            ProfileTextField(stringResource(R.string.field_device), TweakerPreferences.KEY_SPOOF_DEVICE, settings.spoofDevice, onSpoofValueChanged)
            ProfileTextField(stringResource(R.string.field_product), TweakerPreferences.KEY_SPOOF_PRODUCT, settings.spoofProduct, onSpoofValueChanged)
            ProfileTextField(stringResource(R.string.field_fingerprint), TweakerPreferences.KEY_SPOOF_FINGERPRINT, settings.spoofFingerprint, onSpoofValueChanged)
            ProfileTextField(stringResource(R.string.field_sales_code), TweakerPreferences.KEY_SPOOF_SALES_CODE, settings.spoofSalesCode, onSpoofValueChanged)
            ProfileTextField(stringResource(R.string.field_country), TweakerPreferences.KEY_SPOOF_COUNTRY_ISO, settings.spoofCountryIso, onSpoofValueChanged)
            ProfileTextField(stringResource(R.string.field_mcc), TweakerPreferences.KEY_SPOOF_MCC, settings.spoofMcc, onSpoofValueChanged)
            ProfileTextField(stringResource(R.string.field_mnc), TweakerPreferences.KEY_SPOOF_MNC, settings.spoofMnc, onSpoofValueChanged)
            ProfileTextField(stringResource(R.string.field_operator), TweakerPreferences.KEY_SPOOF_OPERATOR, settings.spoofOperator, onSpoofValueChanged)
            ProfileTextField(stringResource(R.string.field_operator_name), TweakerPreferences.KEY_SPOOF_OPERATOR_NAME, settings.spoofOperatorName, onSpoofValueChanged)
        }
    }
    item {
        Button(
            onClick = onRestoreDefaultProfile,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(stringResource(R.string.restore_default_profile))
        }
    }
}
