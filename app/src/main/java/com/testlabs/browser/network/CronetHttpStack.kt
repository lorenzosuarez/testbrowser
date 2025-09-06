package com.testlabs.browser.network

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.chromium.net.CronetEngine
import org.chromium.net.CronetException
import org.chromium.net.UrlRequest
import org.chromium.net.UrlResponseInfo
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.Executor
import java.util.concurrent.Executors
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

private const val TAG = "CronetHttpStack"
private const val REDIRECT_LIMIT = 10
private val SAFE_METHODS = setOf("GET", "HEAD", "OPTIONS", "TRACE")
private val HOP_BY_HOP_HEADERS = setOf(
    "connection", "keep-alive", "proxy-authenticate", "proxy-authorization",
    "te", "trailers", "transfer-encoding", "upgrade"
)

/**
 * F. TLS/ALPN strategy - Cronet mode for closer TLS/JA3/JA4 to Chrome
 * Enable HTTP/2 and QUIC; Brotli on. Note Cronet may not expose zstd.
 */
public class CronetHttpStack(
    private val context: Context,
    private val userAgentProvider: UserAgentProvider,
    private val uaChManager: UserAgentClientHintsManager,
    private val enableQuic: Boolean = false
) : HttpStack {
    override val name: String = "Cronet${if (enableQuic) "+QUIC" else ""}"

    private val executor: Executor = Executors.newCachedThreadPool()

    private val cronetEngine: CronetEngine by lazy {
        CronetEngine.Builder(context)
            .setUserAgent(userAgentProvider.getChromeStableMobileUA())
            .enableHttp2(true)
            .enableBrotli(true)
            .apply {
                if (enableQuic) {
                    enableQuic(true)
                    // Add QUIC hints for common domains
                    addQuicHint("www.google.com", 443, 443)
                    addQuicHint("accounts.google.com", 443, 443)
                    addQuicHint("apis.google.com", 443, 443)
                }
            }
            .build()
    }

    override suspend fun execute(request: ProxyRequest): ProxyResponse =
        withContext(Dispatchers.IO) {
            executeInternal(request)
        }

    private suspend fun executeInternal(request: ProxyRequest): ProxyResponse =
        suspendCoroutine { continuation ->
            val requestBuilder = cronetEngine.newUrlRequestBuilder(
                request.url,
                object : UrlRequest.Callback() {
                    private val responseData = ByteArrayOutputStream()
                    private var responseInfo: UrlResponseInfo? = null

                    override fun onRedirectReceived(
                        request: UrlRequest?,
                        info: UrlResponseInfo?,
                        newLocationUrl: String?
                    ) {
                        request?.followRedirect()
                    }

                    override fun onResponseStarted(request: UrlRequest?, info: UrlResponseInfo?) {
                        responseInfo = info
                        request?.read(ByteBuffer.allocateDirect(32 * 1024))
                    }

                    override fun onReadCompleted(
                        request: UrlRequest?,
                        info: UrlResponseInfo?,
                        byteBuffer: ByteBuffer?
                    ) {
                        byteBuffer?.flip()
                        if (byteBuffer?.hasRemaining() == true) {
                            val bytes = ByteArray(byteBuffer.remaining())
                            byteBuffer.get(bytes)
                            responseData.write(bytes)
                        }
                        byteBuffer?.clear()
                        request?.read(byteBuffer)
                    }

                    override fun onSucceeded(request: UrlRequest?, info: UrlResponseInfo?) {
                        val response = buildProxyResponse(info, responseData.toByteArray())
                        continuation.resume(response)
                    }

                    override fun onFailed(
                        request: UrlRequest,
                        info: UrlResponseInfo,
                        error: CronetException
                    ) {
                        continuation.resumeWithException(
                            error
                        )
                    }
                },
                executor
            )

            // Set HTTP method
            if (request.method.uppercase() != "GET") {
                requestBuilder.setHttpMethod(request.method)
            }

            // Add headers with proper UA-CH support
            val origin = request.url.substringBefore("/", request.url.substringAfter("://"))
            request.headers.forEach { (name, value) ->
                val lowerName = name.lowercase()
                when {
                    lowerName == "x-requested-with" -> { /* Skip */ }
                    lowerName == "user-agent" -> {
                        requestBuilder.addHeader("User-Agent", userAgentProvider.getChromeStableMobileUA())
                    }
                    lowerName.startsWith("sec-ch-ua") -> {
                        addClientHintHeader(requestBuilder, lowerName, origin)
                    }
                    lowerName == "accept" && value.contains("text/html") -> {
                        requestBuilder.addHeader(
                            "Accept",
                            "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7"
                        )
                    }
                    else -> requestBuilder.addHeader(name, value)
                }
            }

            // Add request body if present
            if (request.body != null && request.body.isNotEmpty()) {
                val uploadDataProvider = ByteArrayUploadDataProvider(request.body)
                requestBuilder.setUploadDataProvider(uploadDataProvider, executor)
            }

            requestBuilder.build().start()
        }

    /**
     * Adds UA-CH headers to the Cronet request builder. This method is thread-safe and
     * does not perform any blocking or suspending work; it only derives deterministic
     * header values from the current environment and providers.
     */
    private fun addClientHintHeader(
        requestBuilder: UrlRequest.Builder,
        hintName: String,
        origin: String
    ) {
        when (hintName) {
            "sec-ch-ua" -> {
                val brands = userAgentProvider.getChromeUserAgentDataBrands()
                val brandString = brands.joinToString(", ") { """"${it.first}";v="${it.second}"""" }
                requestBuilder.addHeader("Sec-CH-UA", brandString)
            }
            "sec-ch-ua-mobile" -> {
                requestBuilder.addHeader("Sec-CH-UA-Mobile", "?1")
            }
            "sec-ch-ua-platform" -> {
                requestBuilder.addHeader("Sec-CH-UA-Platform", "\"Android\"")
            }
            else -> {
                try {
                    when (hintName) {
                        "sec-ch-ua-arch" -> requestBuilder.addHeader("Sec-CH-UA-Arch", "\"arm\"")
                        "sec-ch-ua-bitness" -> requestBuilder.addHeader("Sec-CH-UA-Bitness", "\"64\"")
                        "sec-ch-ua-model" -> requestBuilder.addHeader("Sec-CH-UA-Model", "\"${android.os.Build.MODEL}\"")
                        "sec-ch-ua-platform-version" ->
                            requestBuilder.addHeader("Sec-CH-UA-Platform-Version", "\"${android.os.Build.VERSION.RELEASE}\"")
                        "sec-ch-ua-full-version-list" -> {
                            val brands = userAgentProvider.getChromeUserAgentDataBrands()
                            val fullVersionList = brands.joinToString(", ") {
                                """"${it.first}";v="${if (it.first.contains("Chrome")) userAgentProvider.getChromeStableFullVersion() else it.second}""""
                            }
                            requestBuilder.addHeader("Sec-CH-UA-Full-Version-List", fullVersionList)
                        }
                    }
                } catch (e: Exception) {
                    Log.w("CronetHttpStack", "Error setting UA-CH header $hintName", e)
                }
            }
        }
    }

    private fun buildProxyResponse(info: UrlResponseInfo?, data: ByteArray): ProxyResponse {
        val headers = mutableMapOf<String, String>()

        info?.allHeaders?.forEach { (name, values) ->
            val lowerName = name.lowercase()
            // Cronet already handles decompression, so remove encoding headers
            if (lowerName !in setOf("content-encoding", "content-length", "transfer-encoding")) {
                headers[name] = values.joinToString(", ")
            }
        }

        return ProxyResponse(
            statusCode = info?.httpStatusCode ?: 200,
            reasonPhrase = info?.httpStatusText ?: "OK",
            headers = headers,
            body = ByteArrayInputStream(data)
        )
    }

    private class ByteArrayUploadDataProvider(private val data: ByteArray) :
        org.chromium.net.UploadDataProvider() {

        override fun getLength(): Long = data.size.toLong()

        override fun read(uploadDataSink: org.chromium.net.UploadDataSink?, byteBuffer: ByteBuffer?) {
            byteBuffer?.put(data)
            uploadDataSink?.onReadSucceeded(false)
        }

        override fun rewind(uploadDataSink: org.chromium.net.UploadDataSink?) {
            uploadDataSink?.onRewindSucceeded()
        }
    }

    public fun shutdown() {
        cronetEngine.shutdown()
    }
}
