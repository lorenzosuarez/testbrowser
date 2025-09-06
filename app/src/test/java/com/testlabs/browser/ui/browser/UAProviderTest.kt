/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.ui.browser

import org.junit.Test
import kotlin.test.assertTrue

private class FakeVersionProvider(
    private val chrome: String?,
    private val webView: String?,
) : VersionProvider {
    override fun androidVersion(): String = "14"

    override fun chromeFullVersion(): String = chrome ?: webView ?: "120.0.0.0"

    override fun chromeMajor(): Int = chrome?.substringBefore(".")?.toIntOrNull()
        ?: webView?.substringBefore(".")?.toIntOrNull()
        ?: 120

    override fun deviceModel(): String = "TestDevice"
}

/** Tests for [ChromeUAProvider]. */
class UAProviderTest {
    @Test
    fun usesChromeVersionWhenAvailable() {
        val provider = ChromeUAProvider(
            versionProvider = FakeVersionProvider("123.0.0.0", null)
        )
        assertTrue(provider.userAgent(desktop = false).contains("Chrome/123.0.0.0"))
    }

    @Test
    fun fallsBackToWebViewVersion() {
        val provider = ChromeUAProvider(
            versionProvider = FakeVersionProvider(null, "200.0.0.0")
        )
        assertTrue(provider.userAgent(desktop = false).contains("Chrome/200.0.0.0"))
    }

    @Test
    fun noAndroidWebViewBrand() {
        val provider = ChromeUAProvider(
            versionProvider = FakeVersionProvider("123.0.0.0", null)
        )
        val ua = provider.userAgent(desktop = false)
        assertTrue(!ua.contains("Android WebView"))
    }
}
