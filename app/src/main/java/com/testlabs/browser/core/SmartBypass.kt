package com.testlabs.browser.core

import android.net.Uri
import android.os.SystemClock
import android.util.Log
import android.webkit.WebResourceRequest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * File: core/bypass/SmartBypass.kt
 * Purpose: Centralized, thread-safe bypass registry and decision logic for proxy usage.
 * Overview:
 * - Decides when to bypass the proxy based on request properties and learned site health.
 * - Uses an in-memory TTL cache keyed by canonical origin to remember problematic sites.
 * - Wall-clock independent via elapsedRealtime; safe across sleep/clock changes.
 */
public object SmartBypass {
    private const val TAG = "SmartBypass"
    private const val MAX_UPLOAD_BYTES: Long = 5L * 1024 * 1024 // 5 MB

    private val expirations = ConcurrentHashMap<String, Long>()

    /**
     * Returns a reason string when the request should bypass the proxy, or null when proxying is
     * recommended. Cheap heuristics only; no network I/O.
     */
    @JvmStatic
    public fun bypassReason(request: WebResourceRequest): String? {
        val url = request.url
        val scheme = url.scheme?.lowercase(Locale.US)
        if (scheme != "http" && scheme != "https") return "non-http"

        val method = request.method.uppercase(Locale.US)
        if (method != "GET" && method != "HEAD") return "method-$method"

        val origin = canonicalOrigin(url)
        expirations[origin]?.let { exp ->
            val now = SystemClock.elapsedRealtime()
            return if (exp > now) "ttl-active" else { expirations.remove(origin); null }
        }

        val headers = request.requestHeaders
        headers.keys.firstOrNull { it.equals("Range", true) || it.equals("If-Range", true) }?.let {
            return "range"
        }
        headers["Upgrade"]?.let { return "upgrade" }
        headers["Connection"]?.let { if (it.lowercase(Locale.US).contains("upgrade")) return "upgrade" }
        headers.entries.firstOrNull { it.key.equals("Content-Length", true) }
            ?.value?.toLongOrNull()?.let { if (it > MAX_UPLOAD_BYTES) return "large-upload" }

        return null
    }

    /**
     * Marks the origin for bypass for [ttlMillis].
     */
    @JvmStatic
    public fun markBypass(origin: String, ttlMillis: Long, reason: String) {
        val until = SystemClock.elapsedRealtime() + ttlMillis
        expirations[origin] = until
        Log.d(TAG, "bypass $origin for ${ttlMillis}ms reason=$reason")
    }

    /**
     * Clears a learned bypass entry.
     */
    @JvmStatic
    public fun clearBypass(origin: String) {
        expirations.remove(origin)
    }

    /**
     * Computes a canonical origin key of the form: scheme://host[:port]
     * Default ports are omitted; explicit ports are retained.
     */
    @JvmStatic
    public fun canonicalOrigin(uri: Uri): String {
        val scheme = uri.scheme?.lowercase(Locale.US).orEmpty()
        val host = uri.host?.lowercase(Locale.US).orEmpty()
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
