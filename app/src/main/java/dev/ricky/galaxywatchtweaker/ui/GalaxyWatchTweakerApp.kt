package dev.ricky.galaxywatchtweaker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.HealthAndSafety
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.ricky.galaxywatchtweaker.R
import dev.ricky.galaxywatchtweaker.settings.PackageVersions
import dev.ricky.galaxywatchtweaker.settings.TweakerSettings
import dev.ricky.galaxywatchtweaker.settings.TweakerSettingsViewModel

@Composable
fun GalaxyWatchTweakerApp(viewModel: TweakerSettingsViewModel) {
    val settings by viewModel.settings.collectAsState()
    GalaxyWatchTweakerScreen(
        settings = settings,
        versions = viewModel.packageVersions,
        onShmPatchChanged = viewModel::setShmPatchEnabled,
        onCompanionIdentityChanged = viewModel::setCompanionIdentityEnabled,
        onCapabilityExchangeChanged = viewModel::setCapabilityExchangeEnabled,
        onConnectionRecoveryChanged = viewModel::setConnectionRecoveryEnabled,
        onWatchAliasOverrideChanged = viewModel::setWatchAliasOverrideEnabled,
        onSpoofValueChanged = viewModel::setSpoofValue,
        onRestoreDefaultProfile = viewModel::restoreDefaultProfile,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GalaxyWatchTweakerScreen(
    settings: TweakerSettings,
    versions: PackageVersions,
    onShmPatchChanged: (Boolean) -> Unit,
    onCompanionIdentityChanged: (Boolean) -> Unit,
    onCapabilityExchangeChanged: (Boolean) -> Unit,
    onConnectionRecoveryChanged: (Boolean) -> Unit,
    onWatchAliasOverrideChanged: (Boolean) -> Unit,
    onSpoofValueChanged: (String, String) -> Unit,
    onRestoreDefaultProfile: () -> Unit,
) {
    var selectedDestination by remember { mutableIntStateOf(0) }
    val destinations = listOf(
        Destination(
            title = stringResource(R.string.tab_hooks),
            selectedIcon = Icons.Filled.HealthAndSafety,
            unselectedIcon = Icons.Outlined.HealthAndSafety,
        ),
        Destination(
            title = stringResource(R.string.tab_profile),
            selectedIcon = Icons.Filled.Devices,
            unselectedIcon = Icons.Outlined.Devices,
        ),
        Destination(
            title = stringResource(R.string.tab_versions),
            selectedIcon = Icons.Filled.Info,
            unselectedIcon = Icons.Outlined.Info,
        ),
    )

    Scaffold(
        containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                ),
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                destinations.forEachIndexed { index, destination ->
                    val selected = selectedDestination == index
                    NavigationBarItem(
                        selected = selected,
                        onClick = { selectedDestination = index },
                        icon = {
                            Icon(
                                imageVector = if (selected) destination.selectedIcon else destination.unselectedIcon,
                                contentDescription = destination.title,
                            )
                        },
                        label = { Text(destination.title) },
                    )
                }
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentAlignment = Alignment.TopCenter,
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 680.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                when (selectedDestination) {
                    0 -> hooksPage(
                        settings = settings,
                        onShmPatchChanged = onShmPatchChanged,
                        onCompanionIdentityChanged = onCompanionIdentityChanged,
                        onCapabilityExchangeChanged = onCapabilityExchangeChanged,
                        onConnectionRecoveryChanged = onConnectionRecoveryChanged,
                    )
                    1 -> profilePage(
                        settings = settings,
                        onWatchAliasOverrideChanged = onWatchAliasOverrideChanged,
                        onSpoofValueChanged = onSpoofValueChanged,
                        onRestoreDefaultProfile = onRestoreDefaultProfile,
                    )
                    else -> versionsPage(versions)
                }
            }
        }
    }
}

private data class Destination(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
)
