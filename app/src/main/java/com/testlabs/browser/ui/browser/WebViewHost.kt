package com.testlabs.browser.ui.browser

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Message
import android.webkit.CookieManager
import android.webkit.MimeTypeMap
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.util.Log
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
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.webkit.ServiceWorkerClientCompat
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.testlabs.browser.domain.settings.AcceptLanguageMode
import com.testlabs.browser.domain.settings.WebViewConfig
import com.testlabs.browser.js.JsBridge
import org.koin.compose.getKoin
import org.koin.core.parameter.parametersOf
import java.util.Locale
import java.util.concurrent.atomic.AtomicReference

private const val TAG = "WebViewHost"

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
) {
    val koin = getKoin()
    val networkProxy: NetworkProxy = remember(config) { koin.get(parameters = { parametersOf(config) }) }

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var controllerRef by remember { mutableStateOf<RealWebViewController?>(null) }

    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier.pointerInteropFilter { false },
        factory = { ctx ->
            val wv = WebView(ctx)
            webViewRef = wv
            setupWebViewDefaults(wv)

            val controller = RealWebViewController(wv, networkProxy, config)
            controllerRef = controller
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
            wv
        },
        update = { webView ->

            controllerRef?.updateConfig(config)

            applyFullWebViewConfiguration(
                webView = webView,
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

            val newUserAgent = config.customUserAgent ?: uaProvider.userAgent(desktop = config.desktopMode)
            if (webView.settings.userAgentString != newUserAgent) {
                webView.settings.userAgentString = newUserAgent
            }

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
    s.supportMultipleWindows()
    s.javaScriptCanOpenWindowsAutomatically = true

    webView.isVerticalScrollBarEnabled = true
    webView.isClickable = true
    webView.isFocusable = true
    webView.isFocusableInTouchMode = true

    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true)
}

private fun WebView.applyConfig(
    config: WebViewConfig,
    uaProvider: UAProvider,
    jsCompat: JsCompatScriptProvider,
    networkProxy: NetworkProxy,
    onUrlChange: (String) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String) -> Unit,
    onNavState: (Boolean, Boolean) -> Unit,
    onError: (String) -> Unit,
) {
    val s = settings

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

    if (config.suppressXRequestedWith &&
        WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)
    ) {
        WebSettingsCompat.setRequestedWithHeaderOriginAllowList(s, emptySet())
    }

    val ua = config.customUserAgent ?: uaProvider.userAgent(desktop = config.desktopMode)
    s.userAgentString = ua

    val acceptLanguage = when (config.acceptLanguageMode) {
        AcceptLanguageMode.Baseline -> config.acceptLanguages
        AcceptLanguageMode.DeviceList -> buildDeviceAcceptLanguage()
    }

    if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)) {
        runCatching {
            val controller = ServiceWorkerControllerCompat.getInstance()
            controller.setServiceWorkerClient(object : ServiceWorkerClientCompat() {
                override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                    if (request.isForMainFrame) return null
                    val uaNow = config.customUserAgent ?: uaProvider.userAgent(desktop = config.desktopMode)
                    return networkProxy.interceptRequest(
                        request = request,
                        userAgent = uaNow,
                        acceptLanguage = acceptLanguage,
                        proxyEnabled = config.proxyEnabled
                    )
                }
            })
        }
    }

    CookieManager.getInstance().setAcceptCookie(true)
    CookieManager.getInstance().setAcceptThirdPartyCookies(this, config.enableThirdPartyCookies)

    val jsBridge: JsBridge = object : JsBridge(ua = uaProvider) {
        override fun script(): String = if (config.jsCompatibilityMode) {
            getDocStartScript(jsCompat)
        } else {
            ""
        }
    }

    webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            super.onPageStarted(view, url, favicon)
            url?.let {
                onPageStarted(it)
                onUrlChange(it)
                onNavState(view?.canGoBack() == true, view?.canGoForward() == true)
            }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            url?.let {
                onPageFinished(it)
                onUrlChange(it)
                onNavState(view?.canGoBack() == true, view?.canGoForward() == true)
            }
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            super.doUpdateVisitedHistory(view, url, isReload)
            url?.let {
                onUrlChange(it)
                onNavState(view?.canGoBack() == true, view?.canGoForward() == true)
            }
        }

        override fun onPageCommitVisible(view: WebView?, url: String?) {
            super.onPageCommitVisible(view, url)
            url?.let {
                onUrlChange(it)
                onNavState(view?.canGoBack() == true, view?.canGoForward() == true)
            }
        }

        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
            super.onReceivedError(view, errorCode, description, failingUrl)
            onError("Error $errorCode: $description")
        }

        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            if (request?.isForMainFrame == true) {
                request.url?.toString()?.let { url -> onUrlChange(url) }
            }
            return try {
                networkProxy.interceptRequest(
                    request = request ?: return null,
                    userAgent = ua,
                    acceptLanguage = acceptLanguage,
                    proxyEnabled = config.proxyEnabled
                )
            } catch (e: Exception) {
                null
            }
        }
    }

    if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)) {
        runCatching {
            val controller = ServiceWorkerControllerCompat.getInstance()
            controller.setServiceWorkerClient(object : ServiceWorkerClientCompat() {
                override fun shouldInterceptRequest(request: WebResourceRequest): WebResourceResponse? {
                    if (request.isForMainFrame) return null
                    val uaNow = config.customUserAgent ?: uaProvider.userAgent(desktop = config.desktopMode)
                    return networkProxy.interceptRequest(
                        request = request,
                        userAgent = uaNow,
                        acceptLanguage = acceptLanguage,
                        proxyEnabled = config.proxyEnabled
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

    val cookieManager = CookieManager.getInstance()
    cookieManager.setAcceptCookie(true)
    cookieManager.setAcceptThirdPartyCookies(webView, config.enableThirdPartyCookies)

    val ua = config.customUserAgent ?: uaProvider.userAgent(desktop = config.desktopMode)
    s.userAgentString = ua

    val acceptLanguage = when (config.acceptLanguageMode) {
        AcceptLanguageMode.Baseline -> config.acceptLanguages
        AcceptLanguageMode.DeviceList -> buildDeviceAcceptLanguage()
    }

    webView.webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
            Log.d(TAG, "onPageStarted $url")
            super.onPageStarted(view, url, favicon)
            url?.let {
                onPageStarted(it)
                onUrlChange(it)
                onNavState(view?.canGoBack() == true, view?.canGoForward() == true)
            }
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            Log.d(TAG, "onPageFinished $url")
            super.onPageFinished(view, url)
            url?.let {
                onPageFinished(it)
                onUrlChange(it)
                onNavState(view?.canGoBack() == true, view?.canGoForward() == true)
            }
        }

        override fun doUpdateVisitedHistory(view: WebView?, url: String?, isReload: Boolean) {
            Log.d(TAG, "doUpdateVisitedHistory $url")
            super.doUpdateVisitedHistory(view, url, isReload)
            url?.let {
                onUrlChange(it)
                onNavState(view?.canGoBack() == true, view?.canGoForward() == true)
            }
        }

        override fun onPageCommitVisible(view: WebView?, url: String?) {
            super.onPageCommitVisible(view, url)
            url?.let {
                onUrlChange(it)
                onNavState(view?.canGoBack() == true, view?.canGoForward() == true)
            }
        }

        override fun onReceivedError(view: WebView?, errorCode: Int, description: String?, failingUrl: String?) {
            Log.e(TAG, "onReceivedError $errorCode $description")
            super.onReceivedError(view, errorCode, description, failingUrl)
            onError("Error $errorCode: $description")
        }

        override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
            if (request?.isForMainFrame == true) {
                request.url?.toString()?.let { url ->
                    onUrlChange(url)
                }
            }
            return try {
                networkProxy.interceptRequest(
                    request = request ?: return null,
                    userAgent = ua,
                    acceptLanguage = acceptLanguage,
                    proxyEnabled = config.proxyEnabled
                )
            } catch (e: Exception) {
                null
            }
        }

        override fun onRenderProcessGone(view: WebView?, detail: RenderProcessGoneDetail?): Boolean {
            Log.e(TAG, "renderProcessGone crash=${detail?.didCrash()}")
            return super.onRenderProcessGone(view, detail)
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

        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            Log.d(TAG, "onCreateWindow")
            val transport = resultMsg?.obj as? WebView.WebViewTransport ?: return false
            val popup = WebView(view?.context ?: return false)
            setupWebViewDefaults(popup)
            popup.webViewClient = object : WebViewClient() {
                override fun onPageStarted(v: WebView?, url: String?, favicon: Bitmap?) {
                    if (url != null) {
                        view.loadUrl(url)
                    }
                }
            }
            transport.webView = popup
            resultMsg.sendToTarget()
            return true
        }

        override fun onCloseWindow(window: WebView?) {}

        override fun onJsAlert(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            return super.onJsAlert(view, url, message, result)
        }

        override fun onJsConfirm(view: WebView?, url: String?, message: String?, result: JsResult?): Boolean {
            return super.onJsConfirm(view, url, message, result)
        }

        override fun onJsPrompt(view: WebView?, url: String?, message: String?, defaultValue: String?, result: JsPromptResult?): Boolean {
            return super.onJsPrompt(view, url, message, defaultValue, result)
        }

        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            val launcher = filePickerLauncher
            return if (launcher != null && filePathCallback != null) {
                fileCallbackRef.set(filePathCallback)
                val intent = fileChooserParams?.createIntent()
                if (intent != null) {
                    launcher.launch(intent)
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
    }

    if (WebViewFeature.isFeatureSupported(WebViewFeature.FORCE_DARK)) {
        WebSettingsCompat.setForceDark(
            s,
            if (config.forceDarkMode) WebSettingsCompat.FORCE_DARK_ON else WebSettingsCompat.FORCE_DARK_OFF
        )
    }
}

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
    override fun requestedWithHeaderMode(): RequestedWithHeaderMode = RequestedWithHeaderMode.UNKNOWN
    override fun proxyStackName(): String = proxy.stackName
    override fun dumpSettings(): String = dumpWebViewConfig(webView, currentConfig)

    internal fun updateConfig(config: WebViewConfig) {
        currentConfig = config
    }
}

private fun extractFileName(contentDisposition: String?, url: String, mimeType: String?): String {
    var fileName: String? = null

    if (contentDisposition != null) {
        val index = contentDisposition.indexOf("filename=")
        if (index >= 0) {
            fileName = contentDisposition.substring(index + 9)
            fileName = fileName.replace("\"", "")
        }
    }

    if (fileName == null) {
        fileName = url.substringAfterLast("/")
        if (fileName.isEmpty()) {
            fileName = "download"
        }
    }

    if (!fileName.contains('.') && mimeType != null) {
        val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        if (extension != null) {
            fileName += ".$extension"
        }
    }

    return fileName
}

private fun buildDeviceAcceptLanguage(): String {
    val locales = mutableListOf<Locale>()

    val localeList = android.content.res.Resources.getSystem().configuration.locales
    for (i in 0 until localeList.size()) {
        locales.add(localeList[i])
    }

    return locales.mapIndexed { index, locale ->
        val quality = 1.0 - (index * 0.1)
        "${locale.language}-${locale.country};q=${"%.1f".format(quality)}"
    }.joinToString(",")
}

private fun getDocStartScript(jsCompat: JsCompatScriptProvider): String {
    return jsCompat.getCompatibilityScript()
}

public fun onFileChooserResult(uris: Array<Uri>?) {
    fileCallbackRef.getAndSet(null)?.onReceiveValue(uris)
}

private val fileCallbackRef: AtomicReference<ValueCallback<Array<Uri>>?> = AtomicReference(null)
