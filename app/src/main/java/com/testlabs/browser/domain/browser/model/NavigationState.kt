package com.testlabs.browser.domain.browser.model

/**
 * Domain model representing browser navigation state.
 * Encapsulates the current navigation capabilities of the browser.
 *
 * @param canGoBack Whether backward navigation is available
 * @param canGoForward Whether forward navigation is available
 */
public data class NavigationState(
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
)
