package com.testlabs.browser.ui.browser

/**
 * [UAProvider] implementation that mimics Chrome's User-Agent format.
 * It never includes the "Android WebView" token to avoid exposing the host.
 */
public class ChromeUAProvider(
    private val versionProvider: VersionProvider
) : UAProvider {

    override fun userAgent(desktop: Boolean): String {
        val chromeVersion = versionProvider.chromeFullVersion()
        val androidVersion = versionProvider.androidVersion()
        val model = versionProvider.deviceModel()

        return if (desktop) {
            // Desktop mode pretends to be Chrome on Linux x86_64 similar to Chrome's device emulation.
            "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/$chromeVersion Safari/537.36"
        } else {
            // Mobile mode mirrors Chrome for Android on a device.
            "Mozilla/5.0 (Linux; Android $androidVersion; $model) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$chromeVersion Mobile Safari/537.36"
        }
    }
}
