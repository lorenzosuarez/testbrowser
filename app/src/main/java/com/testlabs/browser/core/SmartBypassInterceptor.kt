/**
 * File: core/bypass/SmartBypassInterceptor.kt
 * Purpose: High-level interception utility to integrate SmartBypass into the request pipeline.
 * Usage:
 * - Wrap the existing proxy execution with [intercept].
 * - When bypassing is decided, returns null so WebView loads directly.
 * - On success path, removes debug-only headers from the response to preserve Chrome parity.
 */
package com.testlabs.browser.core

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse

public object SmartBypassInterceptor {
    /**
     * Executes [proceed] only when proxying is appropriate for [request].
     * On a proxied response, strips debug-only headers for Chrome parity.
     */
    @JvmStatic
    public fun intercept(
        request: WebResourceRequest,
        proceed: () -> WebResourceResponse?,
    ): WebResourceResponse? {
        if (SmartBypass.shouldBypass(request)) return null
        val response = proceed() ?: return null
        val sanitized = response.responseHeaders
            ?.filterKeys { !it.equals("X-Proxy-Engine", ignoreCase = true) }
        response.responseHeaders = sanitized
        return response
    }
}
