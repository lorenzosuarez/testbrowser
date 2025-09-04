package com.testlabs.browser.ui.browser

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
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
public class AndroidVersionProvider(private val context: Context) : VersionProvider {
    override fun chromeMajor(): String? = runCatching {
        context.packageManager.getPackageInfo("com.android.chrome", 0)
            .versionName?.substringBefore('.')
    }.getOrNull()

    override fun webViewMajor(): String? = runCatching {
        WebView.getCurrentWebViewPackage()?.versionName?.substringBefore('.')
    }.getOrNull()
}

/**
 * Default implementation supplying Chrome user agents using runtime version information.
 */
public class DefaultUAProvider(
    private val context: Context,
    private val versionProvider: VersionProvider = AndroidVersionProvider(context),
    registerReceivers: Boolean = true,
) : UAProvider {

    private val _uaFlow = MutableStateFlow(buildMobileUa())
    override val uaFlow: StateFlow<String> = _uaFlow.asStateFlow()

    private var customUa: String? = null

    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(ctx: Context?, intent: Intent?) {
            val pkg = intent?.data?.schemeSpecificPart ?: return
            if (pkg == "com.android.chrome" ||
                pkg == WebView.getCurrentWebViewPackage()?.packageName
            ) {
                refresh()
            }
        }
    }

    init {
        if (registerReceivers) {
            val filter = IntentFilter(Intent.ACTION_PACKAGE_REPLACED).apply {
                addDataScheme("package")
            }
            context.registerReceiver(packageReceiver, filter)
        }
    }

    override fun userAgent(desktop: Boolean): String {
        val ua = customUa ?: if (desktop) buildDesktopUa() else buildMobileUa()
        _uaFlow.value = ua
        return ua
    }

    override fun setCustomUserAgent(ua: String?) {
        customUa = ua
        _uaFlow.value = ua ?: buildMobileUa()
    }

    private fun refresh() {
        _uaFlow.value = customUa ?: buildMobileUa()
    }

    private fun buildMobileUa(): String {
        val major = versionProvider.chromeMajor()
            ?: versionProvider.webViewMajor()
            ?: FALLBACK_CHROME_MAJOR
        val androidVersion = Build.VERSION.RELEASE
        val model = Build.MODEL
        return "Mozilla/5.0 (Linux; Android $androidVersion; $model) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$major.0.0.0 Mobile Safari/537.36"
    }

    private fun buildDesktopUa(): String {
        val major = versionProvider.chromeMajor()
            ?: versionProvider.webViewMajor()
            ?: FALLBACK_CHROME_MAJOR
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/$major.0.0.0 Safari/537.36"
    }

    private companion object {
        private const val FALLBACK_CHROME_MAJOR = "120"
    }
}

