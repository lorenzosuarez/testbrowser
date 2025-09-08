/**
 * File: ui/browser/NetworkProxy_SmartBypass.kt
 * Purpose: Entry point to integrate SmartBypass in the interception path used by WebViewClient.
 * Usage:
 * - Replace the direct proxy call with [SmartBypassInterceptor.intercept] and pass your existing lambda.
 * - Example: return SmartBypassInterceptor.intercept(request) { existingProxyCall(request) }
 */
package com.testlabs.browser.ui.browser

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.testlabs.browser.core.SmartBypassInterceptor

public object NetworkProxySmartBypass {
    /**
     * Wraps an existing proxy execution with SmartBypass. Returns null when the request should load natively.
     */
    @JvmStatic
    public fun intercept(
        request: WebResourceRequest,
        executeProxy: () -> WebResourceResponse?,
    ): WebResourceResponse? = SmartBypassInterceptor.intercept(request, executeProxy)
}
