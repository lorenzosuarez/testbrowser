/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */


package com.testlabs.browser.di

import com.testlabs.browser.presentation.browser.BrowserViewModel
import com.testlabs.browser.settings.DeveloperSettings
import com.testlabs.browser.js.JsBridge
import com.testlabs.browser.ui.browser.UAProvider
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Application module providing core dependencies.
 */
public val appModule: Module =
    module {
        
        viewModelOf(constructor = ::BrowserViewModel)

        
        single { DeveloperSettings() }
        single { JsBridge(get<UAProvider>()) }
    }
