package com.testlabs.browser.ui.browser

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Tests for [DefaultUAProvider].
 */
class UAProviderTest {
    @Test
    fun desktopModeReturnsDesktopUa() {
        val provider = DefaultUAProvider()
        assertTrue(provider.userAgent(desktop = true).contains("Windows"))
    }
}
