package com.testlabs.browser.ui.browser.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import com.testlabs.browser.R
import com.testlabs.browser.domain.settings.WebViewConfig

/**
 * Dialog allowing editing of [WebViewConfig].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
public fun BrowserSettingsDialog(
    config: WebViewConfig,
    onConfigChange: (WebViewConfig) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onClearBrowsingData: () -> Unit,
    userAgent: String,
    acceptLanguages: String,
    headerMode: String,
    jsCompatEnabled: Boolean,
    onCopyDiagnostics: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = onConfirm) { Text(text = stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(text = stringResource(android.R.string.cancel)) }
        },
        title = { Text(text = stringResource(id = R.string.settings_title)) },
        text = {
            Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(id = R.string.settings_desktop_mode))
                    Switch(
                        checked = config.desktopMode,
                        onCheckedChange = { onConfigChange(config.copy(desktopMode = it)) },
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(id = R.string.settings_js_enabled))
                    Switch(
                        checked = config.javascriptEnabled,
                        onCheckedChange = { onConfigChange(config.copy(javascriptEnabled = it)) },
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = stringResource(id = R.string.settings_js_compat))
                    Switch(
                        checked = config.jsCompatibilityMode,
                        onCheckedChange = { onConfigChange(config.copy(jsCompatibilityMode = it)) },
                    )
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onClearBrowsingData) {
                        Text(text = stringResource(id = R.string.settings_clear_browsing_data))
                    }
                }
                Spacer(modifier = Modifier.height(0.dp))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = stringResource(id = R.string.settings_diagnostics_header))
                    Text(text = stringResource(id = R.string.settings_user_agent_label) + ": " + userAgent)
                    Text(text = stringResource(id = R.string.settings_accept_language_label) + ": " + acceptLanguages)
                    Text(text = stringResource(id = R.string.settings_header_mode_label) + ": " + headerMode)
                    Text(text = stringResource(id = R.string.settings_js_compat_status) + ": " + if (jsCompatEnabled) "on" else "off")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        IconButton(onClick = onCopyDiagnostics) {
                            Icon(
                                imageVector = ImageVector.vectorResource(R.drawable.ic_copy),
                                contentDescription = stringResource(id = R.string.settings_copy_diagnostics)
                            )
                        }
                    }
                }
            }
        },
    )
}
