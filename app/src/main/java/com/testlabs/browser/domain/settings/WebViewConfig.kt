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
)
