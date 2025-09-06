/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.presentation.browser

import androidx.compose.runtime.Immutable
import com.testlabs.browser.core.ValidatedUrl
import com.testlabs.browser.domain.settings.WebViewConfig

/**
 * Immutable state representing the complete browser UI state.
 */
@Immutable
public data class BrowserState(
    val url: ValidatedUrl = ValidatedUrl.fromInput(""),
    val title: String = "",
    val progress: Float = 0f,
    val isLoading: Boolean = false,
    val isPullToRefresh: Boolean = false,
    val canGoBack: Boolean = false,
    val canGoForward: Boolean = false,
    val inputUrl: String = "",
    val errorMessage: String? = null,
    val isSettingsDialogVisible: Boolean = false,
    val settingsCurrent: WebViewConfig = WebViewConfig(),
    val settingsDraft: WebViewConfig = WebViewConfig(),
    val isUrlInputEditing: Boolean = false,
    val mode: BrowserMode = BrowserMode.StartPage,
    val webViewInstanceKey: Int = 0,
)
