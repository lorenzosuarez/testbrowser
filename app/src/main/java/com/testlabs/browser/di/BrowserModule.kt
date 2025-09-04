package com.testlabs.browser.di

import com.testlabs.browser.presentation.browser.BrowserViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Browser module providing browser-specific dependencies.
 */
val browserModule = module {
    viewModel { BrowserViewModel() }
}
