package com.testlabs.browser.core

import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse

public object SmartBypassInterceptor {
    @JvmStatic
    public fun intercept(
        request: WebResourceRequest,
        proxyMainDocument: Boolean,
        debugFeatures: Boolean,
        isServiceWorker: Boolean = false,
        fetcher: () -> WebResourceResponse?,
    ): WebResourceResponse? {
        val decision = SmartBypass.decide(request, proxyMainDocument, debugFeatures)
        val method = request.method
        val url = request.url.toString()
        val feat = decision.fingerprint
        val isDocument = decision.reason == "document"
        return when (decision.route) {
            SmartBypass.Route.BYPASS -> {
                val msg = if (isServiceWorker) {
                    SmartBypassEvents.swBypass(decision.reason, method, url, feat)
                } else {
                    SmartBypassEvents.bypass(decision.reason, method, url, feat)
                }
                Log.d("SmartBypass", msg)
                null
            }
            SmartBypass.Route.PROXY -> {
                val msg = if (isServiceWorker) {
                    SmartBypassEvents.swIntercept(method, url, feat, isDocument)
                } else {
                    SmartBypassEvents.intercept(method, url, feat, isDocument)
                }
                Log.d("SmartBypass", msg)
                fetcher()
            }
        }
    }
}
