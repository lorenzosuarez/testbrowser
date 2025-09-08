/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.ui.browser.webview

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.WebView
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.ServiceWorkerWebSettingsCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.testlabs.browser.domain.settings.WebViewConfig

/**
 * Handles Requested-With header policy configuration for WebView and Service Workers.
 */
public object RequestedWithHeaderManager {

    private const val TAG = "RequestedWith"

    @SuppressLint("WebViewFeature", "RequiresFeature", "RestrictedApi")
    public fun applyPolicy(webView: WebView, config: WebViewConfig) {
        val allow = emptySet<String>()
        val control = WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_CONTROL)
        val allowList = WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)

        if (control) {
            try {
                WebSettingsCompat.setRequestedWithHeaderMode(
                    webView.settings,
                    WebSettingsCompat.REQUESTED_WITH_HEADER_MODE_REMOVE
                )
                Log.d(TAG, "X-Requested-With REMOVE")
            } catch (_: IllegalArgumentException) {
                Log.d(TAG, "X-Requested-With REMOVE")
                WebSettingsCompat.setRequestedWithHeaderMode(
                    webView.settings,
                    WebSettingsCompat.REQUESTED_WITH_HEADER_MODE_REMOVE
                )
            }
        } else if (allowList) {
            try {
                WebSettingsCompat.setRequestedWithHeaderOriginAllowList(webView.settings, allow)
                Log.d(TAG, "X-Requested-With REMOVE")
            } catch (_: IllegalArgumentException) {
                Log.d(TAG, "X-Requested-With REMOVE")
                WebSettingsCompat.setRequestedWithHeaderOriginAllowList(webView.settings, allow)
            }
        } else {
            Log.d(TAG, "X-Requested-With UNSUPPORTED")
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE)) {
            val swSettings = ServiceWorkerControllerCompat
                .getInstance()
                .serviceWorkerWebSettings
            if (control) {
                try {
                    swSettings.requestedWithHeaderMode = ServiceWorkerWebSettingsCompat.REQUESTED_WITH_HEADER_MODE_REMOVE
                } catch (_: IllegalArgumentException) {
                    swSettings.requestedWithHeaderMode = ServiceWorkerWebSettingsCompat.REQUESTED_WITH_HEADER_MODE_REMOVE
                }
            } else if (allowList) {
                try {
                    swSettings.requestedWithHeaderOriginAllowList = allow
                } catch (_: IllegalArgumentException) {
                    swSettings.requestedWithHeaderOriginAllowList = allow
                }
            }
        }
    }

    private fun Iterable<String>.filterIsOriginLike(): List<String> =
        this.map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { it.startsWith("https://") || it.startsWith("http://") }
            .map { it.removeSuffix("/") }
}
