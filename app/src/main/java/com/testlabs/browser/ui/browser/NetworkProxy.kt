/**
 * Author: Lorenzo Suarez
 * Date: 06/09/2025
 */

package com.testlabs.browser.ui.browser

import android.content.Context
import android.util.Log
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import com.testlabs.browser.domain.settings.EngineMode
import com.testlabs.browser.domain.settings.WebViewConfig
import com.testlabs.browser.network.CronetHolder
import com.testlabs.browser.network.CronetHttpStack
import com.testlabs.browser.network.OkHttpStack
import com.testlabs.browser.network.ProxyRequest
import com.testlabs.browser.network.UserAgentClientHintsManager
import kotlinx.coroutines.runBlocking
import org.brotli.dec.BrotliInputStream
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.Locale
import java.util.zip.GZIPInputStream
import java.util.zip.InflaterInputStream

public interface NetworkProxy {
    public val stackName: String
    public fun interceptRequest(
        request: WebResourceRequest,
        userAgent: String,
        acceptLanguage: String,
        proxyEnabled: Boolean
    ): WebResourceResponse?
}

public class DefaultNetworkProxy(
    context: Context,
    config: WebViewConfig,
    uaProvider: UAProvider,
    private val chManager: UserAgentClientHintsManager,
    private val cookieManager: CookieManager = CookieManager.getInstance()
) : NetworkProxy {

    private val TAG = "NetProxy"

    private val httpStack = if (config.engineMode == EngineMode.Cronet) {
        val ua = config.customUserAgent ?: uaProvider.userAgent(desktop = config.desktopMode)
        val engine = CronetHolder.getEngine(context, ua)
        if (engine != null) CronetHttpStack(engine, uaProvider, chManager) else OkHttpStack(
            uaProvider,
            chManager
        )
    } else {
        OkHttpStack(uaProvider, chManager)
    }

    override val stackName: String = httpStack.name

    override fun interceptRequest(
        request: WebResourceRequest,
        userAgent: String,
        acceptLanguage: String,
        proxyEnabled: Boolean
    ): WebResourceResponse? {
        val url = request.url.toString()
        val isMain = request.isForMainFrame
        val method = request.method
        if (!proxyEnabled) {
            Log.d(TAG, "MISS (proxy disabled)  main=$isMain  $method $url")
            return null
        }

        val t0 = System.nanoTime()
        Log.d(TAG, "→ INTERCEPT  main=$isMain  stack=$stackName  $method $url")

        val headers = normalizeHeaders(request.requestHeaders, userAgent).toMutableMap()
        headers["User-Agent"] = userAgent
        headers["Accept-Language"] = acceptLanguage
        try {
            val cookie = cookieManager.getCookie(url)
            if (!cookie.isNullOrBlank()) headers["Cookie"] = cookie
        } catch (_: Throwable) {
        }

        val proxyReq = ProxyRequest(url = url, method = method, headers = headers)

        val resp = try {
            runBlocking { httpStack.execute(proxyReq) }
        } catch (t: Throwable) {
            val dt = (System.nanoTime() - t0) / 1e6
            Log.e(TAG, "ERROR stack=$stackName (${dt}ms) $method $url", t)
            return null
        }

        try {
            resp.headers["Set-Cookie"]?.forEach { value ->
                val lines = value.split("\r\n", "\n").filter { it.isNotBlank() }
                if (lines.isEmpty()) cookieManager.setCookie(
                    url,
                    value
                ) else lines.forEach { cookieManager.setCookie(url, it) }
            }
            runCatching { cookieManager.flush() }
        } catch (_: Throwable) {
        }

        val rawCt = firstHeader(resp.headers, "Content-Type")
        val guessedCt = rawCt ?: pickContentTypeByUrl(url)
        val (mime0, charset0) = splitMimeAndCharset(guessedCt)
        val textual = isTextualMime(mime0)

        val encoding = firstHeader(resp.headers, "Content-Encoding")?.lowercase(Locale.US)
        val bodyBytes: ByteArray?
        val decoded: ByteArray?
        if (textual) {
            bodyBytes = try {
                resp.body.readBytes()
            } catch (_: Throwable) {
                null
            }
            decoded = maybeDecode(bodyBytes, encoding)
        } else {
            bodyBytes = null
            decoded = null
        }

        val dropEncoding = decoded != null
        val headerMap = buildWebViewResponseHeaders(resp.headers, dropEncoding)

        val mime = mime0.ifBlank { "application/octet-stream" }
        val charset = charset0
        val reason = safeReason(resp.statusCode, resp.reasonPhrase)

        val sizeHint = try {
            when {
                decoded != null -> decoded.size
                bodyBytes != null -> bodyBytes.size
                else -> resp.body.available()
            }
        } catch (_: Throwable) {
            -1
        }

        val dt = (System.nanoTime() - t0) / 1e6
        Log.d(
            TAG,
            "← RESPONSE  code=${resp.statusCode} reason='${reason}' mime=$mime charset=$charset size~$sizeHint  (${dt}ms)  $method $url"
        )

        val dataStream: InputStream = when {
            decoded != null -> ByteArrayInputStream(decoded)
            bodyBytes != null -> ByteArrayInputStream(bodyBytes)
            else -> resp.body
        }

        return WebResourceResponse(mime, charset, dataStream).apply {
            responseHeaders = headerMap
            setStatusCodeAndReasonPhrase(resp.statusCode, reason)
        }
    }

    public fun normalizeHeaders(incoming: Map<String, String>, ua: String): Map<String, String> {
        val sanitized = incoming.toMutableMap()
        sanitized.keys.filter { it.equals("x-requested-with", true) }.toList()
            .forEach { sanitized.remove(it) }
        sanitized.keys.filter { it.lowercase(Locale.US).startsWith("sec-ch-ua") }.toList()
            .forEach { sanitized.remove(it) }
        sanitized.keys.firstOrNull { it.equals("accept-encoding", true) }
            ?.let { sanitized.remove(it) }
        sanitized["Accept-Encoding"] = "identity"
        sanitized.keys.filter { it.equals("range", true) || it.equals("if-range", true) }.toList()
            .forEach { sanitized.remove(it) }

        val isMobile = ua.contains(" Mobile") && !ua.contains("X11;")
        if (chManager.enabled) chManager.asMap(isMobile = isMobile)
            .forEach { (k, v) -> sanitized[k] = v }

        val platform = platformFromUA(ua)
        sanitized["Sec-CH-UA-Platform"] = "\"$platform\""

        if (platform != "Android") {
            listOf("sec-ch-ua-platform-version", "sec-ch-ua-model")
                .mapNotNull { key -> sanitized.keys.firstOrNull { it.equals(key, true) } }
                .forEach { sanitized.remove(it) }
            val mobileKey = sanitized.keys.firstOrNull { it.equals("sec-ch-ua-mobile", true) }
            if (mobileKey != null) sanitized[mobileKey] = "?0" else sanitized["Sec-CH-UA-Mobile"] =
                "?0"
        } else {
            val mobileKey = sanitized.keys.firstOrNull { it.equals("sec-ch-ua-mobile", true) }
            if (mobileKey != null) sanitized[mobileKey] = "?1" else sanitized["Sec-CH-UA-Mobile"] =
                "?1"
        }

        return sanitized
    }


    private fun platformFromUA(ua: String): String {
        val u = ua.lowercase(Locale.US)
        return when {
            "android" in u -> "Android"
            "windows nt" in u -> "Windows"
            "mac os x" in u || "macintosh" in u -> "macOS"
            "cros " in u -> "Chrome OS"
            "x11; linux" in u || "linux" in u -> "Linux"
            "iphone" in u || "ipad" in u || "ios" in u -> "iOS"
            else -> "Linux"
        }
    }

    private fun buildWebViewResponseHeaders(
        src: Map<String, List<String>>,
        dropContentEncoding: Boolean
    ): LinkedHashMap<String, String> {
        val out = linkedMapOf<String, String>()
        src.forEach { (k, vlist) ->
            if (k.isBlank()) return@forEach
            val low = k.lowercase(Locale.US)
            if (isHopByHop(k)) return@forEach
            if (k.equals("Set-Cookie", true)) return@forEach
            if (dropContentEncoding && (low == "content-encoding" || low == "content-length" || low == "transfer-encoding" || low == "content-range")) return@forEach
            out[k] = vlist.joinToString(",")
        }
        return out
    }

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

    private fun isTextualMime(mime: String): Boolean {
        val m = mime.lowercase(Locale.US)
        return m.startsWith("text/") ||
                m == "text/javascript" ||
                m == "application/javascript" ||
                m == "application/json" ||
                m == "application/xml" ||
                m == "image/svg+xml" ||
                m == "application/xhtml+xml"
    }

    private fun pickContentTypeByUrl(url: String): String {
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
        val charset = when {
            mime.startsWith("text/") || mime == "text/javascript" || mime == "application/json" -> "UTF-8"
            else -> null
        }
        return if (charset != null) "$mime; charset=$charset" else mime
    }

    private fun splitMimeAndCharset(contentType: String): Pair<String, String?> {
        val parts = contentType.split(';').map { it.trim() }
        val mime = parts.firstOrNull()?.lowercase(Locale.US).orEmpty()
        val cs = parts.firstOrNull { it.startsWith("charset=", true) }?.substringAfter('=')
            ?.trim('"', ' ')
        return mime to cs
    }

    private fun safeReason(code: Int, raw: String?): String {
        val r = (raw ?: "").trim()
        if (r.isNotEmpty()) return r
        return when (code) {
            100 -> "Continue"
            101 -> "Switching Protocols"
            200 -> "OK"
            201 -> "Created"
            202 -> "Accepted"
            203 -> "Non-Authoritative Information"
            204 -> "No Content"
            205 -> "Reset Content"
            206 -> "Partial Content"
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
            429 -> "Too Many Requests"
            500 -> "Internal Server Error"
            502 -> "Bad Gateway"
            503 -> "Service Unavailable"
            else -> "OK"
        }
    }

    private fun firstHeader(map: Map<String, List<String>>, name: String): String? {
        val target = name.lowercase(Locale.US)
        map.forEach { (k, v) ->
            if (k.equals(name, true) || k.lowercase(Locale.US) == target) return v.firstOrNull()
        }
        return null
    }

    private fun maybeDecode(bytes: ByteArray?, encoding: String?): ByteArray? {
        if (bytes == null) return null
        return try {
            when (encoding) {
                "gzip" -> GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
                "deflate" -> InflaterInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
                "br" -> BrotliInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
                else -> tryDecodeHeuristics(bytes)
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun tryDecodeHeuristics(bytes: ByteArray): ByteArray? {
        return try {
            GZIPInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
        } catch (_: Throwable) {
            try {
                InflaterInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
            } catch (_: Throwable) {
                try {
                    BrotliInputStream(ByteArrayInputStream(bytes)).use { it.readBytes() }
                } catch (_: Throwable) {
                    null
                }
            }
        }
    }
}
