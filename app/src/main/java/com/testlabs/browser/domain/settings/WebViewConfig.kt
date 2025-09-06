/*
 * @author Lorenzo Suarez
 * @date 09/04//2025
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
    val desktopMode: Boolean = false,
    val javascriptEnabled: Boolean = true,
    val domStorageEnabled: Boolean = true,
    val mixedContentAllowed: Boolean = true,
    val forceDarkMode: Boolean = false,
    val fileAccessEnabled: Boolean = true,
    val mediaAutoplayEnabled: Boolean = true,
    val acceptLanguages: String = "en-US,en;q=0.9",
    val jsCompatibilityMode: Boolean = true,
    val proxyEnabled: Boolean = true,
    val proxyInterceptEnabled: Boolean = false, // NUEVO: Control específico para intercept
    val customUserAgent: String? = null,

    // 4) Feature toggles (in-app developer section)
    // F. TLS/ALPN strategy
    val engineMode: EngineMode = EngineMode.OkHttp,
    val enableQuic: Boolean = false,

    // Accept-Language policy
    val acceptLanguageMode: AcceptLanguageMode = AcceptLanguageMode.Baseline,

    // UA auto-update policy
    val uaUpdateMode: UaUpdateMode = UaUpdateMode.Manual,

    // Chrome compatibility injection
    val chromeCompatibilityEnabled: Boolean = true,

    // D. Platform-level X-Requested-With suppression
    val suppressXRequestedWith: Boolean = true,

    // E. Cookies - third-party cookie support
    val enableThirdPartyCookies: Boolean = true,
)

public enum class EngineMode {
    OkHttp,
    Cronet
}

public enum class AcceptLanguageMode {
    Baseline,      // en-US,en;q=0.9
    DeviceList     // full device list with Chrome-like weighting
}

public enum class UaUpdateMode {
    Manual,        // manual UA string
    AutoUpdate     // VersionHistory-driven UA (cache on DataStore)
}
