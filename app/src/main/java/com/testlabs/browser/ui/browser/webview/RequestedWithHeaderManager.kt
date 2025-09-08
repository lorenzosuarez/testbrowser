/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.ui.browser.webview

import android.annotation.SuppressLint
import android.webkit.WebView
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.testlabs.browser.domain.settings.RequestedWithHeaderMode
import com.testlabs.browser.domain.settings.WebViewConfig

/**
 * Handles Requested-With header policy configuration for WebView and Service Workers.
 */
public object RequestedWithHeaderManager {

    @SuppressLint("WebViewFeature", "RequiresFeature", "RestrictedApi")
    public fun applyPolicy(webView: WebView, config: WebViewConfig) {
        val allow: Set<String> = when (config.requestedWithHeaderMode) {
            RequestedWithHeaderMode.ELIMINATED -> emptySet()
            RequestedWithHeaderMode.ALLOW_LIST -> config.requestedWithHeaderAllowList
            RequestedWithHeaderMode.UNSUPPORTED -> return
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)) {
            try {
                WebSettingsCompat.setRequestedWithHeaderOriginAllowList(webView.settings, allow)
            } catch (iae: IllegalArgumentException) {
                val sanitized = allow.filterIsOriginLike().toSet()
                WebSettingsCompat.setRequestedWithHeaderOriginAllowList(webView.settings, sanitized)
            }
        } else {
            return
        }

        if (WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE) &&
            WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST)
        ) {
            val swSettings = ServiceWorkerControllerCompat
                .getInstance()
                .serviceWorkerWebSettings

            try {
                swSettings.requestedWithHeaderOriginAllowList = allow
            } catch (iae: IllegalArgumentException) {
                val sanitized = allow.filterIsOriginLike().toSet()
                swSettings.requestedWithHeaderOriginAllowList = sanitized
            }
        }
    }

    private fun Iterable<String>.filterIsOriginLike(): List<String> =
        this.map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { it.startsWith("https://") || it.startsWith("http://") }
            .map { it.removeSuffix("/") }
}
