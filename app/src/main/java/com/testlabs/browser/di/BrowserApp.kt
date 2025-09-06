/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.di

import android.app.Application
import android.webkit.WebView
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

public class BrowserApp : Application() {
    override fun onCreate() {
        super.onCreate()

        WebView.setWebContentsDebuggingEnabled(true)

        startKoin {
            androidContext(this@BrowserApp)
            modules(browserModule, appModule, settingsModule, coreModule)
        }
    }
}
