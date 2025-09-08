/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */

package com.testlabs.browser.di

import com.testlabs.browser.domain.settings.WebViewConfig
import com.testlabs.browser.network.UserAgentClientHintsManager
import com.testlabs.browser.presentation.browser.BrowserViewModel
import com.testlabs.browser.ui.browser.AndroidVersionProvider
import com.testlabs.browser.ui.browser.ChromeUAProvider
import com.testlabs.browser.ui.browser.DefaultNetworkProxy
import com.testlabs.browser.ui.browser.JsCompatScriptProvider
import com.testlabs.browser.ui.browser.NetworkProxy
import com.testlabs.browser.ui.browser.UAProvider
import com.testlabs.browser.ui.browser.VersionProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.module.Module
import org.koin.dsl.module

public val browserModule: Module = module {
    single<VersionProvider> { AndroidVersionProvider(androidContext()) }
    single { UserAgentClientHintsManager(get()) }
    single<UAProvider> { ChromeUAProvider(get()) }
    factory<NetworkProxy> { (config: WebViewConfig) ->
        DefaultNetworkProxy(androidContext(), config, get(), get())
    }
    factory<JsCompatScriptProvider> { JsCompatScriptProvider { "" } }
    viewModelOf(::BrowserViewModel)
}
