package com.testlabs.browser.ui.browser

import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.HttpURLConnection
import java.util.concurrent.TimeUnit

private const val TAG = "NetworkProxy"
private const val MAX_REDIRECTS = 10

/**
 * Network proxy that intercepts WebView requests and recreates them with OkHttp,
 * ensuring X-Requested-With header is absent and all other headers match Chrome.
 */
public class NetworkProxy(
    private val cookieManager: CookieManager = CookieManager.getInstance()
) {

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false) // We handle redirects manually
        .followSslRedirects(false)
        .build()

    /**
     * Intercepts a WebView request and proxies it through OkHttp if enabled and applicable.
     * Returns null for schemes that should be handled natively by WebView.
     */
    public fun interceptRequest(
        request: WebResourceRequest,
        userAgent: String,
        acceptLanguage: String,
        proxyEnabled: Boolean
    ): WebResourceResponse? {
        val url = request.url.toString()

        // Skip proxy for specific schemes that must remain native
        if (!proxyEnabled || !shouldProxy(url)) {
            return null
        }

        return try {
            runBlocking(Dispatchers.IO) {
                proxyRequest(request, userAgent, acceptLanguage)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Proxy request failed for $url", e)
            createErrorResponse(e)
        }
    }

    private fun shouldProxy(url: String): Boolean {
        val scheme = url.substringBefore(':').lowercase()
        return when (scheme) {
            "http", "https" -> true
            "blob", "data", "file", "ws", "wss", "intent", "chrome", "chrome-extension" -> false
            else -> false
        }
    }

    private suspend fun proxyRequest(
        webRequest: WebResourceRequest,
        userAgent: String,
        acceptLanguage: String
    ): WebResourceResponse {
        val url = webRequest.url.toString()
        val httpUrl = url.toHttpUrlOrNull()
            ?: throw IllegalArgumentException("Invalid URL: $url")

        // Build OkHttp request with all necessary headers
        val requestBuilder = Request.Builder()
            .url(httpUrl)
            .method(webRequest.method, createRequestBody(webRequest))

        // Add standard headers, ensuring X-Requested-With is NEVER included
        addStandardHeaders(requestBuilder, webRequest, userAgent, acceptLanguage)

        val response = executeWithRedirects(requestBuilder.build())
        return convertToWebResourceResponse(response, url)
    }

    private fun createRequestBody(request: WebResourceRequest): okhttp3.RequestBody? {
        // WebResourceRequest doesn't expose the request body in Android WebView API
        // This is a known limitation - we can only handle GET requests properly
        // For POST/PUT requests, the body would need to be captured at a higher level
        return null
    }

    private fun addStandardHeaders(
        builder: Request.Builder,
        webRequest: WebResourceRequest,
        userAgent: String,
        acceptLanguage: String
    ) {
        // Add User-Agent
        builder.header("User-Agent", userAgent)

        // Add Accept-Language
        builder.header("Accept-Language", acceptLanguage)

        // Add standard Accept headers based on request type
        val url = webRequest.url.toString()
        when {
            webRequest.requestHeaders["Accept"] != null -> {
                builder.header("Accept", webRequest.requestHeaders["Accept"]!!)
            }
            url.contains("css") -> {
                builder.header("Accept", "text/css,*/*;q=0.1")
            }
            url.contains("js") -> {
                builder.header("Accept", "*/*")
            }
            webRequest.isForMainFrame -> {
                builder.header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8")
            }
            else -> {
                builder.header("Accept", "*/*")
            }
        }

        // Copy other headers from original request, EXCLUDING X-Requested-With
        webRequest.requestHeaders.forEach { (name, value) ->
            when (name.lowercase()) {
                "x-requested-with" -> {
                    // EXPLICITLY SKIP - this is the whole point
                    Log.d(TAG, "Skipping X-Requested-With header")
                }
                "user-agent", "accept-language", "accept" -> {
                    // Already handled above
                }
                "host" -> {
                    // Let OkHttp handle Host header
                }
                else -> {
                    builder.header(name, value)
                }
            }
        }

        // Add cookies for this domain
        cookieManager.getCookie(webRequest.url.toString())?.let { cookies ->
            builder.header("Cookie", cookies)
        }

        // Add cache control
        builder.header("Cache-Control", "no-cache")

        // Add standard Chrome headers
        builder.header("Sec-Fetch-Site", "none")
        builder.header("Sec-Fetch-Mode", "navigate")
        builder.header("Sec-Fetch-User", "?1")
        builder.header("Sec-Fetch-Dest", if (webRequest.isForMainFrame) "document" else "empty")
    }

    private fun executeWithRedirects(request: Request): Response {
        var currentRequest = request
        var redirectCount = 0

        while (redirectCount < MAX_REDIRECTS) {
            val response = httpClient.newCall(currentRequest).execute()

            when (response.code) {
                HttpURLConnection.HTTP_MOVED_PERM,
                HttpURLConnection.HTTP_MOVED_TEMP,
                HttpURLConnection.HTTP_SEE_OTHER,
                307, 308 -> {
                    val location = response.header("Location")
                    response.close()

                    if (location == null) {
                        throw IOException("Redirect without Location header")
                    }

                    val newUrl = currentRequest.url.resolve(location)
                        ?: throw IOException("Invalid redirect URL: $location")

                    currentRequest = currentRequest.newBuilder()
                        .url(newUrl)
                        .build()

                    redirectCount++
                }
                else -> return response
            }
        }

        throw IOException("Too many redirects")
    }

    private fun convertToWebResourceResponse(response: Response, originalUrl: String): WebResourceResponse {
        // Store cookies
        response.headers.values("Set-Cookie").forEach { cookie ->
            cookieManager.setCookie(originalUrl, cookie)
        }

        // Determine MIME type and encoding
        val contentType = response.header("Content-Type") ?: "text/html"
        val mimeType = contentType.substringBefore(';').trim()
        val encoding = if (contentType.contains("charset=", ignoreCase = true)) {
            contentType.substringAfter("charset=", "UTF-8").trim()
        } else "UTF-8"

        // Convert headers (excluding hop-by-hop headers)
        val responseHeaders = mutableMapOf<String, String>()
        response.headers.forEach { (name, value) ->
            when (name.lowercase()) {
                "connection", "keep-alive", "proxy-authenticate",
                "proxy-authorization", "te", "trailers", "transfer-encoding", "upgrade" -> {
                    // Skip hop-by-hop headers
                }
                else -> responseHeaders[name] = value
            }
        }

        // Get the input stream - this will be automatically closed when WebView is done with it
        val inputStream = response.body?.byteStream() ?: "".byteInputStream()

        // Ensure we have a non-empty reason phrase for WebResourceResponse
        val reasonPhrase = response.message.takeIf { it.isNotBlank() } ?: getDefaultReasonPhrase(response.code)

        return WebResourceResponse(
            mimeType,
            encoding,
            response.code,
            reasonPhrase,
            responseHeaders,
            inputStream
        )
    }

    private fun getDefaultReasonPhrase(statusCode: Int): String {
        return when (statusCode) {
            200 -> "OK"
            201 -> "Created"
            202 -> "Accepted"
            204 -> "No Content"
            301 -> "Moved Permanently"
            302 -> "Found"
            304 -> "Not Modified"
            400 -> "Bad Request"
            401 -> "Unauthorized"
            403 -> "Forbidden"
            404 -> "Not Found"
            405 -> "Method Not Allowed"
            408 -> "Request Timeout"
            409 -> "Conflict"
            410 -> "Gone"
            429 -> "Too Many Requests"
            500 -> "Internal Server Error"
            501 -> "Not Implemented"
            502 -> "Bad Gateway"
            503 -> "Service Unavailable"
            504 -> "Gateway Timeout"
            else -> "Unknown"
        }
    }

    private fun createErrorResponse(error: Exception): WebResourceResponse {
        val errorHtml = """
            <!DOCTYPE html>
            <html>
            <head><title>Network Error</title></head>
            <body>
                <h1>Network Error</h1>
                <p>Failed to load resource: ${error.message}</p>
            </body>
            </html>
        """.trimIndent()

        return WebResourceResponse(
            "text/html",
            "UTF-8",
            500,
            "Internal Server Error",
            emptyMap(),
            errorHtml.byteInputStream()
        )
    }
}
