package com.testlabs.browser.domain.settings

import kotlinx.coroutines.flow.Flow

/**
 * Repository providing access to persistent browser configuration.
 */
interface BrowserSettingsRepository {
    /**
     * Observable stream of current configuration.
     */
    val config: Flow<WebViewConfig>

    /**
     * Persists the supplied configuration.
     */
    suspend fun save(config: WebViewConfig)
}
