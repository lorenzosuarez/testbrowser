package com.testlabs.browser

import android.os.Bundle
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.viewinterop.AndroidView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import com.testlabs.browser.js.JsBridge
import com.testlabs.browser.network.OkHttpEngine
import com.testlabs.browser.network.UserAgentProvider
import com.testlabs.browser.settings.DeveloperSettings
import com.testlabs.browser.webview.BrowserWebViewClient
import org.koin.android.ext.android.inject

public class MainActivity : ComponentActivity() {

    private val settings: DeveloperSettings by inject()
    private val ua: UserAgentProvider by inject()
    private val js: JsBridge by inject()

    override fun onCreate(savedInstanceState: Bundle?): Unit {
        super.onCreate(savedInstanceState)
        val engine = OkHttpEngine(settings, ua)
        setContent { BrowserScreen(engine, js, ua) }
    }
}

@Composable
private fun BrowserScreen(engine: OkHttpEngine, js: JsBridge, ua: UserAgentProvider) {
    AndroidView(factory = { context ->
        WebView(context).apply {
            val s: WebSettings = settings
            s.javaScriptEnabled = true
            s.domStorageEnabled = true
            s.databaseEnabled = true
            s.allowFileAccess = true
            s.mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
            val uaString = ua.get()
            s.userAgentString = uaString
            if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_CONTROL)) {
                WebViewCompat.setRequestedWithHeaderAllowList(this, emptyList())
            }
            webViewClient = BrowserWebViewClient(engine, js)
            webChromeClient = object : WebChromeClient() {
                override fun onConsoleMessage(consoleMessage: ConsoleMessage): Boolean {
                    Log.d("WebConsole", consoleMessage.message())
                    return super.onConsoleMessage(consoleMessage)
                }
            }
        }
    })
}
