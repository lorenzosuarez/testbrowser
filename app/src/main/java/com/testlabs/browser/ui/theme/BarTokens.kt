/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */


package com.testlabs.browser.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * Shared color values for top and bottom bars.
 */
@Immutable
public data class BarColors(
    val container: Color,
    val content: Color,
)

/**
 * Theme token provider.
 */
public object BrowserThemeTokens {
    /**
     * Returns unified bar colors derived from [MaterialTheme].
     */
    @Composable
    public fun barColors(): BarColors {
        val container = MaterialTheme.colorScheme.surface
        val content = contentColorFor(container)
        return BarColors(container, content)
    }
}
