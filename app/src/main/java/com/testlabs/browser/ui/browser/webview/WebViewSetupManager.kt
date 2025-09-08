/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.ui.browser.webview

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.activity.result.ActivityResultLauncher
import androidx.webkit.ServiceWorkerClientCompat
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.testlabs.browser.domain.settings.AcceptLanguageMode
import com.testlabs.browser.domain.settings.WebViewConfig
import com.testlabs.browser.js.JsBridge
import com.testlabs.browser.ui.browser.JsCompatScriptProvider
import com.testlabs.browser.ui.browser.NetworkProxy
import com.testlabs.browser.ui.browser.UAProvider
import com.testlabs.browser.ui.browser.utils.DeviceLanguageUtils
import com.testlabs.browser.ui.browser.utils.JsScriptUtils
import com.testlabs.browser.webview.BrowserWebViewClient

private const val TAG = "WebViewSetup"

/**
 * Handles comprehensive WebView configuration including settings, clients, and service workers.
 */
public object WebViewSetupManager {

    /**
     * Applies the full WebView configuration, installs a BrowserWebViewClient with first-class logging,
     * and mirrors interception on the Service Worker path so subresources fetched via SW are also proxied.
     */
    @SuppressLint("SetJavaScriptEnabled", "RestrictedApi")
    public fun applyFullConfiguration(
        webView: WebView,
        config: WebViewConfig,
        uaProvider: UAProvider,
        jsCompat: JsCompatScriptProvider,
        networkProxy: NetworkProxy,
        onTitle: (String?) -> Unit,
        onProgress: (Int) -> Unit,
        onUrlChange: (String) -> Unit,
        onPageStarted: (String) -> Unit,
        onPageFinished: (String) -> Unit,
        onNavState: (Boolean, Boolean) -> Unit,
        onError: (String) -> Unit,
        filePickerLauncher: ActivityResultLauncher<Intent>?
    ) {
        configureSettings(webView, config, uaProvider)
        setupClients(
            webView = webView,
            config = config,
            uaProvider = uaProvider,
            jsCompat = jsCompat,
            networkProxy = networkProxy,
            onTitle = onTitle,
            onProgress = onProgress,
            onUrlChange = onUrlChange,
            onPageStarted = onPageStarted,
            onPageFinished = onPageFinished,
            onNavState = onNavState,
            onError = onError,
            filePickerLauncher = filePickerLauncher
        )
        setupServiceWorker(webView, config, uaProvider, networkProxy)
        configureDarkMode(webView, config)
        configureJavaScriptInjection(webView, config, jsCompat)
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun configureSettings(webView: WebView, config: WebViewConfig, uaProvider: UAProvider) {
        val s = webView.settings

        s.javaScriptEnabled = config.javascriptEnabled
        s.domStorageEnabled = config.domStorageEnabled
        s.databaseEnabled = true
        s.allowFileAccess = config.fileAccessEnabled
        s.allowContentAccess = config.fileAccessEnabled
        s.mediaPlaybackRequiresUserGesture = !config.mediaAutoplayEnabled
        s.mixedContentMode = if (config.mixedContentAllowed) {
            WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
        } else {
            WebSettings.MIXED_CONTENT_NEVER_ALLOW
        }
        s.setSupportMultipleWindows(true)
        s.javaScriptCanOpenWindowsAutomatically = true
        s.useWideViewPort = true
        s.loadWithOverviewMode = true
        s.cacheMode = if (config.smartProxy) {
            WebSettings.LOAD_NO_CACHE
        } else {
            WebSettings.LOAD_DEFAULT
        }

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, config.enableThirdPartyCookies)

        val ua = config.customUserAgent ?: uaProvider.userAgent(desktop = config.desktopMode)
        s.userAgentString = ua
    }

