package com.testlabs.browser.ui.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebStorage
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.ServiceWorkerClientCompat
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebSettingsCompat
import com.testlabs.browser.domain.settings.WebViewConfig
private const val TAG = "WebViewHost"
private const val MIME_TYPE_GUESS = "application/octet-stream"

/**
 * Hosts an Android [WebView] inside Jetpack Compose with a modern, privacy-aware configuration.
 *
 * Features:
 * - First-class Compose integration with lifecycle-aware creation and teardown of the underlying [WebView].
 * - Standards-compliant networking with consistent headers (custom User-Agent and `Accept-Language`).
 * - Strict suppression of the `X-Requested-With` header for page and Service Worker requests.
 * - Service Worker interception via `ServiceWorkerControllerCompat` to mirror page-level request policies.
 * - File uploads via `onShowFileChooser` bridged to an app-provided [ActivityResultLauncher].
 * - Download handling (including `blob:` URLs) delegated to an app-provided [DownloadHandler].
 * - Dark mode using algorithmic darkening when available (`WebSettingsCompat.setAlgorithmicDarkeningAllowed`),
 *   with safe fallback when not supported.
 * - Controlled media autoplay, DOM storage, and mixed-content behavior via [WebViewConfig].
 * - Scroll delta callback for UI chrome coordination (positive = scrolling down, negative = up).
 *
 * Threading & lifecycle:
 * - Must be invoked on the main thread. All callbacks are delivered on the main thread.
 * - The internal [WebView] is recreated only when configuration changes demand it (see [WebViewConfig]).
 * - The [WebView] is explicitly stopped and destroyed on disposal to prevent leaks.
 *
 * Limitations & notes:
 * - External schemes (`mailto:`, `tel:`, etc.) are passed to the system when possible.
 * - `blob:` navigation is intercepted and routed to the download flow rather than in-place navigation.
 * - Error callbacks surface only main-frame failures to avoid noisy subresource errors.
 */

/**
 * Imperative controller for the hosted [WebView]. Safe to use only while the associated [WebViewHost]
 * is in composition; methods no-op if the underlying view is gone.
 *
 * - [loadUrl] applies the current User-Agent and an `Accept-Language` header derived from [WebViewConfig].
 * - [clearBrowsingData] clears cache, history, WebStorage, and all cookies; completion is reported asynchronously.
 * - [recreateWebView] requests a full teardown and fresh [WebView] instance on the next recomposition.
 * - [requestedWithHeaderMode] reports the current suppression mode for the `X-Requested-With` header.
 */
public interface WebViewController {
    /**
     * Loads the given [url] with current headers.
     */
    public fun loadUrl(url: String)

    /**
     * Reloads the current page.
     */
    public fun reload()

    /**
     * Navigates back in history if possible.
     */
    public fun goBack()

    /**
     * Navigates forward in history if possible.
     */
    public fun goForward()

    /**
     * Returns whether back navigation is possible.
     */
    public fun canGoBack(): Boolean

    /**
     * Returns whether forward navigation is possible.
     */
    public fun canGoForward(): Boolean

    /**
     * Clears cache, history, WebStorage, and all cookies. Invokes [onComplete] when finished.
     */
    public fun clearBrowsingData(onComplete: () -> Unit)

    /**
     * Schedules a full [WebView] recreation to apply configuration changes that require a new instance.
     */
    public fun recreateWebView()

    /**
     * Returns the current `X-Requested-With` header suppression mode detected for this engine.
     */
    public fun requestedWithHeaderMode(): RequestedWithHeaderMode
}


