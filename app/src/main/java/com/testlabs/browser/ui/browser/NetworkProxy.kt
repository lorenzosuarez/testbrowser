package com.testlabs.browser.ui.browser

import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.testlabs.browser.network.HttpStack
import com.testlabs.browser.network.ProxyRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream
import com.github.luben.zstd.ZstdInputStream

private const val TAG = "NetworkProxy"

public class NetworkProxy(
    private val stack: HttpStack,
    private val cookieManager: android.webkit.CookieManager = android.webkit.CookieManager.getInstance(),
) {
    public val stackName: String get() = stack.name

    public fun interceptRequest(
        request: WebResourceRequest,
        userAgent: String,
        acceptLanguage: String,
        proxyEnabled: Boolean
    ): WebResourceResponse? {
        val url = request.url.toString()

        // A. Main-frame policy: Do not intercept top-level document navigations
        if (request.isForMainFrame && isHtmlRequest(request, url)) {
            Log.d(TAG, "❌ Bypassing main-frame HTML navigation: $url")
            return null
        }

        // Enhanced logging for all requests
        Log.d(TAG, "=== REQUEST ANALYSIS ===")
        Log.d(TAG, "URL: $url")
        Log.d(TAG, "Method: ${request.method}")
        Log.d(TAG, "Main frame: ${request.isForMainFrame}")
        Log.d(TAG, "Proxy enabled: $proxyEnabled")
        Log.d(TAG, "Request headers: ${request.requestHeaders.keys}")

        // Only proxy HTTP/HTTPS schemes - let WebView handle special schemes natively
        if (!shouldProxy(url)) {
            Log.d(TAG, "❌ Bypassing special scheme: $url")
            return null
        }

        if (!proxyEnabled) {
            Log.d(TAG, "❌ Proxy disabled, bypassing: $url")
            return null
        }

        // Log specific resource types for debugging
        val resourceType = getResourceType(url)
        Log.d(TAG, "✅ Intercepting $resourceType: ${request.method} $url")

        return try {
            runBlocking(Dispatchers.IO) {
                // C. Request header parity (proxy layer)
                val headers = buildRequestHeaders(request, userAgent, acceptLanguage, url)
                val body = extractRequestBody(request)
                val proxyRequest = ProxyRequest(url, request.method, headers, body)

                Log.d(TAG, "🚀 Executing via ${stack.name}: ${proxyRequest.url}")
                val response = stack.execute(proxyRequest)

                // Enhanced response logging
                val contentType = response.headers["Content-Type"] ?: response.headers["content-type"] ?: ""
                val contentEncoding = response.headers["Content-Encoding"] ?: response.headers["content-encoding"]
                Log.d(TAG, "📥 Response ${response.statusCode} ${response.reasonPhrase}")
                Log.d(TAG, "📥 Content-Type: $contentType")
                Log.d(TAG, "📥 Content-Encoding: $contentEncoding")
                Log.d(TAG, "📥 Headers: ${response.headers.keys}")

                // Check for potential bot challenges
                if (resourceType == "JavaScript" && contentType.startsWith("text/html")) {
                    Log.e(TAG, "🚨 BOT CHALLENGE DETECTED!")
                    Log.e(TAG, "🚨 JavaScript resource served as HTML: $url")
                    Log.e(TAG, "🚨 This indicates CDN bot detection!")
                }

                // E. Cookies: Handle cookies from response
                handleSetCookies(url, response.headers)

                // B. Response decoding normalization (critical)
                val webResponse = createNormalizedWebResourceResponse(response, url)
                Log.d(TAG, "✅ Successfully created normalized WebResourceResponse for $url")
                webResponse
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 ERROR proxying request: $url", e)
            createErrorResponse(e)
        }
    }

    private fun isHtmlRequest(request: WebResourceRequest, url: String): Boolean {
        val acceptHeader = request.requestHeaders["Accept"] ?: ""
        return acceptHeader.contains("text/html") ||
               url.endsWith(".html") ||
               url.endsWith("/") ||
               (!url.contains(".") && !url.contains("api"))
    }

    private fun getResourceType(url: String): String = when {
        url.endsWith(".js") || url.endsWith(".mjs") -> "JavaScript"
        url.endsWith(".css") -> "CSS"
        url.endsWith(".html") || url.endsWith("/") -> "HTML"
        url.contains("api") -> "API"
        url.endsWith(".json") -> "JSON"
        url.endsWith(".woff") || url.endsWith(".woff2") || url.endsWith(".ttf") -> "Font"
        url.endsWith(".png") || url.endsWith(".jpg") || url.endsWith(".jpeg") || url.endsWith(".gif") || url.endsWith(".webp") -> "Image"
        else -> "Other"
    }

    private fun shouldProxy(url: String): Boolean {
        val scheme = url.substringBefore(':').lowercase(Locale.US)
        return scheme in setOf("http", "https")
    }

    // C. Request header parity (proxy layer)
    private fun buildRequestHeaders(
        request: WebResourceRequest,
        userAgent: String,
        acceptLanguage: String,
        url: String
    ): Map<String, String> {
        val headers = mutableMapOf<String, String>()

        // Forward all headers from WebResourceRequest unmodified, except specific ones
        request.requestHeaders.forEach { (name, value) ->
            val lowerName = name.lowercase(Locale.US)
            when (lowerName) {
                "x-requested-with" -> {
                    // Remove X-Requested-With everywhere
                    Log.d(TAG, "Suppressing X-Requested-With header")
                }
                "user-agent" -> {
                    // Overwrite User-Agent with exact Chrome Stable mobile UA
                    headers["User-Agent"] = userAgent
                    Log.d(TAG, "Set User-Agent: $userAgent")
                }
                "accept-language" -> {
                    // Ensure Accept-Language follows device locales with Chrome-like weighting
                    headers["Accept-Language"] = acceptLanguage
                    Log.d(TAG, "Set Accept-Language: $acceptLanguage")
                }
                else -> {
                    // Copy all other headers including Accept, Referer, Sec-*, Cache-Control, etc.
                    headers[name] = value
                }
            }
        }

        // Ensure required headers are set
        if (!headers.containsKey("User-Agent")) {
            headers["User-Agent"] = userAgent
        }
        if (!headers.containsKey("Accept-Language")) {
            headers["Accept-Language"] = acceptLanguage
        }

        // E. Cookies: read cookies from CookieManager and attach to proxied requests
        try {
            val cookies = cookieManager.getCookie(url)
            if (!cookies.isNullOrBlank()) {
                headers["Cookie"] = cookies
                Log.d(TAG, "Added cookies for $url")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting cookies for $url", e)
        }

        Log.d(TAG, "Final request headers for $url: ${headers.keys}")
        return headers
    }

    private fun extractRequestBody(request: WebResourceRequest): ByteArray? {
        // WebResourceRequest doesn't expose body directly
        // This would need to be handled at a higher level if POST/PUT data is needed
        return null
    }

    // E. Cookies: capture all Set-Cookie headers and write into CookieManager
    private fun handleSetCookies(url: String, headers: Map<String, String>) {
        headers.forEach { (name, value) ->
            if (name.equals("Set-Cookie", ignoreCase = true)) {
                try {
                    cookieManager.setCookie(url, value)
                    Log.d(TAG, "Set cookie for $url: ${value.substringBefore(';')}")
                } catch (e: Exception) {
                    Log.w(TAG, "Error setting cookie for $url", e)
                }
            }
        }

        // Flush cookies to storage for synchronization
        try {
            cookieManager.flush()
        } catch (e: Exception) {
            Log.w(TAG, "Error flushing cookies", e)
        }
    }

    // B. Response decoding normalization (critical)
    private fun createNormalizedWebResourceResponse(
        response: com.testlabs.browser.network.ProxyResponse,
        url: String
    ): WebResourceResponse {
        // Read Content-Type exactly; derive mime and charset
        val contentTypeHeader = response.headers["Content-Type"] ?: response.headers["content-type"] ?: "application/octet-stream"
        val (mimeType, charset) = parseContentType(contentTypeHeader)

        Log.d(TAG, "📋 Normalizing response for $url")
        Log.d(TAG, "📋 Original Content-Type: $contentTypeHeader")
        Log.d(TAG, "📋 Parsed MIME: $mimeType, Charset: $charset")

        // Determine if the response body is encoded or decoded
        val contentEncoding = response.headers["Content-Encoding"] ?: response.headers["content-encoding"]
        val transferEncoding = response.headers["Transfer-Encoding"] ?: response.headers["transfer-encoding"]
        val contentLength = response.headers["Content-Length"] ?: response.headers["content-length"]

        Log.d(TAG, "📋 Original Content-Encoding: $contentEncoding")
        Log.d(TAG, "📋 Original Content-Length: $contentLength")

        // Read the response body
        val originalBody = response.body.readBytes()

        // Determine if body is actually encoded by checking magic bytes
        val bodyEncoding = detectBodyEncoding(originalBody)
        Log.d(TAG, "📋 Detected body encoding: $bodyEncoding")

        val (finalBody, normalizedHeaders) = when {
            // If headers say encoded but body is already decoded: strip encoding headers
            contentEncoding != null && bodyEncoding == null -> {
                Log.d(TAG, "📋 Body already decoded, stripping encoding headers")
                val headers = response.headers.toMutableMap()
                headers.remove("Content-Encoding")
                headers.remove("content-encoding")
                headers.remove("Transfer-Encoding")
                headers.remove("transfer-encoding")
                headers.remove("Content-Length")
                headers.remove("content-length")
                Pair(originalBody, headers)
            }
            // If headers say encoded and body is still encoded: decode transparently
            contentEncoding != null && bodyEncoding != null -> {
                Log.d(TAG, "📋 Body is encoded, decoding transparently")
                val decodedBody = decodeBody(originalBody, bodyEncoding)
                val headers = response.headers.toMutableMap()
                headers.remove("Content-Encoding")
                headers.remove("content-encoding")
                headers.remove("Transfer-Encoding")
                headers.remove("transfer-encoding")
                headers.remove("Content-Length")
                headers.remove("content-length")
                Pair(decodedBody, headers)
            }
            // Body is not encoded: pass through unchanged
            else -> {
                Log.d(TAG, "📋 Body not encoded, passing through")
                Pair(originalBody, response.headers)
            }
        }

        // Filter hop-by-hop headers and never synthesize CORS headers
        val filteredHeaders = filterResponseHeaders(normalizedHeaders)

        Log.d(TAG, "📋 Final normalized headers: ${filteredHeaders.keys}")

        // Handle special status codes
        return when (response.statusCode) {
            304 -> {
                Log.d(TAG, "Not modified response for $url")
                WebResourceResponse(
                    null, null,
                    response.statusCode,
                    response.reasonPhrase.ifBlank { getDefaultStatusText(response.statusCode) },
                    filteredHeaders,
                    ByteArrayInputStream(ByteArray(0))
                )
            }
            204, 205 -> {
                Log.d(TAG, "No content response ${response.statusCode} for $url")
                WebResourceResponse(
                    "text/plain", charset,
                    response.statusCode,
                    response.reasonPhrase.ifBlank { getDefaultStatusText(response.statusCode) },
                    filteredHeaders,
                    ByteArrayInputStream(ByteArray(0))
                )
            }
            else -> {
                WebResourceResponse(
                    mimeType, charset,
                    response.statusCode,
                    response.reasonPhrase.ifBlank { getDefaultStatusText(response.statusCode) },
                    filteredHeaders,
                    ByteArrayInputStream(finalBody)
                )
            }
        }
    }

    // Recognize encodings via magic bytes
    private fun detectBodyEncoding(body: ByteArray): String? {
        if (body.size < 4) return null

        return when {
            // gzip → 1F 8B
            body[0] == 0x1F.toByte() && body[1] == 0x8B.toByte() -> "gzip"
            // zstd → 28 B5 2F FD
            body[0] == 0x28.toByte() && body[1] == 0xB5.toByte() &&
            body[2] == 0x2F.toByte() && body[3] == 0xFD.toByte() -> "zstd"
            // brotli has no clear magic bytes, assume encoded if header says so
            else -> null
        }
    }

    private fun decodeBody(body: ByteArray, encoding: String): ByteArray {
        return try {
            when (encoding.lowercase()) {
                "gzip" -> GZIPInputStream(ByteArrayInputStream(body)).readBytes()
                "deflate" -> InflaterInputStream(ByteArrayInputStream(body)).readBytes()
                "zstd" -> ZstdInputStream(ByteArrayInputStream(body)).readBytes()
                // For brotli, we'd need a brotli decoder library
                "br", "brotli" -> {
                    Log.w(TAG, "Brotli decoding not implemented, returning raw bytes")
                    body
                }
                else -> {
                    Log.w(TAG, "Unknown encoding: $encoding, returning raw bytes")
                    body
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding body with $encoding", e)
            body
        }
    }

    private fun parseContentType(contentType: String): Pair<String, String> {
        val parts = contentType.split(';').map { it.trim() }
        val mimeType = parts.firstOrNull() ?: "application/octet-stream"

        val charset = parts
            .find { it.startsWith("charset=", ignoreCase = true) }
            ?.substringAfter('=')
            ?.trim('"', ' ')
            ?: "UTF-8"  // Default to UTF-8 if no charset

        return Pair(mimeType, charset)
    }

    // Never synthesize CORS headers. Forward server CORS headers exactly as received.
    private fun filterResponseHeaders(headers: Map<String, String>): Map<String, String> {
        val hopByHopHeaders = setOf(
            "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
            "te", "trailers", "transfer-encoding", "upgrade"
        )

        return headers.filterKeys { key ->
            key.lowercase() !in hopByHopHeaders
        }
    }

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

    private fun createErrorResponse(error: Exception): WebResourceResponse {
        Log.e(TAG, "Creating error response for: ${error.message}")
        return WebResourceResponse(
            "text/plain", "UTF-8", 502, "Bad Gateway",
            emptyMap(),
            "Network error: ${error.message}".byteInputStream()
        )
    }
}
