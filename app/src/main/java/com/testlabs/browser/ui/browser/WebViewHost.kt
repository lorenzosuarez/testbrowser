package com.testlabs.browser.ui.browser

import android.annotation.SuppressLint
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.URLUtil
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.testlabs.browser.domain.settings.WebViewConfig

interface WebViewController {
    fun loadUrl(url: String)
    fun reload()
    fun goBack()
    fun goForward()
    fun canGoBack(): Boolean
    fun canGoForward(): Boolean
}

@Composable
fun WebViewHost(
    onProgressChanged: (Float) -> Unit,
    onPageStarted: (String) -> Unit,
    onPageFinished: (String) -> Unit,
    onTitleChanged: (String) -> Unit,
    onNavigationStateChanged: (Boolean, Boolean) -> Unit,
    onError: (String) -> Unit,
    filePickerLauncher: ActivityResultLauncher<Intent>,
    uaProvider: UAProvider,
    config: WebViewConfig,
    onControllerReady: (WebViewController) -> Unit,
    onScrollDelta: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val webView = remember {
        createWebView(
            context = context,
            onProgressChanged = onProgressChanged,
            onPageStarted = onPageStarted,
            onPageFinished = onPageFinished,
            onTitleChanged = onTitleChanged,
            onNavigationStateChanged = onNavigationStateChanged,
            onError = onError,
            filePickerLauncher = filePickerLauncher,
            uaProvider = uaProvider,
            config = config,
            onScrollDelta = onScrollDelta
        )
    }

    DisposableEffect(webView) {
        val controller = object : WebViewController {
            override fun loadUrl(url: String) = webView.loadUrl(url)
            override fun reload() = webView.reload()
            override fun goBack() = webView.goBack()
            override fun goForward() = webView.goForward()
            override fun canGoBack(): Boolean = webView.canGoBack()
            override fun canGoForward(): Boolean = webView.canGoForward()
        }
        onControllerReady(controller)
        onDispose { webView.destroy() }
    }

    AndroidView(factory = { webView }, modifier = modifier)

    LaunchedEffect(config) {
        webView.applyConfig(config, uaProvider)
    }
}

private class ObservableWebView(
    context: Context,
    private val onScrollDelta: (Int) -> Unit
) : WebView(context) {
    override fun onScrollChanged(l: Int, t: Int, oldl: Int, oldt: Int) {
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
    filePickerLauncher: ActivityResultLauncher<Intent>,
    uaProvider: UAProvider,
    config: WebViewConfig,
    onScrollDelta: (Int) -> Unit
): WebView {
    return ObservableWebView(context, onScrollDelta).apply {
        configureSettings(uaProvider, config)
        setupWebViewClient(onPageStarted, onPageFinished, onNavigationStateChanged, onError)
        setupWebChromeClient(onProgressChanged, onTitleChanged, filePickerLauncher)
        setupDownloadManager(context)
    }
}

private fun WebView.applyConfig(config: WebViewConfig, uaProvider: UAProvider) {
    settings.apply {
        javaScriptEnabled = config.javascriptEnabled
        domStorageEnabled = true
        databaseEnabled = true
        allowFileAccess = true
        allowContentAccess = true
        allowFileAccessFromFileURLs = false
        allowUniversalAccessFromFileURLs = false
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
    }
    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
}

private fun WebView.configureSettings(uaProvider: UAProvider, config: WebViewConfig) {
    applyConfig(config, uaProvider)
}

private fun WebView.setupWebViewClient(
    onPageStarted: (String) -> Unit,
    onPageFinished: (String) -> Unit,
    onNavigationStateChanged: (Boolean, Boolean) -> Unit,
    onError: (String) -> Unit
) {
    webViewClient = object : WebViewClient() {
        override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
            super.onPageStarted(view, url, favicon)
            url?.let { onPageStarted(it) }
        }
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            url?.let { onPageFinished(it) }
            view?.let { onNavigationStateChanged(it.canGoBack(), it.canGoForward()) }
        }
        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            error?.let { onError("Error loading page: ${it.description}") }
        }
        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
            handler?.cancel()
            error?.let { onError("SSL Error: ${it.primaryError}") }
        }
    }
}

private fun WebView.setupWebChromeClient(
    onProgressChanged: (Float) -> Unit,
    onTitleChanged: (String) -> Unit,
    filePickerLauncher: ActivityResultLauncher<Intent>
) {
    var fileUploadCallback: ValueCallback<Array<Uri>>? = null
    webChromeClient = object : WebChromeClient() {
        override fun onProgressChanged(view: WebView?, newProgress: Int) {
            super.onProgressChanged(view, newProgress)
            onProgressChanged(newProgress / 100f)
        }
        override fun onReceivedTitle(view: WebView?, title: String?) {
            super.onReceivedTitle(view, title)
            title?.let { onTitleChanged(it) }
        }
        override fun onShowFileChooser(
            webView: WebView?,
            filePathCallback: ValueCallback<Array<Uri>>?,
            fileChooserParams: FileChooserParams?
        ): Boolean {
            fileUploadCallback?.onReceiveValue(null)
            fileUploadCallback = filePathCallback
            val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "*/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, fileChooserParams?.mode == FileChooserParams.MODE_OPEN_MULTIPLE)
            }
            filePickerLauncher.launch(Intent.createChooser(intent, "Select File"))
            return true
        }
    }
}

private fun WebView.setupDownloadManager(context: Context) {
    setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
        url?.let { downloadUrl ->
            val request = DownloadManager.Request(downloadUrl.toUri()).apply {
                setMimeType(mimeType)
                addRequestHeader("User-Agent", userAgent)
                setDescription("Downloading file...")
                setTitle(URLUtil.guessFileName(downloadUrl, contentDisposition, mimeType))
                allowScanningByMediaScanner()
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(
                    Environment.DIRECTORY_DOWNLOADS,
                    URLUtil.guessFileName(downloadUrl, contentDisposition, mimeType)
                )
            }
            val dm = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            dm.enqueue(request)
        }
    }
}
