/**
 * Author: Lorenzo Suarez
 * Date: 06/09/2025
 */

package com.testlabs.browser.presentation.browser

import com.testlabs.browser.core.ValidatedUrl
import com.testlabs.browser.domain.settings.WebViewConfig

/**
 * Sealed interface representing all possible user intents in the browser.
 */
public sealed interface BrowserIntent {
    /**
     * User wants to navigate to a specific URL.
     */
    public data class NavigateToUrl(
        val url: ValidatedUrl,
    ) : BrowserIntent

    /**
     * User wants to reload the current page.
     */
    public data object Reload : BrowserIntent

    /**
     * User initiated pull-to-refresh gesture.
     */
    public data object PullToRefresh : BrowserIntent

    /**
     * User wants to go back in navigation history.
     */
    public data object GoBack : BrowserIntent

    /**
     * User wants to go forward in navigation history.
     */
    public data object GoForward : BrowserIntent

    /**
     * User changed the URL input field.
     */
    public data class UpdateInputUrl(
        val inputUrl: String,
    ) : BrowserIntent

    /**
     * WebView page started loading.
     */
    public data class PageStarted(
        val url: ValidatedUrl,
    ) : BrowserIntent

    /**
     * WebView page finished loading.
     */
    public data class PageFinished(
        val url: ValidatedUrl,
    ) : BrowserIntent

    /**
     * WebView loading progress changed.
     */
    public data class ProgressChanged(
        val progress: Float,
    ) : BrowserIntent

    /**
     * WebView page title changed.
     */
    public data class TitleChanged(
        val title: String,
    ) : BrowserIntent

    /**
     * Navigation capability changed.
     */
    public data class NavigationStateChanged(
        val canGoBack: Boolean,
        val canGoForward: Boolean,
    ) : BrowserIntent

    /**
     * An error occurred during navigation.
     */
    public data class NavigationError(
        val message: String,
    ) : BrowserIntent

    /**
     * Clear any displayed error message.
     */
    public data object ClearError : BrowserIntent

    public data object EditUrlRequested : BrowserIntent

    public data class UrlInputEditing(val editing: Boolean) : BrowserIntent

    public data object NavigateHome : BrowserIntent

    /**
     * User opened the settings dialog.
     */
    public data object OpenSettings : BrowserIntent

    /**
     * User closed the settings dialog.
     */
    public data object CloseSettings : BrowserIntent

    /**
     * User modified settings configuration.
     */
    public data class UpdateSettings(
        val config: WebViewConfig,
    ) : BrowserIntent

    public data class ApplySettingsAndRestartWebView(
        val config: WebViewConfig,
    ) : BrowserIntent

    /**
     * User confirmed and applied settings.
     */
    public data object ApplySettings : BrowserIntent

    /**
     * WebView URL changed during navigation (redirects, etc.)
     */
    public data class UrlChanged(
        val url: ValidatedUrl,
    ) : BrowserIntent

    /**
     * User wants to clear browsing data.
     */
    public data object ClearBrowsingData : BrowserIntent
}
