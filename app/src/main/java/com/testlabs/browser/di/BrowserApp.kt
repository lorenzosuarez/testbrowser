package com.testlabs.browser.di

import android.annotation.SuppressLint
import android.app.Application
import android.os.Build
import android.util.Log
import android.webkit.WebSettings
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import com.testlabs.browser.network.CronetHolder
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

private const val TAG = "BrowserApp"

public class BrowserApp : Application() {
    override fun onCreate() {
        super.onCreate()
        initializeWebViewSafely()
        initializeKoin()
    }

    override fun onTerminate() {
        super.onTerminate()
        // Shutdown Cronet engine when app terminates
        CronetHolder.shutdown()
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initializeWebViewSafely() {
        runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val processName = getProcessName()
                if (packageName != processName) runCatching { WebView.setDataDirectorySuffix(processName) }
            }
            val wv = WebView(this)
            wv.settings.apply {
                javaScriptEnabled = false
                domStorageEnabled = false
                allowFileAccess = false
                allowContentAccess = false
                databaseEnabled = false
                cacheMode = WebSettings.LOAD_NO_CACHE
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                mediaPlaybackRequiresUserGesture = true
                setSupportZoom(false)
            }
            runCatching { WebViewCompat.startSafeBrowsing(this, null) }
            wv.loadUrl("about:blank")
            wv.post { runCatching { wv.destroy() } }
        }.onFailure { Log.e(TAG, "WebView init failed", it) }
    }

    private fun initializeKoin() {
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@BrowserApp)
            modules(appModule, settingsModule, coreModule)
        }
    }
}