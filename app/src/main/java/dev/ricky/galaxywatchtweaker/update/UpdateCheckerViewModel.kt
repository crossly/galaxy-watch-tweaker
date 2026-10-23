package dev.ricky.galaxywatchtweaker.update

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import dev.ricky.galaxywatchtweaker.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UpdateCheckerViewModel(application: Application) : AndroidViewModel(application) {
    private val client = GitHubReleasesClient()
    private val mutableStatus = MutableStateFlow<UpdateStatus>(UpdateStatus.Idle)
    val status: StateFlow<UpdateStatus> = mutableStatus.asStateFlow()

    val currentVersionName: String = BuildConfig.VERSION_NAME
    val currentVersionCode: Int = BuildConfig.VERSION_CODE

    init {
        checkForUpdates()
    }

    fun checkForUpdates() {
        if (mutableStatus.value == UpdateStatus.Checking) return
        mutableStatus.value = UpdateStatus.Checking
        viewModelScope.launch {
            mutableStatus.value = try {
                val json = withContext(Dispatchers.IO) { client.fetchLatestRelease() }
                val release = ReleaseInfo.parse(json)
                if (release.versionCode > currentVersionCode) {
                    UpdateStatus.Available(release)
                } else {
                    UpdateStatus.UpToDate(release.versionName)
                }
            } catch (throwable: Throwable) {
                Log.w(TAG, "Update check failed", throwable)
                UpdateStatus.Failed(throwable.message ?: "Unknown error")
            }
        }
    }

    private companion object {
        const val TAG = "GalaxyWatchTweakerUpdate"
    }
}
