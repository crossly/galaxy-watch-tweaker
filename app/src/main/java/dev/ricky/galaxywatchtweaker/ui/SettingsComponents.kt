package dev.ricky.galaxywatchtweaker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.ricky.galaxywatchtweaker.settings.TweakerPreferences

@Composable
internal fun SettingsGroup(
    title: String,
    content: @Composable () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            modifier = Modifier.padding(horizontal = 16.dp),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 1.dp,
        ) {
            Column(content = { content() })
        }
    }
}

@Composable
internal fun ToggleListItem(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(text = title, fontWeight = FontWeight.Medium)
        },
        supportingContent = { Text(description) },
        trailingContent = {
            Switch(checked = checked, onCheckedChange = null)
        },
        colors = settingsListItemColors(),
        modifier = Modifier.clickable(
            role = Role.Switch,
            onClick = { onCheckedChange(!checked) },
        ),
    )
}

@Composable
internal fun ProfileTextField(
    label: String,
    key: String,
    value: String,
    onValueChanged: (String, String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = { onValueChanged(key, it) },
        label = { Text(label) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        singleLine = key != TweakerPreferences.KEY_SPOOF_FINGERPRINT,
    )
}

@Composable
internal fun VersionListItem(label: String, version: String?) {
    ListItem(
        headlineContent = { Text(label, fontWeight = FontWeight.Medium) },
        trailingContent = {
            Text(
                text = version ?: androidx.compose.ui.res.stringResource(
                    dev.ricky.galaxywatchtweaker.R.string.version_unknown,
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        colors = settingsListItemColors(),
    )
}

@Composable
internal fun GroupDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun settingsListItemColors() = ListItemDefaults.colors(
    containerColor = MaterialTheme.colorScheme.surfaceContainer,
)
