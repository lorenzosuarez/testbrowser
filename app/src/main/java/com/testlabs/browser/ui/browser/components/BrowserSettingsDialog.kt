package com.testlabs.browser.ui.browser.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.testlabs.browser.R
import com.testlabs.browser.domain.settings.WebViewConfig

/**
 * Enhanced dialog allowing editing of all [WebViewConfig] options with Apply & Restart functionality.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun BrowserSettingsDialog(
    config: WebViewConfig,
    onConfigChange: (WebViewConfig) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onApplyAndRestart: () -> Unit,
    onClearBrowsingData: () -> Unit,
    userAgent: String,
    acceptLanguages: String,
    headerMode: String,
    jsCompatEnabled: Boolean,
    onCopyDiagnostics: () -> Unit,
    proxyStack: String,
) {
    var tempConfig by remember { mutableStateOf(config) }
    val hasChanges = tempConfig != config

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (hasChanges) {
                    Button(
                        onClick = {
                            onConfigChange(tempConfig)
                            onApplyAndRestart()
                        }
                    ) {
                        Text("Apply & Restart WebView")
                    }
                }
                TextButton(onClick = {
                    onConfigChange(tempConfig)
                    onConfirm()
                }) {
                    Text(text = stringResource(android.R.string.ok))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(android.R.string.cancel)) }
        },
        title = { Text(text = stringResource(id = R.string.settings_title)) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Core Browser Settings
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Core Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        SettingRow(
                            label = stringResource(id = R.string.settings_desktop_mode),
                            checked = tempConfig.desktopMode,
                            onCheckedChange = { tempConfig = tempConfig.copy(desktopMode = it) }
                        )

                        SettingRow(
                            label = stringResource(id = R.string.settings_js_enabled),
                            checked = tempConfig.javascriptEnabled,
                            onCheckedChange = { tempConfig = tempConfig.copy(javascriptEnabled = it) }
                        )

                        SettingRow(
                            label = "DOM Storage",
                            checked = tempConfig.domStorageEnabled,
                            onCheckedChange = { tempConfig = tempConfig.copy(domStorageEnabled = it) }
                        )

                        SettingRow(
                            label = "Mixed Content",
                            checked = tempConfig.mixedContentAllowed,
                            onCheckedChange = { tempConfig = tempConfig.copy(mixedContentAllowed = it) }
                        )

                        SettingRow(
                            label = "Force Dark Mode",
                            checked = tempConfig.forceDarkMode,
                            onCheckedChange = { tempConfig = tempConfig.copy(forceDarkMode = it) }
                        )

                        SettingRow(
                            label = "File Access",
                            checked = tempConfig.fileAccessEnabled,
                            onCheckedChange = { tempConfig = tempConfig.copy(fileAccessEnabled = it) }
                        )

                        SettingRow(
                            label = "Media Autoplay",
                            checked = tempConfig.mediaAutoplayEnabled,
                            onCheckedChange = { tempConfig = tempConfig.copy(mediaAutoplayEnabled = it) }
                        )

                        SettingRow(
                            label = stringResource(id = R.string.settings_js_compat),
                            checked = tempConfig.jsCompatibilityMode,
                            onCheckedChange = { tempConfig = tempConfig.copy(jsCompatibilityMode = it) }
                        )
                    }
                }

                // Network Settings
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Network Settings",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Column {
                            SettingRow(
                                label = stringResource(id = R.string.settings_proxy_toggle),
                                checked = tempConfig.proxyEnabled,
                                onCheckedChange = { tempConfig = tempConfig.copy(proxyEnabled = it) }
                            )
                            Text(
                                text = "Required to eliminate X-Requested-With header on all requests",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        OutlinedTextField(
                            value = tempConfig.acceptLanguages,
                            onValueChange = { tempConfig = tempConfig.copy(acceptLanguages = it) },
                            label = { Text("Accept-Language") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = tempConfig.customUserAgent ?: "",
                            onValueChange = { value ->
                                tempConfig = tempConfig.copy(
                                    customUserAgent = if (value.isBlank()) null else value
                                )
                            },
                            label = { Text("Custom User Agent (optional)") },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Leave empty for auto-generated Chrome UA") }
                        )
                    }
                }

                // Actions
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClearBrowsingData) {
                        Text(text = stringResource(id = R.string.settings_clear_browsing_data))
                    }
                }

                // Diagnostics
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(id = R.string.settings_diagnostics_header),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = onCopyDiagnostics) {
                                Icon(
                                    imageVector = ImageVector.vectorResource(R.drawable.ic_copy),
                                    contentDescription = stringResource(id = R.string.settings_copy_diagnostics)
                                )
                            }
                        }

                        DiagnosticItem(stringResource(id = R.string.settings_user_agent_label), userAgent)
                        DiagnosticItem(stringResource(id = R.string.settings_accept_language_label), acceptLanguages)
                        DiagnosticItem(stringResource(id = R.string.settings_header_mode_label), headerMode)
                        DiagnosticItem(stringResource(id = R.string.settings_js_compat_status), if (jsCompatEnabled) "Enabled" else "Disabled")
                        DiagnosticItem(stringResource(id = R.string.settings_proxy_stack), proxyStack)
                    }
                }
            }
        },
    )
}

@Composable
private fun SettingRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun DiagnosticItem(
    label: String,
    value: String,
) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
