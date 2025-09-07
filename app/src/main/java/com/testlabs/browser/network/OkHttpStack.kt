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
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.InputStream

/**
 * OkHttp-based HttpStack that provides header control and Accept-Encoding parity.
 * Transparent decompression is disabled by explicitly sending Accept-Encoding.
 */

public class OkHttpStack(
    private val uaProvider: UAProvider,
    private val chManager: UserAgentClientHintsManager,
    private val client: OkHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .build()
) : HttpStack {

    override val name: String = "okhttp"

    override suspend fun execute(request: ProxyRequest): ProxyResponse = withContext(Dispatchers.IO) {
        val builder = Request.Builder().url(request.url)
        when (request.method.uppercase()) {
            "GET", "HEAD" -> builder.method(request.method, null)
            else -> {
                val bytes = request.body ?: ByteArray(0)
                val mt = (request.headers["Content-Type"] ?: "application/octet-stream").toMediaTypeOrNull()
                val body: RequestBody = bytes.toRequestBody(mt)
                builder.method(request.method, body)
            }
        }
        request.headers.forEach { (k, v) -> if (k.isNotBlank()) builder.header(k, v) }
        val call = client.newCall(builder.build())
        val response = try { call.execute() } catch (e: IOException) { throw e }
        val status = response.code
        val reason = response.message.ifBlank { defaultReason(status) }
        val headers = response.headers.toMultimap()
        val src: InputStream = response.body.byteStream()
        val wrapped = object : InputStream() {
            override fun read(): Int = src.read()
            override fun read(b: ByteArray, off: Int, len: Int): Int = src.read(b, off, len)
            override fun available(): Int = try { src.available() } catch (_: Throwable) { 0 }
            override fun close() {
                try { src.close() } catch (_: Throwable) {}
                try { response.close() } catch (_: Throwable) {}
            }
        }
        ProxyResponse(
            statusCode = status,
            reasonPhrase = reason,
            headers = headers,
            body = wrapped
        )
    }

    private fun defaultReason(code: Int): String = when (code) {
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
