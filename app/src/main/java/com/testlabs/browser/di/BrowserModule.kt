/**
 * Author: Lorenzo Suarez
 * Date: 06/09/2025
 */

package com.testlabs.browser.di

import com.testlabs.browser.presentation.browser.BrowserViewModel
import com.testlabs.browser.ui.browser.AndroidVersionProvider
import com.testlabs.browser.ui.browser.ChromeUAProvider
import com.testlabs.browser.ui.browser.JsCompatScriptProvider
import com.testlabs.browser.ui.browser.NetworkProxy
import com.testlabs.browser.ui.browser.UAProvider
import com.testlabs.browser.ui.browser.VersionProvider
import com.testlabs.browser.ui.browser.DefaultNetworkProxy
import com.testlabs.browser.domain.settings.WebViewConfig
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

public val browserModule: Module = module {
    viewModelOf(constructor = ::BrowserViewModel)

    single<VersionProvider> { AndroidVersionProvider(androidContext()) }

    single<UAProvider> { ChromeUAProvider(get()) }

    single<JsCompatScriptProvider> {
        object : JsCompatScriptProvider {
            override fun getCompatibilityScript(): String = ""
        }
    }

    factory<NetworkProxy> { (config: WebViewConfig) ->
        DefaultNetworkProxy(androidContext(), config, get(), get())
    }
}