/**
 * Composable host for a configured [WebView] with robust networking, storage, and UI integration.
 *
 * @param onProgressChanged Page load progress in the range [0f, 1f].
 * @param onPageStarted Callback when a main-frame navigation starts, with the page URL.
 * @param onPageFinished Callback when a main-frame navigation finishes, with the page URL.
 * @param onTitleChanged Callback for title updates from the page.
 * @param onNavigationStateChanged Emits `[canGoBack, canGoForward]` whenever history changes.
 * @param onError Emits human-readable messages for main-frame HTTP/SSL/load errors.
 * @param onUrlChanged Emits the current main-frame URL when visited history updates.
 * @param filePickerLauncher Launcher used to complete file chooser requests from the page.
 * @param uaProvider Provider for mobile/desktop User-Agent strings.
 * @param jsCompat Injects optional JavaScript compatibility shims, respecting [WebViewConfig.jsCompatibilityMode].
 * @param config Declarative configuration for features such as JavaScript, storage, mixed content, autoplay, proxy, and dark mode.
 * @param onControllerReady Provides a [WebViewController] tied to this host’s lifecycle.
 * @param onScrollDelta Vertical scroll delta in pixels; positive when scrolling down, negative when up.
 * @param modifier Compose modifier for layout/semantics.
 */
@Composable
public fun WebViewHost(
    onProgressChanged: (Float) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onNavigationStateChanged: (Boolean, Boolean) -> Unit,
    onError: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    filePickerLauncher: ActivityResultLauncher<Intent>,
    uaProvider: UAProvider,
    jsCompat: JsCompatScriptProvider,
    config: WebViewConfig,
    onControllerReady: (WebViewController) -> Unit,
    onScrollDelta: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val downloadHandler = remember { DownloadHandler(context) }
    val fileUploadHandler = remember { FileUploadHandler(context) }
    val networkProxy = remember { NetworkProxy() }

    var latestConfig by remember { mutableStateOf(config) }
    var webViewKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(config) {
        if (shouldRecreateWebView(latestConfig, config)) webViewKey++
        latestConfig = config
    }

    LaunchedEffect(filePickerLauncher) { fileUploadHandler.initialize(filePickerLauncher) }
    LaunchedEffect(fileUploadHandler) { (context as? com.testlabs.browser.MainActivity)?.setFileUploadHandler(fileUploadHandler) }

    val webView =
        remember(webViewKey) {
            createWebView(
                context = context,
                onProgressChanged = onProgressChanged,
                onPageStarted = onPageStarted,
                onPageFinished = onPageFinished,
                onTitleChanged = onTitleChanged,
                onNavigationStateChanged = onNavigationStateChanged,
                onError = onError,
                onUrlChanged = onUrlChanged,
                downloadHandler = downloadHandler,
                fileUploadHandler = fileUploadHandler,
                uaProvider = uaProvider,
                jsCompat = jsCompat,
                config = latestConfig,
                onScrollDelta = onScrollDelta,
                networkProxy = networkProxy,
            )
        }

    DisposableEffect(webView) {
        val controller =
            object : WebViewController {
                override fun loadUrl(url: String) {
                    webView.settings.userAgentString = uaProvider.userAgent(latestConfig.desktopMode)
                    val headers = mapOf("Accept-Language" to latestConfig.acceptLanguages)
                    webView.loadUrl(url, headers)
                }
                override fun reload() = webView.reload()
                override fun goBack() = webView.goBack()
                override fun goForward() = webView.goForward()
                override fun canGoBack(): Boolean = webView.canGoBack()
                override fun canGoForward(): Boolean = webView.canGoForward()
                override fun recreateWebView() { webViewKey++ }
                override fun clearBrowsingData(onComplete: () -> Unit) {
                    runCatching {
                        webView.clearCache(true)
                        webView.clearHistory()
                        WebStorage.getInstance().deleteAllData()
                        val cm = CookieManager.getInstance()
                        cm.removeAllCookies { cm.flush(); onComplete() }
                    }.onFailure {
                        Log.e(TAG, "Error clearing data", it)
                        onComplete()
                    }
                }
                override fun requestedWithHeaderMode(): RequestedWithHeaderMode = requestedWithHeaderModeOf(webView)
            }
        onControllerReady(controller)
        onDispose {
            try {
                webView.stopLoading()
                webView.loadUrl("about:blank")
            } finally {
                webView.destroy()
            }
        }
    }

    AndroidView(factory = { webView }, modifier = modifier)
    LaunchedEffect(config) { webView.applyConfig(config, uaProvider, jsCompat) }
}

