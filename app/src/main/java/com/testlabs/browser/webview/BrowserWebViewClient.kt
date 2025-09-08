package com.testlabs.browser.webview

import android.graphics.Bitmap
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.testlabs.browser.js.JsBridge
import com.testlabs.browser.core.SmartBypass
import com.testlabs.browser.core.SmartBypassEvents
import com.testlabs.browser.ui.browser.NetworkProxy
import com.testlabs.browser.ui.browser.NetworkProxySmartBypass
import com.testlabs.browser.ui.browser.UAProvider
import java.io.ByteArrayInputStream
import java.util.Locale

public open class BrowserWebViewClient(
    private val proxy: NetworkProxy,
    private val jsBridge: JsBridge,
    private val uaProvider: UAProvider,
    private val acceptLanguage: String,
    private val desktopMode: Boolean = false,
    private val proxyEnabled: Boolean = true
) : WebViewClient() {

    private val TAG = "BWVClient"
    private val blockedHosts: Set<String> = setOf(
        "aa.online-metrix.net",
        "fp-cdn.online-metrix.net",
        "h.online-metrix.net"
    )
    private var startScriptInstalled = false
    private var lastMainWasProxied: Boolean = false

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (!startScriptInstalled && url != null) {
            jsBridge.injectScript(view, url)
            startScriptInstalled = true
        }
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
        if (request.isForMainFrame) {
            lastMainWasProxied = false
        }

        if (!proxyEnabled) return null

        if (!request.isForMainFrame) {
            val host = request.url.host?.lowercase(Locale.US)
            if (host != null && host in blockedHosts) {
                return WebResourceResponse(
                    "text/plain", "UTF-8", 204, "No Content",
                    emptyMap(), ByteArrayInputStream(ByteArray(0))
                )
            }
        }

        val response = NetworkProxySmartBypass.intercept(proxy, request)

        if (request.isForMainFrame) {
            lastMainWasProxied = response != null
        }

        return response
    }

    override fun onReceivedError(
        view: WebView,
        request: WebResourceRequest,
        error: WebResourceError
    ) {
        super.onReceivedError(view, request, error)
        if (!request.isForMainFrame) return
        if (!lastMainWasProxied) return

        val origin = SmartBypass.originOf(request.url)
        if (lastMainWasProxied && SmartBypass.markOnce(origin)) {
            android.util.Log.d(TAG, SmartBypassEvents.markTtl(origin))
            android.util.Log.d(TAG, SmartBypassEvents.reloadOnce(origin))
            view.post { view.reload() }
        }
    }

    override fun onReceivedHttpError(
        view: WebView,
        request: WebResourceRequest,
        errorResponse: WebResourceResponse
    ) {
        super.onReceivedHttpError(view, request, errorResponse)
        if (!request.isForMainFrame) return
        val code = errorResponse.statusCode
        if (!lastMainWasProxied) return
        val origin = SmartBypass.originOf(request.url)
        if (code >= 500 && SmartBypass.markOnce(origin)) {
            android.util.Log.d(TAG, SmartBypassEvents.markTtl(origin))
            android.util.Log.d(TAG, SmartBypassEvents.reloadOnce(origin))
            view.post { view.reload() }
        }
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        // no-op
    }
}
