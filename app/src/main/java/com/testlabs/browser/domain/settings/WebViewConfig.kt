/*
 * @author Lorenzo Suarez
 * @date 09/04//2025
 */

package com.testlabs.browser.domain.settings

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Immutable configuration applied to the WebView.
 */
@Immutable
@Serializable
public data class WebViewConfig(
    val desktopMode: Boolean = false,
    val javascriptEnabled: Boolean = true,
    val acceptLanguages: String = "en-US,en;q=0.9",
    val jsCompatibilityMode: Boolean = true,
)
