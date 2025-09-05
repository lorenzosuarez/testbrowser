package com.testlabs.browser.di

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module
import com.testlabs.browser.settings.DeveloperSettings
import com.testlabs.browser.network.UserAgentProvider
import com.testlabs.browser.js.JsBridge

public class BrowserApp : Application() {
    override fun onCreate(): Unit {
        super.onCreate()
        startKoin {
            androidContext(this@BrowserApp)
            modules(appModule)
        }
    }
}

public val appModule = module {
    single { DeveloperSettings() }
    single { UserAgentProvider() }
    single { JsBridge(get()) }
}
