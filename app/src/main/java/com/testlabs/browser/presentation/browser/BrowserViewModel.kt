/**
 * Author: Lorenzo Suarez
 * Date: 06/09/2025
 */

package com.testlabs.browser.presentation.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.testlabs.browser.core.ValidatedUrl
import com.testlabs.browser.domain.settings.BrowserSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * MVI ViewModel that reduces [BrowserIntent] into [BrowserState] and emits one-off [BrowserEffect].
 *
 * Design:
 * - State: hot [StateFlow] with immediate replay for Compose.
 * - Effects: hot [SharedFlow] with bounded buffer to avoid backpressure on UI.
 * - Persistence: saves settings on Apply intents on an IO dispatcher.
 */
public class BrowserViewModel(
    private val settingsRepository: BrowserSettingsRepository,
) : ViewModel() {

    private val _state: MutableStateFlow<BrowserState> = MutableStateFlow(BrowserState())

    /** Public immutable state for UI. */
    public val state: StateFlow<BrowserState> = _state.asStateFlow()

    private val _effects: MutableSharedFlow<BrowserEffect> = MutableSharedFlow(extraBufferCapacity = 64)

    /** One-off effects stream for navigation, toasts, and WebView commands. */
    public val effects: SharedFlow<BrowserEffect> = _effects

    /**
     * Processes a [BrowserIntent], reduces it with the current state, persists configuration
     * when required, and emits any resulting [BrowserEffect].
     */
    public fun handleIntent(intent: BrowserIntent) {
        viewModelScope.launch {
            val (newState, effect) = BrowserReducer.reduce(_state.value, intent)
            _state.value = newState

            if (intent is BrowserIntent.ApplySettings || intent is BrowserIntent.ApplySettingsAndRestartWebView) {
                withContext(Dispatchers.IO) { settingsRepository.save(newState.settingsCurrent) }
            }

            effect?.let { _effects.tryEmit(it) }
        }
    }

    /**
     * Validates and dispatches a navigation intent for a raw user-entered URL.
     */
    public fun submitUrl(inputUrl: String) {
        val validated = ValidatedUrl.fromInput(inputUrl)
        handleIntent(BrowserIntent.NavigateToUrl(validated))
    }

    init {
        settingsRepository.config
            .distinctUntilChanged()
            .onEach { cfg ->
                _state.update { s ->
                    s.copy(
                        settingsCurrent = cfg,
                        settingsDraft = cfg
                    )
                }
            }
            .launchIn(viewModelScope)
    }
}
