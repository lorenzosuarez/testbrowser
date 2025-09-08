package com.testlabs.browser.ui.browser

import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.testlabs.browser.core.SmartBypass
import com.testlabs.browser.core.SmartBypassEvents

public object NetworkProxySmartBypass {
    @JvmStatic
    public fun intercept(
        proxy: NetworkProxy,
        request: WebResourceRequest,
        isServiceWorker: Boolean = false,
    ): WebResourceResponse? {
        val decision = SmartBypass.decide(request)
        val method = request.method
        val url = request.url.toString()
        return when (decision.route) {
            SmartBypass.Route.BYPASS -> {
                val msg = if (isServiceWorker) {
                    SmartBypassEvents.swBypass(decision.reason, method, url)
                } else {
                    SmartBypassEvents.bypass(decision.reason, method, url)
                }
                Log.d("SmartBypass", msg)
                null
            }
            SmartBypass.Route.PROXY -> {
                val msg = if (isServiceWorker) {
                    SmartBypassEvents.swIntercept(method, url)
                } else {
                    SmartBypassEvents.intercept(method, url)
                }
                Log.d("SmartBypass", msg)
                proxy.fetchStaticGet(request)
            }
        }
    }
}
