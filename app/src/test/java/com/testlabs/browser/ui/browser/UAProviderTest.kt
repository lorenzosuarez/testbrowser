package com.testlabs.browser.ui.browser

import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertTrue

private class FakeVersionProvider(
    private val chrome: String?,
    private val webView: String?,
) : VersionProvider {
    override fun chromeMajor(): String? = chrome
    override fun webViewMajor(): String? = webView
}

/** Tests for [DefaultUAProvider]. */
class UAProviderTest {

    @Test
    fun usesChromeVersionWhenAvailable() {
        val provider = DefaultUAProvider(
            context = mockk(relaxed = true),
            versionProvider = FakeVersionProvider("123", null),
            registerReceivers = false,
        )
        assertTrue(provider.userAgent(desktop = false).contains("Chrome/123.0.0.0"))
    }

    @Test
    fun fallsBackToWebViewVersion() {
        val provider = DefaultUAProvider(
            context = mockk(relaxed = true),
            versionProvider = FakeVersionProvider(null, "200"),
            registerReceivers = false,
        )
        assertTrue(provider.userAgent(desktop = false).contains("Chrome/200.0.0.0"))
    }
}

