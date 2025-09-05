package com.testlabs.browser.ui.browser

import android.util.Log
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.testlabs.browser.network.HttpStack
import com.testlabs.browser.network.ProxyRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.InputStream
import java.util.Locale

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

        // Enhanced logging for all requests
        Log.d(TAG, "=== REQUEST ANALYSIS ===")
        Log.d(TAG, "URL: $url")
        Log.d(TAG, "Method: ${request.method}")
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
        val resourceType = when {
            url.endsWith(".js") || url.endsWith(".mjs") -> "JavaScript"
            url.endsWith(".css") -> "CSS"
            url.endsWith(".html") || url.endsWith("/") -> "HTML"
            url.contains("api") -> "API"
            else -> "Other"
        }
        Log.d(TAG, "✅ Intercepting $resourceType: ${request.method} $url")

        return try {
            runBlocking(Dispatchers.IO) {
                // Build fresh request for this specific URL - clone headers 1:1
                val headers = buildRequestHeaders(request, userAgent, acceptLanguage, url)
                val body = extractRequestBody(request)
                val proxyRequest = ProxyRequest(url, request.method, headers, body)

                Log.d(TAG, "🚀 Executing via ${stack.name}: ${proxyRequest.url}")
                val response = stack.execute(proxyRequest)

                // Enhanced response logging
                val contentType = response.headers["Content-Type"] ?: response.headers["content-type"] ?: ""
                Log.d(TAG, "📥 Response ${response.statusCode} ${response.reasonPhrase}")
                Log.d(TAG, "📥 Content-Type: $contentType")
                Log.d(TAG, "📥 Headers: ${response.headers.keys}")

                // Check for potential bot challenges
                if (resourceType == "JavaScript" && contentType.startsWith("text/html")) {
                    Log.e(TAG, "🚨 BOT CHALLENGE DETECTED!")
                    Log.e(TAG, "🚨 JavaScript resource served as HTML: $url")
                    Log.e(TAG, "🚨 This indicates CDN bot detection!")
                }

                // Handle cookies from response
                handleSetCookies(url, response.headers)

                val webResponse = createWebResourceResponse(response, url)
                Log.d(TAG, "✅ Successfully created WebResourceResponse for $url")
                webResponse
            }
        } catch (e: Exception) {
            Log.e(TAG, "💥 ERROR proxying request: $url", e)
            createErrorResponse(e)
        }
    }

    private fun shouldProxy(url: String): Boolean {
        val scheme = url.substringBefore(':').lowercase(Locale.US)
        return scheme in setOf("http", "https")
    }

    private fun buildRequestHeaders(
        request: WebResourceRequest,
        userAgent: String,
        acceptLanguage: String,
        url: String
    ): Map<String, String> {
        val headers = mutableMapOf<String, String>()

        // Copy all headers from WebResourceRequest 1:1, preserving important ones
        request.requestHeaders.forEach { (name, value) ->
            val lowerName = name.lowercase(Locale.US)
            when (lowerName) {
                "x-requested-with" -> {
                    // Never add X-Requested-With - suppress completely
                    Log.d(TAG, "Suppressing X-Requested-With header")
                }
                else -> {
                    // Copy all other headers including Accept, Referer, Sec-*, Cache-Control, etc.
                    headers[name] = value
                }
            }
        }

        // Ensure User-Agent and Accept-Language are set
        if (!headers.containsKey("User-Agent")) {
            headers["User-Agent"] = userAgent
        }
        if (!headers.containsKey("Accept-Language")) {
            headers["Accept-Language"] = acceptLanguage
        }

        // Add cookies from CookieManager for synchronization
        try {
            val cookies = cookieManager.getCookie(url)
            if (!cookies.isNullOrBlank()) {
                headers["Cookie"] = cookies
                Log.d(TAG, "Added cookies for $url")
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error getting cookies for $url", e)
        }

        // Don't manually set Accept-Encoding - let OkHttp + interceptors handle it

        Log.d(TAG, "Request headers for $url: ${headers.keys}")
        return headers
    }

    private fun extractRequestBody(request: WebResourceRequest): ByteArray? {
        // WebResourceRequest doesn't expose body directly
        // This would need to be handled at a higher level if POST/PUT data is needed
        return null
    }

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

    private fun createWebResourceResponse(
        response: com.testlabs.browser.network.ProxyResponse,
        url: String
    ): WebResourceResponse {
        // Parse Content-Type into mimeType + charset
        val contentTypeHeader = response.headers["Content-Type"] ?: response.headers["content-type"] ?: "application/octet-stream"
        val (mimeType, charset) = parseContentType(contentTypeHeader)

        // Check for bot challenge before creating response
        checkForBotChallenge(url, contentTypeHeader, response.statusCode)

        // Handle special status codes
        when (response.statusCode) {
            304 -> {
                // Not Modified - return with empty body and caching headers
                Log.d(TAG, "Not modified response for $url")
                return WebResourceResponse(
                    null, null,
                    response.statusCode,
                    response.reasonPhrase.ifBlank { getDefaultStatusText(response.statusCode) },
                    filterHopByHopHeaders(response.headers),
                    "".byteInputStream()
                )
            }
            204, 205 -> {
                // No Content/Reset Content - return empty body
                Log.d(TAG, "No content response ${response.statusCode} for $url")
                return WebResourceResponse(
                    "text/plain", "UTF-8",
                    response.statusCode,
                    response.reasonPhrase.ifBlank { getDefaultStatusText(response.statusCode) },
                    filterHopByHopHeaders(response.headers),
                    "".byteInputStream()
                )
            }
        }

        // Filter hop-by-hop headers and content-encoding/length (already decoded by Cronet)
        val filteredHeaders = filterHopByHopHeaders(response.headers)

        val statusText = response.reasonPhrase.ifBlank { getDefaultStatusText(response.statusCode) }

        return WebResourceResponse(
            mimeType,
            charset,
            response.statusCode,
            statusText,
            filteredHeaders,
            response.body
        )
    }

    private fun filterHopByHopHeaders(headers: Map<String, String>): Map<String, String> {
        val hopByHopHeaders = setOf(
            "connection", "transfer-encoding", "keep-alive", "proxy-authenticate",
            "proxy-authorization", "te", "trailer", "content-encoding", "content-length"
        )

        return headers.filterKeys { key ->
            key.lowercase(Locale.US) !in hopByHopHeaders
        }
    }

    private fun parseContentType(contentType: String): Pair<String, String?> {
        val parts = contentType.split(';', limit = 2)
        val mimeType = parts[0].trim()

        val charset = if (parts.size > 1) {
            val charsetPart = parts[1].trim()
            if (charsetPart.startsWith("charset=", ignoreCase = true)) {
                charsetPart.substring(8).trim()
            } else null
        } else null

        // Default charset for text types
        val finalCharset = when {
            charset != null -> charset
            mimeType.startsWith("text/") -> "UTF-8"
            else -> null
        }

        return mimeType to finalCharset
    }

    private fun checkForBotChallenge(url: String, contentType: String, statusCode: Int) {
        try {
            if ((url.endsWith(".js") || url.endsWith(".mjs") || url.contains("type=module")) &&
                contentType.startsWith("text/html") && statusCode == 200) {

                Log.w(TAG, "BOT_CHALLENGE_SUSPECT detected for $url")
                Log.w(TAG, "  - Content-Type: $contentType")
                Log.w(TAG, "  - Status: $statusCode")
                Log.w(TAG, "  - URL pattern: JS module served as HTML")
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error checking bot challenge for $url", e)
        }
    }

    private fun createErrorResponse(error: Exception): WebResourceResponse {
        Log.e(TAG, "Creating error response", error)
        return WebResourceResponse(
            "text/plain", "UTF-8", 500, "Internal Server Error",
            emptyMap(), "Network error: ${error.message}".byteInputStream()
        )
    }

    private fun getDefaultStatusText(statusCode: Int): String = when (statusCode) {
        100 -> "Continue"
        101 -> "Switching Protocols"
        200 -> "OK"
        201 -> "Created"
        202 -> "Accepted"
        204 -> "No Content"
        205 -> "Reset Content"
        206 -> "Partial Content"
        300 -> "Multiple Choices"
        301 -> "Moved Permanently"
        302 -> "Found"
        303 -> "See Other"
        304 -> "Not Modified"
        307 -> "Temporary Redirect"
        308 -> "Permanent Redirect"
        400 -> "Bad Request"
        401 -> "Unauthorized"
        403 -> "Forbidden"
        404 -> "Not Found"
        405 -> "Method Not Allowed"
        406 -> "Not Acceptable"
        408 -> "Request Timeout"
        409 -> "Conflict"
        410 -> "Gone"
        411 -> "Length Required"
        413 -> "Payload Too Large"
        414 -> "URI Too Long"
        415 -> "Unsupported Media Type"
        418 -> "I'm a teapot"
        421 -> "Misdirected Request"
        422 -> "Unprocessable Entity"
        429 -> "Too Many Requests"
        500 -> "Internal Server Error"
        501 -> "Not Implemented"
        502 -> "Bad Gateway"
        503 -> "Service Unavailable"
        504 -> "Gateway Timeout"
        else -> "Unknown"
    }
}
