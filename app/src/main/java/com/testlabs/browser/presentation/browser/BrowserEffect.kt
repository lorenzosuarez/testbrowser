package com.testlabs.browser.presentation.browser

import com.testlabs.browser.core.ValidatedUrl

/**
 * Sealed interface representing side effects that should be handled outside the reducer.
 */
sealed interface BrowserEffect {

    /**
     * Effect to load a URL in the WebView.
     */
    data class LoadUrl(val url: ValidatedUrl) : BrowserEffect

    /**
     * Effect to reload the current page in the WebView.
     */
    data object ReloadPage : BrowserEffect

    /**
     * Effect to navigate back in the WebView.
     */
    data object NavigateBack : BrowserEffect

    /**
     * Effect to navigate forward in the WebView.
     */
    data object NavigateForward : BrowserEffect

    /**
     * Effect to show a toast or snackbar message.
     */
    data class ShowMessage(val message: String) : BrowserEffect
}
