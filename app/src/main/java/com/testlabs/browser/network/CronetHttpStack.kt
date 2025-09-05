package com.testlabs.browser.network

import java.io.IOException
import java.io.PipedInputStream
import java.io.PipedOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import kotlinx.coroutines.suspendCancellableCoroutine
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

public class CronetHttpStack(
    private val engine: CronetEngine,
) : HttpStack {
    override val name: String = "Cronet"
    private val executor = Executors.newCachedThreadPool()
    override suspend fun execute(request: ProxyRequest): ProxyResponse = suspendCancellableCoroutine { cont ->
        val input = PipedInputStream()
        val output = PipedOutputStream(input)
        val callback = object : UrlRequest.Callback() {
            var status = 0
            var reason = ""
            var headers: Map<String, String> = emptyMap()
            var redirects = 0
            override fun onRedirectReceived(req: UrlRequest, info: UrlResponseInfo, newLocationUrl: String) {
                if (redirects++ >= 10) {
                    req.cancel()
                    output.close()
                    cont.resumeWithException(IOException("Too many redirects"))
                } else req.followRedirect()
            }
            override fun onResponseStarted(req: UrlRequest, info: UrlResponseInfo) {
                status = info.httpStatusCode
                reason = info.httpStatusText ?: ""
                headers = info.allHeaders
                req.read(ByteBuffer.allocateDirect(32 * 1024))
            }
            override fun onReadCompleted(req: UrlRequest, info: UrlResponseInfo, buffer: ByteBuffer) {
                buffer.flip()
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                output.write(bytes)
                buffer.clear()
                req.read(buffer)
            }
            override fun onSucceeded(req: UrlRequest, info: UrlResponseInfo) {
                output.close()
                cont.resume(ProxyResponse(status, reason, headers, input))
            }
            override fun onFailed(req: UrlRequest, info: UrlResponseInfo?, error: CronetException) {
                output.close()
                cont.resumeWithException(IOException(error.message ?: "Cronet error"))
            }
            override fun onCanceled(req: UrlRequest, info: UrlResponseInfo?) {
                output.close()
                cont.resumeWithException(IOException("Request canceled"))
            }
        }
        val builder = engine.newUrlRequestBuilder(request.url, callback, executor).setHttpMethod(request.method)
        request.headers.forEach { (k, v) -> builder.addHeader(k, v) }
        val urlRequest = builder.build()
        urlRequest.start()
    }
}
