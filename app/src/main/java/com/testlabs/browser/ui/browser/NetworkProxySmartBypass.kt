/**
 * File: ui/browser/NetworkProxy_SmartBypass.kt
 * Purpose: Entry point to integrate SmartBypass in the interception path used by WebViewClient.
 * Usage:
 * - Replace the direct proxy call with [SmartBypassInterceptor.maybeBypass] and pass your existing lambda.
 * - Example: return SmartBypassInterceptor.maybeBypass(request) { existingProxyCall(request) }
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
        executeProxy: () -> WebResourceResponse?
    ): WebResourceResponse? = SmartBypassInterceptor.maybeBypass(request, executeProxy)
}
