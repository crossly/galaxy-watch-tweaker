package dev.ricky.galaxywatchtweaker.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ricky.galaxywatchtweaker.R
import dev.ricky.galaxywatchtweaker.settings.PackageVersions

internal fun LazyListScope.versionsPage(versions: PackageVersions) {
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