private fun shouldRecreateWebView(oldConfig: WebViewConfig, newConfig: WebViewConfig): Boolean {
    return oldConfig.javascriptEnabled != newConfig.javascriptEnabled ||
            oldConfig.domStorageEnabled != newConfig.domStorageEnabled ||
            oldConfig.mixedContentAllowed != newConfig.mixedContentAllowed ||
            oldConfig.fileAccessEnabled != newConfig.fileAccessEnabled ||
            oldConfig.mediaAutoplayEnabled != newConfig.mediaAutoplayEnabled ||
            oldConfig.proxyEnabled != newConfig.proxyEnabled ||
            oldConfig.forceDarkMode != newConfig.forceDarkMode
}

@SuppressLint("ViewConstructor")
private class ObservableWebView(
    context: Context,
    private val onScrollDelta: (Int) -> Unit,
) : WebView(context) {
    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
        super.onScrollChanged(l, t, oldl, oldt)
        onScrollDelta(t - oldt)
    }
}

@SuppressLint("SetJavaScriptEnabled", "RequiresFeature")
private fun createWebView(
    context: Context,
    onProgressChanged: (Float) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onNavigationStateChanged: (Boolean, Boolean) -> Unit,
    onError: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    downloadHandler: DownloadHandler,
    fileUploadHandler: FileUploadHandler,
    uaProvider: UAProvider,
    jsCompat: JsCompatScriptProvider,
    config: WebViewConfig,
    onScrollDelta: (Int) -> Unit,
    networkProxy: NetworkProxy,
): WebView =
    ObservableWebView(context, onScrollDelta).apply {
        configureSettings(uaProvider, jsCompat, config)
        setupWebViewClient(onPageStarted, onPageFinished, onNavigationStateChanged, onError, onUrlChanged, downloadHandler, networkProxy, uaProvider, config)
        setupWebChromeClient(onProgressChanged, onTitleChanged, fileUploadHandler)
        setupDownloadManager(downloadHandler)
        ServiceWorkerControllerCompat.getInstance().setServiceWorkerClient(object : ServiceWorkerClientCompat() {
            override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                return networkProxy.interceptRequest(
                    request,
                    config.customUserAgent ?: uaProvider.userAgent(config.desktopMode),
                    config.acceptLanguages,
                    config.proxyEnabled
                )
            }
        })
    }

private fun WebView.applyConfig(
    config: WebViewConfig,
    uaProvider: UAProvider,
    jsCompat: JsCompatScriptProvider,
) {
    @SuppressLint("RestrictedApi", "RequiresFeature")
    fun applyInternal() {
        settings.apply {
            javaScriptEnabled = config.javascriptEnabled
            domStorageEnabled = config.domStorageEnabled
            allowFileAccess = config.fileAccessEnabled
            allowContentAccess = config.fileAccessEnabled
            mixedContentMode = if (config.mixedContentAllowed) WebSettings.MIXED_CONTENT_ALWAYS_ALLOW else WebSettings.MIXED_CONTENT_NEVER_ALLOW
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportMultipleWindows(true)
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            userAgentString = config.customUserAgent ?: uaProvider.userAgent(config.desktopMode)
            loadsImagesAutomatically = true
            blockNetworkImage = false
            blockNetworkLoads = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = !config.mediaAutoplayEnabled
            javaScriptCanOpenWindowsAutomatically = true
            setGeolocationEnabled(true)
        }
        runCatching { WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, config.forceDarkMode) }
        applyRequestedWithHeaderSuppression()
        jsCompat.apply(this@applyConfig, config.acceptLanguages, config.jsCompatibilityMode)
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(this@applyConfig, true)
        }
    }
    applyInternal()
}

@SuppressLint("WebViewFeature", "RequiresFeature")
private fun WebView.applyRequestedWithHeaderSuppression() {
    runCatching { WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet()) }
    runCatching {
        val sw = ServiceWorkerControllerCompat.getInstance().serviceWorkerWebSettings
        sw.requestedWithHeaderOriginAllowList = emptySet()
    }
}

private fun WebView.configureSettings(
    uaProvider: UAProvider,
    jsCompat: JsCompatScriptProvider,
    config: WebViewConfig,
) { applyConfig(config, uaProvider, jsCompat) }

