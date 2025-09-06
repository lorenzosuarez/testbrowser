package com.testlabs.browser.webview

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
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
    private val desktopMode: Boolean = false, // AGREGADO: Desktop Mode
    private val proxyInterceptEnabled: Boolean = false // NUEVO: Control de proxy intercept
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
        if (!startScriptInstalled &&
            WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)
        ) {
            WebViewCompat.addDocumentStartJavaScript(
                view,
                jsBridge.script(),
                setOf("*")
            )
            startScriptInstalled = true
        }
    }

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        val url = request.url
        val scheme = url.scheme?.lowercase(Locale.US) ?: return null
        if (request.isForMainFrame) return null
        if (scheme !in proxySchemes) return null

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

        // CORREGIDO: Usar configuración de proxy intercept
        return if (proxyInterceptEnabled) {
            val ua = uaProvider.userAgent(desktop = desktopMode)
            proxy.interceptRequest(
                request = request,
                userAgent = ua,
                acceptLanguage = acceptLanguage,
                proxyEnabled = true
            )
        } else {
            null // Default: no intercept
        }
    }
}