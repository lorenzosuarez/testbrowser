/**
 * WebViewClient that routes main-frame and subresource requests through a network proxy
 * when enabled, enabling Chrome-like TLS and header normalization.
 *
 * Main responsibilities:
 * - Intercept main-frame requests when proxy interception is enabled.
 * - Intercept subresource requests for http/https schemes, except blocked hosts.
 * - Delegate to NetworkProxy to produce a WebResourceResponse with normalized headers.
 * - Optionally injects late scripts via JsBridge when document-start injection is unavailable.
 */
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
    private val proxyInterceptEnabled: Boolean = false
) : WebViewClient() {

    private val blockedHosts: Set<String> = setOf(
        "aa.online-metrix.net",
        "fp-cdn.online-metrix.net",
        "h.online-metrix.net"
    )

    private val proxySchemes: Set<String> = setOf("http", "https")

    private var startScriptInstalled: Boolean = false

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        if (!startScriptInstalled && url != null) {
            jsBridge.injectScript(view, url)
            startScriptInstalled = true
        }
    }

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        if (!proxyInterceptEnabled) return null

        val url = request.url
        val scheme = url.scheme?.lowercase(Locale.US) ?: return null
        if (scheme !in proxySchemes) return null

        if (!request.isForMainFrame) {
            val host = url.host?.lowercase(Locale.US)
            if (host != null && host in blockedHosts) {
                return WebResourceResponse(
                    "text/plain",
                    "UTF-8",
                    204,
                    "No Content",
                    emptyMap(),
                    ByteArrayInputStream(ByteArray(0))
                )
            }
        }

        val ua = uaProvider.userAgent(desktop = desktopMode)
        return proxy.interceptRequest(
            request = request,
            userAgent = ua,
            acceptLanguage = acceptLanguage,
            proxyEnabled = true
        )
    }
}
