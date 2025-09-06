/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
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
    val proxyInterceptEnabled: Boolean = false, 
    val customUserAgent: String? = null,

    
    
    val engineMode: EngineMode = EngineMode.OkHttp,
    val enableQuic: Boolean = false,

    
    val acceptLanguageMode: AcceptLanguageMode = AcceptLanguageMode.Baseline,

    
    val uaUpdateMode: UaUpdateMode = UaUpdateMode.Manual,

    
    val chromeCompatibilityEnabled: Boolean = true,

    
    val suppressXRequestedWith: Boolean = true,

    
    val enableThirdPartyCookies: Boolean = true,
)

public enum class EngineMode {
    OkHttp,
    Cronet
}

public enum class AcceptLanguageMode {
    Baseline,      
    DeviceList     
}

public enum class UaUpdateMode {
    Manual,        
    AutoUpdate     
}
