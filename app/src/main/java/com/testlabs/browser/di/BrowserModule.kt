package com.testlabs.browser.di

import com.testlabs.browser.presentation.browser.BrowserViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Browser module providing browser-specific dependencies.
 */
val browserModule = module {
    viewModelOf(constructor = ::BrowserViewModel)
}
