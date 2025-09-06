/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.di

import com.testlabs.browser.presentation.browser.BrowserViewModel
import com.testlabs.browser.ui.browser.JsCompatScriptProvider
import com.testlabs.browser.ui.browser.NetworkProxy
import com.testlabs.browser.ui.browser.UAProvider
import com.testlabs.browser.ui.browser.DefaultNetworkProxy
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

public val browserModule: Module = module {
    viewModelOf(constructor = ::BrowserViewModel)

    single<UAProvider> {
        object : UAProvider {
            override fun userAgent(desktop: Boolean): String {
                val chromeVersion = "120.0.0.0"
                return if (desktop) {
                    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Safari/537.36"
                } else {
                    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Mobile Safari/537.36"
                }
            }
        }
    }

    single<JsCompatScriptProvider> {
        object : JsCompatScriptProvider {
            override fun getCompatibilityScript(): String = ""
        }
    }

    single<NetworkProxy> { DefaultNetworkProxy() }
}
