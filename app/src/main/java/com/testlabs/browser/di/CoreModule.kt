/**
 * Author: Lorenzo Suarez
 * Date: 06/09/2025
 */

package com.testlabs.browser.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.testlabs.browser.settings.DeveloperSettings
import com.testlabs.browser.network.UserAgentClientHintsManager
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "browser_settings")

public val coreModule: Module = module {
    single { androidContext().dataStore }
    single { DeveloperSettings() }
    single { UserAgentClientHintsManager(get()) }
}
