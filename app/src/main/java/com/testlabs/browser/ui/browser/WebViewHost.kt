package com.testlabs.browser.ui.browser

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Message
import android.util.Log
import android.webkit.ConsoleMessage
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
import androidx.webkit.WebViewFeature
import com.testlabs.browser.domain.settings.AcceptLanguageMode
import com.testlabs.browser.domain.settings.WebViewConfig
import com.testlabs.browser.network.UserAgentProvider
import org.koin.compose.koinInject
import androidx.core.net.toUri
import java.util.concurrent.atomic.AtomicReference

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
    /** Loads the given [url] with current headers. */
    public fun loadUrl(url: String)

    /** Reloads the current page. */
    public fun reload()

    /** Navigates back in history if possible. */
    public fun goBack()

    /** Navigates forward in history if possible. */
    public fun goForward()

    /** Returns whether back navigation is possible. */
    public fun canGoBack(): Boolean

    /** Returns whether forward navigation is possible. */
    public fun canGoForward(): Boolean

    /** Clears cache, history, WebStorage, and all cookies. Invokes [onComplete] when finished. */
    public fun clearBrowsingData(onComplete: () -> Unit)

    /** Schedules a full [WebView] recreation to apply configuration changes that require a new instance. */
    public fun recreateWebView()

    /** Returns the current `X-Requested-With` header suppression mode detected for this engine. */
    public fun requestedWithHeaderMode(): RequestedWithHeaderMode

    /** Returns the name of the active proxy stack. */
    public fun proxyStackName(): String
}

public interface FileUploadHandlerOwner {
    public fun attachFileUploadHandler(handler: FileUploadHandler)
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
    val networkProxy: NetworkProxy = koinInject()

    var latestConfig by remember { mutableStateOf(config) }
    var webViewKey by remember { mutableIntStateOf(0) }

    LaunchedEffect(config) {
        if (shouldRecreateWebView(latestConfig, config)) webViewKey++
        latestConfig = config
    }

    LaunchedEffect(filePickerLauncher) { fileUploadHandler.initialize(filePickerLauncher) }
    LaunchedEffect(fileUploadHandler) {
        (context as? FileUploadHandlerOwner)?.attachFileUploadHandler(fileUploadHandler)
    }

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
                    webView.settings.userAgentString =
                        uaProvider.userAgent(latestConfig.desktopMode)
                    (webView as? ObservableWebView)?.updateUserAgentSnapshot(webView.settings.userAgentString)
                    val headers = mapOf("Accept-Language" to latestConfig.acceptLanguages)
                    (webView as? ObservableWebView)?.updateAcceptLanguageSnapshot(headers["Accept-Language"].orEmpty())
                    if (url.contains("browserscan.net") || url.contains("tls.peet.ws") || url.contains(
                            "httpbin.org"
                        )
                    ) {
                        ProxyValidator.validateProxyHealth(webView)
                    }
                    webView.loadUrl(url, headers)
                }

                override fun reload() = webView.reload()
                override fun goBack() = webView.goBack()
                override fun goForward() = webView.goForward()
                override fun canGoBack(): Boolean = webView.canGoBack()
                override fun canGoForward(): Boolean = webView.canGoForward()
                override fun recreateWebView() {
                    webViewKey++
                }

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

                override fun requestedWithHeaderMode(): RequestedWithHeaderMode =
                    requestedWithHeaderModeOf(webView)

                override fun proxyStackName(): String = networkProxy.stackName
            }

        ProxyValidator.logProxyStatus()

        onControllerReady(controller)
        onDispose {
            Log.d(TAG, "Disposing WebView")
            webView.destroy()
        }
    }

    AndroidView(factory = { webView }, modifier = modifier)
    LaunchedEffect(config) {
        webView.applyConfig(config, uaProvider, jsCompat)
        (webView as? ObservableWebView)?.updateUserAgentSnapshot(webView.settings.userAgentString)
        val acceptLanguage = when (config.acceptLanguageMode) {
            AcceptLanguageMode.Baseline -> "en-US,en;q=0.9"
            AcceptLanguageMode.DeviceList -> generateDeviceLanguageList()
        }
        (webView as? ObservableWebView)?.updateAcceptLanguageSnapshot(acceptLanguage)
    }
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

/**
 * WebView subclass that reports scroll deltas and holds thread-safe snapshots
 * of headers needed on worker-thread interception callbacks.
 */
