/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */
package com.testlabs.browser.ui.browser.controller

import android.webkit.WebView
import com.testlabs.browser.domain.settings.WebViewConfig

/**
 * Utilities for dumping WebView configuration and settings for debugging.
 */
public object WebViewDumper {

    /**
     * Creates a debug dump of current WebView configuration.
     */
    public fun dumpWebViewConfig(webView: WebView, config: WebViewConfig): String {
        val settings = webView.settings
        return buildString {
            appendLine("=== WebView Configuration Dump ===")
            appendLine("JavaScript Enabled: ${settings.javaScriptEnabled}")
            appendLine("DOM Storage Enabled: ${settings.domStorageEnabled}")
            appendLine("File Access Enabled: ${settings.allowFileAccess}")
            appendLine("User Agent: ${settings.userAgentString}")
            appendLine("Cache Mode: ${settings.cacheMode}")
            appendLine("Mixed Content Mode: ${settings.mixedContentMode}")
            appendLine("Desktop Mode: ${config.desktopMode}")
            appendLine("Proxy Enabled (Smart Proxy): ${config.smartProxy}")
            appendLine("JS Compatibility Mode: ${config.jsCompatibilityMode}")
            appendLine("Force Dark Mode: ${config.forceDarkMode}")
            appendLine("Accept Languages: ${config.acceptLanguages}")
            appendLine("Engine Mode: ${config.engineMode}")
            appendLine("=== End Dump ===")
        }
    }
}
