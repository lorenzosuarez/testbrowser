package com.testlabs.browser.webview

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import com.testlabs.browser.js.JsBridge
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

    private val blockedHosts: Set<String> = setOf(
        "aa.online-metrix.net",
        "fp-cdn.online-metrix.net",
        "h.online-metrix.net"
    )
    private val proxySchemes: Set<String> = setOf("http", "https")
    private var startScriptInstalled = false

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (!startScriptInstalled && url != null) {
            jsBridge.injectScript(view, url)
            startScriptInstalled = true
        }
    }

    override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
        val scheme = request.url.scheme?.lowercase(Locale.US) ?: return null
        if (scheme !in proxySchemes) return null
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
        return proxy.interceptRequest(
            request = request,
            userAgent = ua,
            acceptLanguage = acceptLanguage,
            proxyEnabled = proxyEnabled
        )
    }
}
