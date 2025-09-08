/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */

package com.testlabs.browser.domain.settings

import android.annotation.SuppressLint
import android.webkit.WebView
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
    UNSUPPORTED,
}

@SuppressLint("WebViewFeature", "RequiresFeature")
public fun requestedWithHeaderModeOf(webView: WebView): RequestedWithHeaderMode =
    runCatching {
        val allow = WebSettingsCompat.getRequestedWithHeaderOriginAllowList(webView.settings)
        if (allow.isEmpty()) RequestedWithHeaderMode.ELIMINATED else RequestedWithHeaderMode.ALLOW_LIST
    }.getOrElse { RequestedWithHeaderMode.UNSUPPORTED }

public fun parseRequestedWithHeaderAllowList(raw: String): Set<String> = raw
    .split(',', ' ', '\n')
    .map { it.trim() }
    .filter { it.isNotEmpty() }
    .toSet()
