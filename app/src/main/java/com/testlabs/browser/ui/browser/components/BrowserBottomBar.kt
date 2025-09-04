package com.testlabs.browser.ui.browser.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.testlabs.browser.R

/**
 * Browser bottom navigation bar with navigation controls and floating action button for new tab.
 * Implements Material 3 BottomAppBar with integrated FloatingActionButton.
 */
@Composable
fun BrowserBottomBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onReloadClick: () -> Unit,
    onNewTabClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BottomAppBar(
        actions = {
            IconButton(
                onClick = onBackClick,
                enabled = canGoBack
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.browser_back),
                    tint = if (canGoBack)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
            IconButton(
                onClick = onForwardClick,
                enabled = canGoForward
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.browser_forward),
                    tint = if (canGoForward)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                )
            }
            IconButton(onClick = onReloadClick) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.browser_reload)
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTabClick) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.browser_new_tab)
                )
            }
        },
        modifier = modifier
    )
}
