/**
 * File: core/network/ChromeHeaderSanitizer.kt
 * Purpose: Outgoing header scrubber to avoid debug/impl leaks and preserve Chrome parity.
 * Integration:
 * - Invoke [sanitizeOutgoing] before building the Cronet/OkHttp request.
 * - Removes X-Proxy-Engine and any casing variants.
 */
package com.testlabs.browser.network

public object ChromeHeaderSanitizer {
    /**
     * Removes non-Chrome headers that could reveal proxy implementation details.
     */
    @JvmStatic
    public fun sanitizeOutgoing(headers: MutableMap<String, String>) {
        headers.keys.removeAll { it.equals("X-Proxy-Engine", ignoreCase = true) }
    }
}
