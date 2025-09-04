package com.testlabs.browser.ui.browser

import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Integration test suite verifying complete browser functionality.
 */
class BrowserIntegrationTest {

    private val viewModel = BrowserViewModel()
    private val uaProvider = DefaultUAProvider()

    @Test
    fun `complete navigation flow updates state correctly`() = runTest {
        // Start page loading
        viewModel.handleIntent(BrowserIntent.PageStarted(
            com.testlabs.browser.core.ValidatedUrl.fromInput("example.com")
        ))

        var state = viewModel.state.value
        assertTrue(state.isLoading)
        assertEquals("https://example.com", state.url.value)

        // Update progress
        viewModel.handleIntent(BrowserIntent.ProgressChanged(0.5f))
        state = viewModel.state.value
        assertEquals(0.5f, state.progress)

        // Update title
        viewModel.handleIntent(BrowserIntent.TitleChanged("Example Domain"))
        state = viewModel.state.value
        assertEquals("Example Domain", state.title)

        // Page finished
        viewModel.handleIntent(BrowserIntent.PageFinished(
            com.testlabs.browser.core.ValidatedUrl.fromInput("example.com")
        ))
        state = viewModel.state.value
        assertFalse(state.isLoading)
        assertEquals(1f, state.progress)
    }

    @Test
    fun `user agent provider switches correctly between mobile and desktop`() {
        // Initial state is mobile
        assertFalse(uaProvider.isDesktopMode())
        assertTrue(uaProvider.getCurrentUserAgent().contains("Mobile"))
        assertTrue(uaProvider.getCurrentUserAgent().contains("Android"))

        // Switch to desktop
        uaProvider.switchToDesktop()
        assertTrue(uaProvider.isDesktopMode())
        assertTrue(uaProvider.getCurrentUserAgent().contains("Windows"))
        assertFalse(uaProvider.getCurrentUserAgent().contains("Mobile"))

        // Switch back to mobile
        uaProvider.switchToMobile()
        assertFalse(uaProvider.isDesktopMode())
        assertTrue(uaProvider.getCurrentUserAgent().contains("Mobile"))
    }
}
