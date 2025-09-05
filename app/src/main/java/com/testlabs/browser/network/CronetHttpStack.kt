package com.testlabs.browser.network

import android.util.Log
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.Locale
import java.util.concurrent.Executors
import kotlinx.coroutines.suspendCancellableCoroutine
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

private const val TAG = "CronetHttpStack"
private const val REDIRECT_LIMIT = 10
private val SAFE_METHODS = setOf("GET", "HEAD", "OPTIONS", "TRACE")
private val HOP_BY_HOP_HEADERS = setOf(
    "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
    "te", "trailers", "transfer-encoding", "upgrade"
)

public class CronetHttpStack(
    private val engine: CronetEngine,
) : HttpStack {
    override val name: String = "Cronet"
    private val executor = Executors.newCachedThreadPool()

    override suspend fun execute(request: ProxyRequest): ProxyResponse = suspendCancellableCoroutine { cont ->
        // Use ByteArrayOutputStream to collect all data first, then create InputStream
        val responseData = ByteArrayOutputStream()
        var urlRequest: UrlRequest?

        Log.d(TAG, "Executing Cronet request: ${request.method} ${request.url}")

        val callback = object : UrlRequest.Callback() {
            var status = 0
            var reason = ""
            var headers: Map<String, String> = emptyMap()
            var redirectCount = 0
            var currentMethod = request.method

            override fun onRedirectReceived(req: UrlRequest, info: UrlResponseInfo, newLocationUrl: String) {
                if (++redirectCount > REDIRECT_LIMIT) {
                    Log.w(TAG, "Too many redirects ($redirectCount) for ${request.url}")
                    req.cancel()
                    cont.resumeWithException(IOException("Too many redirects"))
                    return
                }

                // Handle method changes for redirects according to HTTP specs
                val statusCode = info.httpStatusCode
                currentMethod = when {
                    statusCode == 303 -> "GET" // Always change to GET for 303
                    statusCode in listOf(301, 302) && currentMethod.uppercase(Locale.US) !in SAFE_METHODS -> "GET"
                    else -> currentMethod // Keep original method for 307, 308 and safe methods
                }

                Log.d(TAG, "Following redirect $redirectCount: $statusCode -> $newLocationUrl (method: $currentMethod)")
                req.followRedirect()
            }

            override fun onResponseStarted(req: UrlRequest, info: UrlResponseInfo) {
                status = info.httpStatusCode
                reason = info.httpStatusText ?: getDefaultStatusText(status)

                // Filter headers properly - remove hop-by-hop and compression headers
                // since Cronet automatically decompresses content
                headers = info.allHeaders
                    .filterKeys { key ->
                        val lowerKey = key.lowercase(Locale.US)
                        lowerKey !in HOP_BY_HOP_HEADERS &&
                        lowerKey !in setOf("content-encoding", "content-length")
                    }
                    .mapValues { it.value.joinToString(", ") }

                Log.d(TAG, "Response started: $status $reason")
                Log.d(TAG, "Content-Type: ${headers["content-type"] ?: "not specified"}")
                Log.d(TAG, "Filtered headers count: ${headers.size}")

                req.read(ByteBuffer.allocateDirect(32 * 1024))
            }

            override fun onReadCompleted(req: UrlRequest, info: UrlResponseInfo, buffer: ByteBuffer) {
                buffer.flip()
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)

                Log.d(TAG, "Read ${bytes.size} bytes from response")

                try {
                    responseData.write(bytes)
                } catch (e: IOException) {
                    Log.w(TAG, "Error writing response data", e)
                    req.cancel()
                    return
                }

                buffer.clear()
                req.read(buffer)
            }

            override fun onSucceeded(req: UrlRequest, info: UrlResponseInfo) {
                val totalBytes = responseData.size()
                Log.d(TAG, "Request completed successfully: ${request.url}")
                Log.d(TAG, "Final response details:")
                Log.d(TAG, "  - Status: $status $reason")
                Log.d(TAG, "  - HTTP version: ${info.httpStatusText}")
                Log.d(TAG, "  - Total bytes downloaded: $totalBytes")
                Log.d(TAG, "  - All headers: ${info.allHeaders}")

                // Create ByteArrayInputStream from collected data
                val inputStream = ByteArrayInputStream(responseData.toByteArray())

                // Log first few bytes for debugging (HTML content)
                if (totalBytes > 0) {
                    val preview = responseData.toByteArray().take(200)
                    val previewText = String(preview.toByteArray(), Charsets.UTF_8)
                    Log.d(TAG, "Response preview (first 200 chars): $previewText")
                }

                cont.resume(ProxyResponse(status, reason, headers, inputStream))
            }

            override fun onFailed(req: UrlRequest, info: UrlResponseInfo?, error: CronetException) {
                Log.e(TAG, "Request failed: ${request.url}", error)
                cont.resumeWithException(IOException("Cronet error: ${error.message}"))
            }

            override fun onCanceled(req: UrlRequest, info: UrlResponseInfo?) {
                Log.d(TAG, "Request canceled: ${request.url}")
                cont.resumeWithException(IOException("Request canceled"))
            }
        }

        try {
            val builder = engine.newUrlRequestBuilder(request.url, callback, executor)
                .setHttpMethod(request.method)
                .setPriority(UrlRequest.Builder.REQUEST_PRIORITY_MEDIUM)

            // Add all headers from the request, ensuring no X-Requested-With is added
            request.headers.forEach { (key, value) ->
                if (key.lowercase(Locale.US) != "x-requested-with") {
                    builder.addHeader(key, value)
                }
            }

            // Add request body if present
            request.body?.let { body ->
                builder.setUploadDataProvider(
                    object : org.chromium.net.UploadDataProvider() {
                        private var position = 0

                        override fun getLength(): Long = body.size.toLong()

                        override fun read(uploadDataSink: org.chromium.net.UploadDataSink, byteBuffer: ByteBuffer) {
                            val remaining = body.size - position
                            if (remaining <= 0) {
                                uploadDataSink.onReadSucceeded(true) // EOF
                                return
                            }

                            val toRead = minOf(remaining, byteBuffer.remaining())
                            byteBuffer.put(body, position, toRead)
                            position += toRead
                            uploadDataSink.onReadSucceeded(false)
                        }

                        override fun rewind(uploadDataSink: org.chromium.net.UploadDataSink) {
                            position = 0
                            uploadDataSink.onRewindSucceeded()
                        }
                    },
                    executor
                )
            }

            urlRequest = builder.build()
            urlRequest?.start()

            cont.invokeOnCancellation {
                urlRequest?.cancel()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error creating Cronet request for ${request.url}", e)
            cont.resumeWithException(e)
        }
    }

    private fun checkForBotChallenge(url: String, contentType: String, statusCode: Int, body: ByteArrayInputStream) {
        try {
            if ((url.endsWith(".js") || url.endsWith(".mjs") || url.contains("type=module")) &&
                contentType.startsWith("text/html") && statusCode == 200) {

                Log.w(TAG, "BOT_CHALLENGE_SUSPECT detected for $url - Content-Type: $contentType, Status: $statusCode")

                // Read first 1KB for debugging without consuming the stream
                val available = body.available()
                if (available > 0) {
                    val debugSize = minOf(1024, available)
                    Log.w(TAG, "First $debugSize bytes available for debugging $url")
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error checking bot challenge for $url", e)
        }
    }
}

private fun getDefaultStatusText(statusCode: Int): String = when (statusCode) {
    100 -> "Continue"
    101 -> "Switching Protocols"
    200 -> "OK"
    201 -> "Created"
    202 -> "Accepted"
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
    429 -> "Too Many Requests"
    500 -> "Internal Server Error"
    501 -> "Not Implemented"
    502 -> "Bad Gateway"
    503 -> "Service Unavailable"
    504 -> "Gateway Timeout"
    else -> "Unknown"
}
