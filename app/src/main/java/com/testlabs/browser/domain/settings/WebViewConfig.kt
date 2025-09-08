/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */

package com.testlabs.browser.domain.settings

import androidx.compose.runtime.Immutable
import kotlinx.serialization.Serializable

/**
 * Immutable configuration applied to the WebView.
 * Enhanced with Chrome Mobile compatibility features.
 */
@Immutable
@Serializable
public data class WebViewConfig(
    val javascriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val fileAccessEnabled: Boolean = false,
    val mediaAutoplayEnabled: Boolean = false,
    val mixedContentAllowed: Boolean = false,
    val enableThirdPartyCookies: Boolean = true,
    val desktopMode: Boolean = false,
    val customUserAgent: String? = null,
    val acceptLanguages: String = "en-US,en;q=0.9",
    val acceptLanguageMode: AcceptLanguageMode = AcceptLanguageMode.Baseline,
    val jsCompatibilityMode: Boolean = true,
    val forceDarkMode: Boolean = false,
    val smartProxy: Boolean = true,
    val requestedWithHeaderMode: RequestedWithHeaderMode = RequestedWithHeaderMode.ELIMINATED,
    val requestedWithHeaderAllowList: Set<String> = emptySet(),
    val engineMode: EngineMode = EngineMode.Cronet,
)

public enum class AcceptLanguageMode {
    Baseline,
    DeviceList
}

public enum class EngineMode {
    Cronet,
    OkHttp
}
