package com.testlabs.browser.ui.browser

import android.webkit.WebView
import com.testlabs.browser.domain.settings.WebViewConfig

/**
 * Extension function to apply configuration to WebView
 */
public fun WebView.applyConfig(
    config: WebViewConfig,
    uaProvider: UAProvider,
    jsCompat: JsCompatScriptProvider
) {
    // This function is called after WebView is already configured
    // Any additional configuration changes can be applied here
    // For now, we don't need to do anything as the configuration
    // is already applied in configureWebViewSafely
}
