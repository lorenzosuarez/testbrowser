package com.testlabs.browser.network

import android.os.Build

public class UserAgentProvider {
    // F. TLS/ALPN strategy - Chrome Stable version matching
    private val chromeStableMajorVersion: Int = 131  // Updated to current Chrome Stable
    private val chromeStableFullVersion: String = "$chromeStableMajorVersion.0.6778.135"

    // G. JS-visible parity - consistent with headers
    public fun getChromeStableMajor(): Int = chromeStableMajorVersion
    public fun getChromeStableFullVersion(): String = chromeStableFullVersion

    // C. Request header parity - exact Chrome Stable mobile UA
    public fun get(): String {
        return getChromeStableMobileUA()
    }

    public fun getChromeStableMobileUA(): String {
        val androidVersion = Build.VERSION.RELEASE
        val deviceModel = Build.MODEL

        return "Mozilla/5.0 (Linux; Android $androidVersion; $deviceModel) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/$chromeStableFullVersion Mobile Safari/537.36"
    }

    public fun getChromeStableDesktopUA(): String {
        // For desktop mode if needed
        return "Mozilla/5.0 (X11; Linux x86_64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/$chromeStableFullVersion Safari/537.36"
    }

    // G. JS-visible parity - navigator.userAgentData brands
    public fun getChromeUserAgentDataBrands(): List<Pair<String, String>> {
        return listOf(
            "Google Chrome" to chromeStableMajorVersion.toString(),
            "Chromium" to chromeStableMajorVersion.toString(),
            "Not(A:Brand" to "24"  // Chrome's "Not A Brand" entry with current format
        )
    }

    // G. JS-visible parity - platform information
    public fun getPlatformInfo(): Map<String, Any> {
        return mapOf(
            "platform" to "Linux aarch64",
            "platformVersion" to Build.VERSION.RELEASE,
            "architecture" to "arm",
            "model" to Build.MODEL,
            "mobile" to true,
            "bitness" to "64"
        )
    }

    public fun major(): String = chromeStableMajorVersion.toString()
    public fun fullVersion(): String = chromeStableFullVersion
}
