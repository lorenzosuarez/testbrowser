/**
 * File: core/bypass/SmartBypassEvents.kt
 * Purpose: Event hooks to feed site health signals back into the bypass registry.
 * Integration:
 * - Invoke [onMainFrameNetworkError] from WebViewClient.onReceivedError for main-frame failures.
 * - Invoke [onMainFrameHttpError] from WebViewClient.onReceivedHttpError for main-frame HTTP errors.
 * Behavior:
 * - Marks the origin unhealthy and returns true if a one-time reload without proxy should be attempted.
 */
package com.testlabs.browser.core

import android.net.Uri

public object SmartBypassEvents {
    private val httpTriggerCodes = setOf(400, 403, 405, 415, 421, 425, 429, 451, 497, 598, 599)
    private const val DEFAULT_TTL = 10 * 60_000L

    /**
     * Records a transport-layer failure for the main frame and schedules a bypass.
     */
    @JvmStatic
    public fun onMainFrameNetworkError(url: Uri): Boolean {
        val origin = SmartBypass.canonicalOrigin(url)
        SmartBypass.markBypass(origin, DEFAULT_TTL, "network")
        return true
    }

    /**
     * Records an HTTP-layer failure for the main frame when the status code indicates proxy friction.
     */
    @JvmStatic
    public fun onMainFrameHttpError(url: Uri, statusCode: Int): Boolean {
        if (statusCode !in httpTriggerCodes) return false
        val origin = SmartBypass.canonicalOrigin(url)
        SmartBypass.markBypass(origin, DEFAULT_TTL, "http$statusCode")
        return true
    }
}
