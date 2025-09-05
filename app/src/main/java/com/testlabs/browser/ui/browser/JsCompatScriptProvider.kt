package com.testlabs.browser.ui.browser

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.webkit.WebView
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/** Provides device metrics for the JS compatibility layer. */
public interface DeviceInfoProvider {
    public val hardwareConcurrency: Int
    public val deviceMemory: Int
    public val platform: String?
}

/** Default implementation sourcing metrics from the system. */
public class DefaultDeviceInfoProvider(private val context: Context) : DeviceInfoProvider {
    override val hardwareConcurrency: Int
        get() = Runtime.getRuntime().availableProcessors().coerceAtMost(8)

    override val deviceMemory: Int
        get() {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            val gb = (info.totalMem / (1024L * 1024L * 1024L)).toInt()
            return when {
                gb >= 12 -> 12
                gb >= 8 -> 8
                gb >= 6 -> 6
                gb >= 4 -> 4
                else -> 2
            }
        }

    override val platform: String?
        get() = if (Build.SUPPORTED_ABIS.any { it.contains("64") }) "Linux aarch64" else null
}

/** Builds and applies document-start JavaScript for Chrome-like signals. */
public class JsCompatScriptProvider(private val deviceInfoProvider: DeviceInfoProvider) {
    private val ids = mutableMapOf<WebView, String>()

    public fun apply(webView: WebView, acceptLanguages: String, enabled: Boolean) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return
        val existing = ids[webView]
        if (enabled) {
            if (existing == null) {
                val script = buildScript(acceptLanguages)
                val id = WebViewCompat.addDocumentStartJavaScript(webView, script, setOf("*"))
                ids[webView] = id
            }
        } else {
            if (existing != null) {
                WebViewCompat.removeDocumentStartJavaScript(webView, existing)
                ids.remove(webView)
            }
        }
    }

    private fun buildScript(acceptLanguages: String): String {
        val langTokens = acceptLanguages.split(',').map { it.substringBefore(';') }
        val language = langTokens.firstOrNull() ?: "en-US"
        val languagesArray = langTokens.joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]")
        val hw = deviceInfoProvider.hardwareConcurrency
        val mem = deviceInfoProvider.deviceMemory
        val platformEntry = deviceInfoProvider.platform?.let {
            "Object.defineProperty(navigator,'platform',{value:'$it',configurable:false});"
        } ?: ""
        return "(()=>{Object.defineProperty(navigator,'vendor',{value:'Google Inc.',configurable:false});" +
            "Object.defineProperty(navigator,'hardwareConcurrency',{value:$hw,configurable:false});" +
            "Object.defineProperty(navigator,'deviceMemory',{value:$mem,configurable:false});" +
            "Object.defineProperty(navigator,'languages',{value:$languagesArray,configurable:false});" +
            "Object.defineProperty(navigator,'language',{value:'$language',configurable:false});" +
            platformEntry + "})()"
    }
}
