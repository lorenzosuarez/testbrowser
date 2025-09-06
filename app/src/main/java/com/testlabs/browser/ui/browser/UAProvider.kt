package com.testlabs.browser.ui.browser

import android.content.Context
import android.os.Build
import android.webkit.WebView
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Provides Chrome-like user agent values for the current device.
 */
public interface UAProvider {
    /** Returns a full UA string matching Chrome Mobile for the device. */
    public fun userAgent(desktop: Boolean = false): String

    /** Baseline Accept-Language string in Chrome style. */
    public val acceptLanguage: StateFlow<String>

    /** Current Chrome major version used for UA-CH. */
    public val chromeMajor: Int
}

/**
 * Android-backed UA provider that derives a Chrome-like UA from WebView/Chrome package version.
 */
public class ChromeUAProvider(
    private val versionProvider: VersionProvider
) : UAProvider {
    private val _acceptLanguage = MutableStateFlow("en-US,en;q=0.9")
    override val acceptLanguage: StateFlow<String> = _acceptLanguage

    override val chromeMajor: Int by lazy { versionProvider.chromeMajor() }

    override fun userAgent(desktop: Boolean): String {
        val androidVersion = versionProvider.androidVersion()
        val deviceModel = versionProvider.deviceModel()
        val brandModelToken = if (desktop) "" else " Mobile"
        val chromeFull = versionProvider.chromeFullVersion()
        return "Mozilla/5.0 (Linux; Android $androidVersion; $deviceModel) AppleWebKit/537.36 (KHTML, like Gecko)$brandModelToken Chrome/$chromeFull Mobile Safari/537.36"
    }
}

/**
 * Provides version and device information required to construct UA strings.
 */
public interface VersionProvider {
    /** Android version string such as 13 or 14. */
    public fun androidVersion(): String

    /** Full Chrome version such as 122.0.6261.105. */
    public fun chromeFullVersion(): String

    /** Chrome major version such as 122. */
    public fun chromeMajor(): Int

    /** Short device model token for UA. */
    public fun deviceModel(): String
}

/**
 * Default VersionProvider based on platform APIs.
 */
public class AndroidVersionProvider(private val context: Context) : VersionProvider {
    override fun androidVersion(): String = Build.VERSION.RELEASE ?: Build.VERSION.SDK_INT.toString()

    override fun chromeFullVersion(): String {
        val pkg = try { WebView.getCurrentWebViewPackage() } catch (_: Throwable) { null }
        val name = pkg?.versionName ?: fallbackChromeVersion()
        return name
    }

    override fun chromeMajor(): Int {
        val full = chromeFullVersion()
        val major = full.substringBefore(".").toIntOrNull()
        return major ?: 120
    }

    override fun deviceModel(): String {
        val model = Build.MODEL ?: "Android"
        return model.replace(" ", "_")
    }

    private fun fallbackChromeVersion(): String {
        val defaultMajor = 120
        return "$defaultMajor.0.0.0"
    }
}
