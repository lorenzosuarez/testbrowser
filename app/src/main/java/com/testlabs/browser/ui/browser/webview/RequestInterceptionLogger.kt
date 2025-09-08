package com.testlabs.browser.ui.browser.webview

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse

public object RequestInterceptionLogger {
    public fun logAndIntercept(
        request: WebResourceRequest,
        interceptor: () -> WebResourceResponse?,
    ): WebResourceResponse? {
        return try {
            interceptor()
        } catch (_: Throwable) {
            null
        }
    }
}
