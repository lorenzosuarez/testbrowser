package com.testlabs.browser.ui.browser

import android.os.Build
import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Provides user agent strings matching Chrome for both mobile and desktop modes.
 */
public interface UAProvider {
    /** Flow of the current user agent. */
    public val uaFlow: StateFlow<String>

    /** Returns a user agent string for the requested mode. */
    public fun userAgent(desktop: Boolean): String

    /** Sets a custom user agent override or clears it when null. */
    public fun setCustomUserAgent(ua: String?)
}

/** Sources version information for Chrome and WebView. */
public interface VersionProvider {
    public fun chromeMajor(): String?

    public fun webViewMajor(): String?
}

/**
 * Android implementation of [VersionProvider].
 */
public class AndroidVersionProvider(
    private val context: android.content.Context,
) : VersionProvider {
    override fun chromeMajor(): String? =
        runCatching {
            context.packageManager
                .getPackageInfo("com.android.chrome", 0)
                .versionName
                ?.substringBefore('.')
        }.getOrNull()

    override fun webViewMajor(): String? =
        runCatching {
            WebView.getCurrentWebViewPackage()?.versionName?.substringBefore('.')
        }.getOrNull()
}

/**
 * Chrome-compatible User Agent provider that generates UA strings matching Chrome Stable.
 * Detects Chrome version via package manager or falls back to WebView version.
 */
public class ChromeUAProvider(
    private val versionProvider: VersionProvider,
) : UAProvider {

    private val _uaFlow = MutableStateFlow(generateMobileUA())
    override val uaFlow: StateFlow<String> = _uaFlow.asStateFlow()

    private var customUA: String? = null

    override fun userAgent(desktop: Boolean): String {
        return customUA ?: if (desktop) generateDesktopUA() else generateMobileUA()
    }

    override fun setCustomUserAgent(ua: String?) {
        customUA = ua
        _uaFlow.value = ua ?: generateMobileUA()
    }

    private fun generateMobileUA(): String {
        val chromeVersion = getChromeVersion()
        val androidVersion = Build.VERSION.RELEASE
        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

        return "Mozilla/5.0 (Linux; Android $androidVersion; $deviceModel) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/$chromeVersion Mobile Safari/537.36"
    }

    private fun generateDesktopUA(): String {
        val chromeVersion = getChromeVersion()

        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
                "AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/$chromeVersion Safari/537.36"
    }

    private fun getChromeVersion(): String {
        // Try Chrome Stable first, then WebView, then fallback
        return versionProvider.chromeMajor()?.let { "$it.0.0.0" }
            ?: versionProvider.webViewMajor()?.let { "$it.0.0.0" }
            ?: "120.0.0.0" // Fallback to recent stable version
    }
}
