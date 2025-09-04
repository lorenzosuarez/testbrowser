package com.testlabs.browser.presentation.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.testlabs.browser.core.ValidatedUrl
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * ViewModel that orchestrates browser state management using MVI pattern.
 * Handles user intents, produces immutable states, and emits side effects.
 */
class BrowserViewModel : ViewModel() {

    private val _state = MutableStateFlow(BrowserState())

    /**
     * Current browser state as immutable StateFlow.
     */
    val state: StateFlow<BrowserState> = _state.asStateFlow()

    private val _effects = Channel<BrowserEffect>(Channel.UNLIMITED)

    /**
     * Side effects flow for handling WebView operations and UI events.
     */
    val effects = _effects.receiveAsFlow()

    /**
     * Processes user intents and updates state accordingly.
     */
    fun handleIntent(intent: BrowserIntent) {
        viewModelScope.launch {
            val currentState = _state.value
            val (newState, effect) = BrowserReducer.reduce(currentState, intent)

            _state.value = newState

            effect?.let { _effects.send(it) }
        }
    }

    /**
     * Handles URL submission from the address bar.
     */
    fun submitUrl(inputUrl: String) {
        val validatedUrl = ValidatedUrl.fromInput(inputUrl)
        handleIntent(BrowserIntent.NavigateToUrl(validatedUrl))
    }

    override fun onCleared() {
        super.onCleared()
        _effects.close()
    }
}
