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

    /**
     * Records a transport-layer failure for the main frame and returns true if this is the first
     * activation within TTL, which is suitable to trigger a single reload without proxy.
     */
    @JvmStatic
    public fun onMainFrameNetworkError(url: Uri): Boolean {
        return SmartBypass.activate(url)
    }

    /**
     * Records an HTTP-layer failure for the main frame when the status code indicates proxy friction.
     * Returns true if this is the first activation within TTL, which is suitable to trigger a single
     * reload without proxy.
     */
    @JvmStatic
    public fun onMainFrameHttpError(url: Uri, statusCode: Int): Boolean {
        if (statusCode !in httpTriggerCodes) return false
        return SmartBypass.activate(url)
    }
}
