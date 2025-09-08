/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */

package com.testlabs.browser.ui.browser.webview

import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.testlabs.browser.core.SmartBypassEvents

public object RequestInterceptionLogger {
    public fun logAndIntercept(
        request: WebResourceRequest,
        interceptor: () -> WebResourceResponse?,
    ): WebResourceResponse? {
        return try {
            interceptor()
        } catch (_: Throwable) {
            Log.d(
                "SmartBypass",
                SmartBypassEvents.bypass("default", request.method, request.url.toString())
            )
            null
        }
    }
}
