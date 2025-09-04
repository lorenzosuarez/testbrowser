package com.testlabs.browser.ui.utils

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

/**
 * Utility functions for handling system UI insets and status bar.
 */
object SystemUiUtils {

    /**
     * Gets the status bar height as a Dp value.
     *
     * @return Status bar height in Dp
     */
    @Composable
    fun getStatusBarHeight(): Dp {
        val density = LocalDensity.current
        return with(density) {
            WindowInsets.statusBars.getTop(this).toDp()
        }
    }
}
