/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */


package com.testlabs.browser.di

import com.testlabs.browser.presentation.browser.BrowserViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Browser module providing browser-specific dependencies.
 */
public val browserModule: org.koin.core.module.Module =
    module {
        viewModelOf(constructor = ::BrowserViewModel)
    }
