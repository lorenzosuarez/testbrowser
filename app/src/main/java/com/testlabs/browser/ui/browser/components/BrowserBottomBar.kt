/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.testlabs.browser.R
import com.testlabs.browser.ui.theme.BrowserThemeTokens

/**
 * Browser bottom navigation bar with navigation controls and floating action button for new tab.
 * Implements Material 3 BottomAppBar with integrated FloatingActionButton.
 */
@Composable
public fun BrowserBottomBar(
    canGoBack: Boolean,
    canGoForward: Boolean,
    onBackClick: () -> Unit,
    onForwardClick: () -> Unit,
    onReloadClick: () -> Unit,
    onNewTabClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val barColors = BrowserThemeTokens.barColors()
    BottomAppBar(
        containerColor = barColors.container,
        contentColor = barColors.content,
        actions = {
            IconButton(
                onClick = onBackClick,
                enabled = canGoBack,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.browser_back),
                    tint =
                        if (canGoBack) {
                            barColors.content
                        } else {
                            barColors.content.copy(alpha = 0.38f)
                        },
                )
            }
            IconButton(
                onClick = onForwardClick,
                enabled = canGoForward,
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = stringResource(R.string.browser_forward),
                    tint =
                        if (canGoForward) {
                            barColors.content
                        } else {
                            barColors.content.copy(alpha = 0.38f)
                        },
                )
            }
            IconButton(onClick = onReloadClick) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = stringResource(R.string.browser_reload),
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onNewTabClick) {
                Icon(
                    Icons.Filled.Add,
                    contentDescription = stringResource(R.string.browser_new_tab),
                )
            }
        },
        modifier = modifier,
    )
}
