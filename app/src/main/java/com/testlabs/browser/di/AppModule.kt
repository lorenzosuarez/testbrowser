package com.testlabs.browser.di

import com.testlabs.browser.presentation.browser.BrowserViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Application module providing core dependencies.
 */
public val appModule: org.koin.core.module.Module = module {
    // Browser
    viewModelOf(constructor = ::BrowserViewModel)

    // Core services
    single<com.testlabs.browser.ui.browser.UAProvider> {
        com.testlabs.browser.ui.browser.DefaultUAProvider()
    }
}
