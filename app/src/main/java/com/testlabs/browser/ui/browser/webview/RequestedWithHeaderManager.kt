/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.ui.browser.webview

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.WebView
import androidx.webkit.ServiceWorkerControllerCompat
import androidx.webkit.WebSettingsCompat
import androidx.webkit.WebViewFeature
import com.testlabs.browser.domain.settings.WebViewConfig

public object RequestedWithHeaderManager {

    private const val TAG = "RequestedWith"

    @SuppressLint("WebViewFeature", "RestrictedApi", "RequiresFeature")
    public fun applyPolicy(webView: WebView, @Suppress("UNUSED_PARAMETER") config: WebViewConfig) {
        val hasAllowList =
            runCatching { WebViewFeature.isFeatureSupported(WebViewFeature.REQUESTED_WITH_HEADER_ALLOW_LIST) }
                .getOrDefault(false)

        if (hasAllowList) {
            runCatching {
                WebSettingsCompat.setRequestedWithHeaderOriginAllowList(
                    webView.settings,
                    emptySet()
                )
            }.onSuccess {
                Log.i(TAG, "X-Requested-With: ALLOWLIST size=0 (WebView)")
            }.onFailure {
                Log.w(TAG, "X-Requested-With: failed to set ALLOWLIST=0 (WebView): ${it.javaClass.simpleName}")
            }
        } else {
            Log.w(TAG, "X-Requested-With: UNSUPPORTED on this WebView build → rely on proxy & main-frame PROXY")
        }

        val hasSwBasic =
            runCatching { WebViewFeature.isFeatureSupported(WebViewFeature.SERVICE_WORKER_BASIC_USAGE) }
                .getOrDefault(false)

        if (hasSwBasic) {
            val swSettings = ServiceWorkerControllerCompat.getInstance().serviceWorkerWebSettings
            if (hasAllowList) {
                runCatching {
                    swSettings.requestedWithHeaderOriginAllowList = emptySet()
                }.onSuccess {
                    Log.i(TAG, "X-Requested-With: ALLOWLIST size=0 (ServiceWorker)")
                }.onFailure {
                    Log.w(TAG, "X-Requested-With: failed to set ALLOWLIST=0 (SW): ${it.javaClass.simpleName}")
                }
            } else {
                Log.w(TAG, "X-Requested-With (SW): UNSUPPORTED → rely on proxy & main-frame PROXY")
            }
        } else {
            Log.d(TAG, "ServiceWorker BASIC_USAGE not supported; skipping SW header policy")
        }
    }
}
