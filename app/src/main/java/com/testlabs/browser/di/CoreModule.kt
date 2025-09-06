package com.testlabs.browser.di

import com.testlabs.browser.network.HttpStack
import com.testlabs.browser.network.HttpStackFactory
import com.testlabs.browser.network.UserAgentClientHintsManager
import com.testlabs.browser.settings.DeveloperSettings
import com.testlabs.browser.ui.browser.*
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Core DI module wiring Chrome-compatible primitives and HTTP stack.
 */
public val coreModule: Module = module {
    single<VersionProvider> { AndroidVersionProvider(androidContext()) }
    single<UAProvider> { ChromeUAProvider(get()) }
    single<DeviceInfoProvider> { DefaultDeviceInfoProvider(androidContext()) }
    single { JsCompatScriptProvider(get()) }
    single { UserAgentClientHintsManager(androidContext()) }
    single { ChromeCompatibilityInjector(get<UAProvider>()) }
    single { DeveloperSettings() }

    single<HttpStack> {
        HttpStackFactory.create(
            context = androidContext(),
            settings = get(),
            uaProvider = get()
        )
    }

    single { NetworkProxy(get()) }
}