@SuppressLint("ViewConstructor")
private class ObservableWebView(
    context: Context,
    private val onScrollDelta: (Int) -> Unit,
) : WebView(context) {

    /** Thread-safe snapshot of the current User-Agent for worker-thread use. */
    private val userAgentRef = AtomicReference("")

    /** Thread-safe snapshot of the current Accept-Language for worker-thread use. */
    private val acceptLanguageRef = AtomicReference("")

    /** Updates the cached User-Agent from the UI thread. */
    fun updateUserAgentSnapshot(value: String) {
        userAgentRef.set(value)
    }

    /** Updates the cached Accept-Language from the UI thread. */
    fun updateAcceptLanguageSnapshot(value: String) {
        acceptLanguageRef.set(value)
    }

    /** Returns the cached User-Agent safe for worker-thread access. */
    fun currentUserAgent(): String = userAgentRef.get()

    /** Returns the cached Accept-Language safe for worker-thread access. */
    fun currentAcceptLanguage(): String = acceptLanguageRef.get()

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
    try {
        Log.d(TAG, "Creating WebView with enhanced safety measures...")

        ObservableWebView(context, onScrollDelta).apply {
            try {
                Log.d(TAG, "Setting up basic WebView configuration...")

                settings.apply {
                    javaScriptEnabled = false
                    domStorageEnabled = false
                    allowFileAccess = false
                    allowContentAccess = false
                    allowFileAccessFromFileURLs = false
                    allowUniversalAccessFromFileURLs = false
                    useWideViewPort = true
                    loadWithOverviewMode = true
                    setSupportZoom(false)
                    builtInZoomControls = false
                    displayZoomControls = false
                    mediaPlaybackRequiresUserGesture = true
                    javaScriptCanOpenWindowsAutomatically = false
                    setGeolocationEnabled(false)
                    cacheMode = WebSettings.LOAD_NO_CACHE
                    blockNetworkImage = true
                    blockNetworkLoads = true
                    userAgentString = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36"
                }

                updateUserAgentSnapshot(settings.userAgentString)
                updateAcceptLanguageSnapshot("en-US,en;q=0.9")

                Log.d(TAG, "Basic WebView settings applied successfully")

                loadUrl("about:blank")

                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        super.onPageFinished(view, url)
                        if (url == "about:blank") {
                            Log.d(TAG, "WebView renderer initialized successfully")
                            post {
                                try {
                                    configureWebViewSafely(
                                        config,
                                        uaProvider,
                                        jsCompat,
                                        onProgressChanged,
                                        onPageStarted,
                                        onPageFinished,
                                        onTitleChanged,
                                        onNavigationStateChanged,
                                        onError,
                                        onUrlChanged,
                                        downloadHandler,
                                        fileUploadHandler,
                                        networkProxy
                                    )
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error during full WebView configuration", e)
                                    onError("WebView configuration failed: ${e.message}")
                                }
                            }
                        }
                    }

                    override fun onReceivedError(
                        view: WebView?,
                        request: WebResourceRequest?,
                        error: WebResourceError?
                    ) {
                        Log.w(TAG, "WebView error during initialization: ${error?.description}")
                        if (request?.isForMainFrame == true) {
                            onError("WebView initialization error: ${error?.description}")
                        }
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error during basic WebView setup", e)
                onError("Failed to initialize WebView: ${e.message}")
            }
        }
    } catch (e: Exception) {
        Log.e(TAG, "Critical error creating WebView", e)

        ObservableWebView(context, onScrollDelta).apply {
            loadUrl("about:blank")
            webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView?, url: String?) {
                    onError("WebView creation failed - using minimal fallback")
                }
            }
        }
    }

