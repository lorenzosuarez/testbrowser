/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.network

import com.testlabs.browser.ui.browser.UAProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import org.chromium.net.CronetEngine
import org.chromium.net.UrlRequest
import org.chromium.net.UrlRequest.Builder
import org.chromium.net.UrlResponseInfo
import java.io.ByteArrayInputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * HTTP stack backed by a CronetEngine.
 *
 * Responsibilities:
 * - Performs HTTP requests with ALPN negotiation (HTTP/1.1, HTTP/2, QUIC when available).
 * - Streams response data via Cronet callbacks, buffering in-memory before exposure.
 * - Preserves header ordering using a LinkedHashMap.
 * - Normalizes request: filters hop-by-hop / forbidden headers and injects User-Agent / Accept-Language if absent.
 * - Supports upload bodies for non-GET/HEAD methods using Cronet UploadDataProvider.
 * - Integrates with Kotlin coroutines: cancellation propagates to the underlying Cronet request.
 *
 * Concurrency model:
 * - A dedicated single-thread executor is created per request for Cronet callbacks.
 *
 * Error handling:
 * - Propagates CronetException and cancellation as coroutine failures.
 *
 * Memory note:
 * - Aggregates received ByteBuffer fragments into a single byte array before creating the InputStream.
 */
public class CronetHttpStack(
    private val engine: CronetEngine,
    private val uaProvider: UAProvider,
    private val chManager: UserAgentClientHintsManager
) : HttpStack {

    override val name: String = "cronet"

    override suspend fun execute(request: ProxyRequest): ProxyResponse =
        suspendCancellableCoroutine { cont ->
            val executor = Executors.newSingleThreadExecutor()
            val headersOut = linkedMapOf<String, MutableList<String>>()
            val bufferList = ArrayList<ByteArray>()
            val callback = object : UrlRequest.Callback() {
                override fun onRedirectReceived(req: UrlRequest, info: UrlResponseInfo, newLocationUrl: String) {
                    req.followRedirect()
                }
                override fun onResponseStarted(req: UrlRequest, info: UrlResponseInfo) {
                    info.allHeaders.forEach { (k, v) ->
                        headersOut.getOrPut(k) { mutableListOf() }.addAll(v)
                    }
                    req.read(ByteBuffer.allocateDirect(32 * 1024))
                }
                override fun onReadCompleted(req: UrlRequest, info: UrlResponseInfo, byteBuffer: ByteBuffer) {
                    byteBuffer.flip()
                    val bytes = ByteArray(byteBuffer.remaining())
                    byteBuffer.get(bytes)
                    if (bytes.isNotEmpty()) bufferList.add(bytes)
                    byteBuffer.clear()
                    req.read(byteBuffer)
                }
                override fun onSucceeded(req: UrlRequest, info: UrlResponseInfo) {
                    val body = if (bufferList.isEmpty()) ByteArray(0) else bufferList.reduce { a, b -> a + b }
                    cont.resume(
                        ProxyResponse(
                            statusCode = info.httpStatusCode,
                            reasonPhrase = info.httpStatusText ?: "",
                            headers = headersOut,
                            body = ByteArrayInputStream(body)
                        )
                    )
                    executor.shutdown()
                }
                override fun onFailed(req: UrlRequest, info: UrlResponseInfo?, error: org.chromium.net.CronetException) {
                    cont.resumeWithException(error)
                    executor.shutdown()
                }
                override fun onCanceled(req: UrlRequest, info: UrlResponseInfo?) {
                    cont.resumeWithException(IllegalStateException("canceled"))
                    executor.shutdown()
                }
            }
            val builder: Builder = engine.newUrlRequestBuilder(request.url, callback, executor)
            builder.setHttpMethod(request.method)
            builder.disableCache()
            val forbidden = setOf(
                "host", "connection", "proxy-connection",
                "transfer-encoding", "content-length",
                "te", "trailer", "upgrade",
                "accept-encoding"
            )

            val headers = request.headers.toMutableMap().apply {
                keys.filter { it.equals("x-requested-with", true) }.forEach { remove(it) }
                listOf("sec-ch-ua", "sec-ch-ua-mobile", "sec-ch-ua-platform").forEach { h ->
                    keys.firstOrNull { it.equals(h, true) }?.let { remove(it) }
                }
            }
            val ua = uaProvider.userAgent(false)
            headers["User-Agent"] = ua
            val acceptLang = headers["Accept-Language"] ?: "en-US,en;q=0.9"
            headers["Accept-Language"] = acceptLang

            headers.forEach { (k, v) ->
                val lk = k.lowercase()
                if (lk !in forbidden) {
                    builder.addHeader(k, v)
                }
            }

            // Canonical UA-CH
            chManager.asMap(isMobile = true).forEach { (k, v) -> builder.addHeader(k, v) }
            val hasBody = request.body != null && request.method.uppercase() !in setOf("GET", "HEAD")
            if (hasBody) {
                val provider = org.chromium.net.UploadDataProviders.create(request.body)
                val contentType = request.headers.entries
                    .firstOrNull { it.key.equals("Content-Type", true) }
                    ?.value ?: "application/octet-stream"
                builder.setUploadDataProvider(provider, executor)
                builder.addHeader("Content-Type", contentType)
            }
            val urlRequest = builder.build()
            cont.invokeOnCancellation {
                try { urlRequest.cancel() } catch (_: Throwable) {}
                executor.shutdown()
            }

            urlRequest.start()
        }
}