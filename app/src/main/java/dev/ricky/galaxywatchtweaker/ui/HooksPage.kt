package dev.ricky.galaxywatchtweaker.ui

import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ricky.galaxywatchtweaker.R
import dev.ricky.galaxywatchtweaker.settings.TweakerSettings

internal fun LazyListScope.hooksPage(
    settings: TweakerSettings,
    onShmPatchChanged: (Boolean) -> Unit,
    onCompanionIdentityChanged: (Boolean) -> Unit,
    onCapabilityExchangeChanged: (Boolean) -> Unit,
    onConnectionRecoveryChanged: (Boolean) -> Unit,
) {
    item {
        Text(
            text = stringResource(R.string.restart_notice),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    item {
        SettingsGroup(title = stringResource(R.string.section_samsung_health_monitor)) {
            ToggleListItem(
                title = stringResource(R.string.shm_patch_title),
                description = stringResource(R.string.shm_patch_description),
                checked = settings.shmPatchEnabled,
                onCheckedChange = onShmPatchChanged,
            )
        }
    }
    item {
        SettingsGroup(title = stringResource(R.string.section_watch_companion)) {
            ToggleListItem(
                title = stringResource(R.string.companion_identity_title),
                description = stringResource(R.string.companion_identity_description),
                checked = settings.companionIdentityEnabled,
                onCheckedChange = onCompanionIdentityChanged,
            )
            GroupDivider()
            ToggleListItem(
                title = stringResource(R.string.capability_exchange_title),
                description = stringResource(R.string.capability_exchange_description),
                checked = settings.capabilityExchangeEnabled,
                onCheckedChange = onCapabilityExchangeChanged,
            )
        }
    }
    item {
        SettingsGroup(title = stringResource(R.string.section_connection_recovery)) {
            ToggleListItem(
                title = stringResource(R.string.connection_recovery_title),
                description = stringResource(R.string.connection_recovery_description),
                checked = settings.connectionRecoveryEnabled,
                onCheckedChange = onConnectionRecoveryChanged,
            )
        }
    }
}
