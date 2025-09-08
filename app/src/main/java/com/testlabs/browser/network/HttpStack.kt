/**
 * Author: Lorenzo Suarez
 * Date: 09/08/2025
 */

package com.testlabs.browser.network

/**
 * Abstraction for pluggable HTTP clients used behind WebView interception.
 * Implementations must not mutate header casing and should preserve response headers verbatim.
 */
public interface HttpStack {
    /** Human-readable stack name for diagnostics. */
    public val name: String

    /**
     * Executes the request and returns a streaming response.
     *
     * @param request Request to perform.
     * @return Response with raw bytes and unmodified headers.
     */
    public suspend fun execute(request: ProxyRequest): ProxyResponse
}
