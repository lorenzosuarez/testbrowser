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

    public data class Decision(val route: Route, val reason: String, val fingerprint: String? = null)

    private val STATIC_WHITELIST = setOf(
        ".js", ".mjs", ".css", ".png", ".jpg", ".jpeg", ".webp", ".gif", ".svg",
        ".ico", ".woff", ".woff2", ".ttf", ".otf", ".map"
    )

    private val API_KEYWORDS = setOf(
        "api", "config", "graphql", "token", "auth", "v1", "v2",
        "ip", "status", "probe", "check", "report", "metrics"
    )

    @JvmStatic
    public fun decide(
        request: WebResourceRequest,
        proxyMainDocument: Boolean,
        debugFeatures: Boolean,
    ): Decision {
        val uri = request.url
        val headers = request.requestHeaders

        val method = request.method.uppercase(Locale.US)
        val scheme = uri.scheme?.lowercase(Locale.US) ?: ""
        val path = uri.path?.lowercase(Locale.US).orEmpty()
        val lastSegment = path.substringAfterLast('/')
        val extension = lastSegment.substringAfterLast('.', "").lowercase(Locale.US)
        val hasExt = extension.isNotEmpty()
        val hasStaticExt = STATIC_WHITELIST.contains(".$extension")
        val dest = headers["Sec-Fetch-Dest"]?.lowercase(Locale.US)
        val accept = headers["Accept"]?.lowercase(Locale.US) ?: ""
        val acceptsJson = accept.contains("application/json")
        val acceptsText = accept.contains("text/plain")
        val hasQuery = !uri.query.isNullOrEmpty()
        val looksApiPath = API_KEYWORDS.any { path.contains(it) }
        val origin = originOf(uri)

        val fingerprint = if (debugFeatures) buildString {
            append("feat[")
            append("dest=")
            append(dest ?: "-")
            append(";ext=")
            append(if (extension.isNotEmpty()) extension else "-")
            append(";accept=json?")
            append(if (acceptsJson) "1" else "0")
            append(";apiPath=")
            append(if (looksApiPath) "1" else "0")
            append(";query=")
            append(if (hasQuery) "1" else "0")
            append("]")
        } else null

        if (isActive(origin)) {
            return Decision(Route.BYPASS, "ttl", fingerprint)
        }

        if (scheme != "http" && scheme != "https") {
            return Decision(Route.BYPASS, "non-http", fingerprint)
        }

        if (method !in listOf("GET", "HEAD")) {
            return Decision(Route.BYPASS, "non-idempotent", fingerprint)
        }

        if (request.isForMainFrame || dest == "document") {
            return if (proxyMainDocument) {
                Decision(Route.PROXY, "document", fingerprint)
            } else {
                Decision(Route.BYPASS, "document", fingerprint)
            }
        }

        if (dest != null) {
            when (dest) {
                "image", "style", "script", "font" -> return Decision(Route.PROXY, "static-get", fingerprint)
            }
        }

        if (hasStaticExt) {
            return Decision(Route.PROXY, "static-get", fingerprint)
        }

        if (acceptsJson || acceptsText) {
            return Decision(Route.BYPASS, "api-like", fingerprint)
        }

        if (looksApiPath) {
            return Decision(Route.BYPASS, "api-like", fingerprint)
        }

        if (!hasExt && hasQuery) {
            return Decision(Route.BYPASS, "api-like", fingerprint)
        }

        return Decision(Route.BYPASS, "default", fingerprint)
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
