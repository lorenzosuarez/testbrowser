package com.testlabs.browser.webview

import android.graphics.Bitmap
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.testlabs.browser.js.JsBridge
import com.testlabs.browser.network.OkHttpEngine

public class BrowserWebViewClient(
    private val engine: OkHttpEngine,
    private val js: JsBridge
) : WebViewClient() {

    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?): Unit {
        super.onPageStarted(view, url, favicon)
        if (WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(
                view,
                js.script(),
                setOf("*")
            )
        }
    }

    override fun shouldInterceptRequest(view: WebView, request: WebResourceRequest): WebResourceResponse? {
        return engine.execute(request)
    }
}
