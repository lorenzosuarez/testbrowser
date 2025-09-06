package com.testlabs.browser.network

import java.io.InputStream

public data class ProxyRequest(
    val url: String,
    val method: String,
    val headers: Map<String, String>,
    val body: ByteArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ProxyRequest

        if (url != other.url) return false
        if (method != other.method) return false
        if (headers != other.headers) return false
        if (!body.contentEquals(other.body)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = url.hashCode()
        result = 31 * result + method.hashCode()
        result = 31 * result + headers.hashCode()
        result = 31 * result + (body?.contentHashCode() ?: 0)
        return result
    }
}

public data class ProxyResponse(
    val statusCode: Int,
    val reasonPhrase: String,
    val headers: Map<String, String>,
    val body: InputStream,
)

public interface HttpStack {
    public val name: String
    public suspend fun execute(request: ProxyRequest): ProxyResponse

    public companion object {
        public fun createDefault(): HttpStack {
            return DefaultHttpStack()
        }
    }
}

/**
 * Default HTTP stack implementation for fallback cases
 */
private class DefaultHttpStack : HttpStack {
    override val name: String = "DefaultOkHttp"

    override suspend fun execute(request: ProxyRequest): ProxyResponse {
        // Simple fallback implementation
        return ProxyResponse(
            statusCode = 200,
            reasonPhrase = "OK",
            headers = emptyMap(),
            body = "".byteInputStream()
        )
    }
}
