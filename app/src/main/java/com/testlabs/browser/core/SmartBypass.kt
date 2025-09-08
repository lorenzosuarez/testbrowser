package com.testlabs.browser.core

import android.net.Uri
import android.os.SystemClock
import android.webkit.WebResourceRequest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * File: core/bypass/SmartBypass.kt
 * Purpose: Centralized, thread-safe bypass registry and decision logic for proxy usage.
 * Overview:
 * - Decides when to bypass the proxy based on request properties and learned site health.
 * - Uses an in-memory TTL cache keyed by canonical origin to remember problematic sites.
 * - Wall-clock independent via elapsedRealtime; safe across sleep/clock changes.
 */
public object SmartBypass {
    private val ttlMs: Long = TimeUnit.HOURS.toMillis(1)
    private val expirations = ConcurrentHashMap<String, Long>()

    /**
     * Returns true when the request should bypass the proxy.
     * Rules:
     * - Non-HTTP(S) schemes are bypassed.
     * - Non-GET methods are bypassed due to body forwarding constraints in WebView.
     * - Origins previously marked as unhealthy are bypassed until TTL expiration.
     */
    @JvmStatic
    public fun shouldBypass(request: WebResourceRequest): Boolean {
        val url = request.url
        val scheme = url.scheme?.lowercase()
        if (scheme != "http" && scheme != "https") return true
        if (!request.method.equals("GET", ignoreCase = true)) return true
        val key = canonicalOrigin(url)
        val exp = expirations[key]
        if (exp == null) return false
        if (exp > SystemClock.elapsedRealtime()) return true
        expirations.remove(key)
        return false
    }

    /**
     * Marks the origin as unhealthy for a TTL window. Returns true if this is the first activation
     * for the origin within the current TTL, which is useful to decide a single retry without proxy.
     */
    @JvmStatic
    public fun activate(url: Uri): Boolean {
        val key = canonicalOrigin(url)
        val first = expirations.put(key, SystemClock.elapsedRealtime() + ttlMs) == null
        return first
    }

    /**
     * Computes a canonical origin key of the form: scheme://host[:port]
     * Default ports are omitted; explicit ports are retained.
     */
    @JvmStatic
    public fun canonicalOrigin(uri: Uri): String {
        val scheme = uri.scheme?.lowercase().orEmpty()
        val host = uri.host?.lowercase().orEmpty()
        val port = uri.port
        val needsPort = port != -1 && port != defaultPortFor(scheme)
        return if (needsPort) "$scheme://$host:$port" else "$scheme://$host"
    }

    /**
     * Clears all learned entries. Intended for testing or manual reset.
     */
    @JvmStatic
    public fun clear() {
        expirations.clear()
    }

    private fun defaultPortFor(scheme: String): Int? = when (scheme) {
        "http" -> 80
        "https" -> 443
        else -> null
    }
}