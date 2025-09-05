package com.testlabs.browser.ui.browser

import android.annotation.SuppressLint
import androidx.webkit.WebSettingsCompat


/**
 * Effective policy for the X-Requested-With header in WebView.
 *
 * UNKNOWN: engine does not expose allow-list control.
 * NO_HEADER: no origin is allow-listed, header will not be sent.
 * ALLOW_LIST: at least one origin is allow-listed; header may be sent to those origins.
 */
public enum class RequestedWithHeaderMode { UNKNOWN, NO_HEADER, ALLOW_LIST }

@SuppressLint("WebViewFeature", "RequiresFeature")
internal fun requestedWithHeaderModeOf(webView: android.webkit.WebView): RequestedWithHeaderMode =
    runCatching {
        val allow = WebSettingsCompat.getRequestedWithHeaderOriginAllowList(webView.settings)
        if (allow.isEmpty()) RequestedWithHeaderMode.NO_HEADER else RequestedWithHeaderMode.ALLOW_LIST
    }.getOrElse { RequestedWithHeaderMode.UNKNOWN }
