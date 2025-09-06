package com.testlabs.browser.ui.browser

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.CookieManager
import android.webkit.DownloadListener
import android.webkit.MimeTypeMap
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
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
import androidx.core.net.toUri
import androidx.webkit.ServiceWorkerClientCompat
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.testlabs.browser.domain.settings.AcceptLanguageMode
import com.testlabs.browser.domain.settings.WebViewConfig
import com.testlabs.browser.js.JsBridge
import com.testlabs.browser.webview.BrowserWebViewClient
import org.koin.compose.getKoin
import org.koin.core.parameter.parametersOf
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

private const val TAG = "WebViewHost"

/**
 * Compose host for a single-tab WebView that mirrors Chrome Mobile behavior.
 *
 * This host:
 * - Configures WebView for full browsing capabilities.
 * - Wires a WebViewClient that injects compat JS at `document_start` and proxies only
 *   subresources via NetworkProxy, leaving main-frame HTML to WebView.
 * - Exposes UI callbacks for progress, titles, navigation, and errors.
 * - Integrates file uploads through an ActivityResultLauncher<Intent>.
 * - Mirrors request headers and cookie behavior for fingerprinting parity.
 */
@Composable
public fun WebViewHost(
    modifier: Modifier = Modifier,
    onProgressChanged: (Float) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onNavigationStateChanged: (Boolean, Boolean) -> Unit,
    onError: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    filePickerLauncher: ActivityResultLauncher<Intent>?,
    uaProvider: UAProvider,
    jsCompat: JsCompatScriptProvider,
    config: WebViewConfig,
    onControllerReady: (WebViewController) -> Unit,
    onScrollDelta: (Int) -> Unit,
) {
    val koin = getKoin()
    val networkProxy: NetworkProxy = remember(config) { koin.get(parameters = { parametersOf(config) }) }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }

    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier,
        factory = { ctx ->
            val wv = WebView(ctx)
            webViewRef = wv
            setupWebViewDefaults(wv)

            val controller = RealWebViewController(wv, networkProxy)
            onControllerReady(controller)

            applyFullWebViewConfiguration(
                webView = wv,
                config = config,
                uaProvider = uaProvider,
                jsCompat = jsCompat,
                networkProxy = networkProxy,
                onTitle = { title -> onTitleChanged(title ?: "") },
                onProgress = { p ->
                    progress = p
                    onProgressChanged(p / 100f)
                },
                onUrlChange = { url -> onUrlChanged(url) },
                onPageStarted = onPageStarted,
                onPageFinished = onPageFinished,
                onNavState = { b, f -> onNavigationStateChanged(b, f) },
                onError = onError,
                filePickerLauncher = filePickerLauncher
            )

            wv.setOnScrollChangeListener { _, _, y, _, oldY ->
                onScrollDelta(y - oldY)
            }
            wv
        },
        update = { webView ->
            webView.applyConfig(
                config = config,
                uaProvider = uaProvider,
                jsCompat = jsCompat,
                networkProxy = networkProxy
            )
            onNavigationStateChanged(webView.canGoBack(), webView.canGoForward())
        },
        onRelease = { webView ->
            try {
                runCatching { webView.stopLoading() }
                webView.webChromeClient = WebChromeClient()
                webView.webViewClient = object : WebViewClient() {}
                webView.destroy()
            } catch (_: Throwable) {}
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            try {
                webViewRef?.let { wv ->
                    runCatching { wv.stopLoading() }
                    wv.webChromeClient = WebChromeClient()
                    wv.webViewClient = object : WebViewClient() {}
                    wv.destroy()
                }
            } catch (_: Throwable) {}
        }
    }

    LaunchedEffect(progress) { onProgressChanged(progress / 100f) }
}

/**
 * Applies immutable defaults for a fresh WebView instance.
 */
