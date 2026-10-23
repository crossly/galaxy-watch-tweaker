package dev.ricky.galaxywatchtweaker.update

sealed interface UpdateStatus {
    data object Idle : UpdateStatus
    data object Checking : UpdateStatus
    data class UpToDate(val versionName: String) : UpdateStatus
    data class Available(val release: ReleaseInfo) : UpdateStatus
    data class Failed(val detail: String) : UpdateStatus
}
