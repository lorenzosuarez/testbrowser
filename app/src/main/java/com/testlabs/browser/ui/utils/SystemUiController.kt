package com.testlabs.browser.ui.utils

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * System UI controller for managing status bar and navigation bar appearance.
 * Provides smooth transitions between immersive and normal modes following Material Design guidelines.
 */
@Composable
fun SystemUiController(
    isImmersiveMode: Boolean,
    statusBarColor: Color = Color.Transparent,
    navigationBarColor: Color = Color.Transparent,
    isStatusBarContentDark: Boolean = !isSystemInDarkTheme(),
    isNavigationBarContentDark: Boolean = !isSystemInDarkTheme()
) {
    val view = LocalView.current
    val currentStatusBarColor by rememberUpdatedState(statusBarColor)
    val currentNavigationBarColor by rememberUpdatedState(navigationBarColor)
    val currentIsStatusBarContentDark by rememberUpdatedState(isStatusBarContentDark)
    val currentIsNavigationBarContentDark by rememberUpdatedState(isNavigationBarContentDark)

    DisposableEffect(
        isImmersiveMode,
        currentStatusBarColor,
        currentNavigationBarColor,
        currentIsStatusBarContentDark,
        currentIsNavigationBarContentDark
    ) {
        val window = (view.context as Activity).window
        val windowInsetsController = WindowCompat.getInsetsController(window, view)

        window.statusBarColor = currentStatusBarColor.toArgb()
        window.navigationBarColor = currentNavigationBarColor.toArgb()

        windowInsetsController.isAppearanceLightStatusBars = currentIsStatusBarContentDark
        windowInsetsController.isAppearanceLightNavigationBars = currentIsNavigationBarContentDark

        if (isImmersiveMode) {
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }

        onDispose {
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        }
    }
}

/**
 * Creates an immersive status bar effect for browser screens.
 * Automatically calculates content color based on background luminance.
 *
 * @param isVisible Whether the browser chrome is visible
 * @param backgroundColor Background color for the status bar when visible
 */
@Composable
fun ImmersiveStatusBarEffect(
    isVisible: Boolean,
    backgroundColor: Color = MaterialTheme.colorScheme.surface
) {
    val isDark = backgroundColor.luminance() < 0.5f

    SystemUiController(
        isImmersiveMode = !isVisible,
        statusBarColor = if (isVisible) backgroundColor else Color.Transparent,
        isStatusBarContentDark = !isDark
    )
}

/**
 * Extension to calculate luminance for determining status bar content color.
 */
private fun Color.luminance(): Float {
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