private fun WebView.setupWebViewClient(
    onPageStarted: (String) -> Unit,
    onPageFinished: (String) -> Unit,
    onNavigationStateChanged: (Boolean, Boolean) -> Unit,
    onError: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    downloadHandler: DownloadHandler,
    networkProxy: NetworkProxy,
    uaProvider: UAProvider,
    config: WebViewConfig,
) {
    webViewClient =
        object : WebViewClient() {
            override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                return request?.let { req ->
                    networkProxy.interceptRequest(
                        req,
                        config.customUserAgent ?: uaProvider.userAgent(config.desktopMode),
                        config.acceptLanguages,
                        config.proxyEnabled
                    )
                } ?: super.shouldInterceptRequest(view, request)
            }
            override fun shouldInterceptRequest(view: WebView?, url: String?): WebResourceResponse? = null
            override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let { onPageStarted(it) }
            }
            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let { onPageFinished(it) }
                view?.let { onNavigationStateChanged(it.canGoBack(), it.canGoForward()) }
            }
            override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
                super.doUpdateVisitedHistory(view, url, isReload)
                url?.let {
                    onUrlChanged(it)
                    view?.let { v -> onNavigationStateChanged(v.canGoBack(), v.canGoForward()) }
                }
            }
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true && error != null) onError("Error loading page: ${error.description}")
            }
            override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
                handler?.cancel()
                error?.let {
                    val msg = when (it.primaryError) {
                        android.net.http.SslError.SSL_UNTRUSTED -> "Certificate not trusted"
                        android.net.http.SslError.SSL_EXPIRED -> "Certificate expired"
                        android.net.http.SslError.SSL_IDMISMATCH -> "Certificate hostname mismatch"
                        android.net.http.SslError.SSL_NOTYETVALID -> "Certificate not yet valid"
                        android.net.http.SslError.SSL_DATE_INVALID -> "Certificate date invalid"
                        android.net.http.SslError.SSL_INVALID -> "Certificate invalid"
                        else -> "SSL certificate error"
                    }
                    onError("SSL Error: $msg")
                }
            }
            override fun onReceivedHttpError(view: WebView?, request: WebResourceRequest?, errorResponse: WebResourceResponse?) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request?.isForMainFrame == true && errorResponse != null) onError("HTTP Error: ${errorResponse.statusCode} ${errorResponse.reasonPhrase}")
            }
            override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                val uri = request?.url ?: return false
                val scheme = uri.scheme ?: return false
                return when (scheme) {
                    "http", "https" -> false
                    "blob" -> {
                        downloadHandler.handleDownload(uri.toString(), settings.userAgentString ?: "", "", MIME_TYPE_GUESS, this@setupWebViewClient) { err ->
                            Log.e(TAG, "Blob download error: $err")
                            onError(err)
                        }
                        true
                    }
                    "mailto", "tel", "sms" -> false
                    else -> {
                        runCatching {
                            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                        }.onFailure { Log.w(TAG, "No handler for $uri", it) }
                        true
                    }
                }
            }
        }
}

private fun WebView.setupWebChromeClient(
    onProgressChanged: (Float) -> Unit,
    onTitleChanged: (String) -> Unit,
    fileUploadHandler: FileUploadHandler,
) {
    webChromeClient =
        object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                onProgressChanged(newProgress / 100f)
            }
            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                title?.let { onTitleChanged(it) }
            }
            override fun onShowFileChooser(webView: WebView?, filePathCallback: ValueCallback<Array<Uri>>?, fileChooserParams: FileChooserParams?): Boolean =
                fileUploadHandler.handleFileChooser(filePathCallback, fileChooserParams)
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                consoleMessage?.let { msg -> Log.d("WebViewConsole", "[${msg.messageLevel()}] ${msg.message()} (${msg.sourceId()}:${msg.lineNumber()})") }
                return true
            }
        }
}

private fun WebView.setupDownloadManager(downloadHandler: DownloadHandler) {
    setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
        downloadHandler.handleDownload(
            url = url,
            userAgent = userAgent,
            contentDisposition = contentDisposition,
            mimeType = mimeType,
            webView = this,
            onError = { error ->
                Log.e(TAG, "Download failed: $error")
                post { Log.e(TAG, "Posting download error to main thread: $error") }
            },
        )
    }
}
