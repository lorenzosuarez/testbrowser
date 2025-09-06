/*
 * @author Lorenzo Suarez
 * @date 09/04//2025
 */

package com.testlabs.browser.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme =
    darkColorScheme(
        primary = BrowserPrimaryDark,
        onPrimary = BrowserOnPrimaryDark,
        primaryContainer = BrowserPrimaryContainerDark,
        onPrimaryContainer = BrowserOnPrimaryContainerDark,
        secondary = BrowserSecondaryDark,
        onSecondary = BrowserOnSecondaryDark,
        secondaryContainer = BrowserSecondaryContainerDark,
        onSecondaryContainer = BrowserOnSecondaryContainerDark,
        tertiary = BrowserTertiaryDark,
        onTertiary = BrowserOnTertiaryDark,
        tertiaryContainer = BrowserTertiaryContainerDark,
        onTertiaryContainer = BrowserOnTertiaryContainerDark,
        error = BrowserErrorDark,
        errorContainer = BrowserErrorContainerDark,
        onError = BrowserOnErrorDark,
        onErrorContainer = BrowserOnErrorContainerDark,
        background = BrowserBackgroundDark,
        onBackground = BrowserOnBackgroundDark,
        surface = BrowserSurfaceDark,
        onSurface = BrowserOnSurfaceDark,
        surfaceVariant = BrowserSurfaceVariantDark,
        onSurfaceVariant = BrowserOnSurfaceVariantDark,
        outline = BrowserOutlineDark,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = BrowserPrimary,
        onPrimary = BrowserOnPrimary,
        primaryContainer = BrowserPrimaryContainer,
        onPrimaryContainer = BrowserOnPrimaryContainer,
        secondary = BrowserSecondary,
        onSecondary = BrowserOnSecondary,
        secondaryContainer = BrowserSecondaryContainer,
        onSecondaryContainer = BrowserOnSecondaryContainer,
        tertiary = BrowserTertiary,
        onTertiary = BrowserOnTertiary,
        tertiaryContainer = BrowserTertiaryContainer,
        onTertiaryContainer = BrowserOnTertiaryContainer,
        error = BrowserError,
        errorContainer = BrowserErrorContainer,
        onError = BrowserOnError,
        onErrorContainer = BrowserOnErrorContainer,
        background = BrowserBackground,
        onBackground = BrowserOnBackground,
        surface = BrowserSurface,
        onSurface = BrowserOnSurface,
        surfaceVariant = BrowserSurfaceVariant,
        onSurfaceVariant = BrowserOnSurfaceVariant,
        outline = BrowserOutline,
    )

/**
 * Material 3 theme for TestBrowser application.
 * Automatically adapts to system dark mode preference and provides
 * consistent theming across the application with transparent status bar.
 *
 * @param darkTheme Whether to use dark theme colors
 * @param content The content to be themed
 */
@Composable
public fun TestBrowserTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window

            WindowCompat.setDecorFitsSystemWindows(window, false)

            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()

            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme

            WindowCompat.getInsetsController(window, view)
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
    }

    MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
