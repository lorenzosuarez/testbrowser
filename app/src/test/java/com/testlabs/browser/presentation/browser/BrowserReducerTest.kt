package com.testlabs.browser.presentation.browser

import com.testlabs.browser.domain.settings.WebViewConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [BrowserReducer].
 */
class BrowserReducerTest {
    @Test
    fun openSettings_populatesDraft() {
        val state = BrowserState(settingsCurrent = WebViewConfig(desktopMode = true))
        val (newState, effect) = BrowserReducer.reduce(state, BrowserIntent.OpenSettings)
        assertTrue(newState.isSettingsDialogVisible)
        assertEquals(WebViewConfig(desktopMode = true), newState.settingsDraft)
        assertEquals(null, effect)
    }

    @Test
    fun applySettings_updatesCurrent() {
        val state =
            BrowserState(
                settingsCurrent = WebViewConfig(desktopMode = false),
                settingsDraft = WebViewConfig(desktopMode = true),
                isSettingsDialogVisible = true,
            )
        val (newState, _) = BrowserReducer.reduce(state, BrowserIntent.ApplySettings)
        assertFalse(newState.isSettingsDialogVisible)
        assertTrue(newState.settingsCurrent.desktopMode)
    }
}