@SuppressLint("SetJavaScriptEnabled")
private fun setupWebViewDefaults(webView: WebView) {
    val s = webView.settings
    s.javaScriptEnabled = true
    s.domStorageEnabled = true
    s.databaseEnabled = true
    s.allowFileAccess = true
    s.allowContentAccess = true
    s.loadsImagesAutomatically = true
    s.mediaPlaybackRequiresUserGesture = false
    s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
    s.cacheMode = WebSettings.LOAD_DEFAULT
    s.setSupportZoom(true)
    s.builtInZoomControls = false
    s.displayZoomControls = false
    s.useWideViewPort = true
    s.loadWithOverviewMode = true
}

/**
 * Reconfigures a live WebView when configuration or networking changes.
 */
private fun WebView.applyConfig(
    config: WebViewConfig,
    uaProvider: UAProvider,
    jsCompat: JsCompatScriptProvider,
    networkProxy: NetworkProxy
) {
    val s = settings
    val ua = uaProvider.userAgent(desktop = false)
    s.userAgentString = ua

    val acceptLanguage = when (config.acceptLanguageMode) {
        AcceptLanguageMode.Baseline -> "en-US,en;q=0.9"
        AcceptLanguageMode.DeviceList -> buildDeviceAcceptLanguage()
    }

    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(this, config.enableThirdPartyCookies)

    val jsBridge: JsBridge = object : JsBridge(ua = uaProvider) {
        override fun script(): String = getDocStartScript(jsCompat)
    }

    webViewClient = BrowserWebViewClient(
        proxy = networkProxy,
        jsBridge = jsBridge,
        uaProvider = uaProvider,
        acceptLanguage = acceptLanguage
    )

    if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)) {
        runCatching {
            val controller = ServiceWorkerControllerCompat.getInstance()
            controller.setServiceWorkerClient(object : ServiceWorkerClientCompat() {
                override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                    val uaNow = uaProvider.userAgent(desktop = false)
                    return networkProxy.interceptRequest(
                        request = request,
                        userAgent = uaNow,
                        acceptLanguage = acceptLanguage,
                        proxyEnabled = true
                    )
                }
            })
        }
    }

    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
        WebSettingsCompat.setForceDark(
            s,
            if (config.forceDarkMode) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
        )
    }
}

/**
 * Installs full configuration, clients, document_start script and file upload handling.
 */
@SuppressLint("SetJavaScriptEnabled")
private fun applyFullWebViewConfiguration(
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
    val s = webView.settings
    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookieManager.setAcceptThirdPartyCookies(webView, config.enableThirdPartyCookies)

    val ua = uaProvider.userAgent(desktop = false)
    s.userAgentString = ua

    val acceptLanguage = when (config.acceptLanguageMode) {
        AcceptLanguageMode.Baseline -> "en-US,en;q=0.9"
        AcceptLanguageMode.DeviceList -> buildDeviceAcceptLanguage()
    }

    val jsBridge: JsBridge = object : JsBridge(ua = uaProvider) {
        override fun script(): String = getDocStartScript(jsCompat)
    }

    webView.webViewClient = object : BrowserWebViewClient(
        proxy = networkProxy,
        jsBridge = jsBridge,
        uaProvider = uaProvider,
        acceptLanguage = acceptLanguage
    ) {
        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            onProgress(0)
            url?.let {
                onUrlChange(it)
                onPageStarted(it)
            }
        }

        override fun onPageFinished(view: WebView, url: String?) {
            super.onPageFinished(view, url)
            onProgress(100)
            onTitle(view.title)
            onNavState(view.canGoBack(), view.canGoForward())
            url?.let { onPageFinished(it) }
        }

        override fun onReceivedError(
            view: WebView,
            request: WebResourceRequest,
            error: android.webkit.WebResourceError
        ) {
            onError("${error.errorCode}:${error.description}")
        }
    }

    webView.webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView, newProgress: Int) {
            onProgress(newProgress)
        }
        override fun onReceivedTitle(view: WebView, title: String?) {
            onTitle(title)
        }
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            fileCallbackRef.set(filePathCallback)
            val intent = fileChooserParams?.createIntent() ?: Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
            }
            return try {
                filePickerLauncher?.launch(intent) ?: false
            } catch (_: ActivityNotFoundException) {
                fileCallbackRef.getAndSet(null)?.onReceiveValue(null)
                false
            } as Boolean
        }
    }

    webView.setDownloadListener(
        DownloadListener { url, _, _, mimetype, _ ->
            try {
                val uri = url?.toUri() ?: return@DownloadListener
                val mt = mimetype ?: guessMimeType(uri.toString())
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(uri, mt)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                webView.context.startActivity(intent)
            } catch (_: ActivityNotFoundException) {}
        }
    )

    if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)) {
        runCatching {
            val controller = ServiceWorkerControllerCompat.getInstance()
            controller.setServiceWorkerClient(object : ServiceWorkerClientCompat() {
                override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                    val uaNow = uaProvider.userAgent(desktop = false)
                    return networkProxy.interceptRequest(
                        request = request,
                        userAgent = uaNow,
                        acceptLanguage = acceptLanguage,
                        proxyEnabled = true
                    )
                }
            })
        }
    }
}

