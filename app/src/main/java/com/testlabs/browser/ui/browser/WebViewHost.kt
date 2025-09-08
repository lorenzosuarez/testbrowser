/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.ui.browser

import android.content.Intent
import android.net.Uri
import android.webkit.ValueCallback
import android.webkit.WebView
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
import com.testlabs.browser.domain.settings.WebViewConfig
import com.testlabs.browser.ui.browser.controller.RealWebViewController
import com.testlabs.browser.ui.browser.webview.RequestedWithHeaderManager
import com.testlabs.browser.ui.browser.webview.WebViewConfigurer
import com.testlabs.browser.ui.browser.webview.WebViewLifecycleManager
import com.testlabs.browser.ui.browser.webview.WebViewSetupManager
import com.testlabs.browser.settings.DeveloperSettings
import org.koin.compose.getKoin
import org.koin.core.parameter.parametersOf
import java.util.concurrent.atomic.AtomicReference

/**
 * Composable WebView host that provides a fully configured browser WebView
 * with network interception, JavaScript compatibility, and lifecycle management.
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
) {
    val koin = getKoin()
    val networkProxy: NetworkProxy =
        remember(config) { koin.get(parameters = { parametersOf(config) }) }
    val developerSettings: DeveloperSettings = koin.get()

    var webViewRef by remember { mutableStateOf<WebView?>(null) }
    var progress by remember { mutableIntStateOf(0) }
    var controllerRef by remember { mutableStateOf<RealWebViewController?>(null) }

    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier.pointerInteropFilter { false },
        factory = { ctx ->
            val wv = WebView(ctx)
            webViewRef = wv

            // Apply basic configuration
            WebViewConfigurer.setupDefaults(wv)
            RequestedWithHeaderManager.applyPolicy(wv, config)

            // Create controller
            val controller = RealWebViewController(wv, networkProxy, config)
            controllerRef = controller

            // Apply full configuration
            WebViewSetupManager.applyFullConfiguration(
                webView = wv,
                config = config,
                uaProvider = uaProvider,
                jsCompat = jsCompat,
                networkProxy = networkProxy,
                developerSettings = developerSettings,
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

            onControllerReady(controller)
            wv
        },
        update = { webView ->
            RequestedWithHeaderManager.applyPolicy(webView, config)
            controllerRef?.updateConfig(config)

            WebViewSetupManager.applyFullConfiguration(
                webView = webView,
                config = config,
                uaProvider = uaProvider,
                jsCompat = jsCompat,
                networkProxy = networkProxy,
                developerSettings = developerSettings,
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
            WebViewLifecycleManager.destroyWebView(webView)
        }
    )

    DisposableEffect(Unit) {
        onDispose {
            WebViewLifecycleManager.onWebViewDispose(webViewRef)
        }
    }

    LaunchedEffect(progress) {
        onProgressChanged(progress / 100f)
    }
}

/**
 * File upload callback management for WebView file picker functionality.
 */
public fun onFileChooserResult(uris: Array<Uri>?) {
    fileCallbackRef.getAndSet(null)?.onReceiveValue(uris)
}

private val fileCallbackRef: AtomicReference<ValueCallback<Array<Uri>>?> = AtomicReference(null)
