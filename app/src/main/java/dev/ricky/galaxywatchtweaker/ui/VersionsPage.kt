package dev.ricky.galaxywatchtweaker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ricky.galaxywatchtweaker.R
import dev.ricky.galaxywatchtweaker.settings.PackageVersions
import dev.ricky.galaxywatchtweaker.update.UpdateStatus

internal fun LazyListScope.versionsPage(
    versions: PackageVersions,
    currentVersion: String,
    updateStatus: UpdateStatus,
    onCheckForUpdates: () -> Unit,
) {
    item {
        SettingsGroup(title = stringResource(R.string.section_updates)) {
            VersionListItem(stringResource(R.string.update_current_version), currentVersion)
            GroupDivider()
            ListItem(
                headlineContent = { Text(updateStatusText(updateStatus)) },
                colors = ListItemDefaults.colors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            )
            GroupDivider()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                if (updateStatus is UpdateStatus.Available) {
                    val uriHandler = LocalUriHandler.current
                    val url = updateStatus.release.apkUrl ?: updateStatus.release.releaseUrl
                    Button(
                        onClick = { if (url.isNotBlank()) uriHandler.openUri(url) },
                        enabled = url.isNotBlank(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.update_action_download))
                    }
                } else {
                    Button(
                        onClick = onCheckForUpdates,
                        enabled = updateStatus != UpdateStatus.Checking,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(stringResource(R.string.update_action_check))
                    }
                }
            }
        }
    }
    item {
        SettingsGroup(title = stringResource(R.string.section_version_matrix)) {
            VersionListItem(stringResource(R.string.version_phone_shm), versions.phoneShm)
            GroupDivider()
            VersionListItem(
                stringResource(R.string.version_watch_shm),
                versions.watchShm ?: stringResource(R.string.version_watch_side_only),
            )
            GroupDivider()
            VersionListItem(stringResource(R.string.version_watch_manager), versions.watchManager)
            GroupDivider()
            VersionListItem(stringResource(R.string.version_watch_plugin), versions.watchPlugin)
            GroupDivider()
            VersionListItem(stringResource(R.string.version_google_wear), versions.googleWear)
        }
    }
    item {
        Text(
            text = stringResource(R.string.version_supported_hint),
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun updateStatusText(status: UpdateStatus): String = when (status) {
    UpdateStatus.Idle -> stringResource(R.string.update_status_idle)
    UpdateStatus.Checking -> stringResource(R.string.update_status_checking)
    is UpdateStatus.UpToDate -> stringResource(R.string.update_status_up_to_date)
    is UpdateStatus.Available -> stringResource(R.string.update_status_available, status.release.versionName)
    is UpdateStatus.Failed -> stringResource(R.string.update_status_failed)
}
