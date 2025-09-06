package com.testlabs.browser.ui.browser

import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.testlabs.browser.network.HttpStack
import com.testlabs.browser.network.ProxyRequest
import com.testlabs.browser.network.ProxyResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.brotli.dec.BrotliInputStream
import java.io.ByteArrayInputStream
import java.net.URI
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

private const val TAG = "NetworkProxy"

/**
 * HTTP(S) subresource proxy for WebView.
 * - Skips main-frame HTML.
 * - Mirrors UA/Accept-Language and Cookie bridge.
 * - Follows redirects (max 5) and never returns 3xx to WebView.
 * - Decodes gzip/deflate/brotli and strips hop-by-hop headers.
 */
public class NetworkProxy(
    private val stack: HttpStack,
    private val cookieManager: CookieManager = CookieManager.getInstance(),
) {
    public val stackName: String get() = stack.name

    public fun interceptRequest(
        request: WebResourceRequest,
        userAgent: String,
        acceptLanguage: String,
        proxyEnabled: Boolean
    ): WebResourceResponse? {
        val url = request.url.toString()
        if (!proxyEnabled) return null
        if (request.isForMainFrame && isHtmlRequest(request, url)) return null
        if (!shouldProxy(url)) return null

        return try {
            runBlocking(Dispatchers.IO) {
                val headers = buildRequestHeaders(request, userAgent, acceptLanguage, url)
                headers["Accept-Encoding"] = "gzip, deflate, br"

                var currentUrl = url
                var response = stack.execute(ProxyRequest(currentUrl, request.method, headers, null))

                var hops = 0
                while (response.statusCode in 300..399 && hops < 5) {
                    val loc = firstHeader(response.headers, "Location") ?: firstHeader(response.headers, "location")
                    if (loc.isNullOrBlank()) break
                    response.body.close()
                    currentUrl = resolveUrl(currentUrl, loc)
                    response = stack.execute(ProxyRequest(currentUrl, "GET", headers, null))
                    hops++
                }

                handleSetCookies(currentUrl, response.headers)
                normalizeResponse(response)
            }
        } catch (e: Exception) {
            Log.e(TAG, "proxy error: $url", e)
            errorResponse()
        }
    }

    private fun isHtmlRequest(request: WebResourceRequest, url: String): Boolean {
        val accept = request.requestHeaders["Accept"] ?: ""
        return accept.contains("text/html", true) ||
                url.endsWith(".html", true) ||
                url.endsWith("/", true) ||
                (!url.contains('.', true) && !url.contains("api", true))
    }

    private fun shouldProxy(url: String): Boolean {
        val scheme = url.substringBefore(':').lowercase(Locale.US)
        if (scheme !in setOf("http", "https")) return false
        if (url.startsWith("chrome://") || url.startsWith("about:")) return false
        return true
    }

    private fun buildRequestHeaders(
        request: WebResourceRequest,
        userAgent: String,
        acceptLanguage: String,
        url: String
    ): MutableMap<String, String> {
        val headers = mutableMapOf<String, String>()
        request.requestHeaders.forEach { (name, value) ->
            when (name.lowercase(Locale.US)) {
                "x-requested-with" -> Unit
                "user-agent" -> headers["User-Agent"] = userAgent
                "accept-language" -> headers["Accept-Language"] = acceptLanguage
                else -> headers[name] = value
            }
        }
        headers.putIfAbsent("User-Agent", userAgent)
        headers.putIfAbsent("Accept-Language", acceptLanguage)
        try {
            val cookies = cookieManager.getCookie(url)
            if (!cookies.isNullOrBlank()) headers["Cookie"] = cookies
        } catch (_: Throwable) {}
        return headers
    }

    private fun handleSetCookies(url: String, headers: Map<String, List<String>>) {
        headers.forEach { (name, values) ->
            if (name.equals("Set-Cookie", true)) {
                values.forEach { v ->
                    try { cookieManager.setCookie(url, v) } catch (_: Throwable) {}
                }
            }
        }
        try { cookieManager.flush() } catch (_: Throwable) {}
    }

    private fun normalizeResponse(response: ProxyResponse): WebResourceResponse {
        if (response.statusCode in 300..399) {
            return WebResourceResponse(
                "text/plain",
                "UTF-8",
                204,
                "No Content",
                emptyMap(),
                ByteArrayInputStream(ByteArray(0))
            )
        }

        val ct = firstHeaderCI(response.headers, "Content-Type") ?: "application/octet-stream"
        val (mime, cs) = parseContentType(ct)
        val enc = firstHeaderCI(response.headers, "Content-Encoding")?.lowercase(Locale.US)

        val body = response.body.readBytes()
        val decoded = if (enc in setOf("gzip", "deflate", "br")) decodeBody(body, enc!!) else body

        val single = headersToSingleMap(response.headers).apply {
            keys.removeAll(listOf("Content-Encoding", "content-encoding", "Transfer-Encoding", "transfer-encoding", "Content-Length", "content-length"))
        }

        return WebResourceResponse(
            mime,
            cs ?: "UTF-8",
            response.statusCode,
            response.reasonPhrase.ifBlank { defaultReason(response.statusCode) },
            filterResponseHeaders(single),
            ByteArrayInputStream(decoded)
        )
    }

    private fun parseContentType(contentType: String): Pair<String, String?> {
        val parts = contentType.split(';').map { it.trim() }
        val mime = parts.firstOrNull()?.lowercase(Locale.US).orEmpty().ifBlank { "application/octet-stream" }
        val charset = parts.firstOrNull { it.startsWith("charset=", true) }?.substringAfter('=')?.trim('"', ' ')
            ?: when {
                mime.startsWith("text/") -> "UTF-8"
                mime == "application/javascript" || mime == "text/javascript" || mime == "application/json" -> "UTF-8"
                else -> null
            }
        return mime to charset
    }

    private fun decodeBody(body: ByteArray, encoding: String): ByteArray = try {
        when (encoding) {
            "gzip" -> GZIPInputStream(ByteArrayInputStream(body)).readBytes()
            "deflate" -> InflaterInputStream(ByteArrayInputStream(body)).readBytes()
            "br" -> BrotliInputStream(ByteArrayInputStream(body)).readBytes()
            else -> body
        }
    } catch (_: Throwable) { body }

    private fun firstHeader(headers: Map<String, List<String>>, name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, true) }?.value?.firstOrNull()

    private fun firstHeaderCI(headers: Map<String, List<String>>, name: String): String? =
        headers.entries.firstOrNull { it.key.equals(name, true) }?.value?.firstOrNull()

    private fun resolveUrl(base: String, location: String): String = try {
        URI(base).resolve(location).toString()
    } catch (_: Throwable) { location }

    private fun headersToSingleMap(headers: Map<String, List<String>>): MutableMap<String, String> =
        headers.mapValues { it.value.joinToString(",") }.toMutableMap()

    private fun filterResponseHeaders(headers: Map<String, String>): Map<String, String> {
        val hop = setOf("connection", "keep-alive", "proxy-authenticate", "proxy-authorization", "te", "trailers", "transfer-encoding", "upgrade")
        return headers.filterKeys { it.lowercase(Locale.US) !in hop }
    }

    private fun defaultReason(code: Int): String = when (code) {
        200 -> "OK"
        201 -> "Created"
        204 -> "No Content"
        301 -> "Moved Permanently"
        302 -> "Found"
        304 -> "Not Modified"
        400 -> "Bad Request"
        401 -> "Unauthorized"
        403 -> "Forbidden"
        404 -> "Not Found"
        500 -> "Internal Server Error"
        502 -> "Bad Gateway"
        503 -> "Service Unavailable"
        else -> "Unknown"
    }

    private fun errorResponse(): WebResourceResponse =
        WebResourceResponse("text/plain", "UTF-8", 502, "Bad Gateway", emptyMap(), ByteArrayInputStream(ByteArray(0)))
}