    private fun setupClients(
        webView: WebView,
        config: WebViewConfig,
        uaProvider: UAProvider,
        jsCompat: JsCompatScriptProvider,
        networkProxy: NetworkProxy,
        onTitle: (String?) -> Unit,
        onProgress: (Int) -> Unit,
        onUrlChange: (String) -> Unit,
        onPageStarted: (String) -> Unit,
        onPageFinished: (String) -> Unit,
        onNavState: (Boolean, Boolean) -> Unit,
        onError: (String) -> Unit,
        filePickerLauncher: ActivityResultLauncher<Intent>?
    ) {
        val acceptLanguage = when (config.acceptLanguageMode) {
            AcceptLanguageMode.Baseline -> config.acceptLanguages
            AcceptLanguageMode.DeviceList -> DeviceLanguageUtils.buildDeviceAcceptLanguage()
        }

        webView.webViewClient = object : BrowserWebViewClient(
            proxy = networkProxy,
            jsBridge = object : JsBridge(ua = uaProvider) {
                override fun script(): String = if (config.jsCompatibilityMode) {
                    JsScriptUtils.getDocStartScript(jsCompat)
                } else ""
            },
            uaProvider = uaProvider,
            acceptLanguage = acceptLanguage,
            desktopMode = config.desktopMode,
            proxyEnabled = config.smartProxy,
        ) {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest): WebResourceResponse? {
                return RequestInterceptionLogger.logAndIntercept(request) {
                    super.shouldInterceptRequest(view, request)
                }
            }

            override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                Log.d(TAG, "onPageStarted $url")
                super.onPageStarted(view, url, favicon)
                url?.let {
                    onPageStarted(it)
                    onUrlChange(it)
                    onNavState(view.canGoBack(), view.canGoForward())
                }
            }

            override fun onPageFinished(view: WebView, url: String?) {
                Log.d(TAG, "onPageFinished $url")
                super.onPageFinished(view, url)
                url?.let {
                    onPageFinished(it)
                    onUrlChange(it)
                    onNavState(view.canGoBack(), view.canGoForward())
                }
            }

            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                Log.d(TAG, "doUpdateVisitedHistory $url reload=$isReload")
                super.doUpdateVisitedHistory(view, url, isReload)
                url?.let {
                    onUrlChange(it)
                    onNavState(view.canGoBack(), view.canGoForward())
                }
            }

            override fun onPageCommitVisible(view: WebView, url: String?) {
                super.onPageCommitVisible(view, url)
                Log.d(TAG, "onPageCommitVisible $url")
                url?.let {
                    onUrlChange(it)
                    onNavState(view.canGoBack(), view.canGoForward())
                }
            }
        }

        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                Log.d(TAG, "onProgressChanged $newProgress")
                super.onProgressChanged(view, newProgress)
                onProgress(newProgress)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                Log.d(TAG, "onReceivedTitle $title")
                super.onReceivedTitle(view, title)
                onTitle(title)
            }
        }
    }

    private fun setupServiceWorker(
        webView: WebView,
        config: WebViewConfig,
        uaProvider: UAProvider,
        networkProxy: NetworkProxy
    ) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)) {
            val controller = ServiceWorkerControllerCompat.getInstance()
            val acceptLanguage = when (config.acceptLanguageMode) {
                AcceptLanguageMode.Baseline -> config.acceptLanguages
                AcceptLanguageMode.DeviceList -> DeviceLanguageUtils.buildDeviceAcceptLanguage()
            }

            controller.setServiceWorkerClient(object : ServiceWorkerClientCompat() {
                override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                    val isMain = request.isForMainFrame
                    val url = request.url.toString()
                    val method = request.method
                    val t0 = System.nanoTime()
                    Log.d(TAG, "SW REQ  main=$isMain  $method $url")
                    val uaNow = config.customUserAgent ?: uaProvider.userAgent(desktop = config.desktopMode)
                    val resp = try {
                        networkProxy.interceptRequest(
                            request = request,
                            userAgent = uaNow,
                            acceptLanguage = acceptLanguage,
                            proxyEnabled = config.smartProxy,
                        )
                    } catch (_: Throwable) { null }
                    val dt = (System.nanoTime() - t0) / 1e6
                    if (resp != null) {
                        val code = try { resp.statusCode } catch (_: Throwable) { -1 }
                        val mime = try { resp.mimeType } catch (_: Throwable) { null }
                        val size = try { resp.data?.available() ?: -1 } catch (_: Throwable) { -1 }
                        Log.d(TAG, "SW HIT  code=$code mime=$mime size~${size}B  (${dt}ms)  $method $url")
                    } else {
                        Log.d(TAG, "SW MISS (${dt}ms)  $method $url")
                    }
                    return resp
                }
            })
        }
    }

    private fun configureDarkMode(webView: WebView, config: WebViewConfig) {
        if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
            WebSettingsCompat.setForceDark(
                webView.settings,
                if (config.forceDarkMode) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
            )
        }
    }

    private fun configureJavaScriptInjection(
        webView: WebView,
        config: WebViewConfig,
        jsCompat: JsCompatScriptProvider
    ) {
        if (config.jsCompatibilityMode && WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) {
            WebViewCompat.addDocumentStartJavaScript(
                webView,
                JsScriptUtils.getDocStartScript(jsCompat),
                setOf("*")
            )
        }
    }
}
