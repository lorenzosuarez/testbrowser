package com.testlabs.browser.core

import android.net.Uri
import android.os.SystemClock
import android.webkit.WebResourceRequest
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

public object SmartBypass {
    private const val TTL_MS: Long = 5 * 60_000L

    private val expirations = ConcurrentHashMap<String, Long>()

    public enum class Route { BYPASS, PROXY }

    public data class Decision(val route: Route, val reason: String)

    private val staticExt = setOf(
        ".js", ".mjs", ".css", ".png", ".jpg", ".webp", ".gif", ".svg",
        ".ico", ".woff", ".woff2", ".ttf", ".otf", ".map"
    )

    @JvmStatic
    public fun decide(request: WebResourceRequest): Decision {
        val uri = request.url
        val scheme = uri.scheme?.lowercase(Locale.US)
        if (scheme != "http" && scheme != "https") {
            return Decision(Route.BYPASS, "non-http")
        }

        val method = request.method.uppercase(Locale.US)
        if (method !in listOf("GET", "HEAD")) {
            return Decision(Route.BYPASS, "non-idempotent")
        }

        val origin = originOf(uri)
        if (isActive(origin)) {
            return Decision(Route.BYPASS, "ttl")
        }

        val host = uri.host?.lowercase(Locale.US).orEmpty()
        val path = uri.path?.lowercase(Locale.US).orEmpty()
        val hasExt = staticExt.any { path.endsWith(it) }
        val headers = request.requestHeaders
        val accept = headers["Accept"]?.lowercase(Locale.US) ?: ""
        val apiHost = host.startsWith("api.") || host.contains("api-") || host.contains("ip-scan")
        val apiPath = path.contains("/api/") || path.contains("/config/") || path.contains("/ip")
        val apiAccept = accept.contains("application/json")
        if (apiHost || apiPath || apiAccept || !hasExt) {
            return Decision(Route.BYPASS, "api-get")
        }

        return Decision(Route.PROXY, "static")
    }

    @JvmStatic
    public fun isActive(origin: String): Boolean {
        val exp = expirations[origin] ?: return false
        val now = SystemClock.elapsedRealtime()
        return if (exp > now) true else { expirations.remove(origin); false }
    }

    @JvmStatic
    public fun markOnce(origin: String): Boolean {
        val now = SystemClock.elapsedRealtime()
        val existing = expirations[origin]
        return if (existing != null && existing > now) {
            false
        } else {
            expirations[origin] = now + TTL_MS
            true
        }
    }

    @JvmStatic
    public fun originOf(uri: Uri): String {
        val scheme = uri.scheme?.lowercase(Locale.US).orEmpty()
        val host = uri.host?.lowercase(Locale.US).orEmpty()
        val port = uri.port
        val default = when (scheme) {
            "http" -> 80
            "https" -> 443
            else -> -1
        }
        return if (port == -1 || port == default) "$scheme://$host" else "$scheme://$host:$port"
    }
}
