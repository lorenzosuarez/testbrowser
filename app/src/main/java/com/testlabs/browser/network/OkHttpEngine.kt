package com.testlabs.browser.network

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.testlabs.browser.settings.DeveloperSettings
import com.testlabs.browser.ui.browser.UAProvider
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
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
        .build()

    public fun execute(request: WebResourceRequest): WebResourceResponse? {
        val builder = Request.Builder()
            .url(request.url.toString())
            .method(request.method, null)

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

        return client.newCall(builder.build()).execute().use { response ->
            normalize(response)
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

        val contentType = response.header("Content-Type") ?: "application/octet-stream"
        val mime = contentType.substringBefore(";").lowercase(Locale.ROOT).ifBlank { "application/octet-stream" }
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
