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
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.webkit.WebStorage
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.testlabs.browser.domain.settings.WebViewConfig
import java.util.Locale

private const val TAG = "WebViewHost"
private const val MIME_TYPE_GUESS = "application/octet-stream"

public interface WebViewController {
    public fun loadUrl(url: String)

    public fun reload()

    public fun goBack()

    public fun goForward()

    public fun canGoBack(): Boolean

    public fun canGoForward(): Boolean

    public fun clearBrowsingData(onComplete: () -> Unit)
}

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
    config: WebViewConfig,
    onControllerReady: (WebViewController) -> Unit,
    onScrollDelta: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val downloadHandler = remember { DownloadHandler(context) }
    val fileUploadHandler = remember { FileUploadHandler(context) }

    LaunchedEffect(filePickerLauncher) { fileUploadHandler.initialize(filePickerLauncher) }
    LaunchedEffect(fileUploadHandler) { (context as? com.testlabs.browser.MainActivity)?.setFileUploadHandler(fileUploadHandler) }

    val webView =
        remember {
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
                config = config,
                onScrollDelta = onScrollDelta,
            )
        }

    DisposableEffect(webView) {
        val controller =
            object : WebViewController {
                override fun loadUrl(url: String) {
                    val headers = if (url.startsWith("https://")) mapOf("Accept-Language" to Locale.getDefault().toLanguageTag()) else emptyMap()
                    if (headers.isEmpty()) webView.loadUrl(url) else webView.loadUrl(url, headers)
                }
                override fun reload() = webView.reload()
                override fun goBack() = webView.goBack()
                override fun goForward() = webView.goForward()
                override fun canGoBack(): Boolean = webView.canGoBack()
                override fun canGoForward(): Boolean = webView.canGoForward()
                override fun clearBrowsingData(onComplete: () -> Unit) {
                    try {
                        webView.clearCache(true)
                        webView.clearHistory()
                        WebStorage.getInstance().deleteAllData()
                        val cm = CookieManager.getInstance()
                        cm.removeAllCookies { cm.flush(); onComplete() }
                    } catch (t: Throwable) {
                        Log.e(TAG, "Error clearing data", t)
                        onComplete()
                    }
                }
            }
        onControllerReady(controller)
        onDispose { webView.destroy() }
    }

    AndroidView(factory = { webView }, modifier = modifier)
    LaunchedEffect(config) { webView.applyConfig(config, uaProvider) }
}

@SuppressLint("ViewConstructor")
private class ObservableWebView(
    context: Context,
    private val onScrollDelta: (Int) -> Unit,
) : WebView(context) {
    override fun onScrollChanged(
        l: Int,
        t: Int,
        oldl: Int,
        oldt: Int,
    ) {
        super.onScrollChanged(l, t, oldl, oldt)
        onScrollDelta(t - oldt)
    }
}

@SuppressLint("SetJavaScriptEnabled")
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
    config: WebViewConfig,
    onScrollDelta: (Int) -> Unit,
): WebView =
    ObservableWebView(context, onScrollDelta).apply {
        configureSettings(uaProvider, config)
        setupWebViewClient(onPageStarted, onPageFinished, onNavigationStateChanged, onError, onUrlChanged, downloadHandler)
        setupWebChromeClient(onProgressChanged, onTitleChanged, fileUploadHandler)
        setupDownloadManager(downloadHandler)
    }

private fun WebView.applyConfig(
    config: WebViewConfig,
    uaProvider: UAProvider,
) {
    @SuppressLint("RestrictedApi")
    fun applyInternal() {
        settings.apply {
            javaScriptEnabled = config.javascriptEnabled
            domStorageEnabled = true
            databaseEnabled = true
            allowFileAccess = true
            allowContentAccess = true
            allowFileAccessFromFileURLs = true
            allowUniversalAccessFromFileURLs = true
            mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportMultipleWindows(true)
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            userAgentString = uaProvider.userAgent(config.desktopMode)
            loadsImagesAutomatically = true
            blockNetworkImage = false
            blockNetworkLoads = false
            cacheMode = WebSettings.LOAD_DEFAULT
            setRenderPriority(WebSettings.RenderPriority.HIGH)
            mediaPlaybackRequiresUserGesture = false
            javaScriptCanOpenWindowsAutomatically = true
            setSavePassword(false)
            setSaveFormData(false)
            setGeolocationEnabled(true)
            setJavaScriptEnabled(true)
            setDomStorageEnabled(true)
        }
        if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
            WebSettingsCompat.setRequestedWithHeaderOriginAllowList(settings, emptySet())
        }
        CookieManager.getInstance().apply {
            setAcceptCookie(true)
            setAcceptThirdPartyCookies(this@applyConfig, true)
        }
        setLayerType(android.view.View.LAYER_TYPE_HARDWARE, null)
    }
    applyInternal()
}

private fun WebView.configureSettings(
    uaProvider: UAProvider,
    config: WebViewConfig,
) { applyConfig(config, uaProvider) }

