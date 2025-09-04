package com.testlabs.browser.data.settings

import androidx.datastore.core.DataStore
import com.testlabs.browser.domain.settings.BrowserSettingsRepository
import com.testlabs.browser.domain.settings.WebViewConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

/**
 * [BrowserSettingsRepository] implementation backed by DataStore.
 */
public class BrowserSettingsRepositoryImpl(
    private val dataStore: DataStore<WebViewConfig>,
) : BrowserSettingsRepository {
    override val config: Flow<WebViewConfig> = dataStore.data
        .catch { if (it is IOException) emit(WebViewConfig()) else throw it }
        .map { it }

    override suspend fun save(config: WebViewConfig) {
        dataStore.updateData { config }
    }
}
