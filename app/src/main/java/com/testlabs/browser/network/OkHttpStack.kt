/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.network

import com.testlabs.browser.ui.browser.UAProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * OkHttp-based HttpStack that provides header control and Accept-Encoding parity.
 * Transparent decompression is disabled by explicitly sending Accept-Encoding.
 */
public class OkHttpStack(
    private val uaProvider: UAProvider,
    private val chManager: UserAgentClientHintsManager,
    private val client: OkHttpClient = OkHttpClientProvider.client(chManager)
) : HttpStack {

    override val name: String = "okhttp"

    override suspend fun execute(request: ProxyRequest): ProxyResponse = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(request.url)
        val headers = request.headers.toMutableMap()
        headers.keys
            .filter { it.equals("x-requested-with", true) }
            .forEach { headers.remove(it) }

        val ua = uaProvider.userAgent(false)
        headers["User-Agent"] = ua
        val acceptLang = headers["Accept-Language"] ?: "en-US,en;q=0.9"
        headers["Accept-Language"] = acceptLang
        val hints = chManager.asMap(isMobile = true)
        headers["Sec-CH-UA"] = hints["sec-ch-ua"]!!
        headers["Sec-CH-UA-Mobile"] = hints["sec-ch-ua-mobile"]!!
        headers["Sec-CH-UA-Platform"] = hints["sec-ch-ua-platform"]!!

        headers.forEach { (k, v) -> builder.header(k, v) }

        val method = request.method.uppercase()
        val bodyBytes = request.body
        val body = when {
            method == "GET" || method == "HEAD" || bodyBytes == null -> null
            else -> {
                val ct = headers.entries.firstOrNull { it.key.equals("Content-Type", true) }?.value
                if (ct != null) bodyBytes.toRequestBody(ct.toMediaTypeOrNull())
                else bodyBytes.toRequestBody(null)
            }
        }

        val finalBody = when {
            body != null -> body
            requiresRequestBody(method) -> ByteArray(0).toRequestBody(null)
            else -> null
        }
        builder.method(method, finalBody)

        val call = client.newCall(builder.build())
        val resp = call.execute()

        val headerMap: Map<String, List<String>> = resp.headers.toMultimap()
        val stream = resp.body.byteStream()

        ProxyResponse(
            statusCode = resp.code,
            reasonPhrase = resp.message,
            headers = headerMap,
            body = stream
        )
    }
}

private fun requiresRequestBody(method: String): Boolean {
    return when (method.uppercase()) {
        "POST", "PUT", "PATCH", "PROPPATCH", "REPORT" -> true
        else -> false
    }
}
