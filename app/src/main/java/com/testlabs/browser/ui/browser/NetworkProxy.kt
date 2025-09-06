package com.testlabs.browser.ui.browser

import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.testlabs.browser.network.HttpStack
import com.testlabs.browser.network.ProxyRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.brotli.dec.BrotliInputStream
import java.io.ByteArrayInputStream
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream
import java.net.URI

private const val TAG = "NetworkProxy"

/**
 * Subresource proxy and response normalizer used by WebView interceptors.
 *
 * - Bypasses main-frame HTML navigations so WebView loads documents natively.
 * - Mirrors Chrome-like request headers and bridges cookies both ways.
 * - Normalizes responses to avoid decoding/CORS issues:
 *   If the body is decoded, strips Content-Encoding/Transfer-Encoding/Content-Length.
 *   If not decoded, passes through bytes and headers intact.
 * - Decodes only gzip/deflate/br and never zstd to avoid native dependencies.
 */
public class NetworkProxy(
    private val stack: HttpStack,
    private val cookieManager: android.webkit.CookieManager = android.webkit.CookieManager.getInstance(),
) {
    /** Returns the active HTTP stack name for diagnostics. */
    public val stackName: String get() = stack.name

    /**
     * Intercepts a single WebView request. Main-frame HTML requests are bypassed.
     */
    public fun interceptRequest(
        request: WebResourceRequest,
        userAgent: String,
        acceptLanguage: String,
        proxyEnabled: Boolean
    ): WebResourceResponse? {
        val url = request.url.toString()
        if (request.isForMainFrame && isHtmlRequest(request, url)) return null
        if (!shouldProxy(url)) return null
        if (!proxyEnabled) return null

        return try {
            runBlocking(Dispatchers.IO) {
                val headers = buildRequestHeaders(request, userAgent, acceptLanguage, url)
                var currentUrl = url
                var response = stack.execute(ProxyRequest(currentUrl, request.method, headers, null))

                // Seguir manualmente redirecciones (máx 5) para evitar excepciones 3xx en WebResourceResponse
                var hops = 0
                while (response.statusCode in 300..399 && hops < 5) {
                    val loc = firstHeader(response.headers, "Location") ?: firstHeader(response.headers, "location")
                    if (loc.isNullOrBlank()) break
                    val nextUrl = resolveUrl(currentUrl, loc)
                    try { response.body.close() } catch (_: Throwable) { }
                    currentUrl = nextUrl
                    response = stack.execute(ProxyRequest(currentUrl, "GET", headers, null))
                    hops++
                }

                handleSetCookies(currentUrl, response.headers)
                createNormalizedWebResourceResponse(response, currentUrl)
            }
        } catch (e: Exception) {
            Log.e(TAG, "proxy error: $url", e)
            createErrorResponse(e)
        }
    }

    /**
     * Heuristic to classify an HTML navigation.
     */
    private fun isHtmlRequest(request: WebResourceRequest, url: String): Boolean {
        val acceptHeader = request.requestHeaders["Accept"] ?: ""
        return acceptHeader.contains("text/html") ||
                url.endsWith(".html") ||
                url.endsWith("/") ||
                (!url.contains(".") && !url.contains("api"))
    }

    /**
     * Only HTTP/HTTPS are proxied.
     */
    private fun shouldProxy(url: String): Boolean {
        val scheme = url.substringBefore(':').lowercase(Locale.US)
        return scheme == "http" || scheme == "https"
    }

    /**
     * Builds Chrome-like request headers and attaches cookies read from CookieManager.
     */
    private fun buildRequestHeaders(
        request: WebResourceRequest,
        userAgent: String,
        acceptLanguage: String,
        url: String
    ): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        request.requestHeaders.forEach { (name, value) ->
            val lower = name.lowercase(Locale.US)
            when (lower) {
                "x-requested-with" -> Unit
                "user-agent" -> headers["User-Agent"] = userAgent
                "accept-language" -> headers["Accept-Language"] = acceptLanguage
                else -> headers[name] = value
            }
        }
        if (!headers.containsKey("User-Agent")) headers["User-Agent"] = userAgent
        if (!headers.containsKey("Accept-Language")) headers["Accept-Language"] = acceptLanguage
        try {
            val cookies = cookieManager.getCookie(url)
            if (!cookies.isNullOrBlank()) headers["Cookie"] = cookies
        } catch (_: Throwable) { }
        return headers
    }

    /**
     * Persists Set-Cookie headers into CookieManager and flushes them.
     */
    private fun handleSetCookies(url: String, headers: Map<String, List<String>>) {
        headers.forEach { (name, values) ->
            if (name.equals("Set-Cookie", true)) {
                values.forEach { v ->
                    try { cookieManager.setCookie(url, v) } catch (_: Throwable) { }
                }
            }
        }
        try { cookieManager.flush() } catch (_: Throwable) { }
    }

    /**
     * Produces a WebResourceResponse while enforcing decoding and header normalization rules.
     */
    private fun createNormalizedWebResourceResponse(
        response: com.testlabs.browser.network.ProxyResponse,
        url: String
    ): WebResourceResponse {
        // WebResourceResponse no admite códigos 3xx: si aún tenemos uno, devolvemos 204 vacía
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
        val contentTypeHeader = firstHeader(response.headers, "Content-Type")
            ?: firstHeader(response.headers, "content-type")
            ?: "application/octet-stream"
        val (mimeType, charset) = parseContentType(contentTypeHeader)

        val contentEncoding = firstHeader(response.headers, "Content-Encoding")
            ?: firstHeader(response.headers, "content-encoding")
        val bodyBytes = response.body.readBytes()

        val encoding = contentEncoding?.lowercase(Locale.US)
        val shouldAttemptDecode = when (encoding) {
            "gzip", "deflate", "br" -> true
            else -> false
        }

        return if (shouldAttemptDecode) {
            val decoded = decodeBody(bodyBytes, encoding!!)
            val single = headersToSingleMap(response.headers)
            single.remove("Content-Encoding"); single.remove("content-encoding")
            single.remove("Transfer-Encoding"); single.remove("transfer-encoding")
            single.remove("Content-Length"); single.remove("content-length")
            WebResourceResponse(
                mimeType,
                charset,
                response.statusCode,
                response.reasonPhrase.ifBlank { getDefaultStatusText(response.statusCode) },
                filterResponseHeaders(single),
                ByteArrayInputStream(decoded)
            )
        } else {
            WebResourceResponse(
                mimeType,
                charset,
                response.statusCode,
                response.reasonPhrase.ifBlank { getDefaultStatusText(response.statusCode) },
                filterResponseHeaders(headersToSingleMap(response.headers)),
                ByteArrayInputStream(bodyBytes)
            )
        }
    }

    /**
     * Parses Content-Type into MIME and charset with defaults.
     */
    private fun parseContentType(contentType: String): Pair<String, String> {
        val parts = contentType.split(';').map { it.trim() }
        val originalMime = parts.firstOrNull()?.lowercase(Locale.US).orEmpty().ifBlank { "application/octet-stream" }
        val charset = parts.find { it.startsWith("charset=", true) }?.substringAfter('=')?.trim('"', ' ')
            ?: if (originalMime.startsWith("text/") || originalMime == "application/javascript" || originalMime == "application/json") "UTF-8" else "UTF-8"
        return originalMime to charset
    }

    /**
     * Decodes gzip/deflate/brotli bodies. zstd is passed through unchanged.
     */
    private fun decodeBody(body: ByteArray, encoding: String): ByteArray {
        return try {
            when (encoding) {
                "gzip" -> GZIPInputStream(ByteArrayInputStream(body)).readBytes()
                "deflate" -> InflaterInputStream(ByteArrayInputStream(body)).readBytes()
                "br" -> BrotliInputStream(ByteArrayInputStream(body)).readBytes()
                else -> body
            }
        } catch (_: Throwable) {
            body
        }
    }

    /**
     * Case-insensitive single-value header accessor from a multimap.
     */
    private fun firstHeader(headers: Map<String, List<String>>, name: String): String? {
        val entry = headers.entries.firstOrNull { it.key.equals(name, true) } ?: return null
        return entry.value.firstOrNull()
    }

    /**
     * Resuelve la URL de redirección respecto a la URL base.
     */
    private fun resolveUrl(base: String, location: String): String {
        return try {
            val baseUri = URI(base)
            baseUri.resolve(location).toString()
        } catch (_: Throwable) {
            location
        }
    }

    /**
     * Converts a multimap of headers into single-value map by joining values with comma.
     */
    private fun headersToSingleMap(headers: Map<String, List<String>>): MutableMap<String, String> {
        return headers.mapValues { it.value.joinToString(",") }.toMutableMap()
    }

    /**
     * Filters hop-by-hop headers and forwards the rest unchanged.
     */
    private fun filterResponseHeaders(headers: Map<String, String>): Map<String, String> {
        val hop = setOf(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailers", "transfer-encoding", "upgrade"
        )
        return headers.filterKeys { it.lowercase(Locale.US) !in hop }
    }

    /**
     * Provides default reason phrases when missing.
     */
    private fun getDefaultStatusText(code: Int): String = when (code) {
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

    /**
     * Builds a 502 error response suitable for WebView consumption.
     */
    private fun createErrorResponse(error: Exception): WebResourceResponse {
        return WebResourceResponse(
            "text/plain", "UTF-8", 502, "Bad Gateway",
            emptyMap(),
            "Network error: ${error.message}".byteInputStream()
        )
    }
}