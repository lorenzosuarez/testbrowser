package com.testlabs.browser.network

import java.io.InputStream

/**
 * Immutable response container returned by HttpStack implementations.
 *
 * @property statusCode HTTP status code.
 * @property reasonPhrase HTTP reason phrase. May be blank.
 * @property headers Response headers with full multiplicity preserved. Each key maps to one or more values in order.
 * @property body Stream of raw response bytes as returned by the network stack.
 */
public data class ProxyResponse(
    val statusCode: Int,
    val reasonPhrase: String,
    val headers: Map<String, List<String>>,
    val body: InputStream
)
