/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.presentation.browser

import com.testlabs.browser.core.ValidatedUrl

/**
 * Sealed interface representing side effects that should be handled outside the reducer.
 */
public sealed interface BrowserEffect {
    /**
     * Effect to load a URL in the WebView.
     */
    public data class LoadUrl(
        val url: ValidatedUrl,
    ) : BrowserEffect

    /**
     * Effect to reload the current page in the WebView.
     */
    public data object ReloadPage : BrowserEffect

    /**
     * Effect to navigate back in the WebView.
     */
    public data object NavigateBack : BrowserEffect

    /**
     * Effect to navigate forward in the WebView.
     */
    public data object NavigateForward : BrowserEffect

    /**
     * Effect to show a toast or snackbar message.
     */
    public data class ShowMessage(
        val message: String,
    ) : BrowserEffect

    /**
     * Effect to clear browsing data.
     */
    public data object ClearBrowsingData : BrowserEffect

    public data object FocusUrlEditor : BrowserEffect
}
