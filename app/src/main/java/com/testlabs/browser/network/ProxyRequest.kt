/**
 * Author: Lorenzo Suarez
 * Date: 09/06/2025
 */
package com.testlabs.browser.network

/**
 * Immutable request description used by HttpStack implementations.
 *
 * @property url Absolute request URL.
 * @property method HTTP method in uppercase.
 * @property headers Request headers. The map must contain unique header names; repeated headers should be flattened by the caller if needed.
 * @property body Optional request body bytes. Null means no body.
 */
public data class ProxyRequest(
    val url: String,
    val method: String,
    val headers: Map<String, String>,
    val body: ByteArray? = null
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
