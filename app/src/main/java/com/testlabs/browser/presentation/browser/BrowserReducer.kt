/**
 * Author: Lorenzo Suarez
 * Date: 06/09/2025
 */

package com.testlabs.browser.presentation.browser

import com.testlabs.browser.core.ValidatedUrl

/**
 * Pure reducer function that handles state transitions for browser intents.
 * This function is side-effect free and deterministic.
 */
public object BrowserReducer {
    /**
     * Reduces the current state with a browser intent to produce a new state and optional effect.
     */
    public fun reduce(
        state: BrowserState,
        intent: BrowserIntent,
    ): Pair<BrowserState, BrowserEffect?> =
        when (intent) {
            is BrowserIntent.NavigateToUrl -> {
                val incoming = intent.url.value
                val nextInput = if (state.isUrlInputEditing && state.inputUrl != state.url.value) state.inputUrl else incoming
                state.copy(
                    url = intent.url,
                    inputUrl = nextInput,
                    errorMessage = null,
                    isPullToRefresh = false,
                    mode = BrowserMode.Web,
                ) to BrowserEffect.LoadUrl(intent.url)
            }

            BrowserIntent.Reload -> {
                state.copy(
                    errorMessage = null,
                    isPullToRefresh = false,
                ) to BrowserEffect.ReloadPage
            }

            BrowserIntent.PullToRefresh -> {
                state.copy(
                    isPullToRefresh = true,
                    errorMessage = null,
                ) to BrowserEffect.ReloadPage
            }

            BrowserIntent.GoBack -> {
                if (state.canGoBack) {
                    state.copy(
                        errorMessage = null,
                        isPullToRefresh = false,
                    ) to BrowserEffect.NavigateBack
                } else state to null
            }

            BrowserIntent.GoForward -> {
                if (state.canGoForward) {
                    state.copy(
                        errorMessage = null,
                        isPullToRefresh = false,
                    ) to BrowserEffect.NavigateForward
                } else state to null
            }

            is BrowserIntent.UpdateInputUrl -> {
                state.copy(
                    inputUrl = intent.inputUrl,
                ) to null
            }

            is BrowserIntent.PageStarted -> {
                val incoming = intent.url.value
                val nextInput = if (state.isUrlInputEditing && state.inputUrl != state.url.value) state.inputUrl else incoming
                state.copy(
                    url = intent.url,
                    inputUrl = nextInput,
                    isLoading = true,
                    progress = 0f,
                    errorMessage = null,
                    mode = BrowserMode.Web,
                ) to null
            }

            is BrowserIntent.PageFinished -> {
                val incoming = intent.url.value
                val nextInput = if (state.isUrlInputEditing && state.inputUrl != state.url.value) state.inputUrl else incoming
                state.copy(
                    url = intent.url,
                    inputUrl = nextInput,
                    isLoading = false,
                    isPullToRefresh = false,
                    progress = 1f,
                    mode = BrowserMode.Web,
                ) to null
            }

            is BrowserIntent.ProgressChanged -> state.copy(progress = intent.progress) to null

            is BrowserIntent.TitleChanged -> state.copy(title = intent.title) to null

            is BrowserIntent.NavigationStateChanged -> state.copy(
                canGoBack = intent.canGoBack,
                canGoForward = intent.canGoForward,
            ) to null

            is BrowserIntent.NavigationError -> state.copy(
                isLoading = false,
                isPullToRefresh = false,
                errorMessage = intent.message,
            ) to BrowserEffect.ShowMessage(intent.message)

            BrowserIntent.ClearError -> state.copy(errorMessage = null) to null

            BrowserIntent.EditUrlRequested -> state.copy(
                inputUrl = state.url.value,
                isUrlInputEditing = true,
            ) to BrowserEffect.FocusUrlEditor

            is BrowserIntent.UrlInputEditing -> {
                if (intent.editing) {
                    state.copy(isUrlInputEditing = true)
                } else {
                    state.copy(
                        isUrlInputEditing = false,
                        inputUrl = state.url.value,
                    )
                } to null
            }

            BrowserIntent.OpenSettings -> state.copy(
                isSettingsDialogVisible = true,
                settingsDraft = state.settingsCurrent,
            ) to null

            BrowserIntent.CloseSettings -> state.copy(isSettingsDialogVisible = false) to null

            is BrowserIntent.UpdateSettings -> state.copy(settingsDraft = intent.config) to null

            is BrowserIntent.ApplySettingsAndRestartWebView -> state.copy(
                settingsCurrent = intent.config,
                settingsDraft = intent.config,
                isSettingsDialogVisible = false,
                webViewInstanceKey = state.webViewInstanceKey + 1,
            ) to null

            BrowserIntent.ApplySettings -> state.copy(
                settingsCurrent = state.settingsDraft,
                isSettingsDialogVisible = false,
            ) to null

            is BrowserIntent.UrlChanged -> {
                val incoming = intent.url.value
                val nextInput = if (state.isUrlInputEditing && state.inputUrl != state.url.value) state.inputUrl else incoming
                state.copy(
                    url = intent.url,
                    inputUrl = nextInput,
                    mode = BrowserMode.Web,
                ) to null
            }

            BrowserIntent.NavigateHome -> state.copy(
                url = ValidatedUrl.fromInput(""),
                inputUrl = "",
                isLoading = false,
                isPullToRefresh = false,
                canGoBack = false,
                canGoForward = false,
                errorMessage = null,
                isUrlInputEditing = false,
                mode = BrowserMode.StartPage,
                webViewInstanceKey = state.webViewInstanceKey + 1,
            ) to null

            BrowserIntent.ClearBrowsingData -> state to BrowserEffect.ClearBrowsingData
        }
}
