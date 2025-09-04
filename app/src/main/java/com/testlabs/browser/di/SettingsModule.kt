package com.testlabs.browser.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.dataStore
import com.testlabs.browser.data.settings.BrowserSettingsRepositoryImpl
import com.testlabs.browser.data.settings.BrowserSettingsSerializer
import com.testlabs.browser.domain.settings.BrowserSettingsRepository
import com.testlabs.browser.domain.settings.WebViewConfig
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

private val Context.browserSettingsDataStore: DataStore<WebViewConfig> by dataStore(
    fileName = "browser_settings.json",
    serializer = BrowserSettingsSerializer,
)

/**
 * Module providing persistence for browser configuration.
 */
val settingsModule = module {
    single<DataStore<WebViewConfig>> { androidContext().browserSettingsDataStore }
    single<BrowserSettingsRepository> { BrowserSettingsRepositoryImpl(get()) }
}
