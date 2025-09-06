package com.testlabs.browser.ui.browser

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.webkit.WebView
import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature

public interface DeviceInfoProvider {
    public val hardwareConcurrency: Int
    public val deviceMemoryGiB: Double
    public val platform: String?
}

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
        val langs = acceptLanguages.split(",").map { it.substringBefore(";") }.filter { it.isNotBlank() }
        val primary = langs.firstOrNull() ?: "en-US"
        val arrayLiteral = langs.joinToString(prefix = "[\"", separator = "\",\"", postfix = "\"]")
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
        val platform = deviceInfoProvider.platform ?: "Linux armv8l"

        return """
            (() => {
              try {
                const d=(o,p,v)=>{try{Object.defineProperty(o,p,{get:()=>v,configurable:true});}catch(e){}};
                d(navigator,'vendor','Google Inc.');
                d(navigator,'hardwareConcurrency',$hw);
                d(navigator,'deviceMemory',$memBucket);
                d(navigator,'languages',$arrayLiteral);
                d(navigator,'language','$primary');
                d(navigator,'platform','$platform');
                d(navigator,'maxTouchPoints',5);
                d(navigator,'pdfViewerEnabled',true);
              } catch(e){}
            })();
        """.trimIndent()
    }
}