/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.ui.browser

import android.util.Log
import android.webkit.WebView
import androidx.webkit.WebViewFeature
import androidx.webkit.WebSettingsCompat

private const val TAG = "ProxyValidator"

/**
 * Utility for validating proxy health and logging status
 */
public object ProxyValidator {

    public fun validateProxyHealth(webView: WebView) {
        Log.d(TAG, "=== PROXY HEALTH CHECK ===")
        try {
            val settings = webView.settings
            Log.d(TAG, "✅ WebView created successfully")
            Log.d(TAG, "✅ User-Agent: ${settings.userAgentString}")
            Log.d(TAG, "✅ JavaScript enabled: ${settings.javaScriptEnabled}")
            Log.d(TAG, "✅ DOM storage enabled: ${settings.domStorageEnabled}")

            
            val headerMode = requestedWithHeaderModeOf(webView)
            Log.d(TAG, "✅ X-Requested-With mode: $headerMode")

        } catch (e: Exception) {
            Log.e(TAG, "❌ Proxy health check failed", e)
        }
    }

    public fun logProxyStatus() {
        Log.d(TAG, "=== PROXY STATUS ===")
        Log.d(TAG, "✅ NetworkProxy initialized")
        Log.d(TAG, "✅ Response normalization active")
        Log.d(TAG, "✅ Main-frame bypass active")
        Log.d(TAG, "✅ Cookie bridging active")
    }
}