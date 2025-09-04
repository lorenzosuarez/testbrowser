package com.testlabs.browser.di

import android.app.Application
import android.os.StrictMode
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Main application class that initializes Koin dependency injection.
 */
class BrowserApp : Application() {

    override fun onCreate() {
        super.onCreate()

        if (com.testlabs.browser.BuildConfig.DEBUG) {
            enableStrictMode()
        }

        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@BrowserApp)
            modules(
                coreModule,
                browserModule
            )
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
