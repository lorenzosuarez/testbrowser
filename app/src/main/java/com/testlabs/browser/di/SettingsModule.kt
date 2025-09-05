package com.testlabs.browser.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.testlabs.browser.data.settings.BrowserSettingsRepositoryImpl
import com.testlabs.browser.data.settings.BrowserSettingsSerializer
import com.testlabs.browser.domain.settings.BrowserSettingsRepository
import com.testlabs.browser.domain.settings.WebViewConfig
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

private val Context.browserSettingsDataStore: DataStore<WebViewConfig> by dataStore(
    fileName = "browser_settings.json",
    serializer = BrowserSettingsSerializer,
)

/**
 * Module providing persistence for browser configuration.
 */
public val settingsModule: Module =
    module {
        single<DataStore<WebViewConfig>> { androidContext().browserSettingsDataStore }
        single<BrowserSettingsRepository> { BrowserSettingsRepositoryImpl(get()) }
    }
