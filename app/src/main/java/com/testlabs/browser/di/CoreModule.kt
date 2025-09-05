package com.testlabs.browser.di

import com.testlabs.browser.ui.browser.DefaultUAProvider
import com.testlabs.browser.ui.browser.UAProvider
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Core module providing shared primitives and utilities.
 */
public val coreModule: Module =
    module {
        single<UAProvider> { DefaultUAProvider(androidContext()) }
    }
