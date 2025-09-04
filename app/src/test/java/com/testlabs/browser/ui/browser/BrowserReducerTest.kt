package com.testlabs.browser.ui.browser

import com.testlabs.browser.core.ValidatedUrl
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for BrowserReducer pure functions.
 */
class BrowserReducerTest {

    private val initialState = BrowserState()

    @Test
    fun `NavigateToUrl intent produces LoadUrl effect and updates state`() {
        val url = ValidatedUrl.fromInput("example.com")
        val intent = BrowserIntent.NavigateToUrl(url)

        val (newState, effect) = BrowserReducer.reduce(initialState, intent)

        assertEquals(url, newState.url)
        assertEquals(url.value, newState.inputUrl)
        assertNull(newState.errorMessage)
        assertTrue(effect is BrowserEffect.LoadUrl)
        assertEquals(url, (effect as BrowserEffect.LoadUrl).url)
    }

    @Test
    fun `Reload intent produces ReloadPage effect and clears error`() {
        val stateWithError = initialState.copy(errorMessage = "Some error")
        val intent = BrowserIntent.Reload

        val (newState, effect) = BrowserReducer.reduce(stateWithError, intent)

        assertNull(newState.errorMessage)
        assertTrue(effect is BrowserEffect.ReloadPage)
    }

    @Test
    fun `GoBack when canGoBack is false produces no effect`() {
        val stateCannotGoBack = initialState.copy(canGoBack = false)
        val intent = BrowserIntent.GoBack

        val (newState, effect) = BrowserReducer.reduce(stateCannotGoBack, intent)

        assertEquals(stateCannotGoBack, newState)
        assertNull(effect)
    }

    @Test
    fun `GoBack when canGoBack is true produces NavigateBack effect`() {
        val stateCanGoBack = initialState.copy(canGoBack = true)
        val intent = BrowserIntent.GoBack

        val (newState, effect) = BrowserReducer.reduce(stateCanGoBack, intent)

        assertNull(newState.errorMessage)
        assertTrue(effect is BrowserEffect.NavigateBack)
    }

    @Test
    fun `PageStarted updates url, sets loading state and clears error`() {
        val url = ValidatedUrl.fromInput("example.com")
        val intent = BrowserIntent.PageStarted(url)

        val (newState, effect) = BrowserReducer.reduce(initialState, intent)

        assertEquals(url, newState.url)
        assertTrue(newState.isLoading)
        assertEquals(0f, newState.progress)
        assertNull(newState.errorMessage)
        assertNull(effect)
    }

    @Test
    fun `PageFinished sets loading to false and progress to complete`() {
        val loadingState = initialState.copy(isLoading = true, progress = 0.5f)
        val url = ValidatedUrl.fromInput("example.com")
        val intent = BrowserIntent.PageFinished(url)

        val (newState, effect) = BrowserReducer.reduce(loadingState, intent)

        assertEquals(false, newState.isLoading)
        assertEquals(1f, newState.progress)
        assertNull(effect)
    }

    @Test
    fun `ProgressChanged updates progress value`() {
        val newProgress = 0.75f
        val intent = BrowserIntent.ProgressChanged(newProgress)

        val (newState, effect) = BrowserReducer.reduce(initialState, intent)

        assertEquals(newProgress, newState.progress)
        assertNull(effect)
    }

    @Test
    fun `NavigationError sets error message and produces ShowMessage effect`() {
        val errorMessage = "Network error"
        val intent = BrowserIntent.NavigationError(errorMessage)

        val (newState, effect) = BrowserReducer.reduce(initialState, intent)

        assertEquals(errorMessage, newState.errorMessage)
        assertEquals(false, newState.isLoading)
        assertTrue(effect is BrowserEffect.ShowMessage)
        assertEquals(errorMessage, (effect as BrowserEffect.ShowMessage).message)
    }
}
