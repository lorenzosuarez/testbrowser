/**
 * Author: Lorenzo Suarez
 * Date: 06/09/2025
 */

package com.testlabs.browser.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

@Immutable
public data class BarColors(val container: Color, val content: Color)

public object BrowserThemeTokens {
    @Composable
    public fun barColors(): BarColors {
        val container = MaterialTheme.colorScheme.surface
        val content = contentColorFor(container)
        return BarColors(container, content)
    }

    @Composable
    public fun omniboxColors(): OmniboxColors {
        val cs = MaterialTheme.colorScheme
        val isDark = cs.background.luminance() < 0.5f
        val container = if (isDark) BrowserTextFieldContainerDark else BrowserTextFieldContainer
        return OmniboxColors(
            container = container,
            content = cs.onSurface,
            placeholder = cs.onSurface.copy(alpha = 0.6f),
            trailingIcon = cs.onSurface.copy(alpha = 0.7f)
        )
    }
}