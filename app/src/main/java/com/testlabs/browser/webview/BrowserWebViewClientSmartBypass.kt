/**
 * File: webview/BrowserWebViewClient_SmartBypass.kt
 * Purpose: WebViewClient glue to enable one-time automatic reload without proxy after a failure.
 * Usage:
 * - Call these helpers from the concrete BrowserWebViewClient overrides.
 * - If the function returns true, post a single reload of the failing URL.
 */
package com.testlabs.browser.webview

import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import com.testlabs.browser.core.SmartBypassEvents

public object BrowserWebViewClientSmartBypass {
    /**
     * To be called from onReceivedError. Returns true when a one-time reload without proxy should be scheduled.
     */
    @JvmStatic
    public fun handleNetworkError(view: WebView, request: WebResourceRequest, error: WebResourceError): Boolean {
        if (!request.isForMainFrame) return false
        return SmartBypassEvents.onMainFrameNetworkError(request.url)
    }

    /**
     * To be called from onReceivedHttpError. Returns true when a one-time reload without proxy should be scheduled.
     */
    @JvmStatic
    public fun handleHttpError(
        view: WebView,
        request: WebResourceRequest,
        response: WebResourceResponse
    ): Boolean {
        if (!request.isForMainFrame) return false
        return SmartBypassEvents.onMainFrameHttpError(request.url, response.statusCode)
    }
}