@SuppressLint("RequiresFeature")
private fun WebView.configureWebViewSafely(
    config: WebViewConfig,
    uaProvider: UAProvider,
    jsCompat: JsCompatScriptProvider,
    onProgressChanged: (Float) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onNavigationStateChanged: (Boolean, Boolean) -> Unit,
    onError: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    downloadHandler: DownloadHandler,
    fileUploadHandler: FileUploadHandler,
    networkProxy: NetworkProxy,
) {
    try {
        Log.d(TAG, "Applying full WebView configuration with Chrome Mobile compatibility...")

        val userAgentProvider = UserAgentProvider()
        val chromeInjector = ChromeCompatibilityInjector(userAgentProvider)

        settings.apply {
            javaScriptEnabled = config.javascriptEnabled
            domStorageEnabled = config.domStorageEnabled
            allowFileAccess = config.fileAccessEnabled
            allowContentAccess = config.fileAccessEnabled
            mixedContentMode =
                if (config.mixedContentAllowed) WebSettings.MIXED_CONTENT_ALWAYS_ALLOW else WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            userAgentString = config.customUserAgent ?: userAgentProvider.getChromeStableMobileUA()
            loadsImagesAutomatically = true
            blockNetworkImage = false
            blockNetworkLoads = false
            cacheMode = WebSettings.LOAD_DEFAULT
            mediaPlaybackRequiresUserGesture = !config.mediaAutoplayEnabled
            javaScriptCanOpenWindowsAutomatically = true
            setGeolocationEnabled(true)
            databaseEnabled = config.domStorageEnabled
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            setSupportMultipleWindows(true)
        }

        val acceptLanguage = when (config.acceptLanguageMode) {
            AcceptLanguageMode.Baseline -> "en-US,en;q=0.9"
            AcceptLanguageMode.DeviceList -> generateDeviceLanguageList()
        }

        (this as? ObservableWebView)?.updateUserAgentSnapshot(settings.userAgentString)
        (this as? ObservableWebView)?.updateAcceptLanguageSnapshot(acceptLanguage)

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        if (config.enableThirdPartyCookies) {
            cookieManager.setAcceptThirdPartyCookies(this, true)
        }
        cookieManager.flush()

        if (config.suppressXRequestedWith) {
            try {
                if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
                    try {
                        WebSettingsCompat.setRequestedWithHeaderOriginAllowList(
                            settings,
                            emptySet()
                        )
                        Log.d(TAG, "Cleared X-Requested-With allow list via WebView")
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed clearing X-Requested-With allow list", e)
                    }
                } else {
                    Log.d(
                        TAG,
                        "X-Requested-With suppression feature not supported, relying on proxy"
                    )
                }
            } catch (e: Exception) {
                Log.w(
                    TAG,
                    "Could not suppress X-Requested-With via WebViewFeature, relying on proxy",
                    e
                )
            }
        }

        try {
            if (config.forceDarkMode && WebViewFeature.isFeatureSupported(WebViewFeature.ALGORITHMIC_DARKENING)) {
                WebSettingsCompat.setAlgorithmicDarkeningAllowed(settings, true)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Could not set algorithmic darkening", e)
        }

        webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                super.onProgressChanged(view, newProgress)
                onProgressChanged(newProgress / 100f)
            }

            override fun onReceivedTitle(view: WebView?, title: String?) {
                super.onReceivedTitle(view, title)
                title?.let(onTitleChanged)
            }

            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?
            ): Boolean {
                return fileUploadHandler.onShowFileChooser(
                    webView,
                    filePathCallback,
                    fileChooserParams
                )
            }

            override fun onCreateWindow(
                view: WebView?,
                isDialog: Boolean,
                isUserGesture: Boolean,
                resultMsg: Message?
            ): Boolean {
                Log.d(TAG, "onCreateWindow - handling popup in same WebView")
                return false
            }

            override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                consoleMessage?.let { msg ->
                    val level = when (msg.messageLevel()) {
                        ConsoleMessage.MessageLevel.ERROR -> "E"
                        ConsoleMessage.MessageLevel.WARNING -> "W"
                        ConsoleMessage.MessageLevel.DEBUG -> "D"
                        ConsoleMessage.MessageLevel.LOG -> "I"
                        ConsoleMessage.MessageLevel.TIP -> "I"
                        else -> "I"
                    }
                    Log.println(
                        when (level) {
                            "E" -> Log.ERROR
                            "W" -> Log.WARN
                            "D" -> Log.DEBUG
                            else -> Log.INFO
                        },
                        "WebConsole",
                        "[${msg.sourceId()}:${msg.lineNumber()}] ${msg.message()}"
                    )
                    val message = msg.message()
                    if (message.contains("ERR_CONTENT_DECODING_FAILED", true) ||
                        message.contains("IP acquisition failed", true) ||
                        message.contains("fetch", true) && message.contains("failed", true)
                    ) {
                        Log.e(TAG, "🚨 CRITICAL: ${msg.message()}")
                    }
                }
                return true
            }
        }

        webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                super.onPageStarted(view, url, favicon)
                url?.let {
                    Log.d(TAG, "Page started: $it")
                    onPageStarted(it)
                    onUrlChanged(it)
                    onNavigationStateChanged(canGoBack(), canGoForward())
                }
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                url?.let {
                    Log.d(TAG, "Page finished: $it")
                    if (config.chromeCompatibilityEnabled && config.javascriptEnabled) {
                        val chromeScript = chromeInjector.generateChromeCompatibilityScript()
                        evaluateJavascript(chromeScript) { result ->
                            Log.d(TAG, "Chrome compatibility script executed: $result")
                        }
                    }
                    onPageFinished(it)
                    onNavigationStateChanged(canGoBack(), canGoForward())
                }
            }

            override fun shouldInterceptRequest(
                view: WebView?,
                request: WebResourceRequest?
            ): WebResourceResponse? {
                request ?: return null

                Log.d(TAG, "=== INTERCEPT REQUEST ===")
                Log.d(TAG, "URL: ${request.url}")
                Log.d(TAG, "Method: ${request.method}")
                Log.d(TAG, "IsMainFrame: ${request.isForMainFrame}")
                Log.d(TAG, "Headers: ${request.requestHeaders.keys}")

                val userAgent =
                    (this@configureWebViewSafely as? ObservableWebView)?.currentUserAgent()
                        .orEmpty()
                val acceptLanguage =
                    (this@configureWebViewSafely as? ObservableWebView)?.currentAcceptLanguage()
                        .orEmpty()

                return networkProxy.interceptRequest(
                    request = request,
                    userAgent = userAgent,
                    acceptLanguage = acceptLanguage,
                    proxyEnabled = config.proxyEnabled
                )
            }

            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?
            ) {
                super.onReceivedError(view, request, error)
                if (request?.isForMainFrame == true) {
                    val errorMsg = "Error ${error?.errorCode}: ${error?.description}"
                    Log.e(TAG, "Main frame error: $errorMsg")
                    onError(errorMsg)
                }
            }

            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: SslError?
            ) {
                Log.w(TAG, "SSL Error: ${error?.toString()}")
                handler?.cancel()
                if (error != null) {
                    val errorType = when (error.primaryError) {
                        SslError.SSL_EXPIRED -> "Certificate expired"
                        SslError.SSL_IDMISMATCH -> "Hostname mismatch"
                        SslError.SSL_NOTYETVALID -> "Certificate not yet valid"
                        SslError.SSL_UNTRUSTED -> "Certificate authority not trusted"
                        SslError.SSL_DATE_INVALID -> "Certificate date invalid"
                        SslError.SSL_INVALID -> "Generic SSL error"
                        else -> "Unknown SSL error"
                    }
                    onError("SSL Error: $errorType")
                }
            }

            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                val url = request?.url?.toString() ?: return false
                when {
                    url.startsWith("mailto:") || url.startsWith("tel:") || url.startsWith("sms:") -> {
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                            context.startActivity(intent)
                            return true
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not handle external scheme: $url", e)
                        }
                    }

                    url.startsWith("blob:") -> {
                        downloadHandler.handleBlobDownload(url)
                        return true
                    }
                }
                return false
            }
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)) {
            try {
                val uaRef =
                    (this as? ObservableWebView)?.let { AtomicReference(it.currentUserAgent()) }
                        ?: AtomicReference("")
                val alRef =
                    (this as? ObservableWebView)?.let { AtomicReference(it.currentAcceptLanguage()) }
                        ?: AtomicReference("")
                val serviceWorkerController = ServiceWorkerControllerCompat.getInstance()
                serviceWorkerController.setServiceWorkerClient(object :
                    ServiceWorkerClientCompat() {
                    override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                        Log.d(TAG, "ServiceWorker request: ${request.url}")
                        val userAgent = uaRef.get()
                        val acceptLanguage = alRef.get()
                        return networkProxy.interceptRequest(
                            request = request,
                            userAgent = userAgent,
                            acceptLanguage = acceptLanguage,
                            proxyEnabled = config.proxyEnabled
                        )
                    }
                })
            } catch (e: Exception) {
                Log.w(TAG, "Could not configure ServiceWorker interception", e)
            }
        }

        setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
            downloadHandler.handleDownload(
                url,
                userAgent,
                contentDisposition,
                mimeType,
                contentLength
            )
        }

        Log.d(TAG, "WebView configuration completed successfully")

    } catch (e: Exception) {
        Log.e(TAG, "Error during WebView configuration", e)
        onError("Configuration error: ${e.message}")
    }
}

private fun generateDeviceLanguageList(): String {
    val locales = android.os.LocaleList.getDefault()
    return if (locales.size() > 0) {
        val languages = mutableListOf<String>()
        for (i in 0 until minOf(locales.size(), 3)) {
            val locale = locales[i]
            val lang = locale.language
            val country = locale.country
            if (country.isNotEmpty()) {
                languages.add("$lang-$country")
            }
            if (!languages.contains(lang)) {
                languages.add(lang)
            }
        }
        languages.mapIndexed { index, lang ->
            when (index) {
                0 -> lang
                1 -> "$lang;q=0.9"
                2 -> "$lang;q=0.8"
                else -> "$lang;q=0.7"
            }
        }.joinToString(",")
    } else {
        "en-US,en;q=0.9"
    }
}
