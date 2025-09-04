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
    public fun reduce(state: BrowserState, intent: BrowserIntent): Pair<BrowserState, BrowserEffect?> {
        return when (intent) {
            is BrowserIntent.NavigateToUrl -> {
                state.copy(
                    url = intent.url,
                    inputUrl = intent.url.value,
                    errorMessage = null,
                    isPullToRefresh = false
                ) to BrowserEffect.LoadUrl(intent.url)
            }

            BrowserIntent.Reload -> {
                state.copy(
                    errorMessage = null,
                    isPullToRefresh = false
                ) to BrowserEffect.ReloadPage
            }

            BrowserIntent.PullToRefresh -> {
                state.copy(
                    isPullToRefresh = true,
                    errorMessage = null
                ) to BrowserEffect.ReloadPage
            }

            BrowserIntent.GoBack -> {
                if (state.canGoBack) {
                    state.copy(
                        errorMessage = null,
                        isPullToRefresh = false
                    ) to BrowserEffect.NavigateBack
                } else {
                    state to null
                }
            }

            BrowserIntent.GoForward -> {
                if (state.canGoForward) {
                    state.copy(
                        errorMessage = null,
                        isPullToRefresh = false
                    ) to BrowserEffect.NavigateForward
                } else {
                    state to null
                }
            }

            is BrowserIntent.UpdateInputUrl -> {
                state.copy(
                    inputUrl = intent.inputUrl,
                    shouldFocusUrlInput = false
                ) to null
            }

            is BrowserIntent.PageStarted -> {
                state.copy(
                    url = intent.url,
                    inputUrl = intent.url.value, // Sincronizar URL del WebView con el TextField
                    isLoading = true,
                    progress = 0f,
                    errorMessage = null
                ) to null
            }

            is BrowserIntent.PageFinished -> {
                state.copy(
                    url = intent.url,
                    inputUrl = intent.url.value, // Mantener sincronización en PageFinished también
                    isLoading = false,
                    isPullToRefresh = false,
                    progress = 1f
                ) to null
            }

            is BrowserIntent.ProgressChanged -> {
                state.copy(progress = intent.progress) to null
            }

            is BrowserIntent.TitleChanged -> {
                state.copy(title = intent.title) to null
            }

            is BrowserIntent.NavigationStateChanged -> {
                state.copy(
                    canGoBack = intent.canGoBack,
                    canGoForward = intent.canGoForward
                ) to null
            }

            is BrowserIntent.NavigationError -> {
                state.copy(
                    isLoading = false,
                    isPullToRefresh = false,
                    errorMessage = intent.message
                ) to BrowserEffect.ShowMessage(intent.message)
            }

            BrowserIntent.ClearError -> {
                state.copy(errorMessage = null) to null
            }

            BrowserIntent.NewTab -> {
                state.copy(
                    url = ValidatedUrl.fromInput(""),
                    inputUrl = "",
                    title = "",
                    progress = 0f,
                    isLoading = false,
                    isPullToRefresh = false,
                    canGoBack = false,
                    canGoForward = false,
                    errorMessage = null,
                    shouldFocusUrlInput = true
                ) to BrowserEffect.LoadUrl(ValidatedUrl.fromInput("about:blank"))
            }

            BrowserIntent.OpenSettings -> {
                state.copy(
                    isSettingsDialogVisible = true,
                    settingsDraft = state.settingsCurrent,
                ) to null
            }

            BrowserIntent.CloseSettings -> {
                state.copy(isSettingsDialogVisible = false) to null
            }

            is BrowserIntent.SettingsUpdated -> {
                state.copy(settingsDraft = intent.config) to null
            }

            BrowserIntent.ApplySettings -> {
                state.copy(
                    settingsCurrent = state.settingsDraft,
                    isSettingsDialogVisible = false,
                ) to null
            }

            is BrowserIntent.UrlChanged -> {
                state.copy(
                    url = intent.url,
                    inputUrl = intent.url.value
                ) to null
            }
        }
    }
}
