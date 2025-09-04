package com.testlabs.browser.ui.browser

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for UAProvider implementations.
 */
class UAProviderTest {

    private val uaProvider = DefaultUAProvider()

    @Test
    fun `Initial state should be mobile mode`() {
        assertFalse(uaProvider.isDesktopMode())
        assertTrue(uaProvider.getCurrentUserAgent().contains("Mobile"))
    }

    @Test
    fun `Switch to desktop changes user agent and mode`() {
        uaProvider.switchToDesktop()

        assertTrue(uaProvider.isDesktopMode())
        assertFalse(uaProvider.getCurrentUserAgent().contains("Mobile"))
        assertTrue(uaProvider.getCurrentUserAgent().contains("Windows"))
    }

    @Test
    fun `Switch back to mobile restores mobile user agent`() {
        uaProvider.switchToDesktop()
        uaProvider.switchToMobile()

        assertFalse(uaProvider.isDesktopMode())
        assertTrue(uaProvider.getCurrentUserAgent().contains("Mobile"))
        assertTrue(uaProvider.getCurrentUserAgent().contains("Android"))
    }

    @Test
    fun `Mobile user agent contains required Chrome components`() {
        uaProvider.switchToMobile()
        val userAgent = uaProvider.getCurrentUserAgent()

        assertTrue(userAgent.contains("Chrome/"))
        assertTrue(userAgent.contains("Safari/"))
        assertTrue(userAgent.contains("WebKit/"))
        assertTrue(userAgent.contains("Mozilla/"))
    }

    @Test
    fun `Desktop user agent contains required Chrome components`() {
        uaProvider.switchToDesktop()
        val userAgent = uaProvider.getCurrentUserAgent()

        assertTrue(userAgent.contains("Chrome/"))
        assertTrue(userAgent.contains("Safari/"))
        assertTrue(userAgent.contains("WebKit/"))
        assertTrue(userAgent.contains("Mozilla/"))
        assertFalse(userAgent.contains("Mobile"))
    }
}
