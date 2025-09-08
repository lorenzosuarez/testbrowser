/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */

package com.testlabs.browser.network

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.testlabs.browser.settings.DeveloperSettings
import com.testlabs.browser.ui.browser.UAProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayInputStream
import java.util.Locale

public class OkHttpEngine(
    private val settings: DeveloperSettings,
    private val ua: UAProvider,
    private val chManager: UserAgentClientHintsManager,
    private val cookieManager: CookieManager = CookieManager.getInstance()
) {
    private val client: OkHttpClient = OkHttpClientProvider.client(chManager)

    /**
     * Executes a subresource request and returns a normalized WebResourceResponse.
     */
    public fun execute(request: WebResourceRequest): WebResourceResponse? {
        val url = request.url.toString()
        if (request.isForMainFrame && isHtmlRequest(request, url)) return null
        val method = request.method.uppercase(Locale.US)
        if (method != "GET") return null
        if (!isStaticUrl(url)) return null

        return try {
            val builder = Request.Builder().url(url).get()

            val acceptLanguage = if (settings.richAcceptLanguage.value) "en-US,en;q=0.9" else "en-US"
            val userAgent = ua.userAgent(desktop = false)

            val headers = mutableMapOf<String, String>()
            request.requestHeaders.forEach { (name, value) ->
                when (name.lowercase(Locale.US)) {
                    "x-requested-with" -> {}
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
            } catch (_: Exception) {}

            headers.forEach { (k, v) -> builder.header(k, v) }

            var resp = client.newCall(builder.build()).execute()
            var hops = 0
            while (resp.code in 300..399 && hops < 5) {
                val loc = resp.header("Location") ?: break
                val nextUrl = resp.request.url.resolve(loc)?.toString() ?: loc
                resp.close()
                val nextReq = Request.Builder()
                    .url(nextUrl)
                    .get()
                    .apply { headers.forEach { (k, v) -> header(k, v) } }
                    .build()
                resp = client.newCall(nextReq).execute()
                hops++
            }

            resp.use { response ->
                handleSetCookies(url, response)
                normalize(response)
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Returns true for common static assets to keep scope limited and predictable.
     */
    private fun isStaticUrl(url: String): Boolean {
        val path = url.substringBefore('#').substringBefore('?').lowercase(Locale.US)
        return path.endsWith(".mjs") || path.endsWith(".js") ||
                path.endsWith(".css") ||
                path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") ||
                path.endsWith(".gif") || path.endsWith(".webp") || path.endsWith(".svg") ||
                path.endsWith(".ico") || path.endsWith(".xml") ||
                path.endsWith(".woff2") || path.endsWith(".woff") || path.endsWith(".ttf")
    }

    /**
     * Detects HTML navigations that must be handled by WebView.
     */
    private fun isHtmlRequest(request: WebResourceRequest, url: String): Boolean {
        val acceptHeader = request.requestHeaders["Accept"] ?: ""
        return acceptHeader.contains("text/html", ignoreCase = true) ||
                url.endsWith(".html", true) ||
                url.endsWith("/", true) ||
                (!url.contains('.', ignoreCase = true) && !url.contains("api", ignoreCase = true))
    }

    /**
     * Converts the OkHttp response into a WebView-ready response with safe headers.
     */
    private fun normalize(response: Response): WebResourceResponse {
        val bodyBytes = response.body.bytes()
        val headersMap = response.headers.toMultimap().mapValues { it.value.joinToString(",") }.toMutableMap()

        val url = response.request.url.toString()
        val finalContentType = pickContentType(headersMap["Content-Type"], url)
        val (mime, charset) = splitMimeAndCharset(finalContentType)

        val web = WebResourceResponse(mime, charset, ByteArrayInputStream(bodyBytes))
        web.responseHeaders = buildWebViewResponseHeaders(headersMap, dropContentEncoding = false)

        val safeReason = response.message.ifBlank {
            when (response.code) {
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
        }

        web.setStatusCodeAndReasonPhrase(response.code, safeReason)
        return web
    }

    /**
     * Picks an explicit content type for common static assets when servers return opaque types.
     */
    private fun pickContentType(original: String?, url: String): String {
        if (!original.isNullOrBlank() && !original.startsWith("application/octet-stream", true)) return original
        val path = url.substringBefore('#').substringBefore('?').lowercase(Locale.US)
        val mime = when {
            path.endsWith(".mjs") || path.endsWith(".js") -> "text/javascript"
            path.endsWith(".css") -> "text/css"
            path.endsWith(".html") || path.endsWith(".htm") -> "text/html"
            path.endsWith(".xml") -> "application/xml"
            path.endsWith(".svg") -> "image/svg+xml"
            path.endsWith(".png") -> "image/png"
            path.endsWith(".webp") -> "image/webp"
            path.endsWith(".jpg") || path.endsWith(".jpeg") -> "image/jpeg"
            path.endsWith(".gif") -> "image/gif"
            path.endsWith(".ico") -> "image/x-icon"
            path.endsWith(".woff2") -> "font/woff2"
            path.endsWith(".woff") -> "font/woff"
            path.endsWith(".ttf") -> "font/ttf"
            else -> "application/octet-stream"
        }
        val charset: String? = when {
            mime.startsWith("text/") || mime == "text/javascript" -> "UTF-8"
            else -> null
        }
        return if (charset != null) "$mime; charset=$charset" else mime
    }

    /**
     * Splits content-type into mime and charset.
     */
    private fun splitMimeAndCharset(contentType: String): Pair<String, String?> {
        val parts = contentType.split(';').map { it.trim() }
        val mime = parts.firstOrNull()?.lowercase(Locale.US).orEmpty()
        val charset = parts.firstOrNull { it.startsWith("charset=", ignoreCase = true) }?.substringAfter('=')?.trim('"', ' ')
        return mime to charset
    }

    /**
     * Applies Set-Cookie headers to the WebView cookie store.
     */
    private fun handleSetCookies(url: String, response: Response) {
        response.headers("Set-Cookie").forEach { value ->
            val lines = value.split("\r\n", "\n").filter { it.isNotBlank() }
            if (lines.isEmpty()) {
                try { cookieManager.setCookie(url, value) } catch (_: Exception) {}
            } else {
                lines.forEach { line ->
                    try { cookieManager.setCookie(url, line) } catch (_: Exception) {}
                }
            }
        }
        try { cookieManager.flush() } catch (_: Exception) {}
    }

    /**
     * Builds response headers suitable for WebView.
     */
    private fun buildWebViewResponseHeaders(
        src: Map<String, String>,
        dropContentEncoding: Boolean
    ): LinkedHashMap<String, String> {
        val out = linkedMapOf<String, String>()
        src.forEach { (k, v) ->
            if (k.isBlank()) return@forEach
            val low = k.lowercase(Locale.US)
            if (isHopByHop(k)) return@forEach
            if (k.equals("Set-Cookie", true)) return@forEach
            if (dropContentEncoding && (low == "content-encoding" || low == "content-length" || low == "transfer-encoding" || low == "content-range")) return@forEach
            out[k] = v
        }
        return out
    }

    /**
     * Returns whether a header is hop-by-hop.
     */
    private fun isHopByHop(name: String): Boolean {
        val n = name.lowercase(Locale.US)
        return n == "connection" ||
                n == "proxy-connection" ||
                n == "transfer-encoding" ||
                n == "content-length" ||
                n == "te" ||
                n == "trailer" ||
                n == "upgrade"
    }
}
