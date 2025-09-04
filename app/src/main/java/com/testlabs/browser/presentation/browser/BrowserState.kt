package com.testlabs.browser.presentation.browser

import androidx.compose.runtime.Immutable
import com.testlabs.browser.core.ValidatedUrl
import com.testlabs.browser.domain.settings.WebViewConfig

/**
 * Immutable state representing the complete browser UI state.
 */
@Immutable
data class BrowserState(
    val url: ValidatedUrl = ValidatedUrl.fromInput(""),
    val title: String = "",
    val progress: Float = 0f,
    val isLoading: Boolean = false,
    val isPullToRefresh: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val inputUrl: String = "",
    val errorMessage: String? = null,
    val shouldFocusUrlInput: Boolean = false,
    val isSettingsDialogVisible: Boolean = false,
    val settingsCurrent: WebViewConfig = WebViewConfig(),
    val settingsDraft: WebViewConfig = WebViewConfig(),
)
