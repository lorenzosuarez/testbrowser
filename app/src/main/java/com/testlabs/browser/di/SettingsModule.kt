/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */

package com.testlabs.browser.di

import com.testlabs.browser.data.settings.BrowserSettingsRepositoryImpl
import com.testlabs.browser.domain.settings.BrowserSettingsRepository
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Module providing persistence for browser configuration.
 */
public val settingsModule: Module = module {
    single<BrowserSettingsRepository> { BrowserSettingsRepositoryImpl(get()) }
}
