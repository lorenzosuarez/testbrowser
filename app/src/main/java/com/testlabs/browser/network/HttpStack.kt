package com.testlabs.browser.network

import java.io.InputStream

public data class ProxyRequest(
    val url: String,
    val method: String,
    val headers: Map<String, String>,
    val body: ByteArray? = null,
)

public data class ProxyResponse(
    val statusCode: Int,
    val reasonPhrase: String,
    val headers: Map<String, String>,
    val body: InputStream,
)

public interface HttpStack {
    public val name: String
    public suspend fun execute(request: ProxyRequest): ProxyResponse
}