/**
 * Builds a Chrome-like Accept-Language header from device locales.
 */
private fun buildDeviceAcceptLanguage(): String {
    val tags = runCatching {
        val locales = android.os.LocaleList.getDefault()
        (0 until locales.size()).map { locales[it].toLanguageTag() }.filter { it.isNotBlank() }
    }.getOrElse { listOf(Locale.getDefault().toLanguageTag()) }
    if (tags.isEmpty()) return "en-US,en;q=0.9"

    val uniq = LinkedHashSet<String>()
    tags.forEach { tag ->
        val norm = tag.ifBlank { "en-US" }
        uniq.add(norm)
        val base = norm.substringBefore('-', norm)
        if (base.isNotBlank()) uniq.add(base)
    }
    val ordered = uniq.take(4)
    return ordered.mapIndexed { idx, t ->
        val q = when (idx) {
            0 -> null
            1 -> "0.9"
            2 -> "0.8"
            else -> "0.7"
        }
        if (q == null) t else "$t;q=$q"
    }.joinToString(",")
}

/**
 * Tolerant extractor: returns the JS to inject at document_start from JsCompatScriptProvider.
 */
private fun getDocStartScript(provider: JsCompatScriptProvider): String {
    val candidateNames = listOf("script", "provide", "source", "get", "value")
    val m = provider.javaClass.methods.firstOrNull {
        it.parameterTypes.isEmpty() && it.returnType == String::class.java && it.name in candidateNames
    }
    return try {
        (m?.invoke(provider) as? String)?.trim().orEmpty()
    } catch (_: Throwable) {
        ""
    }
}

/**
 * Forwards file chooser results back to the pending ValueCallback held in a static reference.
 */
public fun onFileChooserResult(uris: Array<Uri>?) {
    fileCallbackRef.getAndSet(null)?.onReceiveValue(uris)
}

private val fileCallbackRef: AtomicReference<ValueCallback<Array<Uri>>?> = AtomicReference(null)

/**
 * Real WebView controller implementation.
 */
public class RealWebViewController(
    private val webView: WebView,
    private val proxy: NetworkProxy
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
    override fun recreateWebView() {}
    override fun clearBrowsingData(done: () -> Unit) {
        CookieManager.getInstance().removeAllCookies { done() }
        webView.clearCache(true)
        webView.clearHistory()
    }
    override fun requestedWithHeaderMode(): RequestedWithHeaderMode = RequestedWithHeaderMode.UNKNOWN
    override fun proxyStackName(): String = proxy.stackName
}

/**
 * Best-effort MIME type detection when the server type is missing.
 */
private fun guessMimeType(url: String): String {
    val ext = MimeTypeMap.getFileExtensionFromUrl(url)
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "application/octet-stream"
}
