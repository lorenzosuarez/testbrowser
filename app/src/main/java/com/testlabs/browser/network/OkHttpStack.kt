package com.testlabs.browser.network

import java.io.IOException
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

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

public class OkHttpStack : HttpStack {
    override val name: String = "OkHttp"
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .followRedirects(false)
        .followSslRedirects(false)
        .build()
    override suspend fun execute(request: ProxyRequest): ProxyResponse {
        var current = buildRequest(request)
        var count = 0
        while (true) {
            val response = client.newCall(current).execute()
            when (response.code) {
                301, 302, 303, 307, 308 -> {
                    val location = response.header("Location") ?: throw IOException("Redirect without Location")
                    response.close()
                    if (++count > 10) throw IOException("Too many redirects")
                    val url = current.url.resolve(location) ?: throw IOException("Bad redirect $location")
                    current = current.newBuilder().url(url).build()
                }
                else -> {
                    val headers = mutableMapOf<String, String>()
                    response.headers.forEach { name, value ->
                        if (name.lowercase() !in hopHeaders) headers[name] = value
                    }
                    val reason = response.message.ifBlank { "OK" }
                    return ProxyResponse(response.code, reason, headers, response.body.byteStream())
                }
            }
        }
    }
    private fun buildRequest(req: ProxyRequest): Request {
        val builder = Request.Builder().url(req.url).method(req.method, req.body?.toRequestBody())
        req.headers.forEach { (k, v) -> builder.header(k, v) }
        return builder.build()
    }
}
