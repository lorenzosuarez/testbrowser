/**
 * Author: Lorenzo Suarez
 * Date: 06/09/2025
 */

package com.testlabs.browser.ui.browser.components

import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    onHomeClick: () -> Unit,
    onEditUrlClick: () -> Unit,
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
            VerticalDivider(modifier = Modifier.padding(vertical = 8.dp).height(24.dp))
            IconButton(onClick = onHomeClick) {
                Icon(
                    Icons.Outlined.Home,
                    contentDescription = stringResource(R.string.browser_home),
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onEditUrlClick,
                elevation = FloatingActionButtonDefaults.elevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    focusedElevation = 0.dp,
                    hoveredElevation = 0.dp
                )
            ) {
                Icon(
                    Icons.Filled.Edit,
                    contentDescription = stringResource(R.string.browser_edit_url),
                )
            }
        },
        modifier = modifier,
    )
}
