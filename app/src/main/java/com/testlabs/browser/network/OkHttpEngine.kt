/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.network

import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.github.luben.zstd.ZstdInputStream
import com.testlabs.browser.settings.DeveloperSettings
import com.testlabs.browser.ui.browser.UAProvider
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.brotli.dec.BrotliInputStream
import java.io.ByteArrayInputStream
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

/**
 * Executes subresource HTTP requests with Chrome-like headers and response normalization.
 * This engine returns null for top-level HTML navigations to allow native WebView handling.
 */
public class OkHttpEngine(
    private val settings: DeveloperSettings,
    private val ua: UAProvider,
    private val cookieManager: CookieManager = CookieManager.getInstance()
) {
    private val client: OkHttpClient = OkHttpClientProvider.client

    /**
     * Intercepts a WebResourceRequest and returns a normalized WebResourceResponse, or null to bypass.
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
            val major = extractChromeMajor(userAgent)

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
            headers["Accept-Encoding"] = "gzip, deflate, br, zstd"
            headers.putIfAbsent("Sec-CH-UA", "\"Chromium\";v=\"$major\", \"Google Chrome\";v=\"$major\"")
            headers.putIfAbsent("Sec-CH-UA-Mobile", "?1")
            headers.putIfAbsent("Sec-CH-UA-Platform", "\"Android\"")
            try {
                val cookies = cookieManager.getCookie(url)
                if (!cookies.isNullOrBlank()) headers["Cookie"] = cookies
            } catch (_: Exception) {}
            headers.forEach { (k, v) -> builder.header(k, v) }

            var resp = OkHttpClientProvider.client.newCall(builder.build()).execute()
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
                resp = OkHttpClientProvider.client.newCall(nextReq).execute()
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

    private fun isStaticUrl(url: String): Boolean {
        val path = url.substringBefore('#').substringBefore('?').lowercase(Locale.US)
        return path.endsWith(".mjs") || path.endsWith(".js") ||
                path.endsWith(".css") || path.endsWith(".json") ||
                path.endsWith(".png") || path.endsWith(".jpg") || path.endsWith(".jpeg") ||
                path.endsWith(".gif") || path.endsWith(".webp") || path.endsWith(".svg") ||
                path.endsWith(".ico") || path.endsWith(".xml") ||
                path.endsWith(".woff2") || path.endsWith(".woff") || path.endsWith(".ttf")
    }

    private fun extractChromeMajor(userAgent: String): String {
        return Regex("Chrome/(\\d+)").find(userAgent)?.groupValues?.get(1) ?: "99"
    }

    private fun isHtmlRequest(request: WebResourceRequest, url: String): Boolean {
        val acceptHeader = request.requestHeaders["Accept"] ?: ""
        return acceptHeader.contains("text/html", ignoreCase = true) ||
                url.endsWith(".html", true) ||
                url.endsWith("/", true) ||
                (!url.contains('.', ignoreCase = true) && !url.contains("api", ignoreCase = true))
    }

    private fun normalize(response: Response): WebResourceResponse {
        val encoding = response.header("Content-Encoding")
        val bodyBytes = response.body.bytes()
        val decoded = decode(bodyBytes, encoding)
        val headers: MutableMap<String, String> =
            response.headers.toMultimap().mapValues { it.value.joinToString(",") }.toMutableMap()
        val didDecode = decoded !== bodyBytes && encoding != null
        if (didDecode) {
            headers.keys
                .filter {
                    it.equals("Content-Encoding", true) ||
                            it.equals("Content-Length", true) ||
                            it.equals("Transfer-Encoding", true)
                }
                .toList()
                .forEach { headers.remove(it) }
        }
        val url = response.request.url.toString()
        val finalContentType = pickContentType(headers["Content-Type"], url)
        val (mime, charset) = splitMimeAndCharset(finalContentType)
        val web = WebResourceResponse(mime, charset, ByteArrayInputStream(decoded))

        web.responseHeaders = headers
        web.setStatusCodeAndReasonPhrase(response.code, response.message.ifBlank { " " })
        return web
    }

    private fun pickContentType(original: String?, url: String): String {
        if (!original.isNullOrBlank() && !original.startsWith("application/octet-stream", true)) return original
        val path = url.substringBefore('#').substringBefore('?').lowercase(Locale.US)
        val mime = when {
            path.endsWith(".mjs") || path.endsWith(".js") -> "text/javascript"
            path.endsWith(".css") -> "text/css"
            path.endsWith(".json") -> "application/json"
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
            mime.startsWith("text/") || mime == "text/javascript" || mime == "application/json" -> "UTF-8"
            else -> null
        }
        return if (charset != null) "$mime; charset=$charset" else mime
    }

    private fun splitMimeAndCharset(contentType: String): Pair<String, String?> {
        val parts = contentType.split(';').map { it.trim() }
        val mime = parts.firstOrNull()?.lowercase(Locale.US).orEmpty()
        val charset = parts
            .firstOrNull { it.startsWith("charset=", ignoreCase = true) }
            ?.substringAfter('=')
            ?.trim('"', ' ')
        return mime to charset
    }

    private fun decode(bytes: ByteArray, encoding: String?): ByteArray {
        return try {
            when (encoding?.lowercase(Locale.US)) {
                "gzip" -> GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
                "deflate" -> InflaterInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
                "zstd" -> ZstdInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
                "br" -> BrotliInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
                else -> bytes
            }
        } catch (_: Exception) {
            bytes
        }
    }

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
}
