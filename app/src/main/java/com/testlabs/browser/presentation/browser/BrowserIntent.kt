package com.testlabs.browser.presentation.browser

import com.testlabs.browser.core.ValidatedUrl

/**
 * Sealed interface representing all possible user intents in the browser.
 */
sealed interface BrowserIntent {

    /**
     * User wants to navigate to a specific URL.
     */
    data class NavigateToUrl(val url: ValidatedUrl) : BrowserIntent

    /**
     * User wants to reload the current page.
     */
    data object Reload : BrowserIntent

    /**
     * User initiated pull-to-refresh gesture.
     */
    data object PullToRefresh : BrowserIntent

    /**
     * User wants to go back in navigation history.
     */
    data object GoBack : BrowserIntent

    /**
     * User wants to go forward in navigation history.
     */
    data object GoForward : BrowserIntent

    /**
     * User changed the URL input field.
     */
    data class UpdateInputUrl(val inputUrl: String) : BrowserIntent

    /**
     * WebView page started loading.
     */
    data class PageStarted(val url: ValidatedUrl) : BrowserIntent

    /**
     * WebView page finished loading.
     */
    data class PageFinished(val url: ValidatedUrl) : BrowserIntent

    /**
     * WebView loading progress changed.
     */
    data class ProgressChanged(val progress: Float) : BrowserIntent

    /**
     * WebView page title changed.
     */
    data class TitleChanged(val title: String) : BrowserIntent

    /**
     * Navigation capability changed.
     */
    data class NavigationStateChanged(val canGoBack: Boolean, val canGoForward: Boolean) : BrowserIntent

    /**
     * An error occurred during navigation.
     */
    data class NavigationError(val message: String) : BrowserIntent

    /**
     * Clear any displayed error message.
     */
    data object ClearError : BrowserIntent

    /**
     * User wants to open a new tab (clear current page and focus URL input).
     */
    data object NewTab : BrowserIntent
}
