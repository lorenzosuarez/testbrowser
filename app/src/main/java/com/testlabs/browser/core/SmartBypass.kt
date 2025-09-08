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
     * Returns true when the request should bypass the proxy.
     * Cheap heuristics only; no network I/O.
     */
    @JvmStatic
    public fun shouldBypass(request: WebResourceRequest): Boolean {
        val url = request.url
        val scheme = url.scheme?.lowercase(Locale.US)
        if (scheme != "http" && scheme != "https") return true

        if (request.isForMainFrame && !request.method.equals("GET", true)) return true

        val origin = canonicalOrigin(url)
        expirations[origin]?.let { exp ->
            val now = SystemClock.elapsedRealtime()
            if (exp > now) return true else expirations.remove(origin)
        }

        val headers = request.requestHeaders
        headers.keys.firstOrNull { it.equals("Range", true) || it.equals("If-Range", true) }?.let {
            return true
        }
        headers["Upgrade"]?.let { return true }
        headers["Connection"]?.let { if (it.lowercase(Locale.US).contains("upgrade")) return true }
        headers.entries.firstOrNull { it.key.equals("Content-Length", true) }
            ?.value?.toLongOrNull()?.let { if (it > MAX_UPLOAD_BYTES) return true }

        return false
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
