package com.testlabs.browser.ui.browser

import app.cash.turbine.test
import com.testlabs.browser.core.ValidatedUrl
import com.testlabs.browser.presentation.browser.BrowserEffect
import com.testlabs.browser.presentation.browser.BrowserIntent
import com.testlabs.browser.presentation.browser.BrowserViewModel
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for BrowserViewModel MVI orchestration.
 */
class BrowserViewModelTest {
    @Test
    fun `initial state is correct`() =
        runTest {
            val viewModel = BrowserViewModel()

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
            val viewModel = BrowserViewModel()

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
            val viewModel = BrowserViewModel()
            val testUrl = ValidatedUrl.fromInput("example.com")

            viewModel.handleIntent(BrowserIntent.PageStarted(testUrl))

            val state = viewModel.state.value
            assertEquals(testUrl, state.url)
            assertTrue(state.isLoading)
            assertEquals(0f, state.progress)
        }
}
