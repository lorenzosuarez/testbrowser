/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.ui.browser.webview

import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient

/**
 * Handles WebView lifecycle management including cleanup and disposal.
 */
public object WebViewLifecycleManager {

    /**
     * Safely destroys a WebView with proper cleanup.
     */
    public fun destroyWebView(webView: WebView) {
        try {
            runCatching { webView.stopLoading() }
            webView.webChromeClient = WebChromeClient()
            webView.webViewClient = object : WebViewClient() {}
            webView.destroy()
        } catch (_: Throwable) {
            // Ignore cleanup errors
        }
    }

    /**
     * Performs cleanup operations on WebView disposal.
     */
    public fun onWebViewDispose(webView: WebView?) {
        try {
            webView?.let { wv ->
                runCatching { wv.stopLoading() }
                wv.webChromeClient = WebChromeClient()
                wv.webViewClient = object : WebViewClient() {}
                wv.destroy()
            }
        } catch (_: Throwable) {
            // Ignore cleanup errors
        }
    }
}
