/*
 * @author Lorenzo Suarez
 * @date 09/04//2025
 */

package com.testlabs.browser.di

import com.testlabs.browser.presentation.browser.BrowserViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

/**
 * Application module providing core dependencies.
 */
public val appModule: org.koin.core.module.Module =
    module {
        viewModelOf(constructor = ::BrowserViewModel)
    }
