package com.testlabs.browser.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.brotli.BrotliInterceptor
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val TIMEOUT_SECONDS = 30L
private const val REDIRECT_LIMIT = 10
private val METHODS_WITH_BODY = setOf("POST", "PUT", "PATCH", "DELETE")
private val SAFE_METHODS = setOf("GET", "HEAD")
private val HOP_HEADERS = setOf(
    "connection",
    "keep-alive",
    "proxy-authenticate",
    "proxy-authorization",
    "te",
    "trailers",
    "transfer-encoding",
    "upgrade",
)

public class OkHttpStack : HttpStack {
    override val name: String = "OkHttp"

    private val client = OkHttpClient.Builder()
        .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .addInterceptor(BrotliInterceptor)     // adds "br" and auto-decompresses
        // Note: Zstd support might not be available in current OkHttp version
        // .addInterceptor(ZstdInterceptor)    // would add "zstd" and auto-decompress
        .followRedirects(false)                // we handle redirects manually
        .followSslRedirects(false)
        .build()

    override suspend fun execute(request: ProxyRequest): ProxyResponse =
        withContext(Dispatchers.IO) { executeInternal(buildRequest(request)) }

    private fun executeInternal(initial: Request): ProxyResponse {
        var current = initial
        var redirects = 0
        while (true) {
            val response = client.newCall(current).execute()
            when (response.code) {
                301, 302, 303, 307, 308 -> {
                    val location = response.header("Location") ?: run {
                        response.close()
                        throw IOException("Redirect without Location")
                    }
                    if (++redirects > REDIRECT_LIMIT) {
                        response.close()
                        throw IOException("Too many redirects")
                    }
                    val target = current.url.resolve(location) ?: run {
                        response.close()
                        throw IOException("Bad redirect: $location")
                    }
                    val nextMethod = normalizeMethodForRedirect(response.code, current.method)
                    val nextBody = if (nextMethod in SAFE_METHODS) null else current.body

                    // Preserve sensitive headers across redirects
                    val nextBuilder = current.newBuilder()
                        .url(target)
                        .method(nextMethod, nextBody)

                    // Remove content headers if body is removed
                    if (nextBody == null) {
                        nextBuilder.removeHeader("Content-Length")
                        nextBuilder.removeHeader("Content-Type")
                    }

                    response.close()
                    current = nextBuilder.build()
                }
                else -> {
                    // Filter headers and remove compression-related ones since OkHttp decompresses
                    val filteredHeaders = response.headers.filterForWebView()
                    val reason: String = response.message.ifBlank { defaultReason(response.code) }
                    val stream: InputStream = ResponseBodyInputStream(response)
                    return ProxyResponse(response.code, reason, filteredHeaders, stream)
                }
            }
        }
    }

    private fun buildRequest(req: ProxyRequest): Request {
        val builder = Request.Builder().url(req.url)

        // Copy headers 1:1 from WebResourceRequest, preserving all important ones
        req.headers.forEach { (name, value) ->
            val lowerName = name.lowercase(Locale.US)
            when {
                // Don't manually set Accept-Encoding - let OkHttp + interceptors handle it
                lowerName == "accept-encoding" -> { /* Skip - OkHttp will add gzip/br */ }
                // Never add X-Requested-With
                lowerName == "x-requested-with" -> { /* Skip completely */ }
                else -> builder.header(name, value)
            }
        }

        val method = req.method.uppercase(Locale.US)

        // Handle request body properly - POST/PUT/PATCH can have empty bodies
        val body = when {
            method in SAFE_METHODS -> null // GET, HEAD never have bodies
            method in METHODS_WITH_BODY -> {
                // For POST/PUT/PATCH, use the provided body or create an empty one
                if (req.body != null && req.body.isNotEmpty()) {
                    req.body.toRequestBody()
                } else {
                    // Empty body for POST requests that don't have content
                    ByteArray(0).toRequestBody()
                }
            }
            else -> null // Other methods (OPTIONS, TRACE, etc.)
        }

        return builder.method(method, body).build()
    }
}

private fun normalizeMethodForRedirect(code: Int, current: String): String =
    when {
        code == 303 -> "GET"  // 303 always changes to GET
        code in listOf(301, 302) && current.uppercase(Locale.US) !in SAFE_METHODS -> "GET"
        else -> current  // 307, 308 preserve method and body
    }

private fun Headers.filterForWebView(): Map<String, String> {
    return toMultimap()
        .asSequence()
        .filter { (name, _) ->
            val lowerName = name.lowercase(Locale.US)
            // Remove hop-by-hop headers and compression headers (content is already decompressed)
            lowerName !in HOP_HEADERS &&
            lowerName !in setOf("content-encoding", "content-length")
        }
        .associate { it.key to it.value.joinToString(", ") }
}

private class ResponseBodyInputStream(private val response: Response) :
    FilterInputStream(response.body.byteStream()) {
    override fun close() {
        try { super.close() } finally { response.close() }
    }
}

private fun defaultReason(code: Int): String = when (code) {
    100 -> "Continue"
    101 -> "Switching Protocols"
    102 -> "Processing"
    200 -> "OK"
    201 -> "Created"
    202 -> "Accepted"
    203 -> "Non-Authoritative Information"
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
    402 -> "Payment Required"
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
    425 -> "Too Early"
    426 -> "Upgrade Required"
    429 -> "Too Many Requests"
    500 -> "Internal Server Error"
    501 -> "Not Implemented"
    502 -> "Bad Gateway"
    503 -> "Service Unavailable"
    504 -> "Gateway Timeout"
    else -> "OK"
}
