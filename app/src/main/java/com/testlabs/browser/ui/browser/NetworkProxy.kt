package com.testlabs.browser.ui.browser

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.testlabs.browser.network.HttpStack
import com.testlabs.browser.network.ProxyRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

private val hopHeaders = setOf(
    "connection",
    "keep-alive",
    "proxy-authenticate",
    "proxy-authorization",
    "te",
    "trailers",
    "transfer-encoding",
    "upgrade",
)

public class NetworkProxy(
    private val stack: HttpStack,
    private val cookieManager: CookieManager = CookieManager.getInstance(),
) {
    public val stackName: String get() = stack.name
    public fun interceptRequest(request: WebResourceRequest, userAgent: String, acceptLanguage: String, proxyEnabled: Boolean): WebResourceResponse? {
        val url = request.url.toString()
        if (!proxyEnabled || !shouldProxy(url)) return null
        return try {
            runBlocking(Dispatchers.IO) {
                val headers = buildHeaders(request, userAgent, acceptLanguage)
                val proxyRequest = ProxyRequest(url, request.method, headers)
                val response = stack.execute(proxyRequest)
                handleCookies(url, response.headers)
                createWebResourceResponse(response)
            }
        } catch (e: Exception) {
            createErrorResponse(e)
        }
    }
    private fun shouldProxy(url: String): Boolean {
        val scheme = url.substringBefore(':').lowercase()
        return scheme == "http" || scheme == "https"
    }
    private fun buildHeaders(request: WebResourceRequest, userAgent: String, acceptLanguage: String): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        headers["User-Agent"] = userAgent
        if (request.isForMainFrame) headers["Accept-Language"] = acceptLanguage
        request.requestHeaders.forEach { (name, value) ->
            val lower = name.lowercase()
            if (lower !in hopHeaders && lower != "user-agent" && lower != "accept-language" && lower != "x-requested-with") {
                headers[name] = value
            }
        }
        cookieManager.getCookie(request.url.toString())?.let { headers["Cookie"] = it }
        return headers
    }
    private fun handleCookies(url: String, headers: Map<String, String>) {
        headers.filter { it.key.equals("Set-Cookie", true) }.forEach { cookieManager.setCookie(url, it.value) }
    }
    private fun createWebResourceResponse(response: com.testlabs.browser.network.ProxyResponse): WebResourceResponse {
        val contentType = response.headers["Content-Type"] ?: "text/html"
        val mime = contentType.substringBefore(';').trim()
        val encoding = if (contentType.contains("charset=", true)) contentType.substringAfter("charset=", "UTF-8").trim() else "UTF-8"
        val filtered = response.headers.filterKeys { it.lowercase() !in hopHeaders && !it.equals("content-type", true) }
        val reason = response.reasonPhrase.ifBlank { defaultReason(response.statusCode) }
        return WebResourceResponse(mime, encoding, response.statusCode, reason, filtered, response.body)
    }
    private fun defaultReason(code: Int): String = when (code) {
        200 -> "OK"
        201 -> "Created"
        202 -> "Accepted"
        204 -> "No Content"
        301 -> "Moved Permanently"
        302 -> "Found"
        303 -> "See Other"
        307 -> "Temporary Redirect"
        308 -> "Permanent Redirect"
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
    private fun createErrorResponse(error: Exception): WebResourceResponse {
        val html = """
            <!DOCTYPE html>
            <html><head><title>Network Error</title></head>
            <body><h1>Network Error</h1><p>${error.message}</p></body>
            </html>
        """.trimIndent().byteInputStream()
        return WebResourceResponse("text/html", "UTF-8", 500, "Internal Server Error", emptyMap(), html)
    }
}
