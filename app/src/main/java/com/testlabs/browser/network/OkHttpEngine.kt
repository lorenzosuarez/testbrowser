package com.testlabs.browser.network

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.testlabs.browser.settings.DeveloperSettings
import com.testlabs.browser.ui.browser.UAProvider
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.RequestBody.Companion.toRequestBody
import org.brotli.dec.BrotliInputStream
import com.github.luben.zstd.ZstdInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Locale
import java.util.zip.GZIPInputStream

public class OkHttpEngine(
    private val settings: DeveloperSettings,
    private val ua: UAProvider
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    public fun execute(request: WebResourceRequest): WebResourceResponse? {
        return try {
            val builder = Request.Builder()
                .url(request.url.toString())

            // Handle different HTTP methods properly
            when (request.method.uppercase()) {
                "GET" -> builder.get()
                "POST" -> builder.post("".toRequestBody())
                "PUT" -> builder.put("".toRequestBody())
                "DELETE" -> builder.delete()
                "HEAD" -> builder.head()
                else -> builder.method(request.method, null)
            }

            val acceptLanguage = if (settings.richAcceptLanguage.value) "en-US,en;q=0.9" else "en-US"
            val userAgent = ua.userAgent(desktop = false)
            val chromeVersion = extractChromeVersion(userAgent)

            builder
                .header("User-Agent", userAgent)
                .header("Accept-Language", acceptLanguage)
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .header("Accept-Encoding", "gzip, deflate, br, zstd")
                .header("Sec-CH-UA", "\"Chromium\";v=\"$chromeVersion\", \"Google Chrome\";v=\"$chromeVersion\"")
                .header("Sec-CH-UA-Mobile", "?1")
                .header("Sec-CH-UA-Platform", "\"Android\"")
                .header("Sec-Fetch-Site", "none")
                .header("Sec-Fetch-Mode", "navigate")
                .header("Sec-Fetch-User", "?1")
                .header("Sec-Fetch-Dest", "document")

            // Add request headers from WebResourceRequest
            request.requestHeaders?.forEach { (key, value) ->
                if (!key.equals("User-Agent", true) && !key.equals("Accept-Language", true)) {
                    builder.header(key, value)
                }
            }

            client.newCall(builder.build()).execute().use { response ->
                normalize(response)
            }
        } catch (e: Exception) {
            // Log error for debugging but return null to let WebView handle fallback
            android.util.Log.w("OkHttpEngine", "Request failed: ${e.message}")
            null // Return null on error, let WebView handle it
        }
    }

    private fun extractChromeVersion(userAgent: String): String {
        return runCatching {
            val chromeRegex = Regex("""Chrome/(\d+)""")
            chromeRegex.find(userAgent)?.groupValues?.get(1) ?: "119"
        }.getOrElse { "119" }
    }

    private fun normalize(response: Response): WebResourceResponse {
        val bodyBytes = response.body.bytes()
        val encoding = response.header("Content-Encoding")
        val decoded = decode(bodyBytes, encoding)

        val headers: MutableMap<String, String> =
            response.headers.toMultimap().mapValues { it.value.joinToString(",") }.toMutableMap()

        headers.keys
            .filter { it.equals("Content-Encoding", true) || it.equals("Content-Length", true) }
            .forEach { headers.remove(it) }

        // Improved MIME type detection
        val originalContentType = response.header("Content-Type")
        val url = response.request.url.toString()

        val contentType = when {
            originalContentType != null -> originalContentType
            url.endsWith(".js") || url.contains(".js?") -> "application/javascript"
            url.endsWith(".mjs") || url.contains(".mjs?") -> "application/javascript"
            url.endsWith(".json") || url.contains(".json?") -> "application/json"
            url.endsWith(".css") || url.contains(".css?") -> "text/css"
            url.endsWith(".html") || url.contains(".html?") -> "text/html"
            url.endsWith(".xml") || url.contains(".xml?") -> "text/xml"
            url.endsWith(".png") -> "image/png"
            url.endsWith(".jpg") || url.endsWith(".jpeg") -> "image/jpeg"
            url.endsWith(".gif") -> "image/gif"
            url.endsWith(".svg") -> "image/svg+xml"
            url.endsWith(".woff") || url.endsWith(".woff2") -> "font/woff"
            url.endsWith(".ttf") -> "font/ttf"
            else -> "application/octet-stream"
        }

        val mime = contentType.substringBefore(";").lowercase(Locale.ROOT).ifBlank {
            if (url.endsWith(".js") || url.contains(".js?")) "application/javascript" else "application/octet-stream"
        }
        val charset = Regex("(?i)charset=([\\w-]+)").find(contentType)?.groupValues?.get(1) ?: "utf-8"

        val web = WebResourceResponse(mime, charset, ByteArrayInputStream(decoded))
        web.responseHeaders = headers
        web.setStatusCodeAndReasonPhrase(response.code, response.message.ifBlank { " " })
        return web
    }

    private fun decode(bytes: ByteArray, encoding: String?): ByteArray {
        val stream: InputStream = when {
            encoding?.contains("br", true) == true ->
                BrotliInputStream(ByteArrayInputStream(bytes))
            encoding?.contains("zstd", true) == true || isZstd(bytes) ->
                ZstdInputStream(ByteArrayInputStream(bytes))
            encoding?.contains("gzip", true) == true || isGzip(bytes) ->
                GZIPInputStream(ByteArrayInputStream(bytes))
            else -> return bytes
        }
        return stream.use { it.readBytes() }
    }

    private fun isGzip(bytes: ByteArray): Boolean =
        bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()

    private fun isZstd(bytes: ByteArray): Boolean =
        bytes.size >= 4 &&
                bytes[0] == 0x28.toByte() &&
                bytes[1] == 0xb5.toByte() &&
                bytes[2] == 0x2f.toByte() &&
                bytes[3] == 0xfd.toByte()
}
