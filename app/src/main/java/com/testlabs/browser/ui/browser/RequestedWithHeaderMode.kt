/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.ui.browser

import android.annotation.SuppressLint
import androidx.webkit.WebSettingsCompat
import kotlin.runCatching


/**
 * Effective policy for the X-Requested-With header in WebView.
 */
public enum class RequestedWithHeaderMode {
    /** Header removed for all origins. */
    ELIMINATED,

    /** Header only sent to allow-listed origins. */
    ALLOW_LIST,

    /** WebView does not expose header control. */
    UNKNOWN,
}

@SuppressLint("WebViewFeature", "RequiresFeature")
internal fun requestedWithHeaderModeOf(webView: android.webkit.WebView): RequestedWithHeaderMode =
    runCatching {
        val allow = WebSettingsCompat.getRequestedWithHeaderOriginAllowList(webView.settings)
        if (allow.isEmpty()) RequestedWithHeaderMode.ELIMINATED else RequestedWithHeaderMode.ALLOW_LIST
    }.getOrElse { RequestedWithHeaderMode.UNKNOWN }

internal fun parseRequestedWithHeaderAllowList(raw: String): Set<String> = raw
    .split(',', ' ', '\n')
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .toSet()
