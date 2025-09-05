package com.testlabs.browser.ui.browser

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.webkit.WebView
import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

/**
 * Supplies coarse-grained device signals aligned with web-exposed fingerprints.
 *
 * - hardwareConcurrency: clamped to [1, 8] to match typical Chrome privacy caps.
 * - deviceMemory: quantized to the Device Memory spec buckets {0.25, 0.5, 1, 2, 4, 8}.
 * - platform: normalized to Chrome-like values on Android ("Linux armv8l" or "Linux x86_64"),
 *   or null to avoid overriding if uncertain.
 *
 * Limitations:
 * - These values are approximations by design to reduce fingerprinting surface.
 */
public interface DeviceInfoProvider {
    public val hardwareConcurrency: Int
    public val deviceMemoryGiB: Double
    public val platform: String?
}

/**
 * Default implementation sourcing metrics from the system and quantizing them to web-facing buckets.
 */
public class DefaultDeviceInfoProvider(private val context: Context) : DeviceInfoProvider {
    override val hardwareConcurrency: Int
        get() = Runtime.getRuntime().availableProcessors().coerceIn(1, 8)

    override val deviceMemoryGiB: Double
        get() {
            val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val info = ActivityManager.MemoryInfo()
            am.getMemoryInfo(info)
            val gb = info.totalMem / (1024.0 * 1024.0 * 1024.0)
            return when {
                gb >= 8.0 -> 8.0
                gb >= 4.0 -> 4.0
                gb >= 2.0 -> 2.0
                gb >= 1.0 -> 1.0
                gb >= 0.5 -> 0.5
                else -> 0.25
            }
        }

    override val platform: String?
        get() {
            val abis = Build.SUPPORTED_ABIS.toList()
            val isX64 = abis.any { it.startsWith("x86_64") }
            val isArm = abis.any { it.startsWith("arm") || it.startsWith("arm64") }
            return when {
                isX64 -> "Linux x86_64"
                isArm -> "Linux armv8l"
                else -> null
            }
        }
}

/**
 * Installs and removes a document-start JavaScript shim using the modern handler-based API.
 *
 * Requirements:
 * - Requires WebViewFeature.DOCUMENT_START_SCRIPT at runtime.
 * - Returns immediately if the feature is not available.
 *
 * Behavior:
 * - When enabled, injects the script for all origins ("*") and stores a ScriptHandler.
 * - When disabled, calls ScriptHandler.remove() and forgets the handler.
 */
public class JsCompatScriptProvider(private val deviceInfoProvider: DeviceInfoProvider) {
    private val handlers = mutableMapOf<WebView, ScriptHandler>()

    public fun apply(webView: WebView, acceptLanguages: String, enabled: Boolean) {
        if (!WebViewFeature.isFeatureSupported(WebViewFeature.DOCUMENT_START_SCRIPT)) return
        val existing = handlers[webView]
        if (enabled && existing == null) {
            val script = buildScript(acceptLanguages)
            handlers[webView] = WebViewCompat.addDocumentStartJavaScript(webView, script, setOf("*"))
        } else if (!enabled && existing != null) {
            existing.remove()
            handlers.remove(webView)
        }
    }

    private fun buildScript(acceptLanguages: String): String {
        val langTokens = acceptLanguages.split(',').map { it.substringBefore(';') }.filter { it.isNotBlank() }
        val primary = langTokens.firstOrNull() ?: "en-US"
        val arrayLiteral = langTokens.joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]")
        val hw = deviceInfoProvider.hardwareConcurrency.coerceIn(1, 8)
        val mem = deviceInfoProvider.deviceMemoryGiB
        val memBucket = when {
            mem >= 8.0 -> "8"
            mem >= 4.0 -> "4"
            mem >= 2.0 -> "2"
            mem >= 1.0 -> "1"
            mem >= 0.5 -> "0.5"
            else -> "0.25"
        }
        val platform = deviceInfoProvider.platform
        val platformEntry = platform?.let { "d(navigator,'platform','$it');" } ?: ""
        return buildString {
            append("(function(){try{")
            append("const d=(o,p,v)=>{try{Object.defineProperty(o,p,{get:()=>v,configurable:true});}catch(e){}};")
            append("d(navigator,'vendor','Google Inc.');")
            append("d(navigator,'hardwareConcurrency',").append(hw).append(");")
            append("d(navigator,'deviceMemory',").append(memBucket).append(");")
            append("const langs=Object.freeze(").append(arrayLiteral).append(");")
            append("d(navigator,'languages',langs);")
            append("d(navigator,'language','").append(primary).append("');")
            append(platformEntry)
            append("}catch(e){}})();")
        }
    }
}