private fun WebView.setupWebViewClient(
    onPageStarted: (String) -> Unit,
    onPageFinished: (String) -> Unit,
    onNavigationStateChanged: (Boolean, Boolean) -> Unit,
    onError: (String) -> Unit,
    onUrlChanged: (String) -> Unit,
    downloadHandler: DownloadHandler,
) {
    webViewClient =
        object : WebViewClient() {
            override fun onPageStarted(
                view: WebView?,
                url: String?,
                favicon: android.graphics.Bitmap?,
            ) {
                super.onPageStarted(view, url, favicon)
                url?.let { onPageStarted(it) }
            }
            override fun onPageFinished(
                view: WebView?,
                url: String?,
            ) {
                super.onPageFinished(view, url)
                url?.let { onPageFinished(it) }
                view?.let { onNavigationStateChanged(it.canGoBack(), it.canGoForward()) }
            }
            override fun doUpdateVisitedHistory(
                view: WebView?,
                url: String?,
                isReload: Boolean,
            ) {
                super.doUpdateVisitedHistory(view, url, isReload)
                url?.let {
                    onUrlChanged(it)
                    view?.let { v -> onNavigationStateChanged(v.canGoBack(), v.canGoForward()) }
                }
            }
            override fun onReceivedError(
                view: WebView?,
                request: WebResourceRequest?,
                error: WebResourceError?,
            ) {
                super.onReceivedError(view, request, error)
                error?.let { if (request?.isForMainFrame == true) onError("Error loading page: ${it.description}") }
            }
            override fun onReceivedSslError(
                view: WebView?,
                handler: SslErrorHandler?,
                error: android.net.http.SslError?,
            ) {
                handler?.cancel()
                error?.let { sslError ->
                    val errorMessage =
                        when (sslError.primaryError) {
                            android.net.http.SslError.SSL_UNTRUSTED -> "Certificate not trusted"
                            android.net.http.SslError.SSL_EXPIRED -> "Certificate expired"
                            android.net.http.SslError.SSL_IDMISMATCH -> "Certificate hostname mismatch"
                            android.net.http.SslError.SSL_NOTYETVALID -> "Certificate not yet valid"
                            android.net.http.SslError.SSL_DATE_INVALID -> "Certificate date invalid"
                            android.net.http.SslError.SSL_INVALID -> "Certificate invalid"
                            else -> "SSL certificate error"
                        }
                    onError("SSL Error: $errorMessage")
                }
            }
            override fun onReceivedHttpError(
                view: WebView?,
                request: WebResourceRequest?,
                errorResponse: android.webkit.WebResourceResponse?,
            ) {
                super.onReceivedHttpError(view, request, errorResponse)
                if (request?.isForMainFrame == true) {
                    errorResponse?.let { response -> onError("HTTP Error: ${response.statusCode} ${response.reasonPhrase}") }
                }
            }
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?,
            ): Boolean {
                val u = request?.url?.toString() ?: return false
                Log.d(TAG, "shouldOverrideUrlLoading: $u")
                if (u.startsWith("blob:")) {
                    Log.d(TAG, "Detected blob URL, triggering download")
                    downloadHandler.handleDownload(u, settings.userAgentString ?: "", "", MIME_TYPE_GUESS, this@setupWebViewClient) { err ->
                        Log.e(TAG, "Blob download error: $err")
                        onError(err)
                    }
                    return true
                }
                request.url?.let { uri ->
                    when (uri.scheme) {
                        "mailto", "tel", "sms" -> return false
                        "http", "https" -> return false
                        else -> return false
                    }
                }
                return false
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
            override fun onProgressChanged(
                view: WebView?,
                newProgress: Int,
            ) {
                super.onProgressChanged(view, newProgress)
                onProgressChanged(newProgress / 100f)
            }
            override fun onReceivedTitle(
                view: WebView?,
                title: String?,
            ) {
                super.onReceivedTitle(view, title)
                title?.let { onTitleChanged(it) }
            }
            override fun onShowFileChooser(
                webView: WebView?,
                filePathCallback: ValueCallback<Array<Uri>>?,
                fileChooserParams: FileChooserParams?,
            ): Boolean = fileUploadHandler.handleFileChooser(filePathCallback, fileChooserParams)
            override fun onConsoleMessage(consoleMessage: android.webkit.ConsoleMessage?): Boolean {
                consoleMessage?.let { msg -> Log.d("WebViewConsole", "[${msg.messageLevel()}] ${msg.message()} (${msg.sourceId()}:${msg.lineNumber()})") }
                return true
            }
        }
}

private fun WebView.setupDownloadManager(downloadHandler: DownloadHandler) {
    Log.d(TAG, "Setting up download listener")
    setDownloadListener { url, userAgent, contentDisposition, mimeType, contentLength ->
        Log.d(TAG, "Download listener triggered!")
        Log.d(TAG, "URL: $url")
        Log.d(TAG, "User Agent: $userAgent")
        Log.d(TAG, "Content Disposition: $contentDisposition")
        Log.d(TAG, "MIME Type: $mimeType")
        Log.d(TAG, "Content Length: $contentLength")
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
