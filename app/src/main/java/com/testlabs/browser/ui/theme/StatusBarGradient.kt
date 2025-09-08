/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */

package com.testlabs.browser.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Creates a gradient background for the status bar area that transitions
 * from the theme's surface color to transparent.
 *
 * @param modifier Modifier to be applied to the gradient box
 * @param surfaceColor The base color for the gradient (defaults to theme surface)
 * @param gradientHeight The height of the gradient effect
 */
@Composable
public fun StatusBarGradient(
    modifier: Modifier = Modifier,
    surfaceColor: Color = MaterialTheme.colorScheme.surface,
    gradientHeight: Int = 24
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .height(gradientHeight.dp)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        surfaceColor.copy(alpha = 0.95f),
                        surfaceColor.copy(alpha = 0.8f),
                        surfaceColor.copy(alpha = 0.4f),
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    )
}

/**
 * Creates a subtle gradient overlay that can be used behind content
 * to ensure text readability over the transparent status bar.
 *
 * @param modifier Modifier to be applied to the overlay
 */
@Composable
public fun StatusBarContentOverlay(
    modifier: Modifier = Modifier
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val overlayColor = when {
        surfaceColor.luminance() > 0.5f -> Color.White.copy(alpha = 0.7f)
        else -> Color.Black.copy(alpha = 0.7f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsTopHeight(WindowInsets.statusBars)
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        overlayColor,
                        Color.Transparent
                    ),
                    startY = 0f,
                    endY = Float.POSITIVE_INFINITY
                )
            )
    )
}

/**
 * Extension function to calculate the luminance of a color
 */
private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}
