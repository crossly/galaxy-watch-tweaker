package dev.ricky.galaxywatchtweaker.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class TweakerSettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = TweakerSettingsRepository(application)

    val settings: StateFlow<TweakerSettings> = repository.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = repository.settings.value,
    )

    val packageVersions: PackageVersions = repository.packageVersions()

    fun setShmPatchEnabled(enabled: Boolean) = repository.setShmPatchEnabled(enabled)

    fun setCompanionIdentityEnabled(enabled: Boolean) = repository.setCompanionIdentityEnabled(enabled)

    fun setCapabilityExchangeEnabled(enabled: Boolean) = repository.setCapabilityExchangeEnabled(enabled)

    fun setConnectionRecoveryEnabled(enabled: Boolean) = repository.setConnectionRecoveryEnabled(enabled)

    fun setSpoofValue(key: String, value: String) = repository.setSpoofValue(key, value)

    fun restoreDefaultProfile() = repository.restoreDefaultProfile()
}
