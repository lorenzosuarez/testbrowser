/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */
package com.testlabs.browser.ui.browser.controller

import android.webkit.CookieManager
import android.webkit.WebView
import com.testlabs.browser.domain.settings.WebViewConfig
import com.testlabs.browser.ui.browser.NetworkProxy
import com.testlabs.browser.ui.browser.WebViewController

/**
 * Real implementation of WebViewController that manages WebView operations.
 */
public class RealWebViewController(
    private val webView: WebView,
    private val proxy: NetworkProxy,
    private var currentConfig: WebViewConfig
) : WebViewController {

    override fun loadUrl(url: String) {
        webView.loadUrl(url)
    }

    override fun reload() {
        webView.reload()
    }

    override fun goBack() {
        if (webView.canGoBack()) webView.goBack()
    }

    override fun goForward() {
        if (webView.canGoForward()) webView.goForward()
    }

    override fun recreateWebView() {
        val currentUrl = webView.url
        webView.stopLoading()
        webView.clearCache(true)
        webView.clearHistory()
        webView.clearFormData()
        CookieManager.getInstance().removeAllCookies(null)
        webView.loadUrl("about:blank")
        currentUrl?.let { url ->
            if (url != "about:blank" && url.isNotEmpty()) {
                webView.postDelayed({
                    webView.loadUrl(url)
                }, 100)
            }
        }
    }

    override fun clearBrowsingData(done: () -> Unit) {
        CookieManager.getInstance().removeAllCookies { done() }
        webView.clearCache(true)
        webView.clearHistory()
    }

    override fun config(): WebViewConfig = currentConfig
    override fun proxyStackName(): String = proxy.stackName
    override fun dumpSettings(): String = WebViewDumper.dumpWebViewConfig(webView, currentConfig)

    internal fun updateConfig(config: WebViewConfig) {
        currentConfig = config
    }
}
