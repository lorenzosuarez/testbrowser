/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */

package com.testlabs.browser.ui.browser

import app.cash.turbine.test
import com.testlabs.browser.core.ValidatedUrl
import com.testlabs.browser.domain.settings.BrowserSettingsRepository
import com.testlabs.browser.domain.settings.WebViewConfig
import com.testlabs.browser.presentation.browser.BrowserEffect
import com.testlabs.browser.presentation.browser.BrowserIntent
import com.testlabs.browser.presentation.browser.BrowserViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for BrowserViewModel MVI orchestration.
 */
class BrowserViewModelTest {

    private class MockBrowserSettingsRepository : BrowserSettingsRepository {
        private val _config = MutableStateFlow(WebViewConfig())
        override val config: Flow<WebViewConfig> = _config.asStateFlow()

        override suspend fun save(config: WebViewConfig) {
            _config.value = config
        }

        override suspend fun reset() {}
    }

    @Test
    fun `initial state is correct`() =
        runTest {
            val settingsRepository = MockBrowserSettingsRepository()
            val viewModel = BrowserViewModel(settingsRepository)

            val initialState = viewModel.state.value

            assertEquals("", initialState.url.value)
            assertEquals("", initialState.title)
            assertEquals(0f, initialState.progress)
            assertEquals(false, initialState.isLoading)
            assertEquals(false, initialState.canGoBack)
            assertEquals(false, initialState.canGoForward)
        }

    @Test
    fun `submitUrl creates NavigateToUrl intent and LoadUrl effect`() =
        runTest {
            val settingsRepository = MockBrowserSettingsRepository()
            val viewModel = BrowserViewModel(settingsRepository)

            viewModel.effects.test {
                viewModel.submitUrl("example.com")

                val effect = awaitItem()
                assertTrue(effect is BrowserEffect.LoadUrl)
                assertEquals("https://example.com", effect.url.value)
            }
        }

    @Test
    fun `handleIntent processes state changes correctly`() =
        runTest {
            val settingsRepository = MockBrowserSettingsRepository()
            val viewModel = BrowserViewModel(settingsRepository)
            val testUrl = ValidatedUrl.fromInput("example.com")

            viewModel.handleIntent(BrowserIntent.PageStarted(testUrl))

            val state = viewModel.state.value
            assertEquals(testUrl, state.url)
            assertTrue(state.isLoading)
            assertEquals(0f, state.progress)
        }
}
