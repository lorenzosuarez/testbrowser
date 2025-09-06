/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.presentation.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.testlabs.browser.core.ValidatedUrl
import com.testlabs.browser.domain.settings.BrowserSettingsRepository
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * ViewModel that orchestrates browser state management using MVI pattern.
 * Handles user intents, produces immutable states, and emits side effects.
 */

/**
 * ViewModel orchestrating state and persistent configuration.
 */
public class BrowserViewModel(
    private val settingsRepository: BrowserSettingsRepository,
) : ViewModel() {
    private val _state = MutableStateFlow(BrowserState())

    /**
     * Current browser state as immutable StateFlow.
     */
    public val state: StateFlow<BrowserState> = _state.asStateFlow()

    private val _effects = Channel<BrowserEffect>(Channel.UNLIMITED)

    /**
     * Side effects flow for handling WebView operations and UI events.
     */
    public val effects: Flow<BrowserEffect> = _effects.receiveAsFlow()

    /**
     * Processes user intents and updates state accordingly.
     */
    public fun handleIntent(intent: BrowserIntent) {
        viewModelScope.launch {
            val currentState = _state.value
            val (newState, effect) = BrowserReducer.reduce(currentState, intent)

            _state.value = newState

            if (intent is BrowserIntent.ApplySettings || intent is BrowserIntent.ApplySettingsAndRestartWebView) {
                settingsRepository.save(newState.settingsCurrent)
            }

            effect?.let { _effects.send(it) }
        }
    }

    /**
     * Handles URL submission from the address bar.
     */
    public fun submitUrl(inputUrl: String) {
        val validatedUrl = ValidatedUrl.fromInput(inputUrl)
        handleIntent(BrowserIntent.NavigateToUrl(validatedUrl))
    }

    init {
        viewModelScope.launch {
            settingsRepository.config.collect { config ->
                _state.value =
                    _state.value.copy(
                        settingsCurrent = config,
                        settingsDraft = config,
                    )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _effects.close()
    }
}
