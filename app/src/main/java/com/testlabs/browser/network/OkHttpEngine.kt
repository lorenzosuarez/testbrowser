package com.testlabs.browser.network

import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.testlabs.browser.settings.DeveloperSettings
import okhttp3.*
import org.brotli.dec.BrotliInputStream
import com.github.luben.zstd.ZstdInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Locale
import java.util.zip.GZIPInputStream

public class OkHttpEngine(
    private val settings: DeveloperSettings,
    private val ua: UserAgentProvider
) {
    private val client: OkHttpClient = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
        .build()

    public fun execute(request: WebResourceRequest): WebResourceResponse? {
        val url = request.url.toString()
        val builder = Request.Builder().url(url).method(request.method, null)

        val acceptLanguage = if (settings.richAcceptLanguage.value) "en-US,en;q=0.9" else "en-US"

        builder
            .header("User-Agent", ua.get())
            .header("Accept-Language", acceptLanguage)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
            .header("Accept-Encoding", "gzip, deflate, br, zstd")
            .header("Sec-CH-UA", "\"Chromium\";v=\"${ua.major()}\", \"Google Chrome\";v=\"${ua.major()}\"")
            .header("Sec-CH-UA-Mobile", "?1")
            .header("Sec-CH-UA-Platform", "\"Android\"")

        val response = client.newCall(builder.build()).execute()
        return normalize(response)
    }

    private fun normalize(response: Response): WebResourceResponse {
        val bodyBytes = response.body?.bytes() ?: ByteArray(0)
        val encoding = response.header("Content-Encoding")
        val decoded = decode(bodyBytes, encoding)

        val headers = response.headers.toMutableMap()
        headers.remove("Content-Encoding")
        headers.remove("Content-Length")

        val contentType = response.header("Content-Type") ?: "application/octet-stream"
        val mime = contentType.substringBefore(";", "application/octet-stream").lowercase(Locale.getDefault())
        val charset = contentType.substringAfter("charset=", "utf-8")

        val web = WebResourceResponse(mime, charset, ByteArrayInputStream(decoded))
        web.responseHeaders = headers
        web.statusCode = response.code
        web.reasonPhrase = response.message
        return web
    }

    private fun decode(bytes: ByteArray, encoding: String?): ByteArray {
        val stream: InputStream = when {
            encoding?.contains("br", true) == true -> BrotliInputStream(ByteArrayInputStream(bytes))
            encoding?.contains("zstd", true) == true || isZstd(bytes) -> ZstdInputStream(ByteArrayInputStream(bytes))
            encoding?.contains("gzip", true) == true || isGzip(bytes) -> GZIPInputStream(ByteArrayInputStream(bytes))
            else -> return bytes
        }
        return stream.readBytes()
    }

    private fun isGzip(bytes: ByteArray): Boolean =
        bytes.size >= 2 && bytes[0] == 0x1f.toByte() && bytes[1] == 0x8b.toByte()

    private fun isZstd(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 0x28.toByte() && bytes[1] == 0xb5.toByte() &&
            bytes[2] == 0x2f.toByte() && bytes[3] == 0xfd.toByte()
}
