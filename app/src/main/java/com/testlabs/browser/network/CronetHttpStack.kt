/**
 * Author: Lorenzo Suarez
 * Date: 06/09/2025
 */

package com.testlabs.browser.network

import com.testlabs.browser.ui.browser.UAProvider
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UploadDataProvider
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import org.chromium.net.apihelpers.UploadDataProviders
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors

public class CronetHttpStack(
    private val engine: CronetEngine,
    private val uaProvider: UAProvider,
    private val chManager: UserAgentClientHintsManager
) : HttpStack {

    override val name: String = "cronet"

    private val executor = Executors.newSingleThreadExecutor()

    override suspend fun execute(request: ProxyRequest): ProxyResponse = withContext(Dispatchers.IO) {
        val result = CompletableDeferred<ProxyResponse>()
        val out = ByteArrayOutputStream()
        val buffer = ByteBuffer.allocateDirect(64 * 1024)

        val callback = object : UrlRequest.Callback() {
            private var statusCode: Int = 0
            private var headers: Map<String, List<String>> = emptyMap()

            override fun onRedirectReceived(request: UrlRequest, info: UrlResponseInfo, newLocationUrl: String) {
                request.followRedirect()
            }

            override fun onResponseStarted(request: UrlRequest, info: UrlResponseInfo) {
                statusCode = info.httpStatusCode
                headers = info.allHeaders
                buffer.clear()
                request.read(buffer)
            }

            override fun onReadCompleted(request: UrlRequest, info: UrlResponseInfo, byteBuffer: ByteBuffer) {
                byteBuffer.flip()
                if (byteBuffer.hasRemaining()) {
                    val bytes = ByteArray(byteBuffer.remaining())
                    byteBuffer.get(bytes)
                    out.write(bytes)
                }
                byteBuffer.clear()
                request.read(byteBuffer)
            }

            override fun onSucceeded(request: UrlRequest, info: UrlResponseInfo) {
                val body = out.toByteArray()
                result.complete(
                    ProxyResponse(
                        statusCode = statusCode,
                        reasonPhrase = defaultReason(statusCode),
                        headers = headers,
                        body = ByteArrayInputStream(body)
                    )
                )
            }

            override fun onFailed(request: UrlRequest, info: UrlResponseInfo?, error: CronetException) {
                result.completeExceptionally(error)
            }

            override fun onCanceled(request: UrlRequest, info: UrlResponseInfo?) {
                result.completeExceptionally(IllegalStateException("canceled"))
            }
        }

        val builder = engine.newUrlRequestBuilder(request.url, callback, executor)
        builder.setHttpMethod(request.method)
        request.headers.forEach { (k, v) -> if (k.isNotBlank()) builder.addHeader(k, v) }
        if (request.body != null && request.method.uppercase() !in setOf("GET", "HEAD")) {
            val provider: UploadDataProvider = UploadDataProviders.create(request.body)
            val contentType = request.headers["Content-Type"] ?: "application/octet-stream"
            builder.setUploadDataProvider(provider, executor)
            builder.addHeader("Content-Type", contentType)
        }
        builder.build().start()
        result.await()
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
