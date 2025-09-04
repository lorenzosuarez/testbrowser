package com.testlabs.browser.di

import com.testlabs.browser.ui.browser.DefaultUAProvider
import com.testlabs.browser.ui.browser.UAProvider
import org.koin.dsl.module

/**
 * Core module providing shared primitives and utilities.
 */
val coreModule = module {
    single<UAProvider> { DefaultUAProvider() }
}
