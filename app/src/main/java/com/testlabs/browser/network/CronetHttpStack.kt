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
 * Cronet-based HttpStack for Chrome-like JA3/ALPN behavior including HTTP/2 and QUIC when available.
 */
public class CronetHttpStack(
    private val engine: CronetEngine,
    private val uaProvider: UAProvider
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
                }
                override fun onFailed(req: UrlRequest, info: UrlResponseInfo?, error: org.chromium.net.CronetException) {
                    cont.resumeWithException(error)
                }
                override fun onCanceled(req: UrlRequest, info: UrlResponseInfo?) {
                    cont.resumeWithException(IllegalStateException("canceled"))
                }
            }

            val builder: Builder = engine.newUrlRequestBuilder(request.url, callback, executor)
            val hasBody = request.body != null && request.method.uppercase() !in setOf("GET", "HEAD")
            builder.setHttpMethod(request.method)
            request.headers.forEach { (k, v) -> builder.addHeader(k, v) }
            if (hasBody) {
                val provider = org.chromium.net.UploadDataProviders.create(request.body!!)
                val contentType = request.headers.entries.firstOrNull { it.key.equals("Content-Type", true) }?.value ?: "application/octet-stream"
                builder.setUploadDataProvider(provider, executor)
                builder.addHeader("Content-Type", contentType)
            }
            builder.build().start()
        }
}
