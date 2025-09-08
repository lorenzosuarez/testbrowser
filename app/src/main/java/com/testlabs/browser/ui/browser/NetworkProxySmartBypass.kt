package com.testlabs.browser.ui.browser

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.testlabs.browser.core.SmartBypassInterceptor
import com.testlabs.browser.settings.DeveloperSettings

public object NetworkProxySmartBypass {
    @JvmStatic
    public fun intercept(
        proxy: NetworkProxy,
        request: WebResourceRequest,
        settings: DeveloperSettings,
        isServiceWorker: Boolean = false,
    ): WebResourceResponse? {
        return SmartBypassInterceptor.intercept(
            request = request,
            proxyMainDocument = settings.proxyMainDocument.value,
            debugFeatures = settings.debugFeatureLogging.value,
            isServiceWorker = isServiceWorker,
        ) { proxy.fetchStaticGet(request) }
    }
}
