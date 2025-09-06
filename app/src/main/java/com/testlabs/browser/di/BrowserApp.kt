package com.testlabs.browser.di

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

public class BrowserApp : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@BrowserApp)
            modules(appModule, settingsModule, coreModule)
        }
    }
}
