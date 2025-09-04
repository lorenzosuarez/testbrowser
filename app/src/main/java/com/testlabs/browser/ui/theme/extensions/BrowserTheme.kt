package com.testlabs.browser.ui.theme.extensions

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import com.testlabs.browser.ui.theme.BrowserTopBarBackground
import com.testlabs.browser.ui.theme.BrowserTopBarBackgroundDark

/**
 * Extended color palette for browser-specific UI elements.
 * Provides theme-aware colors that automatically adapt to light/dark mode.
 */
object BrowserTheme {

    /**
     * Gets the appropriate top bar background color based on current theme.
     */
    val topBarBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = if (isSystemInDarkTheme()) {
            BrowserTopBarBackgroundDark
        } else {
            BrowserTopBarBackground
        }

    /**
     * Gets the appropriate search pill background color based on current theme.
     */
    val searchPillBackground: Color
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.colorScheme.surfaceVariant
}
