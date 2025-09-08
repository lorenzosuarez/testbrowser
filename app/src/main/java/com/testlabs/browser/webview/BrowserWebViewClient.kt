package com.testlabs.browser.webview

import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.testlabs.browser.js.JsBridge
import com.testlabs.browser.core.SmartBypass
import com.testlabs.browser.core.SmartBypassEvents
import com.testlabs.browser.ui.browser.NetworkProxy
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
    private var lastMainOrigin: String? = null
    private var lastMainWasProxied: Boolean = false
    private var retriedWithoutProxy: Boolean = false

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (!startScriptInstalled && url != null) {
            jsBridge.injectScript(view, url)
            startScriptInstalled = true
        }
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
        if (request.isForMainFrame) {
            lastMainOrigin = SmartBypass.canonicalOrigin(request.url)
            lastMainWasProxied = false
            retriedWithoutProxy = false
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

        val ua = uaProvider.userAgent(desktop = desktopMode)
        val response = proxy.interceptRequest(
            request = request,
            userAgent = ua,
            acceptLanguage = acceptLanguage,
            proxyEnabled = proxyEnabled
        )

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

        val shouldReload = SmartBypassEvents.onMainFrameNetworkError(request.url)
        val origin = lastMainOrigin ?: SmartBypass.canonicalOrigin(request.url)
        android.util.Log.d(TAG, "MARK_TTL origin=$origin cause=net")
        if (shouldReload && !retriedWithoutProxy) {
            retriedWithoutProxy = true
            android.util.Log.d(TAG, "RELOAD_ONCE no-proxy url=${request.url}")
            view.post { view.reload() }
        } else if (shouldReload) {
            android.util.Log.d(TAG, "SKIP_RELOAD already-done url=${request.url}")
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
        val shouldReload = SmartBypassEvents.onMainFrameHttpError(request.url, code)
        if (!shouldReload) return
        val origin = lastMainOrigin ?: SmartBypass.canonicalOrigin(request.url)
        android.util.Log.d(TAG, "MARK_TTL origin=$origin cause=http$code")
        if (!retriedWithoutProxy) {
            retriedWithoutProxy = true
            android.util.Log.d(TAG, "RELOAD_ONCE no-proxy url=${request.url}")
            view.post { view.reload() }
        } else {
            android.util.Log.d(TAG, "SKIP_RELOAD already-done url=${request.url}")
        }
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        url?.let {
            val origin = SmartBypass.canonicalOrigin(Uri.parse(it))
            if (origin == lastMainOrigin) {
                retriedWithoutProxy = false
            }
        }
    }
}
