package com.testlabs.browser.di

import android.annotation.SuppressLint
import android.app.Application
import android.os.StrictMode
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Main application class that initializes Koin dependency injection.
 */
public class BrowserApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Initialize WebView early to avoid variations seed issues
        initializeWebView()

        if (com.testlabs.browser.BuildConfig.DEBUG) {
            enableStrictMode()
        }

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@BrowserApp)
            modules(
                appModule,
                settingsModule,
                coreModule,
            )
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initializeWebView() {
        try {
            // Initialize WebView on main thread to prevent variations seed errors
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val processName = getProcessName()
                if (packageName != processName) {
                    android.webkit.WebView.setDataDirectorySuffix(processName)
                }
            }

            // Pre-warm WebView to initialize Chromium engine
            android.webkit.WebView(this).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                loadUrl("about:blank")
                destroy()
            }
        } catch (e: Exception) {
            // WebView initialization failed - continue anyway
            android.util.Log.w("BrowserApp", "WebView initialization failed", e)
        }
    }

    private fun enableStrictMode() {
        StrictMode.setThreadPolicy(
            StrictMode.ThreadPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
        StrictMode.setVmPolicy(
            StrictMode.VmPolicy.Builder()
                .detectAll()
                .penaltyLog()
                .build()
        )
    }
}
