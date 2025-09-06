/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */


package com.testlabs.browser.domain.settings

import kotlinx.coroutines.flow.Flow

/**
 * Repository providing access to persistent browser configuration.
 */
public interface BrowserSettingsRepository {
    /**
     * Observable stream of current configuration.
     */
    public val config: Flow<WebViewConfig>

    /**
     * Persists the supplied configuration.
     */
    public suspend fun save(config: WebViewConfig)

    /**
     * Resets the configuration to default values.
     */
    public suspend fun reset()
}
